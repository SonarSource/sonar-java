/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
 * sonarqube@googlegroups.com
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.Testable;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public abstract class AbstractAnalyzer {

  private final ResourcePerspectives perspectives;
  private final ModuleFileSystem fileSystem;
  private final PathResolver pathResolver;
  private final JavaResourceLocator javaResourceLocator;
  private final boolean readCoveragePerTests;

  private Map<String, File> classFilesCache;
  private JavaClasspath javaClasspath;
  private JacocoReportReader jacocoReportReader;

  public AbstractAnalyzer(ResourcePerspectives perspectives, ModuleFileSystem fileSystem, PathResolver pathResolver,
    JavaResourceLocator javaResourceLocator, JavaClasspath javaClasspath) {
    this(perspectives, fileSystem, pathResolver, javaResourceLocator, javaClasspath, true);
  }

  public AbstractAnalyzer(ResourcePerspectives perspectives, ModuleFileSystem fileSystem,
    PathResolver pathResolver, JavaResourceLocator javaResourceLocator, JavaClasspath javaClasspath, boolean readCoveragePerTests) {
    this.perspectives = perspectives;
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;
    this.javaResourceLocator = javaResourceLocator;
    this.readCoveragePerTests = readCoveragePerTests;
    this.javaClasspath = javaClasspath;
  }

  private static String fullyQualifiedClassName(String packageName, String simpleClassName) {
    return ("".equals(packageName) ? "" : (packageName + "/")) + StringUtils.substringBeforeLast(simpleClassName, ".");
  }

  private Resource getResource(ISourceFileCoverage coverage, SensorContext context) {
    String className = fullyQualifiedClassName(coverage.getPackageName(), coverage.getName());

    Resource resourceInContext = context.getResource(javaResourceLocator.findResourceByClassName(className));
    if (resourceInContext == null) {
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
    classFilesCache = Maps.newHashMap();
    for (File classesDir : javaClasspath.getBinaryDirs()) {
      populateClassFilesCache(classesDir, "");
    }

    if (classFilesCache.isEmpty()) {
      JaCoCoExtensions.LOG.info("No JaCoCo analysis of project coverage can be done since there is no class files.");
      return;
    }
    String path = getReportPath(project);
    File jacocoExecutionData = pathResolver.relativeFile(fileSystem.baseDir(), path);

    readExecutionData(jacocoExecutionData, context);

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

  private void readExecutionData(File jacocoExecutionData, SensorContext context) {
    if (jacocoExecutionData == null || !jacocoExecutionData.isFile()) {
      JaCoCoExtensions.LOG.info("Project coverage is set to 0% as no JaCoCo execution data has been dumped: {}", jacocoExecutionData);
      jacocoExecutionData = null;
    }
    ExecutionDataVisitor executionDataVisitor = new ExecutionDataVisitor();
    jacocoReportReader = new JacocoReportReader(jacocoExecutionData).readJacocoReport(executionDataVisitor, executionDataVisitor);

    boolean collectedCoveragePerTest = readCoveragePerTests(context, executionDataVisitor);

    CoverageBuilder coverageBuilder = jacocoReportReader.analyzeFiles(executionDataVisitor.getMerged(), classFilesCache.values());
    int analyzedResources = 0;
    for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
      Resource resource = getResource(coverage, context);
      if (resource != null) {
        CoverageMeasuresBuilder builder = analyzeFile(resource, coverage);
        saveMeasures(context, resource, builder.createMeasures());
        analyzedResources++;
      }
    }
    if (analyzedResources == 0) {
      JaCoCoExtensions.LOG.warn("Coverage information was not collected. Perhaps you forget to include debug information into compiled classes?");
    } else if (collectedCoveragePerTest) {
      JaCoCoExtensions.LOG.info("Information about coverage per test has been collected.");
    } else if (jacocoExecutionData != null) {
      JaCoCoExtensions.LOG.info("No information about coverage per test.");
    }
  }

  private boolean readCoveragePerTests(SensorContext context, ExecutionDataVisitor executionDataVisitor) {
    boolean collectedCoveragePerTest = false;
    if (readCoveragePerTests) {
      for (Map.Entry<String, ExecutionDataStore> entry : executionDataVisitor.getSessions().entrySet()) {
        if (analyzeLinesCoveredByTests(entry.getKey(), entry.getValue(), context)) {
          collectedCoveragePerTest = true;
        }
      }
    }
    return collectedCoveragePerTest;
  }

  private boolean analyzeLinesCoveredByTests(String sessionId, ExecutionDataStore executionDataStore, SensorContext context) {
    int i = sessionId.indexOf(' ');
    if (i < 0) {
      return false;
    }
    String testClassName = sessionId.substring(0, i);
    String testName = sessionId.substring(i + 1);
    Resource testResource = context.getResource(javaResourceLocator.findResourceByClassName(testClassName));
    if (testResource == null) {
      // No such test class
      return false;
    }

    boolean result = false;
    CoverageBuilder coverageBuilder = jacocoReportReader.analyzeFiles(executionDataStore, classFilesOfStore(executionDataStore));
    for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
      Resource resource = getResource(coverage, context);
      if (resource != null) {
        CoverageMeasuresBuilder builder = analyzeFile(resource, coverage);
        List<Integer> coveredLines = getCoveredLines(builder);
        if (!coveredLines.isEmpty() && addCoverage(resource, testResource, testName, coveredLines)) {
          result = true;
        }
      }
    }
    return result;
  }

  private Collection<File> classFilesOfStore(ExecutionDataStore executionDataStore) {
    Collection<File> result = Lists.newArrayList();
    for (ExecutionData data : executionDataStore.getContents()) {
      String vmClassName = data.getName();
      File classFile = classFilesCache.get(vmClassName);
      if (classFile != null) {
        result.add(classFile);
      }
    }
    return result;
  }

  private static List<Integer> getCoveredLines(CoverageMeasuresBuilder builder) {
    List<Integer> linesCover = newArrayList();
    for (Map.Entry<Integer, Integer> hitsByLine : builder.getHitsByLine().entrySet()) {
      if (hitsByLine.getValue() > 0) {
        linesCover.add(hitsByLine.getKey());
      }
    }
    return linesCover;
  }

  private boolean addCoverage(Resource resource, Resource testFile, String testName, List<Integer> coveredLines) {
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

  private static CoverageMeasuresBuilder analyzeFile(Resource resource, ISourceFileCoverage coverage) {
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
          JaCoCoExtensions.LOG.warn("Unknown status for line {} in {}", lineId, resource);
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

  protected abstract void saveMeasures(SensorContext context, Resource resource, Collection<Measure> measures);

  protected abstract String getReportPath(Project project);

}
