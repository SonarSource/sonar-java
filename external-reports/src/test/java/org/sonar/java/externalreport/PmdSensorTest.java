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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.externalreport.ExternalReportTestUtils.onlyOneLogElement;

public class PmdSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "pmd");
  private static final String PROJECT_ID = "pmd-test";

  private static final SonarRuntime SQ71 = SonarRuntimeImpl.forSonarQube(Version.create(7, 1), SonarQubeSide.SERVER);
  private static final SonarRuntime SQ72 = SonarRuntimeImpl.forSonarQube(Version.create(7, 2), SonarQubeSide.SERVER);

  private static PmdSensor sensor = new PmdSensor();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of PMD issues");
    MapSettings settings = new MapSettings();
    assertThat(sensorDescriptor.configurationPredicate().test(settings.asConfig())).isFalse();
    settings.setProperty(PmdSensor.REPORT_PROPERTY_KEY, "report.xml");
    assertThat(sensorDescriptor.configurationPredicate().test(settings.asConfig())).isTrue();
  }

  @Test
  public void pmd_rules_definition() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new ExternalRulesDefinition(PmdSensor.RULE_LOADER, PmdSensor.LINTER_KEY).define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository("external_pmd");
    assertThat(repository.name()).isEqualTo("PMD");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.isExternal()).isEqualTo(true);

    assertThat(repository.rules().size()).isEqualTo(288);

    RulesDefinition.Rule rule = repository.rule("EqualsNull");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Equals null");
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.htmlDescription()).isEqualTo(
      "See description of PMD rule <code>EqualsNull</code> at the <a href=\"https://pmd.github.io/pmd-6.5.0/pmd_rules_java_errorprone.html#equalsnull\">PMD website</a>.");
    assertThat(rule.tags()).isEmpty();
  }

  @Test
  public void no_report_path_set() throws IOException {
    List<ExternalIssue> externalIssues = execute(SQ72, null);
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = execute(SQ72, "invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.WARN)))
      .startsWith("PMD report not found: ")
      .endsWith("invalid-path.txt");
  }

  @Test
  public void not_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = execute(SQ72, "hello.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).startsWith("Failed to import external issues report:");
  }

  @Test
  public void skip_issue_on_invalid_priority() throws IOException {
    List<ExternalIssue> externalIssues = execute(SQ72, "invalid-severity.xml");
    assertThat(externalIssues).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN).get(0))
      .contains("Can't import issue at line 8")
      .contains("invalid-severity.xml");
    assertThat(logTester.logs(LoggerLevel.WARN).get(1))
      .contains("Can't import issue at line 9")
      .contains("invalid-severity.xml");
  }

  @Test
  public void invalid_text_range() throws IOException {
    List<ExternalIssue> externalIssues = execute(SQ72, "invalid-text-range.xml");
    assertThat(externalIssues).hasSize(2);
    TextRange secondIssueRange = externalIssues.get(1).primaryLocation().textRange();
    assertThat(secondIssueRange).isNotNull();
    assertThat(secondIssueRange.start().line()).isEqualTo(4);
    assertThat(secondIssueRange.end().line()).isEqualTo(4);
    assertThat(logTester.logs(LoggerLevel.WARN).get(0))
      .contains("Can't import issue at line 9")
      .contains("invalid-text-range.xml");
  }

  @Test
  public void no_issues_with_sonarqube_71() throws IOException {
    List<ExternalIssue> externalIssues = execute(SQ71, "pmd-report.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactly("Import of external issues requires SonarQube 7.2 or greater.");
  }

  @Test
  public void issues() throws IOException {
    List<ExternalIssue> externalIssues = execute(SQ72, "pmd-report.xml");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo(PROJECT_ID + ":file1.java");
    assertThat(first.engineId()).isEqualTo("pmd");
    assertThat(first.ruleId()).isEqualTo("UnusedFormalParameter");
    assertThat(first.ruleKey().rule()).isEqualTo("UnusedFormalParameter");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Avoid unused constructor parameters such as 'arg2'.");
    assertThat(first.primaryLocation().textRange()).isEqualTo(
      new DefaultTextRange(new DefaultTextPointer(3, 34), new DefaultTextPointer(3, 38)));
    assertThat(first.remediationEffort()).isEqualTo(5);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo(PROJECT_ID + ":file1.java");
    assertThat(second.ruleKey().rule()).isEqualTo("UnusedLocalVariable");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.primaryLocation().message()).isEqualTo("Avoid unused local variables such as 'x'.");
    assertThat(second.primaryLocation().textRange()).isEqualTo(
      new DefaultTextRange(new DefaultTextPointer(4, 8), new DefaultTextPointer(5, 10)));
    assertThat(second.remediationEffort()).isEqualTo(5);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo(PROJECT_ID + ":file2.java");
    assertThat(third.ruleKey().rule()).isEqualTo("UnusedPrivateMethod");
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(third.primaryLocation().message()).isEqualTo("Avoid unused private methods such as 'privateMethod()'.");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(5);
    assertThat(third.remediationEffort()).isEqualTo(5);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsExactly("No input file found for unknown-file.java. No PMD issue will be imported on this file.");
  }

  private List<ExternalIssue> execute(SonarRuntime sonarRuntime, @Nullable String fileName) throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, sonarRuntime);
    if (fileName != null) {
      String path = PROJECT_DIR.resolve(fileName).toAbsolutePath().toString();
      context.settings().setProperty(PmdSensor.REPORT_PROPERTY_KEY, path);
    }
    sensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

  public static SensorContextTester createContext(Path projectDir, SonarRuntime sonarRuntime) throws IOException {
    SensorContextTester context = SensorContextTester.create(projectDir);
    Files.list(projectDir)
      .forEach(file -> addFileToContext(context, projectDir, file));
    context.setRuntime(sonarRuntime);
    return context;
  }

  private static void addFileToContext(SensorContextTester context, Path projectDir, Path file) {
    try {
      context.fileSystem().add(TestInputFileBuilder.create(PROJECT_ID, projectDir.toFile(), file.toFile())
        .setCharset(UTF_8)
        .setLanguage(language(file))
        .setContents(new String(Files.readAllBytes(file), UTF_8))
        .setType(InputFile.Type.MAIN)
        .build());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String language(Path file) {
    String path = file.toString();
    return path.substring(path.lastIndexOf('.') + 1);
  }
}
