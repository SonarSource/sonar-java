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
package org.sonar.java.externalreport;

import java.io.File;
import java.util.List;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

import static org.sonar.java.externalreport.ExternalIssueUtils.importIfExist;

public class PmdSensor implements Sensor {

  private static final Logger LOG = Loggers.get(PmdSensor.class);

  public static final String REPORT_PROPERTY_KEY = "sonar.java.pmd.reportPaths";

  public static final String LINTER_KEY = "pmd";
  private static final String LINTER_NAME = "PMD";
  private static final String LANGUAGE_KEY = "java";

  public static final ExternalRuleLoader RULE_LOADER = new ExternalRuleLoader(
    PmdSensor.LINTER_KEY,
    PmdSensor.LINTER_NAME,
    "org/sonar/l10n/java/rules/pmd/rules.json",
    PmdSensor.LANGUAGE_KEY);

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PROPERTY_KEY))
      .name("Import of PMD issues");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, REPORT_PROPERTY_KEY);
    reportFiles.forEach(report -> importIfExist(LINTER_NAME, context, report, PmdSensor::importReport));
  }

  private static void importReport(File reportFile, SensorContext context) {
    try {
      LOG.info("Importing {}", reportFile);
      PmdXmlReportReader.read(context, reportFile, RULE_LOADER);
    } catch (Exception e) {
      LOG.error("Failed to import external issues report: " + reportFile.getAbsolutePath(), e);
    }
  }

}
