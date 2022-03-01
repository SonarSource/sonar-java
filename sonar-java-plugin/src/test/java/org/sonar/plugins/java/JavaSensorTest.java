/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.plugins.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.naming.BadMethodNameCheck;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JspCodeVisitor;
import org.sonarsource.performance.measure.PerformanceMeasure;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
class JavaSensorTest {

  private static final CheckFactory checkFactory = mock(CheckFactory.class);
  private static final Checks<Object> checks = mock(Checks.class);

  static {
    when(checks.addAnnotatedChecks(any(Iterable.class))).thenReturn(checks);
    when(checks.ruleKey(any(JavaCheck.class))).thenReturn(RuleKey.of("java", RuleAnnotationUtils.getRuleKey(BadMethodNameCheck.class)));
    when(checkFactory.create(anyString())).thenReturn(checks);
  }

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void test_toString() throws IOException {
    SonarComponents sonarComponents = createSonarComponentsMock(createContext(InputFile.Type.MAIN));
    assertThat(new JavaSensor(sonarComponents, null, null, null, null, null)).hasToString("JavaSensor");
  }

  @Test
  void test_issues_creation_on_main_file() throws IOException {
    testIssueCreation(InputFile.Type.MAIN, 11);
  }

  @Test
  void test_issues_creation_on_test_file() throws IOException { // NOSONAR required to test NOSONAR reporting on test files
    testIssueCreation(InputFile.Type.TEST, 0);
  }

  private void testIssueCreation(InputFile.Type onType, int expectedIssues) throws IOException {
    MapSettings settings = new MapSettings();
    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
    SensorContextTester context = createContext(onType).setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    DefaultFileSystem fs = context.fileSystem();
    fs.setWorkDir(tmp.newFolder().toPath());
    SonarComponents sonarComponents = createSonarComponentsMock(context);
    DefaultJavaResourceLocator javaResourceLocator = new DefaultJavaResourceLocator(new ClasspathForMain(settings.asConfig(), fs));
    JavaSensor jss = new JavaSensor(sonarComponents, fs, javaResourceLocator, settings.asConfig(), noSonarFilter, null);

    jss.execute(context);
    // argument 114 refers to the comment on line #114 in this file
    verify(noSonarFilter, times(1)).noSonarInFile(fs.inputFiles().iterator().next(), Collections.singleton(114));
    verify(sonarComponents, times(expectedIssues)).reportIssue(any(AnalyzerMessage.class));

    settings.setProperty(JavaVersion.SOURCE_VERSION, "wrongFormat");
    jss.execute(context);

    settings.setProperty(JavaVersion.SOURCE_VERSION, "1.7");
    jss.execute(context);
  }

  private static SensorContextTester createContext(InputFile.Type onType) throws IOException {
    SensorContextTester context = SensorContextTester.create(new File("src/test/java/").getAbsoluteFile());
    DefaultFileSystem fs = context.fileSystem();

    String effectiveKey = "org/sonar/plugins/java/JavaSensorTest.java";
    File file = new File(fs.baseDir(), effectiveKey);
    DefaultInputFile inputFile = new TestInputFileBuilder("", effectiveKey).setLanguage("java").setModuleBaseDir(fs.baseDirPath())
      .setType(onType)
      .initMetadata(new String(Files.readAllBytes(file.toPath()), UTF_8))
      .setCharset(UTF_8)
      .build();
    fs.add(inputFile);
    return context;
  }

  private static SonarComponents createSonarComponentsMock(SensorContextTester contextTester) {
    DefaultFileSystem fs = contextTester.fileSystem();
    ClasspathForTest javaTestClasspath = new ClasspathForTest(contextTester.config(), fs);
    ClasspathForMain javaClasspath = new ClasspathForMain(contextTester.config(), fs);

    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
    SonarComponents sonarComponents = spy(new SonarComponents(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory));
    sonarComponents.setSensorContext(contextTester);

    BadMethodNameCheck check = new BadMethodNameCheck();
    when(sonarComponents.mainChecks()).thenReturn(Collections.singletonList(check));
    return sonarComponents;
  }

  @Test
  void should_invoke_visitors_on_generated_code() throws Exception {
    Path base = tmp.newFolder().toPath();
    Path generatedFilePath = tmp.newFile("Generated.java").toPath();
    Files.write(generatedFilePath, "class Generated {}".getBytes());
    GeneratedFile generatedFile = new GeneratedFile(generatedFilePath);

    SensorContextTester context = SensorContextTester.create(base);
    context.fileSystem().setWorkDir(tmp.newFolder().toPath());
    SonarComponents sonarComponents = createSonarComponentsMock(context);
    JavaFileScanner javaFileScanner = mock(JavaFileScanner.class);
    JspCodeScanner testCodeVisitor = mock(JspCodeScanner.class);
    when(sonarComponents.jspChecks()).thenReturn(Collections.singletonList(testCodeVisitor));
    when(sonarComponents.mainChecks()).thenReturn(Collections.singletonList(javaFileScanner));

    Jasper jasper = mock(Jasper.class);
    when(jasper.generateFiles(any(), any())).thenReturn(asList(generatedFile));
    JavaSensor jss = new JavaSensor(sonarComponents, context.fileSystem(), mock(JavaResourceLocator.class),
      new MapSettings().asConfig(), mock(NoSonarFilter.class), null, jasper);
    jss.execute(context);

    ArgumentCaptor<JavaFileScannerContext> scannerContext = ArgumentCaptor.forClass(JavaFileScannerContext.class);
    verify(testCodeVisitor, times(1)).scanFile(scannerContext.capture());
    assertThat(scannerContext.getValue().getInputFile()).isSameAs(generatedFile);

    // normal visitors are not invoked on generated files
    verify(javaFileScanner, never()).scanFile(any());
  }

  @Test
  void should_not_invoke_jasper_jsp_compilation_in_batch_mode_for_security_reasons() throws Exception {
    Path base = tmp.newFolder().toPath();
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.internal.batchMode", "true");

    SensorContextTester context = SensorContextTester.create(base);
    context.setSettings(settings);
    context.fileSystem().setWorkDir(tmp.newFolder().toPath());
    SonarComponents sonarComponents = createSonarComponentsMock(context);
    JspCodeScanner jspCodeVisitor = mock(JspCodeScanner.class);
    when(sonarComponents.mainChecks()).thenReturn(Collections.emptyList());
    when(sonarComponents.testChecks()).thenReturn(Collections.emptyList());
    when(sonarComponents.jspChecks()).thenReturn(Collections.singletonList(jspCodeVisitor));

    Jasper jasper = mock(Jasper.class);
    JavaSensor jss = new JavaSensor(sonarComponents, context.fileSystem(), mock(JavaResourceLocator.class),
      context.config(), mock(NoSonarFilter.class), null, jasper);
    jss.execute(context);

    verify(jasper, never()).generateFiles(any(), any());
    verify(jspCodeVisitor, never()).scanFile(any());
  }

  @Test
  void insert_SymbolicExecutionVisitor_before_first_SECheck() throws IOException {
    List<JavaCheck> javaChecks = Arrays.asList(
      new org.sonar.java.checks.MagicNumberCheck(),
      new org.sonar.java.se.checks.NullDereferenceCheck(),
      new org.sonar.java.se.checks.DivisionByZeroCheck()
    );
    JavaCheck[] ordered = JavaSensor.insertSymbolicExecutionVisitor(javaChecks);
    assertThat(ordered).extracting(JavaCheck::getClass).extracting(Class::getSimpleName)
      .containsExactly(
        "MagicNumberCheck",
        "SymbolicExecutionVisitor",
        "NullDereferenceCheck",
        "DivisionByZeroCheck"
      );
  }

  @Test
  void does_not_insert_SymbolicExecutionVisitor() throws IOException {
    List<JavaCheck> javaChecks = Arrays.asList(
      new org.sonar.java.checks.MagicNumberCheck(),
    new org.sonar.java.checks.ParameterReassignedToCheck()
    );
    JavaCheck[] ordered = JavaSensor.insertSymbolicExecutionVisitor(javaChecks);
    assertThat(ordered).extracting(JavaCheck::getClass).extracting(Class::getSimpleName)
      .containsExactly(
        "MagicNumberCheck",
        "ParameterReassignedToCheck"
      );
  }

  @Test
  void performance_measure_should_not_be_activated_by_default() throws IOException {
    Loggers.get(PerformanceMeasure.class).setLevel(LoggerLevel.DEBUG);
    MapSettings settings = new MapSettings();
    Path workDir = tmp.newFolder().toPath();
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    String debugLogs = String.join("\n", logTester.logs(LoggerLevel.DEBUG));
    assertThat(debugLogs).doesNotContain("Performance Measures:");
    Path performanceFile = workDir.resolve("sonar.java.performance.measure.json");
    assertThat(performanceFile).doesNotExist();
  }

  @Test
  void performance_measure_should_log_in_debug_mode() throws IOException {
    Loggers.get(PerformanceMeasure.class).setLevel(LoggerLevel.DEBUG);
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.performance.measure", "true");
    Path workDir = tmp.newFolder().toPath();
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    String debugLogs = String.join("\n", logTester.logs(LoggerLevel.DEBUG));
    assertThat(debugLogs).contains("Performance Measures:\n{ \"name\": \"JavaSensor\"");
    Path performanceFile = workDir.resolve("sonar.java.performance.measure.json");
    assertThat(performanceFile).exists();
    assertThat(new String(Files.readAllBytes(performanceFile), UTF_8)).contains("\"JavaSensor\"");
  }

  @Test
  void custom_performance_measure_file_path_can_be_provided() throws IOException {
    Loggers.get(PerformanceMeasure.class).setLevel(LoggerLevel.DEBUG);
    MapSettings settings = new MapSettings();
    Path workDir = tmp.newFolder().toPath();
    Path customPerformanceFile = workDir.resolve("custom.performance.measure.json");
    settings.setProperty("sonar.java.performance.measure", "true");
    settings.setProperty("sonar.java.performance.measure.path", customPerformanceFile.toString());
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    String debugLogs = String.join("\n", logTester.logs(LoggerLevel.DEBUG));
    assertThat(debugLogs).contains("{ \"name\": \"JavaSensor\"");
    Path defaultPerformanceFile = workDir.resolve("sonar.java.performance.measure.json");
    assertThat(defaultPerformanceFile).doesNotExist();
    assertThat(customPerformanceFile).exists();
    assertThat(new String(Files.readAllBytes(customPerformanceFile), UTF_8)).contains("\"JavaSensor\"");
  }

  @Test
  void custom_performance_measure_file_path_can_be_empty() throws IOException {
    Loggers.get(PerformanceMeasure.class).setLevel(LoggerLevel.DEBUG);
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.performance.measure", "true");
    settings.setProperty("sonar.java.performance.measure.path", "");
    Path workDir = tmp.newFolder().toPath();
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    String debugLogs = String.join("\n", logTester.logs(LoggerLevel.DEBUG));
    assertThat(debugLogs).contains("{ \"name\": \"JavaSensor\"");
    Path defaultPerformanceFile = workDir.resolve("sonar.java.performance.measure.json");
    assertThat(defaultPerformanceFile).exists();
    assertThat(new String(Files.readAllBytes(defaultPerformanceFile), UTF_8)).contains("\"JavaSensor\"");
  }

  private void executeJavaSensorForPerformanceMeasure(MapSettings settings, Path workDir) throws IOException {
    Configuration configuration = settings.asConfig();
    SensorContextTester context = createContext(InputFile.Type.MAIN)
      .setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(8, 7), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY));
    context.setSettings(settings);
    DefaultFileSystem fs = context.fileSystem();
    fs.setWorkDir(workDir);
    SonarComponents components = createSonarComponentsMock(context);
    DefaultJavaResourceLocator resourceLocator = new DefaultJavaResourceLocator(new ClasspathForMain(configuration, fs));
    JavaSensor jss = new JavaSensor(components, fs, resourceLocator, configuration, mock(NoSonarFilter.class), null);
    jss.execute(context);
  }

  interface JspCodeScanner extends JavaFileScanner, JspCodeVisitor {
  }
}
