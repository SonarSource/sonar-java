/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SonarComponentsTest {

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
  public void test_sonar_components() {
    SensorContextTester sensorContextTester = spy(SensorContextTester.create(new File("")));
    DefaultFileSystem fs = sensorContextTester.fileSystem();
    JavaTestClasspath javaTestClasspath = mock(JavaTestClasspath.class);
    ImmutableList<File> javaTestClasspathList = ImmutableList.of();
    when(javaTestClasspath.getElements()).thenReturn(javaTestClasspathList);
    File file = new File("foo.java");
    fs.add(new DefaultInputFile("", "foo.java"));
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, null, javaTestClasspath, checkFactory);
    sonarComponents.setSensorContext(sensorContextTester);

    CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(0);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(0);
    assertThat(sonarComponents.getFileSystem()).isEqualTo(fs);
    assertThat(sonarComponents.getJavaClasspath()).isEmpty();
    assertThat(sonarComponents.getJavaTestClasspath()).isEqualTo(javaTestClasspathList);
    NewHighlighting newHighlighting = sonarComponents.highlightableFor(file);
    assertThat(newHighlighting).isNotNull();
    verify(sensorContextTester, times(1)).newHighlighting();
    NewSymbolTable newSymbolTable = sonarComponents.symbolizableFor(file);
    assertThat(newSymbolTable ).isNotNull();
    verify(sensorContextTester, times(1)).newSymbolTable();
    assertThat(sonarComponents.fileLinesContextFor(file)).isEqualTo(fileLinesContext);

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

    when(this.checks.all()).thenReturn(Lists.newArrayList(expectedCheck)).thenReturn(new ArrayList<JavaCheck>());
    SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    CodeVisitor[] visitors = sonarComponents.checkClasses();
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

    when(checks.all()).thenReturn(new ArrayList<JavaCheck>()).thenReturn(Lists.newArrayList(expectedCheck));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, null, null, null, checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    CodeVisitor[] visitors = sonarComponents.checkClasses();
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

    CodeVisitor[] visitors = sonarComponents.checkClasses();
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

    Issuable issuable = mock(Issuable.class);

    when(this.checks.all()).thenReturn(Lists.newArrayList(expectedCheck)).thenReturn(new ArrayList<>());
    when(this.checks.ruleKey(any(JavaCheck.class))).thenReturn(null);
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, null, null, null, checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    sonarComponents.addIssue(new File(""), expectedCheck, 0, "message", null);
    verify(issuable, never()).addIssue(any(Issue.class));
  }

  @Test
  public void no_issue_if_file_not_found() throws Exception {
    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);

    DefaultFileSystem fileSystem = new DefaultFileSystem(new File(""));
    File file = new File("file.java");

    when(this.checks.all()).thenReturn(Lists.newArrayList(expectedCheck)).thenReturn(new ArrayList<>());
    when(this.checks.ruleKey(any(JavaCheck.class))).thenReturn(mock(RuleKey.class));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fileSystem, null, null, checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    sonarComponents.addIssue(file, expectedCheck, 0, "message", null);
  }

  @Test
  public void add_issue() throws Exception {
    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = getRegistrar(expectedCheck);

    DefaultFileSystem fileSystem = new DefaultFileSystem(new File(""));
    File file = new File("file.java");
    DefaultInputFile inputFile = new DefaultInputFile("", "file.java");
    inputFile.setLines(45);
    int[] linesOffset = new int[45];
    linesOffset[35] = 12;
    linesOffset[42] = 1;
    inputFile.setOriginalLineOffsets(linesOffset);
    inputFile.setLastValidOffset(420);
    fileSystem.add(inputFile);
    context = mock(SensorContext.class);
    NewIssue newIssue = mock(NewIssue.class);
    when(newIssue.forRule(any(RuleKey.class))).thenReturn(newIssue);
    when(newIssue.gap(anyDouble())).thenReturn(newIssue);
    when(context.newIssue()).thenReturn(newIssue);

    NewIssueLocation newIssueLocation = mock(NewIssueLocation.class);
    when(newIssue.newLocation()).thenReturn(newIssueLocation);
    when(newIssueLocation.at(any(TextRange.class))).thenReturn(newIssueLocation);
    when(newIssueLocation.on(any(InputComponent.class))).thenReturn(newIssueLocation);


    when(this.checks.all()).thenReturn(Lists.newArrayList(expectedCheck)).thenReturn(new ArrayList<>());
    when(this.checks.ruleKey(any(JavaCheck.class))).thenReturn(mock(RuleKey.class));

    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fileSystem, null, null, checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });
    sonarComponents.setSensorContext(context);

    sonarComponents.addIssue(file, expectedCheck, -5, "message on wrong line", null);
    sonarComponents.addIssue(file, expectedCheck, 42, "message on line", 1);
    sonarComponents.addIssue(new File("."), expectedCheck, 42, "message on line", 1);
    sonarComponents.addIssue(new File("unknown_file"), expectedCheck, 42, "message on line", 1);
    sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, file, 35, "other message", 0));
    verify(context, times(3)).newIssue();
  }

  private static CheckRegistrar getRegistrar(final JavaCheck expectedCheck) {
    return new CheckRegistrar() {
      @Override
      public void register(RegistrarContext registrarContext) {
        registrarContext.registerClassesForRepository(
          REPOSITORY_NAME,
          Lists.<Class<? extends JavaCheck>>newArrayList(expectedCheck.getClass()),
          null);
      }
    };
  }

  private static class CustomCheck implements JavaCheck {

  }

  private static class CustomTestCheck implements JavaCheck {
  }
}
