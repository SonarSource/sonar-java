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
package org.sonarsource.plugins.externalreport.spotbugs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.sonar.api.Plugin.Context;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.externalreport.ExternalReportExtensions;
import org.sonar.java.externalreport.commons.ExternalRulesDefinition;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

public class SpotBugsSensor implements Sensor {

  private static final Logger LOG = Loggers.get(SpotBugsSensor.class);

  private static final String LINTER_KEY = "spotbugs";

  private static final String LINTER_NAME = "SpotBugs";

  private static final String LANGUAGE_KEY = "java";

  private static final String REPORT_PROPERTY_KEY = "sonar.java.spotbugs.reportPaths";

  private static class SingletonRuleLoader {
    private static final ExternalRuleLoader INSTANCE = new ExternalRuleLoader(
      SpotBugsSensor.LINTER_KEY,
      SpotBugsSensor.LINTER_NAME,
      "org/sonar/l10n/java/rules/spotbugs/rules.json",
      SpotBugsSensor.LANGUAGE_KEY);
  }

  static ExternalRuleLoader ruleLoader() {
    return SingletonRuleLoader.INSTANCE;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(SpotBugsSensor.LANGUAGE_KEY)
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PROPERTY_KEY))
      .name("Import of SpotBugs issues");
  }

  public static void defineSensor(Context context) {
    context.addExtension(SpotBugsSensor.class);
  }

  public static void defineRulesAndProperties(Context context) {
    context.addExtension(new ExternalRulesDefinition(ruleLoader()));
    context.addExtension(
      PropertyDefinition.builder(SpotBugsSensor.REPORT_PROPERTY_KEY)
        .name("SpotBugs Report Files")
        .description("Paths (absolute or relative) to xml files with SpotBugs issues.")
        .category(ExternalReportExtensions.EXTERNAL_ANALYZERS_CATEGORY)
        .subCategory(ExternalReportExtensions.JAVA_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build());
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, REPORT_PROPERTY_KEY);
    reportFiles.forEach(report -> importReport(report, context));
  }

  private static void importReport(File reportPath, SensorContext context) {
    try (InputStream in = new FileInputStream(reportPath)) {
      LOG.info("Importing {}", reportPath);
      SpotBugsXmlReportReader.read(context, in, SpotBugsSensor::saveIssue);
    } catch (IOException | XMLStreamException | RuntimeException e) {
      LOG.error(e.getClass().getSimpleName() + ": " + e.getMessage() +
        ", no issues information will be saved as the report file '{}' can't be read.", reportPath, e);
    }
  }

  private static void saveIssue(SensorContext context, List<String> sourceDirs,
                                String rule, String relativeLinuxPath, String line, String message) {

    InputFile inputFile = findInputFile(context, sourceDirs, relativeLinuxPath);
    if (inputFile == null) {
      LOG.warn("No input file found for '{}'. No SpotBugs issues will be imported on this file.", relativeLinuxPath);
      return;
    }
    RuleKey ruleKey = RuleKey.of(SpotBugsSensor.LINTER_KEY, rule);
    NewExternalIssue newExternalIssue = context.newExternalIssue();

    ExternalRuleLoader ruleLoader = ruleLoader();
    newExternalIssue
      .type(ruleLoader.ruleType(ruleKey.rule()))
      .severity(ruleLoader.ruleSeverity(ruleKey.rule()))
      .remediationEffortMinutes(ruleLoader.ruleConstantDebtMinutes(ruleKey.rule()));

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

  private static InputFile findInputFile(SensorContext context, List<String> sourceDirs, String relativeLinuxPath) {
    FilePredicates predicates = context.fileSystem().predicates();
    InputFile inputFile = null;
    for (String sourceDir : sourceDirs) {
      File sourceFile = new File(sourceDir, relativeLinuxPath);
      inputFile = context.fileSystem().inputFile(predicates.hasPath(sourceFile.toString()));
      if (inputFile != null) {
        break;
      }
    }
    return inputFile;
  }

}
