/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.surefire.data.UnitTestClassReport;
import org.sonar.plugins.surefire.data.UnitTestIndex;
import org.sonar.plugins.surefire.data.UnitTestResult;
import org.sonar.squidbridge.api.AnalysisException;

import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.Serializable;
import java.util.Map;

/**
 * @since 2.4
 */
@BatchSide
public class SurefireJavaParser {

  private static final Logger LOGGER = Loggers.get(SurefireJavaParser.class);
  private final ResourcePerspectives perspectives;
  private final JavaResourceLocator javaResourceLocator;

  public SurefireJavaParser(ResourcePerspectives perspectives, JavaResourceLocator javaResourceLocator) {
    this.perspectives = perspectives;
    this.javaResourceLocator = javaResourceLocator;
  }

  public void collect(SensorContext context, File reportsDir, boolean reportDirSetByUser) {
    File[] xmlFiles = getReports(reportsDir, reportDirSetByUser);
    if (xmlFiles.length > 0) {
      parseFiles(context, xmlFiles);
    }
  }

  private static File[] getReports(@Nullable File dir, boolean reportDirSetByUser) {
    if (dir == null) {
      return new File[0];
    } else if (!dir.isDirectory()) {
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

  private void parseFiles(SensorContext context, File[] reports) {
    UnitTestIndex index = new UnitTestIndex();
    parseFiles(reports, index);
    sanitize(index);
    save(index, context);
  }

  private static void parseFiles(File[] reports, UnitTestIndex index) {
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
    for (Map.Entry<String, UnitTestClassReport> entry : index.getIndexByClassname().entrySet()) {
      UnitTestClassReport report = entry.getValue();
      if (report.getTests() > 0) {
        negativeTimeTestNumber += report.getNegativeTimeTestNumber();
        InputFile resource = getUnitTestResource(entry.getKey());
        if (resource != null) {
          save(report, resource, context);
        } else {
          LOGGER.warn("Resource not found: {}", entry.getKey());
        }
      }
    }
    if (negativeTimeTestNumber > 0) {
      LOGGER.warn("There is {} test(s) reported with negative time by surefire, total duration may not be accurate.", negativeTimeTestNumber);
    }
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

  protected InputFile getUnitTestResource(String classKey) {
    return javaResourceLocator.findResourceByClassName(classKey);
  }

  private static <T extends Serializable> void saveMeasure(SensorContext context, InputFile inputFile, Metric<T> metric, T value) {
    context.<T>newMeasure().forMetric(metric).on(inputFile).withValue(value).save();
  }

}
