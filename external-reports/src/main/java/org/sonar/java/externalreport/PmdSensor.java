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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

import static org.sonar.java.externalreport.ExternalIssueUtils.importIfExist;

public class PmdSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(PmdSensor.class);

  public static final String REPORT_PROPERTY_KEY = "sonar.java.pmd.reportPaths";

  public static final String LINTER_KEY = "pmd";
  private static final String LINTER_NAME = "PMD";
  private static final String LANGUAGE_KEY = "java";

  private final ExternalRuleLoader ruleLoader;

  public PmdSensor(SonarRuntime sonarRuntime) {
    ruleLoader = new ExternalRuleLoader(
      PmdSensor.LINTER_KEY,
      PmdSensor.LINTER_NAME,
      "org/sonar/l10n/java/rules/pmd/rules.json",
      PmdSensor.LANGUAGE_KEY,
      sonarRuntime);
  }

  public ExternalRuleLoader ruleLoader() {
    return ruleLoader;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PROPERTY_KEY))
      .onlyOnLanguage(LANGUAGE_KEY)
      .name("Import of PMD issues");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, REPORT_PROPERTY_KEY);
    reportFiles.forEach(report -> importIfExist(LINTER_NAME, context, report, this::importReport));
  }

  private void importReport(File reportFile, SensorContext context) {
    try {
      LOG.info("Importing {}", reportFile);
      PmdXmlReportReader.read(context, reportFile, ruleLoader);
    } catch (Exception e) {
      LOG.error("Failed to import external issues report: {}", reportFile.getAbsolutePath(), e);
    }
  }

}
