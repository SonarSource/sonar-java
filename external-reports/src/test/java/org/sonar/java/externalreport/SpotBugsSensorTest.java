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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.externalreport.ExternalReportTestUtils.onlyOneLogElement;

public class SpotBugsSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "spotbugs")
    .toAbsolutePath().normalize();

  private static SpotBugsSensor spotBugsSensor = new SpotBugsSensor();

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void spotbugs_rules_definition() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new ExternalRulesDefinition(SpotBugsSensor.RULE_LOADER, SpotBugsSensor.SPOTBUGS_KEY).define(context);
    new ExternalRulesDefinition(SpotBugsSensor.FINDSECBUGS_LOADER, SpotBugsSensor.FINDSECBUGS_KEY).define(context);
    assertThat(context.repositories()).hasSize(2);

    RulesDefinition.Repository repository = context.repository("external_spotbugs");
    assertThat(repository.name()).isEqualTo("SpotBugs");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.isExternal()).isEqualTo(true);
    assertThat(repository.rules().size()).isEqualTo(468);

    RulesDefinition.Rule rule = repository.rule("AM_CREATES_EMPTY_JAR_FILE_ENTRY");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Bad practice - Creates an empty jar file entry");
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.severity()).isEqualTo("MAJOR");
    assertThat(rule.htmlDescription()).isEqualTo("See description of SpotBugs rule <code>AM_CREATES_EMPTY_JAR_FILE_ENTRY</code> at the " +
      "<a href=\"https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#AM_CREATES_EMPTY_JAR_FILE_ENTRY\">SpotBugs website</a>.");
    assertThat(rule.debtRemediationFunction().baseEffort()).isEqualTo("5min");

    RulesDefinition.Repository findsecbugsRepo = context.repository("external_findsecbugs");
    assertThat(findsecbugsRepo.name()).isEqualTo("FindSecBugs");
    assertThat(findsecbugsRepo.language()).isEqualTo("java");
    assertThat(findsecbugsRepo.isExternal()).isEqualTo(true);
    repository = context.repository("external_findsecbugs");
    assertThat(repository.rules().size()).isEqualTo(128);
  }

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    spotBugsSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of SpotBugs issues");
    assertThat(sensorDescriptor.languages()).containsOnly("java");
    ExternalReportTestUtils.assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void no_issues_with_sonarqube_71() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 1, "spotbugsXml.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactly("Import of external issues requires SonarQube 7.2 or greater.");
  }

  @Test
  public void issues_with_sonarqube_72() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "spotbugsXml.xml");
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

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  public void findsecbugs_issue() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "spotbugsXml-findsecbugs.xml");
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

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  public void no_issues_without_report_paths_property() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, null);
    assertThat(externalIssues).isEmpty();
    ExternalReportTestUtils.assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void no_issues_with_invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.WARN)))
      .startsWith("SpotBugs report not found: ")
      .endsWith("invalid-path.txt");
  }

  @Test
  public void no_issues_with_invalid_spotbugs_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "not-spotbugs-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("Failed to import external issues report:")
      .endsWith("not-spotbugs-file.xml");
  }

  @Test
  public void no_issues_with_invalid_line_number() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "spotbugsXml-with-invalid-line.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("Failed to import external issues report:")
      .endsWith("spotbugsXml-with-invalid-line.xml");
  }

  @Test
  public void no_issues_with_invalid_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "invalid-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("Failed to import external issues report:")
      .endsWith("invalid-file.xml");
  }

  @Test
  public void issues_when_xml_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "spotbugsXml-with-errors.xml");
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("spotbugs-project:src/main/java/org/myapp/Main.java");
    assertThat(first.ruleKey().rule()).isEqualTo("UNKNOWN_RULE");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Message for unknown rule.");
    assertThat(first.primaryLocation().textRange()).isNull();

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.WARN)))
      .startsWith("No input file found for '")
      .endsWith("not-existing-file.java'. No SpotBugs issues will be imported on this file.");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
      "Unexpected empty 'BugCollection/BugInstance/@type'.",
      "Unexpected empty 'BugCollection/BugInstance/SourceLine/@sourcepath' for bug 'HE_EQUALS_USE_HASHCODE'.",
      "Unexpected empty 'BugCollection/BugInstance/LongMessage/text()' for bug 'NO_MESSAGE'");
  }

  @Test
  public void no_issues_without_srcdir() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "spotbugsXml-without-srcdir.xml");
    assertThat(externalIssues).hasSize(0);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
      "Unexpected missing 'BugCollection/Project/SrcDir/text()'.");
  }

  private List<ExternalIssue> executeSensorImporting(int majorVersion, int minorVersion, @Nullable String fileName) throws IOException {
    SensorContextTester context = ExternalReportTestUtils.createContext(PROJECT_DIR, majorVersion, minorVersion);
    if (fileName != null) {
      File reportFile = ExternalReportTestUtils.generateReport(PROJECT_DIR, tmp, fileName);
      context.settings().setProperty("sonar.java.spotbugs.reportPaths", reportFile.getPath());
    }
    spotBugsSensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

}
