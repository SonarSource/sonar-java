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
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

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
    } catch (IOException e) {
      throw new SonarException(e);
    }
  }

  public final void readExecutionData(File jacocoExecutionData, File buildOutputDir, SensorContext context, WildcardMatcher excludes) throws IOException {
    ExecutionDataVisitor executionDataVisitor = new ExecutionDataVisitor();

    if (jacocoExecutionData == null || !jacocoExecutionData.exists() || !jacocoExecutionData.isFile()) {
      JaCoCoUtils.LOG.info("Project coverage is set to 0% as no JaCoCo execution data has been dumped: {}", jacocoExecutionData);
    } else {
      JaCoCoUtils.LOG.info("Analysing {}", jacocoExecutionData);

      ExecutionDataReader reader = new ExecutionDataReader(new FileInputStream(jacocoExecutionData));
      reader.setSessionInfoVisitor(executionDataVisitor);
      reader.setExecutionDataVisitor(executionDataVisitor);
      reader.read();
    }

    for (Map.Entry<String, ExecutionDataStore> entry : executionDataVisitor.getSessions().entrySet()) {
      analyzeLinesCoveredByTests(entry.getKey(), entry.getValue(), buildOutputDir, context, excludes);
    }

    CoverageBuilder coverageBuilder = analyze(executionDataVisitor.getMerged(), buildOutputDir);
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

  private void analyzeLinesCoveredByTests(String sessionId, ExecutionDataStore executionDataStore, File buildOutputDir, SensorContext context, WildcardMatcher excludes) {
    int i = sessionId.indexOf(' ');
    if (i < 0) {
      return;
    }
    String testClassName = sessionId.substring(0, i);
    String testName = sessionId.substring(i + 1);
    Resource testResource = context.getResource(new JavaFile(testClassName, true));
    if (testResource == null) {
      // No such test class
      return;
    }

    CoverageBuilder coverageBuilder = analyze2(executionDataStore, buildOutputDir);
    for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
      JavaFile resource = getResource(coverage, context);
      if (resource != null && !isExcluded(coverage, excludes)) {
        CoverageMeasuresBuilder builder = analyzeFile(resource, coverage);
        List<Integer> coveredLines = getCoveredLines(builder);
        if (!coveredLines.isEmpty()) {
          addCoverage(resource, testResource, testName, coveredLines);
        }
      }
    }
  }

  private CoverageBuilder analyze2(ExecutionDataStore executionDataStore, File buildOutputDir) {
    CoverageBuilder coverageBuilder = new CoverageBuilder();
    Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
    for (ExecutionData data : executionDataStore.getContents()) {
      String vmClassName = data.getName();
      String classFileName = vmClassName.replace('.', '/') + ".class";
      File classFile = new File(buildOutputDir, classFileName);
      if (classFile.isFile()) {
        try {
          analyzer.analyzeAll(classFile);
        } catch (Exception e) {
          JaCoCoUtils.LOG.warn("Exception during analysis of file " + classFile.getAbsolutePath(), e);
        }
      }
    }
    return coverageBuilder;
  }

  private List<Integer> getCoveredLines(CoverageMeasuresBuilder builder) {
    List<Integer> linesCover = newArrayList();
    for (Map.Entry<Integer, Integer> hitsByLine : builder.getHitsByLine().entrySet()) {
      if (hitsByLine.getValue() > 0) {
        linesCover.add(hitsByLine.getKey());
      }
    }
    return linesCover;
  }

  private void addCoverage(JavaFile resource, Resource testFile, String testName, List<Integer> coveredLines) {
    Testable testAbleFile = perspectives.as(MutableTestable.class, resource);
    if (testAbleFile != null) {
      MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, testFile);
      if (testPlan != null) {
        for (MutableTestCase testCase : testPlan.testCasesByName(testName)) {
          testCase.setCoverageBlock(testAbleFile, coveredLines);
        }
      }
    }
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

}
