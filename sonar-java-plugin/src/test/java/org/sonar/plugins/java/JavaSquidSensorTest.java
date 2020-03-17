/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
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
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaTestClasspath;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.naming.BadMethodNameCheck;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.GeneratedFile;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JspCodeVisitor;

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

public class JavaSquidSensorTest {

  private static final CheckFactory checkFactory = mock(CheckFactory.class);
  private static final Checks<Object> checks = mock(Checks.class);

  static {
    when(checks.addAnnotatedChecks(any(Iterable.class))).thenReturn(checks);
    when(checks.ruleKey(any(JavaCheck.class))).thenReturn(RuleKey.of("java", RuleAnnotationUtils.getRuleKey(BadMethodNameCheck.class)));
    when(checkFactory.create(anyString())).thenReturn(checks);
  }

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void test_toString() {
    assertThat(new JavaSquidSensor(null, null, null, null, null).toString()).isEqualTo("JavaSquidSensor");
  }

  @Test
  public void test_issues_creation_on_main_file() throws IOException {
    testIssueCreation(InputFile.Type.MAIN, 4);
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
    DefaultJavaResourceLocator javaResourceLocator = new DefaultJavaResourceLocator(new JavaClasspath(settings.asConfig(), fs));
    JavaSquidSensor jss = new JavaSquidSensor(sonarComponents, fs, javaResourceLocator, settings.asConfig(), noSonarFilter);

    jss.execute(context);
    // argument 98 refers to the comment on line #98 in this file
    verify(noSonarFilter, times(1)).noSonarInFile(fs.inputFiles().iterator().next(), Sets.newHashSet(98));
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

    context.setRuntime(SonarVersion.SQ_79_RUNTIME);
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

    JavaSquidSensor jss = new JavaSquidSensor(sonarComponents, fs, javaResourceLocator, new MapSettings().asConfig(), noSonarFilter);
    jss.execute(context);
  }

  @Test
  public void feedbackShouldNotBeFedIfNoErrors() throws IOException {
    SensorContextTester context = createContext(InputFile.Type.MAIN);
    executeJavaSquidSensor(context);
    assertThat(context.<String>measure("projectKey", "sonarjava_feedback")).isNull();
  }

  @Test
  public void feedbackShouldNotBeFedIfNotSonarCloudHost() throws IOException {
    SensorContextTester context = createParseErrorContext();
    executeJavaSquidSensor(context);
    assertThat(context.<String>measure("projectKey", "sonarjava_feedback")).isNull();
  }

  @Test
  public void should_invoke_visitors_on_generated_code() throws Exception {
    Path base = tmp.newFolder().toPath();
    Path generatedFilePath = tmp.newFile("Generated.java").toPath();
    Files.write(generatedFilePath, "class Generated {}".getBytes());
    GeneratedFile generatedFile = new GeneratedFile(generatedFilePath);

    SensorContextTester context = SensorContextTester.create(base);
    SonarComponents sonarComponents = createSonarComponentsMock(context);
    JavaFileScanner javaFileScanner = mock(JavaFileScanner.class);
    JspCodeScanner testCodeVisitor = mock(JspCodeScanner.class);
    when(sonarComponents.jspCodeVisitors()).thenReturn(asList(testCodeVisitor));
    when(sonarComponents.checkClasses()).thenReturn(new JavaCheck[]{javaFileScanner});

    Jasper jasper = mock(Jasper.class);
    when(jasper.generateFiles(any(), any())).thenReturn(asList(generatedFile));
    JavaSquidSensor jss = new JavaSquidSensor(sonarComponents, context.fileSystem(), mock(JavaResourceLocator.class),
      new MapSettings().asConfig(), mock(NoSonarFilter.class), jasper);
    jss.execute(context);

    ArgumentCaptor<JavaFileScannerContext> scannerContext = ArgumentCaptor.forClass(JavaFileScannerContext.class);
    verify(testCodeVisitor, times(1)).scanFile(scannerContext.capture());
    assertThat(scannerContext.getValue().getInputFile()).isSameAs(generatedFile);

    // normal visitors are not invoked on generated files
    verify(javaFileScanner, never()).scanFile(any());
  }

  interface JspCodeScanner extends JavaFileScanner, JspCodeVisitor {
  }
}
