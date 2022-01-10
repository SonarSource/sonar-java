/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.externalreport.ExternalReportTestUtils.onlyOneLogElement;

@EnableRuleMigrationSupport
class CheckstyleSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "checkstyle")
    .toAbsolutePath().normalize();

  private static CheckstyleSensor checkstyleSensor = new CheckstyleSensor();

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void checkstyle_rules_definition() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new ExternalRulesDefinition(CheckstyleSensor.RULE_LOADER, CheckstyleSensor.LINTER_KEY).define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository("external_checkstyle");
    assertThat(repository.name()).isEqualTo("Checkstyle");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.isExternal()).isTrue();

    assertThat(repository.rules()).hasSize(156);

    RulesDefinition.Rule rule = repository.rule("ArrayTypeStyleCheck");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Array Type Style");
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.severity()).isEqualTo("MAJOR");
    assertThat(rule.htmlDescription()).isEqualTo(
      "See description of Checkstyle rule <code>ArrayTypeStyleCheck</code> at the " +
        "<a href=\"http://checkstyle.sourceforge.net/config_misc.html#ArrayTypeStyle\">Checkstyle website</a>.");
    assertThat(rule.tags()).isEmpty();
    assertThat(rule.debtRemediationFunction().baseEffort()).isEqualTo("5min");
  }

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    checkstyleSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of Checkstyle issues");
    assertThat(sensorDescriptor.languages()).containsOnly("java");
    ExternalReportTestUtils.assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void expected_issues() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("checkstyle-result.xml");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("checkstyle-project:Main.java");
    assertThat(first.engineId()).isEqualTo("checkstyle");
    assertThat(first.ruleId()).isEqualTo("javadoc.JavadocPackageCheck");
    assertThat(first.ruleKey().rule()).isEqualTo("javadoc.JavadocPackageCheck");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.remediationEffort().longValue()).isEqualTo(5L);
    assertThat(first.primaryLocation().message()).isEqualTo("Missing package-info.java file.");
    assertThat(first.primaryLocation().textRange()).isNull();

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("checkstyle-project:Main.java");
    assertThat(second.ruleKey().rule()).isEqualTo("modifier.ModifierOrderCheck");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.remediationEffort().longValue()).isEqualTo(5L);
    assertThat(second.primaryLocation().message()).isEqualTo("'static' modifier out of order with the JLS suggestions.");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(2);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("checkstyle-project:A.java");
    assertThat(third.ruleKey().rule()).isEqualTo("javadoc.JavadocTypeCheck");
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.remediationEffort().longValue()).isEqualTo(5L);
    assertThat(third.primaryLocation().message()).isEqualTo("Missing a Javadoc comment.");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(1);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  void no_issues_without_report_paths_property() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(null);
    assertThat(externalIssues).isEmpty();
    ExternalReportTestUtils.assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void no_issues_with_invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.WARN)))
      .startsWith("Checkstyle report not found: ")
      .endsWith("invalid-path.txt");
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-checkstyle-file.xml", "checkstyle-with-invalid-line.xml", "invalid-file.xml"})
  void no_issues_with_invalid_report(String fileName) throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(fileName);
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("Failed to import external issues report:")
      .endsWith(fileName);
  }

  @Test
  void issues_when_xml_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("checkstyle-with-errors.xml");
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("checkstyle-project:Main.java");
    assertThat(first.ruleKey().rule()).isEqualTo("UnknownRuleKey");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Error at file level with an unknown rule key.");
    assertThat(first.primaryLocation().textRange()).isNull();

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.WARN)))
      .startsWith("No input file found for '")
      .endsWith("not-existing-file.java'. No checkstyle issues will be imported on this file.");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
      "Unexpected error without message for rule: 'com.puppycrawl.tools.checkstyle.checks.ArrayTypeStyleCheck'",
      "Unexpected rule key without 'com.puppycrawl.tools.checkstyle.checks.' prefix: 'invalid-format'");
  }

  private List<ExternalIssue> executeSensorImporting(@Nullable String fileName) throws IOException {
    SensorContextTester context = ExternalReportTestUtils.createContext(PROJECT_DIR);
    if (fileName != null) {
      File reportFile = ExternalReportTestUtils.generateReport(PROJECT_DIR, tmp, fileName);
      context.settings().setProperty("sonar.java.checkstyle.reportPaths", reportFile.getPath());
    }
    checkstyleSensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

}
