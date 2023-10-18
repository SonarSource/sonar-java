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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
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
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.externalreport.ExternalReportTestUtils.onlyOneLogElement;

class PmdSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "pmd");
  private static final String PROJECT_ID = "pmd-test";

  private static final PmdSensor sensor = new PmdSensor();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of PMD issues");
    assertThat(sensorDescriptor.languages()).containsOnly("java");
    MapSettings settings = new MapSettings();
    assertThat(sensorDescriptor.configurationPredicate().test(settings.asConfig())).isFalse();
    settings.setProperty(PmdSensor.REPORT_PROPERTY_KEY, "report.xml");
    assertThat(sensorDescriptor.configurationPredicate().test(settings.asConfig())).isTrue();
  }

  @Test
  void pmd_rules_definition() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new ExternalRulesDefinition(PmdSensor.RULE_LOADER, PmdSensor.LINTER_KEY).define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository("external_pmd");
    assertThat(repository.name()).isEqualTo("PMD");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.isExternal()).isTrue();

    assertThat(repository.rules()).hasSizeGreaterThan(288);

    RulesDefinition.Rule rule = repository.rule("EqualsNull");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Equals null");
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.htmlDescription()).isEqualTo(
      "See description of PMD rule <code>EqualsNull</code> at the <a href=\"https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#equalsnull\">PMD website</a>.");
    assertThat(rule.tags()).isEmpty();
  }

  @Test
  void no_report_path_set() throws IOException {
    List<ExternalIssue> externalIssues = execute(null);
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = execute("invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(Level.WARN)))
      .startsWith("PMD report not found: ")
      .endsWith("invalid-path.txt");
  }

  @Test
  void not_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = execute("hello.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(Level.ERROR).get(0)).startsWith("Failed to import external issues report:");
  }

  @Test
  void skip_issue_on_invalid_priority() throws IOException {
    List<ExternalIssue> externalIssues = execute("invalid-severity.xml");
    assertThat(externalIssues).hasSize(1);
    assertThat(logTester.logs(Level.WARN).get(0))
      .contains("Can't import issue at line 8")
      .contains("invalid-severity.xml");
    assertThat(logTester.logs(Level.WARN).get(1))
      .contains("Can't import issue at line 9")
      .contains("invalid-severity.xml");
  }

  @Test
  void invalid_text_range() throws IOException {
    List<ExternalIssue> externalIssues = execute("invalid-text-range.xml");
    assertThat(externalIssues).hasSize(2);
    TextRange secondIssueRange = externalIssues.get(1).primaryLocation().textRange();
    assertThat(secondIssueRange).isNotNull();
    assertThat(secondIssueRange.start().line()).isEqualTo(4);
    assertThat(secondIssueRange.end().line()).isEqualTo(4);
    assertThat(logTester.logs(Level.WARN).get(0))
      .contains("Can't import issue at line 9")
      .contains("invalid-text-range.xml");
  }

  @Test
  void issues() throws IOException {
    List<ExternalIssue> externalIssues = execute("pmd-report.xml");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);

    ExternalIssueAssert.assertThat(first)
      .hasFileName(PROJECT_ID + ":file1.java")
      .hasEngineId("pmd")
      .hasRuleId("UnusedFormalParameter")
      .hasRuleKey("UnusedFormalParameter")
      .hasRuleType(RuleType.CODE_SMELL)
      .hasSeverity(Severity.MAJOR)
      .hasMessage("Avoid unused constructor parameters such as 'arg2'.")
      .hasTextRange(new DefaultTextRange(new DefaultTextPointer(3, 34), new DefaultTextPointer(3, 38)))
      .hasRemediationEffort(5)
      .verify();

    ExternalIssue second = externalIssues.get(1);

    ExternalIssueAssert.assertThat(second)
      .hasFileName(PROJECT_ID + ":file1.java")
      .hasRuleKey("UnusedLocalVariable")
      .hasRuleType(RuleType.CODE_SMELL)
      .hasSeverity(Severity.MAJOR)
      .hasMessage("Avoid unused local variables such as 'x'.")
      .hasTextRange(new DefaultTextRange(new DefaultTextPointer(4, 8), new DefaultTextPointer(5, 10)))
      .hasRemediationEffort(5)
      .verify();

    ExternalIssue third = externalIssues.get(2);

    ExternalIssueAssert.assertThat(third)
      .hasFileName(PROJECT_ID + ":file2.java")
      .hasRuleKey("UnusedPrivateMethod")
      .hasRuleType(RuleType.CODE_SMELL)
      .hasSeverity(Severity.CRITICAL)
      .hasMessage("Avoid unused private methods such as 'privateMethod()'.")
      .hasTextRangeStartLine(5)
      .hasRemediationEffort(5)
      .verify();

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).containsExactly("No input file found for unknown-file.java. No PMD issue will be imported on this file.");
  }

  private List<ExternalIssue> execute(@Nullable String fileName) throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR);
    if (fileName != null) {
      String path = PROJECT_DIR.resolve(fileName).toAbsolutePath().toString();
      context.settings().setProperty(PmdSensor.REPORT_PROPERTY_KEY, path);
    }
    sensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

  public static SensorContextTester createContext(Path projectDir) throws IOException {
    SensorContextTester context = SensorContextTester.create(projectDir);
    Files.list(projectDir)
      .filter(Files::isRegularFile)
      .forEach(file -> addFileToContext(context, projectDir, file));
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
