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
package org.sonar.java;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.LexerException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.java.TestUtils.computeLineEndOffsets;

@RunWith(MockitoJUnitRunner.class)
public class SonarComponentsTest {

  private static final Version V6_7 = Version.create(6, 7);

  private static final String REPOSITORY_NAME = "custom";

  @Mock
  private FileLinesContextFactory fileLinesContextFactory;

  @Mock
  private CheckFactory checkFactory;

  @Mock
  private Checks<JavaCheck> checks;

  @Mock
  private SensorContext context;

  @Before
  public void setUp() {
    // configure mocks that need verification
    when(this.checkFactory.<JavaCheck>create(anyString())).thenReturn(this.checks);
    when(this.checks.addAnnotatedChecks(any(Iterable.class))).thenReturn(this.checks);
  }

  public void postTestExecutionChecks() {
    // each time a SonarComponent is instantiated the following methods must be called twice
    // once for custom checks, once for custom java checks
    verify(this.checkFactory, times(2)).create(REPOSITORY_NAME);
    verify(this.checks, times(2)).addAnnotatedChecks(any(Iterable.class));
    verify(this.checks, times(2)).all();
  }

  @Test
  public void base_and_work_directories() {
    File baseDir = new File("");
    File workDir = new File("target");
    SensorContextTester context = SensorContextTester.create(baseDir);
    DefaultFileSystem fs = context.fileSystem();
    fs.setWorkDir(workDir.toPath());

    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, null, mock(JavaTestClasspath.class), checkFactory);

    assertThat(sonarComponents.workDir()).isEqualTo(workDir);
    assertThat(sonarComponents.baseDir()).isEqualTo(baseDir);
  }

  @Test
  public void test_sonar_components() {
    SensorContextTester sensorContextTester = spy(SensorContextTester.create(new File("")));
    DefaultFileSystem fs = sensorContextTester.fileSystem();
    JavaTestClasspath javaTestClasspath = mock(JavaTestClasspath.class);
    List<File> javaTestClasspathList = Collections.emptyList();
    when(javaTestClasspath.getElements()).thenReturn(javaTestClasspathList);
    InputFile inputFile = TestUtils.emptyInputFile("foo.java");
    fs.add(inputFile);
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, null, javaTestClasspath, checkFactory);
    sonarComponents.setSensorContext(sensorContextTester);

    JavaCheck[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(0);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(0);
    assertThat(sonarComponents.getJavaClasspath()).isEmpty();
    assertThat(sonarComponents.getJavaTestClasspath()).isEqualTo(javaTestClasspathList);
    NewHighlighting newHighlighting = sonarComponents.highlightableFor(inputFile);
    assertThat(newHighlighting).isNotNull();
    verify(sensorContextTester, times(1)).newHighlighting();
    NewSymbolTable newSymbolTable = sonarComponents.symbolizableFor(inputFile);
    assertThat(newSymbolTable ).isNotNull();
    verify(sensorContextTester, times(1)).newSymbolTable();
    assertThat(sonarComponents.fileLinesContextFor(inputFile)).isEqualTo(fileLinesContext);

    JavaClasspath javaClasspath = mock(JavaClasspath.class);
    List<File> list = mock(List.class);
    when(javaClasspath.getElements()).thenReturn(list);
    sonarComponents = new SonarComponents(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory);
    assertThat(sonarComponents.getJavaClasspath()).isEqualTo(list);
  }

  @Test
  public void creation_of_custom_checks() {
    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);

    when(this.checks.all()).thenReturn(Lists.newArrayList(expectedCheck)).thenReturn(new ArrayList<>());
    SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    JavaCheck[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(1);
    assertThat(visitors[0]).isEqualTo(expectedCheck);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(0);

    postTestExecutionChecks();
  }

  @Test
  public void creation_of_custom_test_checks() {
    JavaCheck expectedCheck = new CustomTestCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);

    when(checks.all()).thenReturn(new ArrayList<>()).thenReturn(Lists.newArrayList(expectedCheck));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, null, null, null, checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    JavaCheck[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(0);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(1);
    assertThat(testChecks.iterator().next()).isEqualTo(expectedCheck);

    postTestExecutionChecks();
  }

  @Test
  public void creation_of_both_types_test_checks() {
    JavaCheck expectedCheck = new CustomCheck();
    JavaCheck expectedTestCheck = new CustomTestCheck();
    CheckRegistrar expectedRegistrar = registrarContext -> registrarContext.registerClassesForRepository(
      REPOSITORY_NAME,
      Lists.<Class<? extends JavaCheck>>newArrayList(CustomCheck.class),
      Lists.<Class<? extends JavaCheck>>newArrayList(CustomTestCheck.class));

    when(this.checks.all()).thenReturn(Lists.newArrayList(expectedCheck)).thenReturn(Lists.newArrayList(expectedTestCheck));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, null, null, null, checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    JavaCheck[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(1);
    assertThat(visitors[0]).isEqualTo(expectedCheck);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(1);
    assertThat(testChecks.iterator().next()).isEqualTo(expectedTestCheck);
    assertThat(sonarComponents.checks()).hasSize(2);

    postTestExecutionChecks();
  }

  @Test
  public void no_issue_when_check_not_found() throws Exception {
    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);

    when(this.checks.ruleKey(any(JavaCheck.class))).thenReturn(null);
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, null, null, null, checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    sonarComponents.addIssue(TestUtils.emptyInputFile("file.java"), expectedCheck, 0, "message", null);
    verify(context, never()).newIssue();
  }

  @Test
  public void no_issue_if_file_not_found() throws Exception {
    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);

    DefaultFileSystem fileSystem = new DefaultFileSystem(new File(""));
    File file = new File("file.java");

    when(this.checks.ruleKey(any(JavaCheck.class))).thenReturn(mock(RuleKey.class));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fileSystem, null, null, checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    sonarComponents.addIssue(file, expectedCheck, 0, "message", null);
    verify(context, never()).newIssue();
  }

  @Test
  public void add_issue_or_parse_error() throws Exception {
    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);
    SensorContextTester context = SensorContextTester.create(new File("."));

    DefaultFileSystem fileSystem = context.fileSystem();
    TestInputFileBuilder inputFileBuilder = new TestInputFileBuilder("", "file.java");
    inputFileBuilder.setLines(45);
    int[] lineStartOffsets = new int[45];
    lineStartOffsets[35] = 12;
    lineStartOffsets[42] = 1;
    int lastValidOffset = 420;
    inputFileBuilder.setOriginalLineStartOffsets(lineStartOffsets);
    inputFileBuilder.setOriginalLineEndOffsets(computeLineEndOffsets(lineStartOffsets, lastValidOffset));
    inputFileBuilder.setLastValidOffset(lastValidOffset);
    InputFile inputFile = inputFileBuilder.build();
    fileSystem.add(inputFile);

    when(this.checks.ruleKey(any(JavaCheck.class))).thenReturn(mock(RuleKey.class));

    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fileSystem, null, null, checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    sonarComponents.addIssue(inputFile, expectedCheck, -5, "message on wrong line", null);
    sonarComponents.addIssue(inputFile, expectedCheck, 42, "message on line 42", 1);
    sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, inputFile, 35, "other message", 0));

    sonarComponents.addIssue(new File(fileSystem.baseDir().toString()), expectedCheck, -1, "message on project directory", 1);
    sonarComponents.addIssue(new File(".."), expectedCheck, -1, "message on non-project directory", 1);

    List<Issue> issues = new ArrayList<>(context.allIssues());
    assertThat(issues).hasSize(4);
    assertThat(issues.get(0).primaryLocation().message()).isEqualTo("message on wrong line");
    assertThat(issues.get(1).primaryLocation().message()).isEqualTo("message on line 42");
    assertThat(issues.get(2).primaryLocation().message()).isEqualTo("other message");
    assertThat(issues.get(3).primaryLocation().message()).isEqualTo("message on project directory");

    RecognitionException parseError = new RecognitionException(new LexerException("parse error"));

    context.setRuntime(SonarRuntimeImpl.forSonarLint(V6_7));
    assertThat(sonarComponents.reportAnalysisError(parseError, inputFile)).isTrue();

    context.setRuntime(SonarRuntimeImpl.forSonarQube(V6_7, SonarQubeSide.SCANNER));
    assertThat(sonarComponents.reportAnalysisError(parseError, inputFile)).isFalse();

  }

  @Test
  public void test_inputFromIOFile() {
    SensorContextTester context = SensorContextTester.create(new File("."));
    DefaultFileSystem fileSystem = context.fileSystem();

    File file = new File("file.java");
    fileSystem.add(TestUtils.emptyInputFile("file.java"));

    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);


    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fileSystem, null, null, checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    assertThat(sonarComponents.inputFromIOFile(file)).isNotNull();
    assertThat(sonarComponents.inputFromIOFileOrDirectory(file)).isNotNull();
    assertThat(sonarComponents.inputFromIOFileOrDirectory(new File("Unknown"))).isNull();
    assertThat(sonarComponents.inputFromIOFileOrDirectory(fileSystem.baseDir())).isNotNull();
    sonarComponents.setSensorContext(null);
    assertThat(sonarComponents.inputFromIOFileOrDirectory(fileSystem.baseDir())).isNull();
  }

  @Test
  public void fail_on_empty_location() {
    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);
    RuleKey ruleKey = RuleKey.of("MyRepo", "CustomCheck");

    InputFile inputFile = new TestInputFileBuilder("", "file.java")
    .initMetadata("class A {\n"
      + "  void foo() {\n"
      + "    System.out.println();\n"
      + "  }\n"
      + "}\n").build();

    SensorContextTester context = SensorContextTester.create(new File(""));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, context.fileSystem(), null, null, checkFactory, new CheckRegistrar[] {expectedRegistrar});
    sonarComponents.setSensorContext(context);

    AnalyzerMessage.TextSpan emptyTextSpan = new AnalyzerMessage.TextSpan(3, 10, 3, 10);
    AnalyzerMessage analyzerMessageEmptyLocation = new AnalyzerMessage(expectedCheck, inputFile, emptyTextSpan, "message", 0);

    assertThatThrownBy(() -> sonarComponents.reportIssue(analyzerMessageEmptyLocation, ruleKey, inputFile, 0.0))
      .isInstanceOf(IllegalStateException.class).hasMessageContaining("Issue location should not be empty");
    assertThat(context.allIssues()).isEmpty();

    AnalyzerMessage.TextSpan nonEmptyTextSpan = new AnalyzerMessage.TextSpan(3, 10, 3, 15);
    AnalyzerMessage analyzerMessageValidLocation = new AnalyzerMessage(expectedCheck, inputFile, nonEmptyTextSpan, "message", 0);
    sonarComponents.reportIssue(analyzerMessageValidLocation, ruleKey, inputFile, 0.0);
    assertThat(context.allIssues()).isNotEmpty();
  }

  @Test
  public void cancellation() {
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    SensorContextTester context = SensorContextTester.create(new File(""));
    sonarComponents.setSensorContext(context);

    context.setRuntime(SonarRuntimeImpl.forSonarLint(V6_7));
    assertThat(sonarComponents.analysisCancelled()).isFalse();

    // cancellation only handled from SQ 6.0
    context.setCancelled(true);

    assertThat(sonarComponents.analysisCancelled()).isTrue();
  }

  @Test
  public void readFileContentFromInputFile() throws Exception {
    // read a file containing kanji set with correct encoding and expecting proper length of read input.
    InputFile inputFile = spy(TestUtils.inputFile("src/test/files/Kanji.java"));

    SensorContextTester context = SensorContextTester.create(new File(""));
    DefaultFileSystem fileSystem = context.fileSystem();
    fileSystem.add(inputFile);
    fileSystem.setEncoding(StandardCharsets.ISO_8859_1);
    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, null, null, null);

    context.setRuntime(SonarRuntimeImpl.forSonarLint(V6_7));
    sonarComponents.setSensorContext(context);

    String fileContent = sonarComponents.inputFileContents(inputFile);
    assertThat(fileContent).hasSize(59);
    List<String> fileLines = sonarComponents.fileLines(inputFile);
    assertThat(fileLines).hasSize(5);
    assertThat(fileLines.get(0)).hasSize(11);

    verify(inputFile, times(1)).contents();
    reset(inputFile);
  }

  @Test
  public void io_error_when_reading_file_should_fail_analysis() {
    SensorContextTester context = SensorContextTester.create(new File(""));
    DefaultFileSystem fileSystem = context.fileSystem();
    InputFile unknownInputFile = TestUtils.emptyInputFile("unknown_file.java");
    fileSystem.add(unknownInputFile);
    context.setRuntime(SonarRuntimeImpl.forSonarLint(V6_7));
    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, null, null, null);
    sonarComponents.setSensorContext(context);

    try {
      sonarComponents.inputFileContents(unknownInputFile);
      fail("reading file content should have failed");
    } catch (AnalysisException e) {
      assertThat(e).hasMessage("Unable to read file 'unknown_file.java'").hasCauseInstanceOf(NoSuchFileException.class);
    } catch (Exception e) {
      fail("reading file content should have failed", e);
    }
    try {
      sonarComponents.fileLines(unknownInputFile);
      fail("reading file lines should have failed");
    } catch (AnalysisException e) {
      assertThat(e).hasMessage("Unable to read file 'unknown_file.java'").hasCauseInstanceOf(NoSuchFileException.class);
    } catch (Exception e) {
      fail("reading file content should have failed");
    }
  }

  private static CheckRegistrar getRegistrar(final JavaCheck expectedCheck) {
    return registrarContext -> registrarContext.registerClassesForRepository(REPOSITORY_NAME,
      Lists.<Class<? extends JavaCheck>>newArrayList(expectedCheck.getClass()), null);
  }

  private static class CustomCheck implements JavaCheck {

  }

  private static class CustomTestCheck implements JavaCheck {
  }

  @Test
  public void sonarcloud_feedback_metric_should_not_exceed_roughly_200ko() {
    File file = new File("src/test/files/ParseError.java");
    SensorContextTester sensorContext = SensorContextTester.create(file.getParentFile().getAbsoluteFile());
    sensorContext.setSettings(new MapSettings().setProperty(SonarComponents.COLLECT_ANALYSIS_ERRORS_KEY, true));
    Measure<String> feedback = analysisWithAnError(sensorContext);
    Collection<AnalysisError> analysisErrorsDeserialized = new Gson().fromJson(feedback.value(), new TypeToken<Collection<AnalysisError>>(){}.getType());
    // because we are storing stracktrace of the exception, number of exceptions we manage to serialize into given size can vary
    assertThat(analysisErrorsDeserialized.size()).isBetween(30, 45);
    assertThat(analysisErrorsDeserialized.iterator().next().getKind()).isEqualTo(AnalysisError.Kind.PARSE_ERROR);
  }

  private Measure<String> analysisWithAnError(SensorContextTester sensorContext) {
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(sensorContext);

    AnalysisError analysisError;
    try {
      throw new IllegalStateException("This is the message of this exception");
    } catch (IllegalStateException iae) {
      analysisError = new AnalysisError(iae, "/abcde/abcde/abcde/abcde/abcde/abcde/abcde/abcde/abcde/abcde/abcde/abcde/abcde/some_very/long/path/FileInError.java", AnalysisError.Kind.PARSE_ERROR);
    }

    for (int i = 0; i < 200_000; i++) {
      sonarComponents.addAnalysisError(analysisError);
    }

    sonarComponents.saveAnalysisErrors();

    return sensorContext.measure("projectKey", "sonarjava_feedback");
  }

  @Test
  public void feedback_should_not_be_sent_in_sonarLintContext_or_when_collecting_is_disabled_or_when_no_errors() {
    File file = new File("src/test/files/ParseError.java");
    SensorContextTester sensorContext = SensorContextTester.create(file.getParentFile().getAbsoluteFile());
    Measure<String> feedback = analysisWithAnError(sensorContext);
    assertThat(feedback).isNull();

    sensorContext = SensorContextTester.create(file.getParentFile().getAbsoluteFile());
    sensorContext.setSettings(new MapSettings().setProperty(SonarComponents.COLLECT_ANALYSIS_ERRORS_KEY, true));
    sensorContext.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    feedback = analysisWithAnError(sensorContext);
    assertThat(feedback).isNull();

    //analysis with no error
    sensorContext = SensorContextTester.create(file.getParentFile().getAbsoluteFile());
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(sensorContext);
    sonarComponents.saveAnalysisErrors();
    assertThat(sensorContext.measure("projectKey", "sonarjava_feedback")).isNull();

  }

}
