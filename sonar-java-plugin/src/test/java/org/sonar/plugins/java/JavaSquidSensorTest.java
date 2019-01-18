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
package org.sonar.plugins.java;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.utils.Version;
import org.sonar.java.AnalysisError;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaTestClasspath;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.naming.BadMethodNameCheck;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JavaSquidSensorTest {

  private static final CheckFactory checkFactory = mock(CheckFactory.class);
  private static final Checks<Object> checks = mock(Checks.class);
  private static final Server server = mock(Server.class);

  static {
    when(checks.addAnnotatedChecks(any(Iterable.class))).thenReturn(checks);
    when(checks.ruleKey(any(JavaCheck.class))).thenReturn(RuleKey.of("squid", RuleAnnotationUtils.getRuleKey(BadMethodNameCheck.class)));
    when(checkFactory.create(anyString())).thenReturn(checks);
  }

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void test_toString() {
    assertThat(new JavaSquidSensor(null, null, null, null, null, null).toString()).isEqualTo("JavaSquidSensor");
  }

  @Test
  public void test_issues_creation_on_main_file() throws IOException {
    testIssueCreation(InputFile.Type.MAIN, 7); // the number of test in this class - their names are not matching the regex of BadMethodNameCheck
  }

  @Test
  public void test_issues_creation_on_test_file() throws IOException { // NOSONAR required to test NOSONAR reporting on test files
    testIssueCreation(InputFile.Type.TEST, 0);
  }

  private void testIssueCreation(InputFile.Type onType, int expectedIssues) throws IOException {
    MapSettings settings = new MapSettings();
    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
    SensorContextTester context = createContext(onType).setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    DefaultFileSystem fs = context.fileSystem();
    SonarComponents sonarComponents = createSonarComponentsMock(context);
    DefaultJavaResourceLocator javaResourceLocator = new DefaultJavaResourceLocator(fs, new JavaClasspath(settings.asConfig(), fs));
    PostAnalysisIssueFilter postAnalysisIssueFilter = new PostAnalysisIssueFilter(fs);
    JavaSquidSensor jss = new JavaSquidSensor(sonarComponents, fs, javaResourceLocator, settings.asConfig(), noSonarFilter, postAnalysisIssueFilter);

    jss.execute(context);
    verify(noSonarFilter, times(1)).noSonarInFile(fs.inputFiles().iterator().next(), Sets.newHashSet(98)); // line of N0S0NAR
    verify(sonarComponents, times(expectedIssues)).reportIssue(any(AnalyzerMessage.class));

    settings.setProperty(Java.SOURCE_VERSION, "wrongFormat");
    jss.execute(context);

    settings.setProperty(Java.SOURCE_VERSION, "1.7");
    jss.execute(context);
  }

  private static SensorContextTester createContext(InputFile.Type onType) throws IOException {
    SensorContextTester context = SensorContextTester.create(new File("src/test/java/").getAbsoluteFile());
    DefaultFileSystem fs = context.fileSystem();

    String effectiveKey = "org/sonar/plugins/java/JavaSquidSensorTest.java";
    File file = new File(fs.baseDir(), effectiveKey);
    DefaultInputFile inputFile = new TestInputFileBuilder("", effectiveKey).setLanguage("java").setModuleBaseDir(fs.baseDirPath())
      .setType(onType)
      .initMetadata(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
      .setCharset(StandardCharsets.UTF_8)
      .build();
    fs.add(inputFile);
    return context;
  }

  private static SonarComponents createSonarComponentsMock(SensorContextTester contextTester) {
    Configuration settings = new MapSettings().asConfig();
    DefaultFileSystem fs = contextTester.fileSystem();
    JavaTestClasspath javaTestClasspath = new JavaTestClasspath(settings, fs);
    JavaClasspath javaClasspath = new JavaClasspath(settings, fs);

    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
    SonarComponents sonarComponents = spy(new SonarComponents(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory));
    sonarComponents.setSensorContext(contextTester);

    BadMethodNameCheck check = new BadMethodNameCheck();
    when(sonarComponents.checkClasses()).thenReturn(new JavaCheck[] {check});
    return sonarComponents;
  }

  @Test
  public void verify_analysis_errors_are_collected_on_parse_error() throws Exception {
    SensorContextTester context = createParseErrorContext();
    context.settings().setProperty(SonarComponents.COLLECT_ANALYSIS_ERRORS_KEY, true);
    executeJavaSquidSensor(context);

    String feedback = context.<String>measure("projectKey", "sonarjava_feedback").value();
    Collection<AnalysisError> analysisErrors = new Gson().fromJson(feedback, new TypeToken<Collection<AnalysisError>>(){}.getType());
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.getMessage()).startsWith("Parse error at line 6 column 1:");
    assertThat(analysisError.getCause()).startsWith("com.sonar.sslr.api.RecognitionException: Parse error at line 6 column 1:");
    assertThat(analysisError.getFilename()).endsWith("ParseError.java");
    assertThat(analysisError.getKind()).isEqualTo(AnalysisError.Kind.PARSE_ERROR);
  }

  private SensorContextTester createParseErrorContext() throws IOException {
    File file = new File("src/test/files/ParseError.java");
    SensorContextTester context = SensorContextTester.create(file.getParentFile().getAbsoluteFile());

    DefaultInputFile defaultFile = new TestInputFileBuilder(file.getParentFile().getPath(), file.getName())
      .setLanguage("java")
      .initMetadata(new String(Files.readAllBytes(file.getAbsoluteFile().toPath()), StandardCharsets.UTF_8))
      .setCharset(StandardCharsets.UTF_8)
      .build();
    context.fileSystem().add(defaultFile);
    return context;
  }

  private void executeJavaSquidSensor(SensorContextTester context) {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(6, 7), SonarQubeSide.SCANNER));
    // Mock visitor for metrics.
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    DefaultFileSystem fs = context.fileSystem().setWorkDir(tmp.getRoot().toPath());
    JavaClasspath javaClasspath = mock(JavaClasspath.class);
    JavaTestClasspath javaTestClasspath = mock(JavaTestClasspath.class);
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory);
    DefaultJavaResourceLocator javaResourceLocator = mock(DefaultJavaResourceLocator.class);
    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
    PostAnalysisIssueFilter postAnalysisIssueFilter = new PostAnalysisIssueFilter(fs);

    JavaSquidSensor jss = new JavaSquidSensor(sonarComponents, fs, javaResourceLocator, new MapSettings().asConfig(),noSonarFilter, postAnalysisIssueFilter);
    jss.execute(context);
  }

  @Test
  public void feedback_should_not_be_fed_if_no_errors() throws IOException {
    SensorContextTester context = createContext(InputFile.Type.MAIN);
    context.settings().setProperty(SonarComponents.COLLECT_ANALYSIS_ERRORS_KEY, true);
    executeJavaSquidSensor(context);
    assertThat(context.<String>measure("projectKey", "sonarjava_feedback")).isNull();
  }

  @Test
  public void feedback_should_not_be_fed_if_not_SonarCloud_Host() throws IOException {
    SensorContextTester context = createParseErrorContext();
    executeJavaSquidSensor(context);
    assertThat(context.<String>measure("projectKey", "sonarjava_feedback")).isNull();
  }

  @Test
  public void test_issues_creation_on_test_file_when_tests_are_First_Citizens() throws IOException {
    MapSettings settings = new MapSettings();
    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);

    SensorContextTester context = SensorContextTester.create(new File("src/test/files/").getAbsoluteFile());
    DefaultFileSystem fs = context.fileSystem();

    String effectiveKey = "TestSourceTest.java";
    File file = new File(fs.baseDir(), effectiveKey);
    DefaultInputFile inputFile = new TestInputFileBuilder("", effectiveKey).setLanguage("java").setModuleBaseDir(fs.baseDirPath())
      .setType(InputFile.Type.TEST)
      .initMetadata(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
      .setCharset(StandardCharsets.UTF_8)
      .build();
    fs.add(inputFile);
    context.setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(6, 7), SonarQubeSide.SCANNER));

    settings.setProperty(Java.TESTS_AS_FIRST_CITIZEN, "true");

    SonarComponents sonarComponents = createSonarComponentsMock(context);
    DefaultJavaResourceLocator javaResourceLocator = new DefaultJavaResourceLocator(fs, new JavaClasspath(settings.asConfig(), fs));
    PostAnalysisIssueFilter postAnalysisIssueFilter = new PostAnalysisIssueFilter(fs);

    JavaSquidSensor jss = new JavaSquidSensor(sonarComponents, fs, javaResourceLocator, settings.asConfig(), noSonarFilter, postAnalysisIssueFilter);

    jss.execute(context);
    // 2 issue from BadMethodName rule, usually applied on MAIN, but filtered on TEST
    verify(sonarComponents, times(2)).reportIssue(any(AnalyzerMessage.class));
    // one issue filtered out -> only 1 accepted issue
    assertThat(context.allIssues().stream()
      .map(JavaSquidSensorTest::mockFilterableIssue)
      .filter(issue -> postAnalysisIssueFilter.accept(issue, i -> true)))
        .hasSize(1);
    // LOC are not counted for test files normally
    assertThat(context.<Integer>measure(inputFile.key(), "ncloc").value()).isEqualTo(7);
  }

  private static FilterableIssue mockFilterableIssue(Issue issue) {
    FilterableIssue result = mock(FilterableIssue.class);
    when(result.ruleKey()).thenReturn(issue.ruleKey());
    when(result.line()).thenReturn(issue.primaryLocation().textRange().start().line());
    when(result.componentKey()).thenReturn(issue.primaryLocation().inputComponent().key());
    return result;
  }

}
