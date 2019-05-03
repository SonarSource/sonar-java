/*
 * SonarQube Java
 * Copyright (C) 2010-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.jacoco;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.Testable;
import org.sonar.api.utils.Version;
import org.sonar.java.AnalysisWarningsWrapper;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;


public class UnitTestAnalyzer {

  private final ResourcePerspectives perspectives;
  private final JavaResourceLocator javaResourceLocator;
  private final AnalysisWarningsWrapper analysisWarnings;

  private Map<String, File> classFilesCache;
  private JavaClasspath javaClasspath;
  private JacocoReportReader jacocoReportReader;
  private final File report;
  private boolean supportCoverageByTest;

  public UnitTestAnalyzer(File report, ResourcePerspectives perspectives, JavaResourceLocator javaResourceLocator,
                          JavaClasspath javaClasspath,
                          AnalysisWarningsWrapper analysisWarnings) {
    this.report = report;
    this.perspectives = perspectives;
    this.javaResourceLocator = javaResourceLocator;
    this.javaClasspath = javaClasspath;
    this.analysisWarnings = analysisWarnings;
  }

  private static String fullyQualifiedClassName(String packageName, String simpleClassName) {
    return ("".equals(packageName) ? "" : (packageName + "/")) + StringUtils.substringBeforeLast(simpleClassName, ".");
  }

  private InputFile getResource(ISourceFileCoverage coverage) {
    String className = fullyQualifiedClassName(coverage.getPackageName(), coverage.getName());

    InputFile inputFile = javaResourceLocator.findResourceByClassName(className);
    if (inputFile == null) {
      // Do not save measures on resource which doesn't exist in the context
      return null;
    }
    if (inputFile.type() == InputFile.Type.TEST) {
      return null;
    }

    return inputFile;
  }

  public final void analyse(SensorContext context) {
    supportCoverageByTest = !context.getSonarQubeVersion().isGreaterThanOrEqual(Version.create(7, 7));
    classFilesCache = new HashMap<>();
    for (File classesDir : javaClasspath.getBinaryDirs()) {
      populateClassFilesCache(classesDir, "");
    }

    if (classFilesCache.isEmpty()) {
      JaCoCoExtensions.LOG.info("No JaCoCo analysis of project coverage can be done since there are no class files.");
      return;
    }
    readExecutionData(report, context);

    classFilesCache = null;
  }

  private void populateClassFilesCache(File dir, String path) {
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (file.isDirectory()) {
        populateClassFilesCache(file, path + file.getName() + "/");
      } else if (file.getName().endsWith(".class")) {
        String className = path + StringUtils.removeEnd(file.getName(), ".class");
        classFilesCache.put(className, file);
      }
    }
  }

  private void readExecutionData(@Nullable File jacocoExecutionData, SensorContext context) {
    File newJacocoExecutionData = jacocoExecutionData;
    if (newJacocoExecutionData == null || !newJacocoExecutionData.isFile()) {
      JaCoCoExtensions.LOG.info("Project coverage is set to 0% as no JaCoCo execution data has been dumped: {}", newJacocoExecutionData);
      newJacocoExecutionData = null;
    }
    ExecutionDataVisitor executionDataVisitor = new ExecutionDataVisitor();
    jacocoReportReader = new JacocoReportReader(newJacocoExecutionData).readJacocoReport(executionDataVisitor, executionDataVisitor);

    boolean collectedCoveragePerTest = readCoveragePerTests(executionDataVisitor);

    CoverageBuilder coverageBuilder = jacocoReportReader.analyzeFiles(executionDataVisitor.getMerged(), classFilesCache.values());
    int analyzedResources = 0;
    for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
      InputFile inputFile = getResource(coverage);
      if (inputFile != null) {
        NewCoverage newCoverage = context.newCoverage().onFile(inputFile);
        analyzeFile(newCoverage, inputFile, coverage);
        newCoverage.save();
        analyzedResources++;
      }
    }
    if (analyzedResources == 0) {
      JaCoCoExtensions.LOG.warn("Coverage information was not collected. Perhaps you forget to include debug information into compiled classes?");
    } else if (collectedCoveragePerTest) {
      logDeprecationForCoveragePerTest();
    }
  }

  private void logDeprecationForCoveragePerTest() {
    String msg;
    if (supportCoverageByTest) {
      msg = "'Coverage per Test' feature is deprecated. Consider removing sonar-jacoco-listeners from your configuration.";
    } else {
      msg = "'Coverage per Test' feature was removed from SonarQube. Remove sonar-jacoco-listeners listener configuration.";
    }
    JaCoCoExtensions.LOG.warn(msg);
    analysisWarnings.addUnique(msg);
  }

  private boolean readCoveragePerTests(ExecutionDataVisitor executionDataVisitor) {
    boolean collectedCoveragePerTest = false;
    for (Map.Entry<String, ExecutionDataStore> entry : executionDataVisitor.getSessions().entrySet()) {
      if (analyzeLinesCoveredByTests(entry.getKey(), entry.getValue())) {
        collectedCoveragePerTest = true;
      }
    }
    return collectedCoveragePerTest;
  }

  private boolean analyzeLinesCoveredByTests(String sessionId, ExecutionDataStore executionDataStore) {
    int i = sessionId.indexOf(' ');
    if (i < 0) {
      return false;
    }
    String testClassName = sessionId.substring(0, i);
    String testName = sessionId.substring(i + 1);
    InputFile testResource = javaResourceLocator.findResourceByClassName(testClassName);
    if (testResource == null) {
      // No such test class
      return false;
    }

    boolean result = false;
    CoverageBuilder coverageBuilder = jacocoReportReader.analyzeFiles(executionDataStore, classFilesOfStore(executionDataStore));
    for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
      InputFile resource = getResource(coverage);
      if (resource != null) {
        List<Integer> coveredLines = coveredLines(coverage);
        if (!coveredLines.isEmpty() && addCoverage(resource, testResource, testName, coveredLines)) {
          result = true;
        }
      }
    }
    return result;
  }

  private Collection<File> classFilesOfStore(ExecutionDataStore executionDataStore) {
    Collection<File> result = new ArrayList<>();
    for (ExecutionData data : executionDataStore.getContents()) {
      String vmClassName = data.getName();
      File classFile = classFilesCache.get(vmClassName);
      if (classFile != null) {
        result.add(classFile);
      }
    }
    return result;
  }


  private boolean addCoverage(InputFile resource, InputFile testFile, String testName, List<Integer> coveredLines) {
    boolean result = false;
    Testable testAbleFile = perspectives.as(MutableTestable.class, resource);
    if (testAbleFile != null) {
      MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, testFile);
      if (testPlan != null) {
        for (MutableTestCase testCase : testPlan.testCasesByName(testName)) {
          testCase.setCoverageBlock(testAbleFile, coveredLines);
          result = true;
        }
      }
    }
    return result;
  }

  private static List<Integer> coveredLines(ISourceFileCoverage coverage) {
    List<Integer> coveredLines = new ArrayList<>();
    for (int lineId = coverage.getFirstLine(); lineId <= coverage.getLastLine(); lineId++) {
      ILine line = coverage.getLine(lineId);
      switch (line.getInstructionCounter().getStatus()) {
        case ICounter.FULLY_COVERED:
        case ICounter.PARTLY_COVERED:
          coveredLines.add(lineId);
          break;
        case ICounter.NOT_COVERED:
          break;
        default:
          continue;
      }
    }
    return coveredLines;
  }

  private static void analyzeFile(NewCoverage newCoverage, InputFile resource, ISourceFileCoverage coverage) {
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
          JaCoCoExtensions.LOG.warn("Unknown status for line {} in {}", lineId, resource);
          continue;
      }
      newCoverage.lineHits(lineId, hits);
      ICounter branchCounter = line.getBranchCounter();
      int conditions = branchCounter.getTotalCount();
      if (conditions > 0) {
        int coveredConditions = branchCounter.getCoveredCount();
        newCoverage.conditions(lineId, conditions, coveredConditions);
      }
    }
  }
}
