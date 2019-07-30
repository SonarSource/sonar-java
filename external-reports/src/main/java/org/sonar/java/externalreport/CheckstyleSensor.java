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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

import static org.sonar.java.externalreport.ExternalIssueUtils.importIfExist;

public class CheckstyleSensor implements Sensor {

  private static final Logger LOG = Loggers.get(CheckstyleSensor.class);

  private static final String LINTER_NAME = "Checkstyle";
  private static final String LANGUAGE_KEY = "java";
  public static final String REPORT_PROPERTY_KEY = "sonar.java.checkstyle.reportPaths";
  public static final String LINTER_KEY = "checkstyle";

  public static final ExternalRuleLoader RULE_LOADER = new ExternalRuleLoader(
    CheckstyleSensor.LINTER_KEY,
    CheckstyleSensor.LINTER_NAME,
    "org/sonar/l10n/java/rules/checkstyle/rules.json",
    CheckstyleSensor.LANGUAGE_KEY);

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
    reportFiles.forEach(report -> importIfExist(LINTER_NAME, context, report, CheckstyleSensor::importReport));
  }

  private static void importReport(File reportPath, SensorContext context) {
    try (InputStream in = new FileInputStream(reportPath)) {
      LOG.info("Importing {}", reportPath);
      CheckstyleXmlReportReader.read(context, in, CheckstyleSensor::saveIssue);
    } catch (Exception e) {
      LOG.error("Failed to import external issues report: " + reportPath, e);
    }
  }

  private static void saveIssue(SensorContext context, InputFile inputFile, String key, String line, String message) {
    ExternalIssueUtils.saveIssue(context, RULE_LOADER, inputFile, CheckstyleSensor.LINTER_KEY, key, line, message);
  }

}
