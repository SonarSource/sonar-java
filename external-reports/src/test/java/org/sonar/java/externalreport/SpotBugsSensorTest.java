/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
import org.slf4j.event.Level;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.java.externalreport.ExternalReportTestUtils.onlyOneLogElement;

@EnableRuleMigrationSupport
class SpotBugsSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "spotbugs")
    .toAbsolutePath().normalize();

  private static SpotBugsSensor spotBugsSensor = new SpotBugsSensor();

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void spotbugs_rules_definition() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new ExternalRulesDefinition(SpotBugsSensor.RULE_LOADER, SpotBugsSensor.SPOTBUGS_KEY).define(context);
    new ExternalRulesDefinition(SpotBugsSensor.FINDSECBUGS_LOADER, SpotBugsSensor.FINDSECBUGS_KEY).define(context);
    new ExternalRulesDefinition(SpotBugsSensor.FBCONTRIB_LOADER, SpotBugsSensor.FBCONTRIB_KEY).define(context);
    assertThat(context.repositories()).hasSize(3);

    RulesDefinition.Repository repository = context.repository("external_spotbugs");
    assertThat(repository.name()).isEqualTo("SpotBugs");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.isExternal()).isTrue();
    assertThat(repository.rules()).hasSizeGreaterThan(468);

    RulesDefinition.Rule rule = repository.rule("AM_CREATES_EMPTY_JAR_FILE_ENTRY");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Bad practice - Creates an empty jar file entry");
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.severity()).isEqualTo("MAJOR");
    assertThat(rule.htmlDescription()).isEqualTo("See description of SpotBugs rule <code>AM_CREATES_EMPTY_JAR_FILE_ENTRY</code> at the " +
      "<a href=\"https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#am-creates-empty-jar-file-entry\">SpotBugs website</a>.");
    assertThat(rule.debtRemediationFunction().baseEffort()).isEqualTo("5min");

    RulesDefinition.Repository findsecbugsRepo = context.repository("external_findsecbugs");
    assertThat(findsecbugsRepo.name()).isEqualTo("FindSecBugs");
    assertThat(findsecbugsRepo.language()).isEqualTo("java");
    assertThat(findsecbugsRepo.isExternal()).isTrue();
    repository = context.repository("external_findsecbugs");
    assertThat(repository.rules()).hasSizeGreaterThan(128);
  }

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    spotBugsSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of SpotBugs issues");
    assertThat(sensorDescriptor.languages()).containsOnly("java");
    ExternalReportTestUtils.assertNoErrorWarnDebugLogs(logTester);
  }


  @Test
  void expected_issues() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("spotbugsXml.xml");
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("spotbugs-project:src/main/java/org/myapp/Main.java");
    assertThat(first.engineId()).isEqualTo("spotbugs");
    assertThat(first.ruleId()).isEqualTo("HE_EQUALS_USE_HASHCODE");
    assertThat(first.ruleKey().rule()).isEqualTo("HE_EQUALS_USE_HASHCODE");
    assertThat(first.ruleKey().repository()).isEqualTo("external_spotbugs");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.remediationEffort().longValue()).isEqualTo(5L);
    assertThat(first.primaryLocation().message()).isEqualTo("org.myapp.Main defines equals and uses Object.hashCode()");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(6);

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  void findsecbugs_issue() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("spotbugsXml-findsecbugs.xml");
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("spotbugs-project:src/main/java/org/myapp/Main.java");
    assertThat(first.engineId()).isEqualTo("findsecbugs");
    assertThat(first.ruleId()).isEqualTo("RSA_KEY_SIZE");
    assertThat(first.ruleKey().rule()).isEqualTo("RSA_KEY_SIZE");
    assertThat(first.ruleKey().repository()).isEqualTo("external_findsecbugs");
    assertThat(first.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.remediationEffort().longValue()).isEqualTo(5L);
    assertThat(first.primaryLocation().message()).isEqualTo("org.myapp.Main defines equals and uses Object.hashCode()");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(6);

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  void fbcontrib_issue() throws Exception {
    List<ExternalIssue> externalIssues = executeSensorImporting("spotbugsXml-fbcontrib.xml");
    assertThat(externalIssues).hasSize(1);
    assertThat(externalIssues).extracting(ExternalIssue::engineId,
      ExternalIssue::ruleId,
      i -> i.primaryLocation().message(),
      i -> i.primaryLocation().textRange().start().line())
      .containsExactly(
        tuple("fbcontrib", "ABC_ARRAY_BASED_COLLECTIONS", "Method org.myapp.App.getGreeting(int[]) uses array as basis of collection", 14)
      );
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
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(Level.WARN)))
      .startsWith("SpotBugs report not found: ")
      .endsWith("invalid-path.txt");
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-spotbugs-file.xml", "spotbugsXml-with-invalid-line.xml", "invalid-file.xml"})
  void no_issues_with_invalid_report(String fileName) throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(fileName);
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(Level.ERROR)))
      .startsWith("Failed to import external issues report:")
      .endsWith(fileName);
  }

  @Test
  void issues_when_xml_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("spotbugsXml-with-errors.xml");
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("spotbugs-project:src/main/java/org/myapp/Main.java");
    assertThat(first.ruleKey().rule()).isEqualTo("UNKNOWN_RULE");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Message for unknown rule.");
    assertThat(first.primaryLocation().textRange()).isNull();

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(Level.WARN)))
      .startsWith("No input file found for '")
      .endsWith("not-existing-file.java'. No SpotBugs issues will be imported on this file.");
    assertThat(logTester.logs(Level.DEBUG)).containsExactlyInAnyOrder(
      "Unexpected empty 'BugCollection/BugInstance/@type'.",
      "Unexpected empty 'BugCollection/BugInstance/SourceLine/@sourcepath' for bug 'HE_EQUALS_USE_HASHCODE'.",
      "Unexpected empty 'BugCollection/BugInstance/LongMessage/text()' for bug 'NO_MESSAGE'");
  }

  @Test
  void no_issues_without_srcdir() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("spotbugsXml-without-srcdir.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).containsExactlyInAnyOrder(
      "Unexpected missing 'BugCollection/Project/SrcDir/text()'.");
  }

  private List<ExternalIssue> executeSensorImporting(@Nullable String fileName) throws IOException {
    SensorContextTester context = ExternalReportTestUtils.createContext(PROJECT_DIR);
    if (fileName != null) {
      File reportFile = ExternalReportTestUtils.generateReport(PROJECT_DIR, tmp, fileName);
      context.settings().setProperty("sonar.java.spotbugs.reportPaths", reportFile.getPath());
    }
    spotBugsSensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

}
