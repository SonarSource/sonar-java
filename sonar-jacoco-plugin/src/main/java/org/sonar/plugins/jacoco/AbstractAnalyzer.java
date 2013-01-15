/*
 * Sonar Java
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.jacoco;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import org.apache.commons.lang.StringUtils;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.runtime.WildcardMatcher;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.tests.ProjectTests;
import org.sonar.api.utils.SonarException;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;

/**
 * @author Evgeny Mandrikov
 */
public abstract class AbstractAnalyzer {

  public final void analyse(Project project, SensorContext context) {
    analyse(project, context, null);
  }

  public final void analyse(Project project, SensorContext context, ProjectTests projectTests) {
    final File buildOutputDir = project.getFileSystem().getBuildOutputDir();
    if (!buildOutputDir.exists()) {
      JaCoCoUtils.LOG.info("Project coverage is set to 0% as build output directory does not exist: {}", buildOutputDir);
      return;
    }
    String path = getReportPath(project);
    File jacocoExecutionData = project.getFileSystem().resolvePath(path);

    WildcardMatcher excludes = new WildcardMatcher(Strings.nullToEmpty(getExcludes(project)));
    try {
      readExecutionData(jacocoExecutionData, buildOutputDir, context, excludes, projectTests);
    } catch (IOException e) {
      throw new SonarException(e);
    }
  }

  public final void readExecutionData(File jacocoExecutionData, File buildOutputDir, SensorContext context, WildcardMatcher excludes,
                                      ProjectTests projectTests) throws IOException {
    final SessionInfoStore sessionInfoStore = new SessionInfoStore();
    final ExecutionDataStore executionDataStore = new ExecutionDataStore();
    final MultipleDataStoreSession multipleDataStoreSession = new MultipleDataStoreSession();

    if (jacocoExecutionData == null || !jacocoExecutionData.exists() || !jacocoExecutionData.isFile()) {
      JaCoCoUtils.LOG.info("Project coverage is set to 0% as no JaCoCo execution data has been dumped: {}", jacocoExecutionData);
    } else {
      JaCoCoUtils.LOG.info("Analysing {}", jacocoExecutionData);
      ExecutionDataReader reader = new ExecutionDataReader(new FileInputStream(jacocoExecutionData));
      reader.setSessionInfoVisitor(new ISessionInfoVisitor() {
        public void visitSessionInfo(final SessionInfo info) {
          sessionInfoStore.visitSessionInfo(info);
          multipleDataStoreSession.addSession(info);
        }
      });
      reader.setExecutionDataVisitor(new IExecutionDataVisitor() {
        public void visitClassExecution(final ExecutionData data) {
          executionDataStore.visitClassExecution(data);
          multipleDataStoreSession.visitClassExecution(data);
        }
      });
      reader.read();
    }

    CoverageBuilder coverageBuilder = analyze(executionDataStore, buildOutputDir);
    int analyzedResources = 0;
    for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
      JavaFile resource = getResource(coverage, context);
      if (resource != null) {
        if (!isExcluded(coverage, excludes)) {
          CoverageMeasuresBuilder builder = analyzeFile(resource, coverage, context);
          saveMeasures(context, resource, builder.createMeasures());
        }
        analyzedResources++;
      }
    }
    if (analyzedResources == 0) {
      JaCoCoUtils.LOG.warn("Coverage information was not collected. Perhaps you forget to include debug information into compiled classes?");
    } else {
      analyzeLinesCoverage(multipleDataStoreSession, buildOutputDir, context, excludes, projectTests);
    }
  }

  private void analyzeLinesCoverage(MultipleDataStoreSession multipleDataStoreSession, File buildOutputDir, SensorContext context, WildcardMatcher excludes,
                                    ProjectTests projectTests){
    for (Map.Entry<SessionInfo, ExecutionDataStore> entry : multipleDataStoreSession.getDataStoreBySession().entrySet()){
      SessionInfo sessionInfo = entry.getKey();
      String id = sessionInfo.getId();
      String test = Iterables.getLast(Splitter.on(".").split(id));
      String fileTest = StringUtils.removeEnd(id, "." + test);
      ExecutionDataStore executionDataStore = entry.getValue();
      CoverageBuilder coverageBuilder = analyze(executionDataStore, buildOutputDir);

      for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
        JavaFile resource = getResource(coverage, context);
        if (resource != null) {
          if (!isExcluded(coverage, excludes)) {
            CoverageMeasuresBuilder builder = analyzeFile(resource, coverage, context);
            projectTests.cover(fileTest, test, "", builder.getHitsByLine().keySet());
          }
        }
      }
    }
  }

  private CoverageBuilder analyze(ExecutionDataStore executionDataStore, File buildOutputDir){
    CoverageBuilder coverageBuilder = new CoverageBuilder();
    Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
    analyzeAll(analyzer, buildOutputDir);
    return coverageBuilder;
  }

  private static boolean isExcluded(ISourceFileCoverage coverage, WildcardMatcher excludesMatcher) {
    String name = coverage.getPackageName() + "/" + coverage.getName();
    return excludesMatcher.matches(name);
  }

  @VisibleForTesting
  static JavaFile getResource(ISourceFileCoverage coverage, SensorContext context) {
    String packageName = StringUtils.replaceChars(coverage.getPackageName(), '/', '.');
    String fileName = StringUtils.substringBeforeLast(coverage.getName(), ".");

    JavaFile resource = new JavaFile(packageName, fileName);

    JavaFile resourceInContext = context.getResource(resource);
    if (null == resourceInContext) {
      // Do not save measures on resource which doesn't exist in the context
      return null;
    }
    if (ResourceUtils.isUnitTestClass(resourceInContext)) {
      // Ignore unit tests
      return null;
    }

    return resourceInContext;
  }

  /**
   * Copied from {@link Analyzer#analyzeAll(File)} in order to add logging.
   */
  private void analyzeAll(Analyzer analyzer, File file) {
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        analyzeAll(analyzer, f);
      }
    } else if (file.getName().endsWith(".class")) {
      try {
        analyzer.analyzeAll(file);
      } catch (Exception e) {
        JaCoCoUtils.LOG.warn("Exception during analysis of file " + file.getAbsolutePath(), e);
      }
    }
  }

  private CoverageMeasuresBuilder analyzeFile(JavaFile resource, ISourceFileCoverage coverage, SensorContext context) {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    for (int lineId = coverage.getFirstLine(); lineId <= coverage.getLastLine(); lineId++) {
      final int hits;
      ILine line = coverage.getLine(lineId);
      switch (line.getInstructionCounter().getStatus()) {
        case ICounter.FULLY_COVERED:
        case ICounter.PARTLY_COVERED:
          hits = 1;
          break;
        case ICounter.NOT_COVERED:
          hits = 0;
          break;
        case ICounter.EMPTY:
          continue;
        default:
          JaCoCoUtils.LOG.warn("Unknown status for line {} in {}", lineId, resource);
          continue;
      }
      builder.setHits(lineId, hits);

      ICounter branchCounter = line.getBranchCounter();
      int conditions = branchCounter.getTotalCount();
      if (conditions > 0) {
        int coveredConditions = branchCounter.getCoveredCount();
        builder.setConditions(lineId, conditions, coveredConditions);
      }
    }
    return builder;
  }

  protected abstract void saveMeasures(SensorContext context, JavaFile resource, Collection<Measure> measures);

  protected abstract String getReportPath(Project project);

  protected abstract String getExcludes(Project project);

  class MultipleDataStoreSession {
    private Map<SessionInfo, ExecutionDataStore> dataStoreBySession;
    private SessionInfo lastSessionInfo;

    public MultipleDataStoreSession() {
      dataStoreBySession = newHashMap();
    }

    public void addSession(SessionInfo info){
      dataStoreBySession.put(info, new ExecutionDataStore());
      lastSessionInfo = info;
    }

    public void visitClassExecution(ExecutionData data){
        getDataStoreFromLastSession().visitClassExecution(data);
    }

    private ExecutionDataStore getDataStoreFromLastSession() {
      return dataStoreBySession.get(lastSessionInfo);
    }

    public Map<SessionInfo, ExecutionDataStore> getDataStoreBySession(){
      return dataStoreBySession;
    }
  }

}
