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
import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.runtime.WildcardMatcher;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.Testable;
import org.sonar.api.utils.SonarException;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @author Evgeny Mandrikov
 */
public abstract class AbstractAnalyzer {

  private final ResourcePerspectives perspectives;

  public AbstractAnalyzer(ResourcePerspectives perspectives) {
    this.perspectives = perspectives;
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

  public final void analyse(Project project, SensorContext context) {
    final File buildOutputDir = project.getFileSystem().getBuildOutputDir();
    if (!buildOutputDir.exists()) {
      JaCoCoUtils.LOG.info("Project coverage is set to 0% as build output directory does not exist: {}", buildOutputDir);
      return;
    }
    String path = getReportPath(project);
    File jacocoExecutionData = project.getFileSystem().resolvePath(path);

    WildcardMatcher excludes = new WildcardMatcher(Strings.nullToEmpty(getExcludes(project)));
    try {
      readExecutionData(jacocoExecutionData, buildOutputDir, context, excludes);
      readLinesCoveredByTestsData(jacocoExecutionData, buildOutputDir, context, excludes);
    } catch (IOException e) {
      throw new SonarException(e);
    }
  }

  public final void readExecutionData(File jacocoExecutionData, File buildOutputDir, SensorContext context, WildcardMatcher excludes) throws IOException {
    SessionInfoStore sessionInfoStore = new SessionInfoStore();
    ExecutionDataStore executionDataStore = new ExecutionDataStore();

    if (jacocoExecutionData == null || !jacocoExecutionData.exists() || !jacocoExecutionData.isFile()) {
      JaCoCoUtils.LOG.info("Project coverage is set to 0% as no JaCoCo execution data has been dumped: {}", jacocoExecutionData);
    } else {
      JaCoCoUtils.LOG.info("Analysing {}", jacocoExecutionData);
      ExecutionDataReader reader = new ExecutionDataReader(new FileInputStream(jacocoExecutionData));
      reader.setSessionInfoVisitor(sessionInfoStore);
      reader.setExecutionDataVisitor(executionDataStore);
      reader.read();
    }

    CoverageBuilder coverageBuilder = analyze(executionDataStore, buildOutputDir);
    int analyzedResources = 0;
    for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
      JavaFile resource = getResource(coverage, context);
      if (resource != null) {
        if (!isExcluded(coverage, excludes)) {
          CoverageMeasuresBuilder builder = analyzeFile(resource, coverage);
          saveMeasures(context, resource, builder.createMeasures());
        }
        analyzedResources++;
      }
    }
    if (analyzedResources == 0) {
      JaCoCoUtils.LOG.warn("Coverage information was not collected. Perhaps you forget to include debug information into compiled classes?");
    }
  }

  public final void readLinesCoveredByTestsData(File jacocoExecutionData, final File buildOutputDir, final SensorContext context, final WildcardMatcher excludes)
      throws IOException {
    if (jacocoExecutionData == null || !jacocoExecutionData.exists() || !jacocoExecutionData.isFile()) {
      JaCoCoUtils.LOG.info("No JaCoCo execution data for tests has been dumped: {}", jacocoExecutionData);
    } else {
      JaCoCoUtils.LOG.info("Analysing {}", jacocoExecutionData);
      final LastSessionInfo lastSessionInfo = new LastSessionInfo();
      ExecutionDataReader reader = new ExecutionDataReader(new FileInputStream(jacocoExecutionData));
      reader.setSessionInfoVisitor(new ISessionInfoVisitor() {
        public void visitSessionInfo(final SessionInfo info) {
          lastSessionInfo.addSession(info);
        }
      });
      reader.setExecutionDataVisitor(new IExecutionDataVisitor() {
        public void visitClassExecution(final ExecutionData data) {
          ExecutionDataStore executionDataStore = new ExecutionDataStore();
          executionDataStore.visitClassExecution(data);
          analyzeLinesCoveredByTests(lastSessionInfo.getLastSessionInfo(), executionDataStore, buildOutputDir, context, excludes);
        }
      });
      reader.read();
    }
  }

  private void analyzeLinesCoveredByTests(SessionInfo sessionInfo, ExecutionDataStore executionDataStore, File buildOutputDir, SensorContext context, WildcardMatcher excludes) {
    String id = sessionInfo.getId();
    if (CharMatcher.anyOf(".").countIn(id) < 2 || id.startsWith("dhcp")) {
      // As we do not have a convention for the id, we use this hack to detect if the id is a test or not
    } else {
      String testName = Iterables.getLast(Splitter.on(".").split(id));
      String testFileKey = StringUtils.removeEnd(id, "." + testName);
      Resource testFile = new JavaFile(testFileKey, true);

      CoverageBuilder coverageBuilder = analyze(executionDataStore, buildOutputDir);

      for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
        JavaFile resource = getResource(coverage, context);
        if (resource != null && !isExcluded(coverage, excludes)) {
          CoverageMeasuresBuilder builder = analyzeFile(resource, coverage);

          Testable testAbleFile = perspectives.as(MutableTestable.class, resource);
          if (testAbleFile != null) {
            MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, testFile);
            if (testPlan != null) {
              MutableTestCase testCase = findTestCase(testPlan, testName);
              testCase.covers(testAbleFile, getLinesCover(builder));
            }
          }
        }
      }
    }
  }

  private MutableTestCase findTestCase(MutableTestPlan testPlan, final String test) {
    return Iterables.find(testPlan.testCases(), new Predicate<MutableTestCase>() {
      public boolean apply(@Nullable MutableTestCase input) {
        return input.name().equals(test);
      }
    });
  }

  private List<Integer> getLinesCover(CoverageMeasuresBuilder builder) {
    List<Integer> linesCover = newArrayList();
    for (Map.Entry<Integer, Integer> hitsByLine : builder.getHitsByLine().entrySet()) {
      if (hitsByLine.getValue() > 0) {
        linesCover.add(hitsByLine.getKey());
      }
    }
    return linesCover;
  }

  private CoverageBuilder analyze(ExecutionDataStore executionDataStore, File buildOutputDir) {
    CoverageBuilder coverageBuilder = new CoverageBuilder();
    Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
    analyzeAll(analyzer, buildOutputDir);
    return coverageBuilder;
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

  private CoverageMeasuresBuilder analyzeFile(JavaFile resource, ISourceFileCoverage coverage) {
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

  private static class LastSessionInfo {
    private SessionInfo lastSessionInfo;

    public void addSession(SessionInfo info) {
      lastSessionInfo = info;
    }

    public SessionInfo getLastSessionInfo() {
      return lastSessionInfo;
    }
  }

}
