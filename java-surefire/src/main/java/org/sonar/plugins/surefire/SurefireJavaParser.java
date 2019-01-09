/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.plugins.surefire;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.AnalysisException;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.surefire.data.UnitTestClassReport;
import org.sonar.plugins.surefire.data.UnitTestIndex;
import org.sonar.plugins.surefire.data.UnitTestResult;

/**
 * @since 2.4
 */
@ScannerSide
public class SurefireJavaParser {

  private static final Logger LOGGER = Loggers.get(SurefireJavaParser.class);
  private final ResourcePerspectives perspectives;
  private final JavaResourceLocator javaResourceLocator;

  public SurefireJavaParser(ResourcePerspectives perspectives, JavaResourceLocator javaResourceLocator) {
    this.perspectives = perspectives;
    this.javaResourceLocator = javaResourceLocator;
  }

  public void collect(SensorContext context, List<File> reportsDirs, boolean reportDirSetByUser) {
    List<File> xmlFiles = getReports(reportsDirs, reportDirSetByUser);
    if (!xmlFiles.isEmpty()) {
      parseFiles(context, xmlFiles);
    }
  }

  private static List<File> getReports(List<File> dirs, boolean reportDirSetByUser) {
    return dirs.stream()
      .map(dir -> getReports(dir, reportDirSetByUser))
      .flatMap(Arrays::stream)
      .collect(Collectors.toList());
  }

  private static File[] getReports(File dir, boolean reportDirSetByUser) {
    if (!dir.isDirectory()) {
      if(reportDirSetByUser) {
        LOGGER.error("Reports path not found or is not a directory: " + dir.getAbsolutePath());
      }
      return new File[0];
    }
    File[] unitTestResultFiles = findXMLFilesStartingWith(dir, "TEST-");
    if (unitTestResultFiles.length == 0) {
      // maybe there's only a test suite result file
      unitTestResultFiles = findXMLFilesStartingWith(dir, "TESTS-");
    }
    if(unitTestResultFiles.length == 0) {
      LOGGER.warn("Reports path contains no files matching TEST-.*.xml : "+dir.getAbsolutePath());
    }
    return unitTestResultFiles;
  }

  private static File[] findXMLFilesStartingWith(File dir, final String fileNameStart) {
    return dir.listFiles((parentDir, name) -> name.startsWith(fileNameStart) && name.endsWith(".xml"));
  }

  private void parseFiles(SensorContext context, List<File> reports) {
    UnitTestIndex index = new UnitTestIndex();
    parseFiles(reports, index);
    sanitize(index);
    save(index, context);
  }

  private static void parseFiles(List<File> reports, UnitTestIndex index) {
    StaxParser parser = new StaxParser(index);
    for (File report : reports) {
      try {
        parser.parse(report);
      } catch (XMLStreamException e) {
        throw new AnalysisException("Fail to parse the Surefire report: " + report, e);
      }
    }
  }

  private static void sanitize(UnitTestIndex index) {
    for (String classname : index.getClassnames()) {
      if (StringUtils.contains(classname, "$")) {
        // Surefire reports classes whereas sonar supports files
        String parentClassName = StringUtils.substringBefore(classname, "$");
        index.merge(classname, parentClassName);
      }
    }
  }

  private void save(UnitTestIndex index, SensorContext context) {
    long negativeTimeTestNumber = 0;
    Map<InputFile, UnitTestClassReport> indexByInputFile = mapToInputFile(index.getIndexByClassname());
    for (Map.Entry<InputFile, UnitTestClassReport> entry : indexByInputFile.entrySet()) {
      UnitTestClassReport report = entry.getValue();
      if (report.getTests() > 0) {
        negativeTimeTestNumber += report.getNegativeTimeTestNumber();
        save(report, entry.getKey(), context);
      }
    }
    if (negativeTimeTestNumber > 0) {
      LOGGER.warn("There is {} test(s) reported with negative time by surefire, total duration may not be accurate.", negativeTimeTestNumber);
    }
  }

  private Map<InputFile, UnitTestClassReport> mapToInputFile(Map<String, UnitTestClassReport> indexByClassname) {
    Map<InputFile, UnitTestClassReport> result = new HashMap<>();
    indexByClassname.forEach((className, index) -> {
      InputFile resource = getUnitTestResource(className, index);
      if (resource != null) {
        UnitTestClassReport report = result.computeIfAbsent(resource, r -> new UnitTestClassReport());
        // in case of repeated/parameterized tests (JUnit 5.x) we may end up with tests having the same name
        index.getResults().forEach(report::add);
      } else {
        LOGGER.debug("Resource not found: {}", className);
      }
    });
    return result;
  }

  private void save(UnitTestClassReport report, InputFile inputFile, SensorContext context) {
    int testsCount = report.getTests() - report.getSkipped();
    saveMeasure(context, inputFile, CoreMetrics.SKIPPED_TESTS, report.getSkipped());
    saveMeasure(context, inputFile, CoreMetrics.TESTS, testsCount);
    saveMeasure(context, inputFile, CoreMetrics.TEST_ERRORS, report.getErrors());
    saveMeasure(context, inputFile, CoreMetrics.TEST_FAILURES, report.getFailures());
    saveMeasure(context, inputFile, CoreMetrics.TEST_EXECUTION_TIME, report.getDurationMilliseconds());
    saveResults(inputFile, report);
  }

  protected void saveResults(InputFile testFile, UnitTestClassReport report) {
    for (UnitTestResult unitTestResult : report.getResults()) {
      MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, testFile);
      if (testPlan != null) {
        testPlan.addTestCase(unitTestResult.getName())
            .setDurationInMs(Math.max(unitTestResult.getDurationMilliseconds(), 0))
            .setStatus(TestCase.Status.of(unitTestResult.getStatus()))
            .setMessage(unitTestResult.getMessage())
            .setStackTrace(unitTestResult.getStackTrace());
      }
    }
  }

  @CheckForNull
  private InputFile getUnitTestResource(String className, UnitTestClassReport unitTestClassReport) {
    InputFile resource = javaResourceLocator.findResourceByClassName(className);
    if (resource == null) {
      // fall back on testSuite class name (repeated and parameterized tests from JUnit 5.0 are using test name as classname)
      // Should be fixed with JUnit 5.1, see: https://github.com/junit-team/junit5/issues/1182
      return unitTestClassReport.getResults().stream()
        .map(r -> javaResourceLocator.findResourceByClassName(r.getTestSuiteClassName()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
    }
    return resource;
  }

  private static <T extends Serializable> void saveMeasure(SensorContext context, InputFile inputFile, Metric<T> metric, T value) {
    context.<T>newMeasure().forMetric(metric).on(inputFile).withValue(value).save();
  }

}
