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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class CheckstyleSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "files", "checkstyle");

  private static CheckstyleSensor checkstyleSensor = new CheckstyleSensor();

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    checkstyleSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of Checkstyle issues");
    assertThat(sensorDescriptor.languages()).containsOnly("java");
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void no_issues_with_sonarqube_71() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 1, "checkstyle-result.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactly("Import of external issues requires SonarQube 7.2 or greater.");
  }

  @Test
  public void issues_with_sonarqube_72() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "checkstyle-result.xml");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("checkstyle-project:Main.java");
    assertThat(first.ruleKey().rule()).isEqualTo("javadoc.JavadocPackageCheck");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MINOR);
    assertThat(first.remediationEffort().longValue()).isEqualTo(30L);
    assertThat(first.primaryLocation().message()).isEqualTo("Missing package-info.java file.");
    assertThat(first.primaryLocation().textRange()).isNull();

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("checkstyle-project:Main.java");
    assertThat(second.ruleKey().rule()).isEqualTo("modifier.ModifierOrderCheck");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.MINOR);
    assertThat(second.remediationEffort().longValue()).isEqualTo(10L);
    assertThat(second.primaryLocation().message()).isEqualTo("'static' modifier out of order with the JLS suggestions.");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(2);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("checkstyle-project:A.java");
    assertThat(third.ruleKey().rule()).isEqualTo("javadoc.JavadocTypeCheck");
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.remediationEffort().longValue()).isEqualTo(10L);
    assertThat(third.primaryLocation().message()).isEqualTo("Missing a Javadoc comment.");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(1);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  public void no_issues_without_report_paths_property() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, null);
    assertThat(externalIssues).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void no_issues_with_invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("invalid-path.txt' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_checkstyle_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "not-checkstyle-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("not-checkstyle-file.xml' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "invalid-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("invalid-file.xml' can't be read.");
  }

  @Test
  public void issues_when_xml_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "checkstyle-with-errors.xml");
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
      "Unexpected rule key without 'com.puppycrawl.tools.checkstyle.checks.' suffix: 'invalid-format'");
  }

  private List<ExternalIssue> executeSensorImporting(int majorVersion, int minorVersion, @Nullable String fileName) throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, majorVersion, minorVersion);
    if (fileName != null) {
      File reportFile = generateReport(fileName);
      context.settings().setProperty("sonar.java.checkstyle.reportPaths", reportFile.getPath());
    }
    checkstyleSensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

  private File generateReport(String fileName) throws IOException {
    Path filePath = PROJECT_DIR.resolve(fileName);
    if (!filePath.toFile().exists()) {
      return filePath.toFile();
    }
    String reportData = new String(Files.readAllBytes(filePath), UTF_8);
    reportData = reportData.replace("${PROJECT_DIR}", PROJECT_DIR.toRealPath() + File.separator);
    File reportFile = tmp.newFile(fileName).getCanonicalFile();
    Files.write(reportFile.toPath(), reportData.getBytes(UTF_8));
    return reportFile;
  }

  public static SensorContextTester createContext(Path projectDir, int majorVersion, int minorVersion) throws IOException {
    SensorContextTester context = SensorContextTester.create(projectDir);
    Files.list(projectDir)
      .forEach(file -> addFileToContext(context, projectDir, file));
    context.setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(majorVersion, minorVersion), SonarQubeSide.SERVER));
    return context;
  }

  public static void assertNoErrorWarnDebugLogs(LogTester logTester) {
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  public static String onlyOneLogElement(List<String> elements) {
    assertThat(elements).hasSize(1);
    return elements.get(0);
  }

  private static void addFileToContext(SensorContextTester context, Path projectDir, Path file) {
    try {
      String projectId = projectDir.getFileName().toString() + "-project";
      context.fileSystem().add(TestInputFileBuilder.create(projectId, projectDir.toFile(), file.toFile())
        .setCharset(UTF_8)
        .setLanguage(file.toString().substring(file.toString().lastIndexOf('.') + 1))
        .setContents(new String(Files.readAllBytes(file), UTF_8))
        .setType(InputFile.Type.MAIN)
        .build());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

}
