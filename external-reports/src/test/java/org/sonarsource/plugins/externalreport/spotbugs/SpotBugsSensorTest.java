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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.externalreport.commons.ExternalRulesDefinition;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

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
    new ExternalRulesDefinition(SpotBugsSensor.ruleLoader()).define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository("external_spotbugs");
    assertThat(repository.name()).isEqualTo("SpotBugs");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.isExternal()).isEqualTo(true);

    assertThat(repository.rules().size()).isEqualTo(562);

    RulesDefinition.Rule rule = repository.rule("AM_CREATES_EMPTY_JAR_FILE_ENTRY");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Bad practice - Creates an empty jar file entry");
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.severity()).isEqualTo("MAJOR");
    assertThat(rule.htmlDescription()).isEqualTo("<p>The code calls <code>putNextEntry()</code>, immediately\n" +
      "followed by a call to <code>closeEntry()</code>. This results\n" +
      "in an empty JarFile entry. The contents of the entry\n" +
      "should be written to the JarFile between the calls to\n" +
      "<code>putNextEntry()</code> and\n" +
      "<code>closeEntry()</code>.</p>");
    assertThat(rule.tags()).containsExactlyInAnyOrder("bad-practice");
    assertThat(rule.debtRemediationFunction().baseEffort()).isEqualTo("1h");
  }

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    spotBugsSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of SpotBugs issues");
    assertThat(sensorDescriptor.languages()).containsOnly("java");
    assertNoErrorWarnDebugLogs(logTester);
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
    assertThat(first.ruleKey().rule()).isEqualTo("HE_EQUALS_USE_HASHCODE");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.remediationEffort().longValue()).isEqualTo(60L);
    assertThat(first.primaryLocation().message()).isEqualTo("org.myapp.Main defines equals and uses Object.hashCode()");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(6);

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
      .startsWith("FileNotFoundException:")
      .contains("invalid-path.txt (No such file or directory)")
      .endsWith("invalid-path.txt' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_spotbugs_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "not-spotbugs-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("IOException: Unexpected document root 'html' instead of 'BugCollection'.")
      .endsWith("not-spotbugs-file.xml' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_line_number() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "spotbugsXml-with-invalid-line.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("NumberFormatException: For input string: \"invalid\"")
      .endsWith("spotbugsXml-with-invalid-line.xml' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "invalid-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("XMLStreamException: ParseError at [row,col]:[2,1]")
      .endsWith("invalid-file.xml' can't be read.");
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
      "Unexpected empty 'BugCollection/BugInstance/SourceLine/@sourcepath' for bug 'HE_EQUALS_USE_HASHCODE'.");
  }

  private List<ExternalIssue> executeSensorImporting(int majorVersion, int minorVersion, @Nullable String fileName) throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, majorVersion, minorVersion);
    if (fileName != null) {
      File reportFile = generateReport(fileName);
      context.settings().setProperty("sonar.java.spotbugs.reportPaths", reportFile.getPath());
    }
    spotBugsSensor.execute(context);
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

    try (Stream<Path> pathStream = Files.walk(projectDir)) {
      pathStream
        .filter(path -> !path.toFile().isDirectory())
        .forEach(path -> addFileToContext(context, projectDir, path));
    }

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
