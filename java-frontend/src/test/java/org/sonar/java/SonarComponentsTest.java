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
package org.sonar.java;

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.Version;
import org.sonar.check.Rule;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.exceptions.ApiMismatchException;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JspCodeVisitor;
import org.sonarsource.sonarlint.core.plugin.commons.sonarapi.SonarLintRuntimeImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.java.TestUtils.computeLineEndOffsets;

@ExtendWith(MockitoExtension.class)
class SonarComponentsTest {

  private static final Version V8_9 = Version.create(8, 9);

  private static final String REPOSITORY_NAME = "custom";

  private static final String LOG_MESSAGE_FILES_CAN_BE_SKIPPED = "The Java analyzer is running in a context where unchanged files can be skipped. Full analysis is performed " +
    "for changed files, optimized analysis for unchanged files.";
  private static final String LOG_MESSAGE_FILES_CANNOT_BE_SKIPPED = "The Java analyzer cannot skip unchanged files in this context. A full analysis is performed for all files.";
  private static final String LOG_MESSAGE_CANNOT_DETERMINE_IF_FILES_CAN_BE_SKIPPED = "Cannot determine whether the context allows skipping unchanged files: canSkipUnchangedFiles not part of sonar-plugin-api. Not skipping. {}";

  @Mock
  private FileLinesContextFactory fileLinesContextFactory;

  @Mock
  private CheckFactory checkFactory;

  @Mock
  private Checks<JavaCheck> checks;

  @Mock
  private SensorContext context;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @BeforeEach
  void setUp() {
    // configure mocks that need verification
    Mockito.lenient().when(this.checkFactory.<JavaCheck>create(anyString())).thenReturn(this.checks);
    Mockito.lenient().when(this.checks.addAnnotatedChecks(any(Iterable.class))).thenReturn(this.checks);
  }

  public void postTestExecutionChecks() {
    // each time a SonarComponent is instantiated the following methods must be called twice
    // once for custom checks, once for custom java checks
    verify(this.checkFactory, times(2)).create(REPOSITORY_NAME);
    verify(this.checks, times(2)).addAnnotatedChecks(any(Iterable.class));
    verify(this.checks, times(2)).all();
  }

  @Test
  void base_and_work_directories() {
    File baseDir = new File("");
    File workDir = new File("target");
    SensorContextTester context = SensorContextTester.create(baseDir);
    DefaultFileSystem fs = context.fileSystem();
    fs.setWorkDir(workDir.toPath());

    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, null, mock(ClasspathForTest.class), checkFactory);

    assertThat(sonarComponents.projectLevelWorkDir()).isEqualTo(workDir);
  }

  @Test
  void set_work_directory_using_project_definition() {
    File baseDir = new File("");
    File workDir = new File("target");
    SensorContextTester context = SensorContextTester.create(baseDir);
    DefaultFileSystem fs = context.fileSystem();
    fs.setWorkDir(workDir.toPath());
    ProjectDefinition parentProjectDefinition = ProjectDefinition.create();
    parentProjectDefinition.setWorkDir(workDir);
    ProjectDefinition childProjectDefinition = ProjectDefinition.create();
    parentProjectDefinition.addSubProject(childProjectDefinition);
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, null, mock(ClasspathForTest.class), checkFactory, childProjectDefinition);
    assertThat(sonarComponents.projectLevelWorkDir()).isEqualTo(workDir);
  }

  @Test
  void test_sonar_components() {
    SensorContextTester sensorContextTester = spy(SensorContextTester.create(new File("")));
    DefaultFileSystem fs = sensorContextTester.fileSystem();
    ClasspathForTest javaTestClasspath = mock(ClasspathForTest.class);
    List<File> javaTestClasspathList = Collections.emptyList();
    when(javaTestClasspath.getElements()).thenReturn(javaTestClasspathList);
    InputFile inputFile = TestUtils.emptyInputFile("foo.java");
    fs.add(inputFile);
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, null, javaTestClasspath, checkFactory);
    sonarComponents.setSensorContext(sensorContextTester);

    List<JavaCheck> visitors = sonarComponents.mainChecks();
    assertThat(visitors).isEmpty();
    Collection<JavaCheck> testChecks = sonarComponents.testChecks();
    assertThat(testChecks).isEmpty();
    assertThat(sonarComponents.getJavaClasspath()).isEmpty();
    assertThat(sonarComponents.getJavaTestClasspath()).isEqualTo(javaTestClasspathList);
    NewHighlighting newHighlighting = sonarComponents.highlightableFor(inputFile);
    assertThat(newHighlighting).isNotNull();
    verify(sensorContextTester, times(1)).newHighlighting();
    NewSymbolTable newSymbolTable = sonarComponents.symbolizableFor(inputFile);
    assertThat(newSymbolTable).isNotNull();
    verify(sensorContextTester, times(1)).newSymbolTable();
    assertThat(sonarComponents.fileLinesContextFor(inputFile)).isEqualTo(fileLinesContext);
    assertThat(sonarComponents.context()).isSameAs(sensorContextTester);

    ClasspathForMain javaClasspath = mock(ClasspathForMain.class);
    List<File> list = mock(List.class);
    when(javaClasspath.getElements()).thenReturn(list);
    sonarComponents = new SonarComponents(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory);
    assertThat(sonarComponents.getJavaClasspath()).isEqualTo(list);
  }

  @Test
  void creation_of_custom_checks() {
    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);

    when(this.checks.all()).thenReturn(Collections.singletonList(expectedCheck)).thenReturn(new ArrayList<>());
    SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, null, null,
      null, this.checkFactory, new CheckRegistrar[]{expectedRegistrar});
    sonarComponents.setSensorContext(context);

    List<JavaCheck> visitors = sonarComponents.mainChecks();
    assertThat(visitors).hasSize(1);
    assertThat(visitors.get(0)).isEqualTo(expectedCheck);
    Collection<JavaCheck> testChecks = sonarComponents.testChecks();
    assertThat(testChecks).isEmpty();

    postTestExecutionChecks();
  }

  @Test
  void creation_of_custom_test_checks() {
    JavaCheck expectedCheck = new CustomTestCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);

    when(checks.all()).thenReturn(new ArrayList<>()).thenReturn(Collections.singletonList(expectedCheck));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, null, null,
      null, checkFactory, new CheckRegistrar[]{expectedRegistrar});
    sonarComponents.setSensorContext(context);

    List<JavaCheck> visitors = sonarComponents.mainChecks();
    assertThat(visitors).isEmpty();
    List<JavaCheck> testChecks = sonarComponents.testChecks();
    assertThat(testChecks).hasSize(1);
    assertThat(testChecks.get(0)).isEqualTo(expectedCheck);

    postTestExecutionChecks();
  }

  @Test
  void order_of_checks_is_kept() {
    class CheckA implements JavaCheck {
    }
    class CheckB implements JavaCheck {
    }
    class CheckC implements JavaCheck {
    }
    CheckRegistrar expectedRegistrar = registrarContext -> registrarContext.registerClassesForRepository(
      REPOSITORY_NAME,
      Arrays.asList(CheckB.class, CheckC.class, CheckA.class),
      Arrays.asList(CheckC.class, CheckA.class, CheckB.class));
    when(this.checks.all())
      .thenReturn(Arrays.asList(new CheckA(), new CheckB(), new CheckC()))
      .thenReturn(Arrays.asList(new CheckA(), new CheckB(), new CheckC()));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, null, null,
      null, checkFactory, new CheckRegistrar[]{expectedRegistrar});
    sonarComponents.setSensorContext(context);

    List<JavaCheck> mainChecks = sonarComponents.mainChecks();
    assertThat(mainChecks).extracting(JavaCheck::getClass).extracting(Class::getSimpleName)
      .containsExactly("CheckB", "CheckC", "CheckA");
    List<JavaCheck> testChecks = sonarComponents.testChecks();
    assertThat(testChecks).extracting(JavaCheck::getClass).extracting(Class::getSimpleName)
      .containsExactly("CheckC", "CheckA", "CheckB");
  }

  @Test
  void filter_checks() {
    class CheckA implements JavaCheck {
    }
    class CheckB implements JavaCheck {
    }
    class CheckC implements JavaCheck {
    }
    CheckRegistrar expectedRegistrar = registrarContext -> registrarContext.registerClassesForRepository(
      REPOSITORY_NAME,
      Arrays.asList(CheckA.class, CheckB.class, CheckC.class),
      Arrays.asList(CheckC.class, CheckB.class, CheckA.class));
    when(this.checks.all())
      .thenReturn(Arrays.asList(new CheckA(), new CheckB(), new CheckC()))
      .thenReturn(Arrays.asList(new CheckC(), new CheckB(), new CheckA()));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, null, null,
      null, checkFactory, new CheckRegistrar[]{expectedRegistrar});
    sonarComponents.setSensorContext(context);
    sonarComponents.setCheckFilter(checks -> checks.stream()
      .filter(c -> !c.getClass().getSimpleName().equals("CheckB")).collect(Collectors.toList()));

    List<JavaCheck> mainChecks = sonarComponents.mainChecks();
    assertThat(mainChecks).extracting(JavaCheck::getClass).extracting(Class::getSimpleName)
      .containsExactly("CheckA", "CheckC");
    List<JavaCheck> testChecks = sonarComponents.testChecks();
    assertThat(testChecks).extracting(JavaCheck::getClass).extracting(Class::getSimpleName)
      .containsExactly("CheckC", "CheckA");
  }

  @Test
  void creation_of_both_types_test_checks() {
    JavaCheck expectedCheck = new CustomCheck();
    JavaCheck expectedTestCheck = new CustomTestCheck();
    CheckRegistrar expectedRegistrar = registrarContext -> registrarContext.registerClassesForRepository(
      REPOSITORY_NAME,
      Collections.singletonList(CustomCheck.class),
      Collections.singletonList(CustomTestCheck.class));

    when(this.checks.all()).thenReturn(Collections.singletonList(expectedCheck)).thenReturn(Collections.singletonList(expectedTestCheck));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, null, null,
      null, checkFactory, new CheckRegistrar[]{expectedRegistrar});
    sonarComponents.setSensorContext(context);

    List<JavaCheck> visitors = sonarComponents.mainChecks();
    assertThat(visitors).hasSize(1);
    assertThat(visitors.get(0)).isEqualTo(expectedCheck);
    List<JavaCheck> testChecks = sonarComponents.testChecks();
    assertThat(testChecks).hasSize(1);
    assertThat(testChecks.get(0)).isEqualTo(expectedTestCheck);

    postTestExecutionChecks();
  }

  @Test
  void no_issue_when_check_not_found() throws Exception {
    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);

    when(this.checks.ruleKey(any(JavaCheck.class))).thenReturn(null);
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, null, null,
      null, checkFactory, new CheckRegistrar[]{expectedRegistrar});
    sonarComponents.setSensorContext(context);

    sonarComponents.addIssue(TestUtils.emptyInputFile("file.java"), expectedCheck, 0, "message", null);
    verify(context, never()).newIssue();
  }

  @Test
  void add_issue_or_parse_error() throws Exception {
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

    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fileSystem, null,
      null, checkFactory, new CheckRegistrar[]{expectedRegistrar});
    sonarComponents.setSensorContext(context);

    sonarComponents.addIssue(inputFile, expectedCheck, -5, "message on wrong line", null);
    sonarComponents.addIssue(inputFile, expectedCheck, 42, "message on line 42", 1);
    sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, inputFile, 35, "other message", 0));

    List<Issue> issues = new ArrayList<>(context.allIssues());
    assertThat(issues).hasSize(3);
    assertThat(issues.get(0).primaryLocation().message()).isEqualTo("message on wrong line");
    assertThat(issues.get(1).primaryLocation().message()).isEqualTo("message on line 42");
    assertThat(issues.get(2).primaryLocation().message()).isEqualTo("other message");

    RecognitionException parseError = new RecognitionException(-1, "invalid code", new Exception("parse error"));

    context.setRuntime(SonarRuntimeImpl.forSonarLint(V8_9));
    assertThat(sonarComponents.reportAnalysisError(parseError, inputFile)).isTrue();

    context.setRuntime(SonarRuntimeImpl.forSonarQube(V8_9, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY));
    assertThat(sonarComponents.reportAnalysisError(parseError, inputFile)).isFalse();

  }

  @Test
  void fail_on_empty_location() {
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
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, context.fileSystem(), null, null, checkFactory, new CheckRegistrar[]{expectedRegistrar});
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
  void cancellation() {
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    SensorContextTester context = SensorContextTester.create(new File(""));
    sonarComponents.setSensorContext(context);

    context.setRuntime(SonarRuntimeImpl.forSonarLint(V8_9));
    assertThat(sonarComponents.analysisCancelled()).isFalse();

    // cancellation only handled from SQ 6.0
    context.setCancelled(true);

    assertThat(sonarComponents.analysisCancelled()).isTrue();
  }

  @Test
  void knows_if_quickfixes_are_supported() {
    SensorContextTester context = SensorContextTester.create(new File(""));
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(context);

    SonarRuntime sonarQube = SonarRuntimeImpl.forSonarQube(V8_9, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
    context.setRuntime(sonarQube);
    assertThat(sonarComponents.isQuickFixCompatible()).isFalse();

    SonarRuntime sonarLintWithoutQuickFix = new SonarLintRuntimeImpl(V8_9, Version.create(5, 3), -1L);
    context.setRuntime(sonarLintWithoutQuickFix);
    assertThat(sonarComponents.isQuickFixCompatible()).isFalse();

    // support of quickfixes introduced in 6.3
    SonarRuntime sonarLintWithQuickFix = new SonarLintRuntimeImpl(V8_9, Version.create(6, 4), -1L);
    context.setRuntime(sonarLintWithQuickFix);
    assertThat(sonarComponents.isQuickFixCompatible()).isTrue();
  }

  @Test
  void knows_if_quickfixes_can_be_advertised() {
    SensorContextTester context = SensorContextTester.create(new File(""));
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(context);

    assertTrue(sonarComponents.isSetQuickFixAvailableCompatible());
  }

  @Test
  void knows_if_quickfixes_can_not_be_advertised() {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(9, 0), SonarQubeSide.SERVER, SonarEdition.COMMUNITY));
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(context);

    assertFalse(sonarComponents.isSetQuickFixAvailableCompatible());
  }

  @Test
  void readFileContentFromInputFile() throws Exception {
    // read a file containing kanji set with correct encoding and expecting proper length of read input.
    InputFile inputFile = spy(TestUtils.inputFile("src/test/files/Kanji.java"));

    SensorContextTester context = SensorContextTester.create(new File(""));
    DefaultFileSystem fileSystem = context.fileSystem();
    fileSystem.add(inputFile);
    fileSystem.setEncoding(StandardCharsets.ISO_8859_1);
    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, null, null, null);

    context.setRuntime(SonarRuntimeImpl.forSonarLint(V8_9));
    sonarComponents.setSensorContext(context);

    String fileContent = sonarComponents.inputFileContents(inputFile);
    assertThat(fileContent).hasSize(59);

    List<String> fileLines = sonarComponents.fileLines(inputFile);
    assertThat(fileLines)
      .hasSize(5)
      .noneMatch(line -> line.endsWith("\n"));
    assertThat(fileLines.get(0)).hasSize(11);

    verify(inputFile, times(2)).contents();
    reset(inputFile);
  }

  @Test
  void io_error_when_reading_file_should_fail_analysis() {
    SensorContextTester context = SensorContextTester.create(new File(""));
    DefaultFileSystem fileSystem = context.fileSystem();
    InputFile unknownInputFile = TestUtils.emptyInputFile("unknown_file.java");
    fileSystem.add(unknownInputFile);
    context.setRuntime(SonarRuntimeImpl.forSonarLint(V8_9));
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

  @Test
  void jsp_classpath_should_include_plugin() throws Exception {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    DefaultFileSystem fs = sensorContextTester.fileSystem();

    ClasspathForMain javaClasspath = mock(ClasspathForMain.class);
    File someJar = new File("some.jar");
    when(javaClasspath.getElements()).thenReturn(Collections.singletonList(someJar));

    File plugin = new File("target/classes");
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, javaClasspath, mock(ClasspathForTest.class), checkFactory);
    List<String> jspClassPath = sonarComponents.getJspClasspath().stream().map(File::getAbsolutePath).collect(Collectors.toList());
    assertThat(jspClassPath).containsExactly(plugin.getAbsolutePath(), someJar.getAbsolutePath());
  }

  @Test
  void autoscan_getters() {
    MapSettings settings = new MapSettings();
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(SensorContextTester.create(new File("")).setSettings(settings));

    // default value
    settings.clear();
    assertThat(sonarComponents.isAutoScan()).isFalse();
    assertThat(sonarComponents.isFileByFileEnabled()).isFalse();
    assertThat(sonarComponents.isAutoScanCheckFiltering()).isFalse();

    // autoscan
    settings.clear();
    settings.setProperty("sonar.internal.analysis.autoscan", "true");
    assertThat(sonarComponents.isAutoScan()).isTrue();
    assertThat(sonarComponents.isFileByFileEnabled()).isFalse();
    assertThat(sonarComponents.isAutoScanCheckFiltering()).isTrue();

    // autoscan, without check filter
    settings.clear();
    settings.setProperty("sonar.internal.analysis.autoscan", "true");
    settings.setProperty("sonar.internal.analysis.autoscan.filtering", "false");
    assertThat(sonarComponents.isAutoScan()).isTrue();
    assertThat(sonarComponents.isFileByFileEnabled()).isFalse();
    assertThat(sonarComponents.isAutoScanCheckFiltering()).isFalse();

    // deprecated autoscan key
    settings.clear();
    settings.setProperty("sonar.java.internal.batchMode", "true");
    assertThat(sonarComponents.isAutoScan()).isTrue();
    assertThat(sonarComponents.isFileByFileEnabled()).isFalse();
    assertThat(sonarComponents.isAutoScanCheckFiltering()).isTrue();
  }

  @Test
  void batch_getters() {
    MapSettings settings = new MapSettings();
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(SensorContextTester.create(new File("")).setSettings(settings));

    // default value
    assertThat(sonarComponents.isAutoScan()).isFalse();
    assertThat(sonarComponents.isFileByFileEnabled()).isFalse();
    assertThat(sonarComponents.isAutoScanCheckFiltering()).isFalse();
    assertThat(sonarComponents.getBatchModeSizeInKB()).isPositive();

    // batch mode: when a batch mode size is explicitly set, we use this value
    settings.setProperty("sonar.java.experimental.batchModeSizeInKB", "1000");
    assertThat(sonarComponents.isAutoScan()).isFalse();
    assertThat(sonarComponents.isFileByFileEnabled()).isFalse();
    assertThat(sonarComponents.getBatchModeSizeInKB()).isEqualTo(1000L);

    // autoscan is not compatible with batch mode size
    settings.setProperty("sonar.internal.analysis.autoscan", "true");
    assertThat(sonarComponents.isAutoScan()).isFalse();
    assertThat(sonarComponents.isFileByFileEnabled()).isFalse();
    assertThat(sonarComponents.getBatchModeSizeInKB()).isEqualTo(1000L);

    // batchModeSizeInKB has the priority over deprecated autoscan key
    settings.setProperty("sonar.java.internal.batchMode", "true");
    assertThat(sonarComponents.getBatchModeSizeInKB()).isEqualTo(1000L);

    // Deprecated autoscan key returns default value
    // Note: it means that if someone used this key outside an autoscan context, the project will be analyzed in a single batch (unless batch size is specified)
    settings.clear();
    settings.setProperty("sonar.java.internal.batchMode", "true");
    assertThat(sonarComponents.getBatchModeSizeInKB()).isEqualTo(-1L);
  }

  @ParameterizedTest
  @CsvSource({
    "50, 2",
    "100, 5",
    "200, 10",
    "1000, 50",
    "10000, 500",
    "20000, 500",
  })
  void batch_size_dynamic_computation(long maxMemoryMB, long expectedBatchSizeKB) {
    long maxMemoryBytes = maxMemoryMB * 1_000_000;
    MapSettings settings = new MapSettings();
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(SensorContextTester.create(new File("")).setSettings(settings));

    LongSupplier oldValue = SonarComponents.maxMemoryInBytesProvider;
    SonarComponents.maxMemoryInBytesProvider = () -> maxMemoryBytes;
    long batchModeSizeInKB = sonarComponents.getBatchModeSizeInKB();
    SonarComponents.maxMemoryInBytesProvider = oldValue;
    assertThat(batchModeSizeInKB).isEqualTo(expectedBatchSizeKB);
  }

  @Test
  void file_by_file_getters() {
    MapSettings settings = new MapSettings();
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(SensorContextTester.create(new File("")).setSettings(settings));

    // default value
    assertThat(sonarComponents.isFileByFileEnabled()).isFalse();

    // file by file
    settings.setProperty("sonar.java.fileByFile", "true");
    assertThat(sonarComponents.isFileByFileEnabled()).isTrue();

    // file by file + batch mode can be both set, the priority will be defined when using the values.
    settings.setProperty("sonar.java.experimental.batchModeSizeInKB", "1000");
    assertThat(sonarComponents.isFileByFileEnabled()).isTrue();
    assertThat(sonarComponents.getBatchModeSizeInKB()).isEqualTo(1000);
  }

  @Test
  void skipUnchangedFiles_returns_result_from_context() throws ApiMismatchException {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));

    SonarComponents sonarComponents = new SonarComponents(
      fileLinesContextFactory,
      sensorContextTester.fileSystem(),
      mock(ClasspathForMain.class),
      mock(ClasspathForTest.class),
      checkFactory
    );

    IncrementalAnalysisSensorContext context = mock(IncrementalAnalysisSensorContext.class);
    when(context.canSkipUnchangedFiles()).thenReturn(true);
    sonarComponents.setSensorContext(context);
    assertThat(sonarComponents.canSkipUnchangedFiles()).isTrue();

    when(context.canSkipUnchangedFiles()).thenReturn(false);
    sonarComponents.setSensorContext(context);
    assertThat(sonarComponents.canSkipUnchangedFiles()).isFalse();
  }

  @Test
  void skipUnchangedFiles_returns_false_by_default() throws ApiMismatchException {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));

    SonarComponents sonarComponents = new SonarComponents(
      fileLinesContextFactory,
      sensorContextTester.fileSystem(),
      mock(ClasspathForMain.class),
      mock(ClasspathForTest.class),
      checkFactory
    );

    assertThat(sonarComponents.canSkipUnchangedFiles()).isFalse();
  }

  @Test
  void skipUnchangedFiles_throws_a_NoSuchMethodError_when_canSkipUnchangedFiles_not_in_API() {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    SonarComponents sonarComponents = new SonarComponents(
      fileLinesContextFactory,
      sensorContextTester.fileSystem(),
      mock(ClasspathForMain.class),
      mock(ClasspathForTest.class),
      checkFactory
    );

    IncrementalAnalysisSensorContext context = mock(IncrementalAnalysisSensorContext.class);
    when(context.canSkipUnchangedFiles()).thenThrow(new NoSuchMethodError("API version mismatch :-("));
    sonarComponents.setSensorContext(context);

    ApiMismatchException error = assertThrows(
      ApiMismatchException.class,
      sonarComponents::canSkipUnchangedFiles
    );
    assertThat(error).hasCause(new NoSuchMethodError("API version mismatch :-("));
  }

  @Test
  void fileCanBeSkipped_returns_false_when_the_file_is_a_generated_file() throws ApiMismatchException {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    SonarComponents sonarComponents = spy(
      new SonarComponents(
        fileLinesContextFactory,
        sensorContextTester.fileSystem(),
        mock(ClasspathForMain.class),
        mock(ClasspathForTest.class),
        checkFactory
      )
    );
    SensorContext contextMock = mock(SensorContext.class);
    sonarComponents.setSensorContext(contextMock);

    InputFile inputFile = new GeneratedFile(Path.of("non-existing-generated-file.java"));

    assertThat(sonarComponents.fileCanBeSkipped(inputFile)).isFalse();
  }

  @Test
  void fileCanBeSkipped_always_returns_false_when_skipUnchangedFiles_is_false() throws ApiMismatchException, IOException {

    SonarComponents sonarComponents = mock(SonarComponents.class, CALLS_REAL_METHODS);
    SensorContext contextMock = mock(SensorContext.class);
    sonarComponents.setSensorContext(contextMock);

    when(sonarComponents.canSkipUnchangedFiles()).thenReturn(false);
    InputFile inputFile = mock(InputFile.class);

    assertThat(sonarComponents.fileCanBeSkipped(inputFile)).isFalse();
  }

  @Test
  void fileCanBeSkipped_returns_false_when_inputFileStatusIsDifferentFromSame() throws ApiMismatchException {
    SonarComponents sonarComponents = mock(SonarComponents.class, CALLS_REAL_METHODS);
    SensorContext contextMock = mock(SensorContext.class);
    sonarComponents.setSensorContext(contextMock);

    when(sonarComponents.canSkipUnchangedFiles()).thenReturn(true);
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.status()).thenReturn(InputFile.Status.CHANGED);
    assertThat(sonarComponents.fileCanBeSkipped(inputFile)).isFalse();
  }

  @Test
  void fileCanBeSkipped_returns_false_when_canSkipUnchangedFile_isFalse() throws ApiMismatchException {
    SonarComponents sonarComponents = mock(SonarComponents.class, CALLS_REAL_METHODS);
    SensorContext contextMock = mock(SensorContext.class);
    sonarComponents.setSensorContext(contextMock);

    ApiMismatchException apiMismatchException = new ApiMismatchException(new NoSuchMethodError("API version mismatch :-("));
    doThrow(apiMismatchException).when(sonarComponents).canSkipUnchangedFiles();

    assertThat(sonarComponents.fileCanBeSkipped(mock(InputFile.class))).isFalse();
  }

  private static Stream<Arguments> fileCanBeSkipped_only_logs_on_first_call_input() throws ApiMismatchException {
    ApiMismatchException apiMismatchException = new ApiMismatchException(new NoSuchMethodError("API version mismatch :-("));

    SonarComponents sonarComponentsThatCanSkipFiles = mock(SonarComponents.class, CALLS_REAL_METHODS);
    doReturn(true).when(sonarComponentsThatCanSkipFiles).canSkipUnchangedFiles();
    SonarComponents sonarComponentsThatCannotSkipFiles = mock(SonarComponents.class, CALLS_REAL_METHODS);
    doReturn(false).when(sonarComponentsThatCannotSkipFiles).canSkipUnchangedFiles();
    SonarComponents sonarComponentsWithApiMismatch = mock(SonarComponents.class, CALLS_REAL_METHODS);
    doThrow(apiMismatchException).when(sonarComponentsWithApiMismatch).canSkipUnchangedFiles();

    InputFile inputFile = mock(InputFile.class);
    doReturn(InputFile.Status.SAME).when(inputFile).status();

    return Stream.of(
      Arguments.of(sonarComponentsThatCanSkipFiles, inputFile, LOG_MESSAGE_FILES_CAN_BE_SKIPPED),
      Arguments.of(sonarComponentsThatCannotSkipFiles, mock(InputFile.class), LOG_MESSAGE_FILES_CANNOT_BE_SKIPPED),
      Arguments.of(sonarComponentsWithApiMismatch, mock(InputFile.class), LOG_MESSAGE_CANNOT_DETERMINE_IF_FILES_CAN_BE_SKIPPED)
    );
  }

  @ParameterizedTest
  @MethodSource("fileCanBeSkipped_only_logs_on_first_call_input")
  void fileCanBeSkipped_only_logs_on_the_first_call(SonarComponents sonarComponents, InputFile inputFile, String logMessage) throws IOException {
    assertThat(logTester.getLogs(Level.INFO)).isEmpty();

    SensorContext contextMock = mock(SensorContext.class);
    sonarComponents.setSensorContext(contextMock);
    when(inputFile.contents()).thenReturn("");
    sonarComponents.fileCanBeSkipped(inputFile);
    List<LogAndArguments> logs = logTester.getLogs(Level.INFO);
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getRawMsg()).isEqualTo(logMessage);

    sonarComponents.fileCanBeSkipped(inputFile);
    logs = logTester.getLogs(Level.INFO);
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getRawMsg()).isEqualTo(logMessage);
  }

  private static Stream<Arguments> provideInputsFor_canSkipUnchangedFiles() {
    return Stream.of(
      Arguments.of(null, null, null),
      Arguments.of(null, true, true),
      Arguments.of(true, null, true),
      Arguments.of(null, false, false),
      Arguments.of(false, null, false),
      Arguments.of(false, false, false),
      Arguments.of(false, true, false),
      Arguments.of(true, false, true),
      Arguments.of(true, true, true)
    );
  }

  @ParameterizedTest
  @MethodSource("provideInputsFor_canSkipUnchangedFiles")
  void canSkipUnchangedFiles(@CheckForNull Boolean overrideFlagVal, @CheckForNull Boolean apiResponseVal, @CheckForNull Boolean expectedResult) throws ApiMismatchException {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    SonarComponents sonarComponents = new SonarComponents(
      fileLinesContextFactory,
      sensorContextTester.fileSystem(),
      mock(ClasspathForMain.class),
      mock(ClasspathForTest.class),
      checkFactory
    );

    IncrementalAnalysisSensorContext context = mock(IncrementalAnalysisSensorContext.class);
    Configuration config = mock(Configuration.class);
    when(context.config()).thenReturn(config);

    when(config.getBoolean(any())).thenReturn(Optional.ofNullable(overrideFlagVal));

    if (apiResponseVal == null) {
      lenient().when(context.canSkipUnchangedFiles()).thenThrow(new NoSuchMethodError("API version mismatch :-("));
    } else {
      lenient().when(context.canSkipUnchangedFiles()).thenReturn(apiResponseVal);
    }

    sonarComponents.setSensorContext(context);

    if (expectedResult == null) {
      ApiMismatchException noSuchMethodError = assertThrows(ApiMismatchException.class, sonarComponents::canSkipUnchangedFiles);
      assertThat(noSuchMethodError).hasCause(new NoSuchMethodError("API version mismatch :-("));
    } else {
      assertThat(sonarComponents.canSkipUnchangedFiles()).isEqualTo(expectedResult);
    }
  }

  @Nested
  class Logging {
    private final DecimalFormat formatter = new DecimalFormat("00");

    private final SensorContextTester context = SensorContextTester.create(new File(""));
    private final DefaultFileSystem fs = context.fileSystem();
    private final Configuration settings = context.config();

    private final ClasspathForMain javaClasspath = new ClasspathForMain(settings, fs);
    private final ClasspathForTest javaTestClasspath = new ClasspathForTest(settings, fs);

    private SonarComponents sonarComponents;

    @RegisterExtension
    public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

    @BeforeEach
    void beforeEach() {
      sonarComponents = new SonarComponents(null, fs, javaClasspath, javaTestClasspath, null);
      sonarComponents.setSensorContext(context);
    }

    @Test
    void log_only_50_undefined_types() {
      String source = generateSource(26);

      // artificially populated the semantic errors with 26 unknown types and 52 errors
      sonarComponents.collectUndefinedTypes(((JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source)).sema.undefinedTypes());

      // triggers log
      sonarComponents.logUndefinedTypes();

      assertThat(logTester.logs(Level.WARN)).containsExactly("Unresolved imports/types have been detected during analysis. Enable DEBUG mode to see them.");

      List<String> debugLogs = logTester.logs(Level.DEBUG);
      assertThat(debugLogs).hasSize(1);

      String list = debugLogs.get(0);
      assertThat(list)
        .startsWith("Unresolved imports/types: (Limited to 50)")
        .endsWith("- ...")
        .doesNotContain("- Y cannot be resolved to a type")
        .doesNotContain("- Z cannot be resolved to a type");
      for (int i = 0; i < 26; i++) {
        char typeName = (char) ('A' + i);
        assertThat(list).contains(String.format("- The import org.package%s cannot be resolved", formatter.format(i + 1)));
        if (typeName < 'Y') {
          assertThat(list).contains(String.format("- %c cannot be resolved to a type", typeName));
        }
      }
    }

    @Test
    void log_all_undefined_types_if_less_than_threshold() {
      String source = generateSource(1);

      // artificially populated the semantic errors with 1 unknown types and 2 errors
      sonarComponents.collectUndefinedTypes(((JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source)).sema.undefinedTypes());

      // triggers log
      sonarComponents.logUndefinedTypes();

      assertThat(logTester.logs(Level.WARN)).containsExactly("Unresolved imports/types have been detected during analysis. Enable DEBUG mode to see them.");

      List<String> debugLogs = logTester.logs(Level.DEBUG);
      assertThat(debugLogs).hasSize(1);

      assertThat(debugLogs.get(0))
        .startsWith("Unresolved imports/types:")
        .doesNotContain("- ...")
        .contains("- A cannot be resolved to a type")
        .contains("- The import org.package01 cannot be resolved");
    }

    @Test
    void suspicious_empty_libraries_should_be_logged() {
      logUndefinedTypesWithOneMainAndOneTest();

      assertThat(logTester.logs(Level.WARN))
        .contains("Dependencies/libraries were not provided for analysis of SOURCE files. The 'sonar.java.libraries' property is empty. Verify your configuration, as you might end up with less precise results.")
        .contains("Dependencies/libraries were not provided for analysis of TEST files. The 'sonar.java.test.libraries' property is empty. Verify your configuration, as you might end up with less precise results.");
    }

    @Test
    void suspicious_empty_libraries_should_not_be_logged_in_autoscan() {
      // Enable autoscan with a property
      context.setSettings(new MapSettings().setProperty(SonarComponents.SONAR_AUTOSCAN, true));

      logUndefinedTypesWithOneMainAndOneTest();

      assertThat(logTester.logs(Level.WARN))
        .contains("Dependencies/libraries were not provided for analysis of SOURCE files. The 'sonar.java.libraries' property is empty. Verify your configuration, as you might end up with less precise results.")
        .doesNotContain("Dependencies/libraries were not provided for analysis of TEST files. The 'sonar.java.test.libraries' property is empty. Verify your configuration, as you might end up with less precise results.");
    }

    private void logUndefinedTypesWithOneMainAndOneTest() {
      String source = generateSource(1);

      // Add one test and one main file
      fs.add(TestUtils.emptyInputFile("fooMain.java", InputFile.Type.MAIN));
      fs.add(TestUtils.emptyInputFile("fooTest.java", InputFile.Type.TEST));

      // artificially populated the semantic errors with 1 unknown types and 2 errors
      sonarComponents.collectUndefinedTypes(((JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source)).sema.undefinedTypes());

      // Call these methods to initiate Main and Test ClassPath
      sonarComponents.getJavaClasspath();
      sonarComponents.getJavaTestClasspath();

      sonarComponents.logUndefinedTypes();
    }

    private String generateSource(int numberUnknownTypes) {
      StringBuilder sourceBuilder = new StringBuilder("package org.foo;\n");
      for (int i = 0; i < numberUnknownTypes; i++) {
        char typeName = (char) ('A' + i);
        sourceBuilder.append(String.format("import org.package%s.%c;\n", formatter.format(i + 1), typeName));
      }
      sourceBuilder.append("class Test {\n");
      for (int i = 0; i < numberUnknownTypes; i++) {
        char typeName = (char) ('A' + i);
        sourceBuilder.append(String.format("  %c variable%d;\n", typeName, i + 1));
      }
      sourceBuilder.append("}");
      return sourceBuilder.toString();
    }
  }

  private static CheckRegistrar getRegistrar(final JavaCheck expectedCheck) {
    return registrarContext -> registrarContext.registerClassesForRepository(REPOSITORY_NAME,
      Collections.singletonList(expectedCheck.getClass()), null);
  }

  private static class CustomCheck implements JavaCheck {
  }

  private static class CustomTestCheck implements JavaCheck {
  }

  @Test
  void should_return_generated_code_visitors() throws Exception {
    ActiveRules activeRules = new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("custom", "jsp")).build())
      .build();
    CheckFactory checkFactory = new CheckFactory(activeRules);

    JspCodeCheck check = new JspCodeCheck();
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, checkFactory, new CheckRegistrar[]{getRegistrar(check)});
    List<JavaCheck> checks = sonarComponents.jspChecks();
    assertThat(checks)
      .isNotEmpty()
      .allMatch(JspCodeCheck.class::isInstance);

    sonarComponents = new SonarComponents(null, null, null, null, checkFactory);
    assertThat(sonarComponents.jspChecks()).isEmpty();
  }

  @Test
  void moduleKey_empty() {
    var sonarComponents = new SonarComponents(null, null, null, null, null);
    assertThat(sonarComponents.getModuleKey()).isEmpty();
  }

  @Test
  void moduleKey_non_empty() {
    var rootProj = mock(ProjectDefinition.class);
    doReturn(new File("/foo/bar/proj")).when(rootProj).getBaseDir();
    var parentModule = mock(ProjectDefinition.class);
    doReturn(rootProj).when(parentModule).getParent();
    var childModule = mock(ProjectDefinition.class);
    doReturn(new File("/foo/bar/proj/pmodule/cmodule")).when(childModule).getBaseDir();
    doReturn(parentModule).when(childModule).getParent();

    var sonarComponents = new SonarComponents(null, null, null, null, null, null, childModule);
    assertThat(sonarComponents.getModuleKey()).isEqualTo("pmodule/cmodule");
  }

  @Rule(key = "jsp")
  public static class JspCodeCheck implements JspCodeVisitor {

  }

  //TODO Remove this extended API after the upgrade of sonar-plugin-api to 9.4
  interface IncrementalAnalysisSensorContext extends SensorContext {
    boolean canSkipUnchangedFiles();
  }
}
