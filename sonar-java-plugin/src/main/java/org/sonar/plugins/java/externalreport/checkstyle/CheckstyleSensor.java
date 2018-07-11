/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.plugins.java.externalreport.checkstyle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.Java;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

public class CheckstyleSensor implements Sensor {

  private static final Logger LOG = Loggers.get(CheckstyleSensor.class);

  static final String LINTER_KEY = "checkstyle";

  static final String LINTER_NAME = "Checkstyle";

  public static final String REPORT_PROPERTY_KEY = "sonar.java.checkstyle.reportPaths";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage( Java.KEY)
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PROPERTY_KEY))
      .name("Import of Checkstyle issues");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, REPORT_PROPERTY_KEY);
    reportFiles.forEach(report -> importReport(report, context));
  }

  private static void importReport(File reportPath, SensorContext context) {
    try (InputStream in = new FileInputStream(reportPath)) {
      LOG.info("Importing {}", reportPath);
      CheckstyleXmlReportReader.read(context, in, CheckstyleSensor::saveIssue);
    } catch (IOException | XMLStreamException | RuntimeException e) {
      LOG.error("No issues information will be saved as the report file '{}' can't be read.", reportPath, e);
    }
  }

  private static void saveIssue(SensorContext context, InputFile inputFile, String key, String line, String message) {
    RuleKey ruleKey = RuleKey.of(CheckstyleSensor.LINTER_KEY, key);
    NewExternalIssue newExternalIssue = context.newExternalIssue();
    String ruleKey1 = ruleKey.rule();
    ExternalRuleLoader externalRuleLoader = CheckstyleRulesDefinition.RULE_LOADER;
    newExternalIssue
      .type(externalRuleLoader.ruleType(ruleKey1))
      .severity(externalRuleLoader.ruleSeverity(ruleKey1))
      .remediationEffortMinutes(externalRuleLoader.ruleConstantDebtMinutes(ruleKey1));

    NewIssueLocation primaryLocation = newExternalIssue.newLocation()
      .message(message)
      .on(inputFile);

    if (!line.isEmpty() && !line.equals("0")) {
      primaryLocation.at(inputFile.selectLine(Integer.parseInt(line)));
    }

    newExternalIssue
      .at(primaryLocation)
      .forRule(ruleKey)
      .save();
  }

}
