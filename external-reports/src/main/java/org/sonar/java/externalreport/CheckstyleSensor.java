/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.externalreport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

import static org.sonar.java.externalreport.ExternalIssueUtils.importIfExist;

public class CheckstyleSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(CheckstyleSensor.class);

  private static final String LINTER_NAME = "Checkstyle";
  private static final String LANGUAGE_KEY = "java";
  public static final String REPORT_PROPERTY_KEY = "sonar.java.checkstyle.reportPaths";
  public static final String LINTER_KEY = "checkstyle";

  private final ExternalRuleLoader ruleLoader;

  public CheckstyleSensor(SonarRuntime sonarRuntime) {
    ruleLoader = new ExternalRuleLoader(
      CheckstyleSensor.LINTER_KEY,
      CheckstyleSensor.LINTER_NAME,
      "org/sonar/l10n/java/rules/checkstyle/rules.json",
      CheckstyleSensor.LANGUAGE_KEY,
      sonarRuntime);
  }

  public ExternalRuleLoader ruleLoader() {
    return ruleLoader;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(CheckstyleSensor.LANGUAGE_KEY)
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PROPERTY_KEY))
      .name("Import of Checkstyle issues");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, REPORT_PROPERTY_KEY);
    reportFiles.forEach(report -> importIfExist(LINTER_NAME, context, report, this::importReport));
  }

  private void importReport(File reportPath, SensorContext context) {
    try (InputStream in = new FileInputStream(reportPath)) {
      LOG.info("Importing {}", reportPath);
      CheckstyleXmlReportReader.read(context, in, this::saveIssue);
    } catch (Exception e) {
      LOG.error("Failed to import external issues report: {}", reportPath, e);
    }
  }

  private void saveIssue(SensorContext context, InputFile inputFile, String key, String line, String message) {
    ExternalIssueUtils.saveIssue(context, ruleLoader, inputFile, CheckstyleSensor.LINTER_KEY, key, line, message);
  }

}
