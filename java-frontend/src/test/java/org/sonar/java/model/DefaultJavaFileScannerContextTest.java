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
package org.sonar.java.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.AnalyzerMessage.TextSpan;
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultJavaFileScannerContextTest {

  private static final File JAVA_FILE = new File("src/test/files/api/JavaFileScannerContext.java");
  private static final InputFile JAVA_INPUT_FILE = TestUtils.inputFile(JAVA_FILE);
  private static final File WORK_DIR = new File("target");
  private static final File BASE_DIR = new File("");
  private static final InputComponent PROJECT_BASE_DIR = new DefaultInputDir("", BASE_DIR.getAbsolutePath());
  private static final int COST = 42;
  private static final JavaCheck CHECK = new JavaCheck() { };
  private static final EndOfAnalysisCheck END_OF_ANALYSIS_CHECK = () -> { };
  private SonarComponents sonarComponents;
  private CompilationUnitTree compilationUnitTree;
  private DefaultJavaFileScannerContext context;
  private AnalyzerMessage reportedMessage;

  @Before
  public void setup() {
    sonarComponents = createSonarComponentsMock();
    compilationUnitTree = (CompilationUnitTree) JavaParser.createParser().parse(JAVA_FILE);
    context = new DefaultJavaFileScannerContext(compilationUnitTree, JAVA_INPUT_FILE, null, sonarComponents, new JavaVersionImpl(), true);
    reportedMessage = null;
  }

  @Test
  public void get_complexity_nodes() {
    assertThat(context.getComplexityNodes(compilationUnitTree)).isEmpty();
  }

  @Test
  public void get_tree() {
    assertThat(context.getTree()).isEqualTo(compilationUnitTree);
  }

  @Test
  public void get_file_parsed() {
    assertThat(context.fileParsed()).isTrue();
  }

  @Test
  public void get_file_key() {
    assertThat(context.getFileKey()).isEqualTo(JAVA_INPUT_FILE.file().getAbsolutePath());
  }

  @Test
  public void get_java_version() {
    assertThat(context.getJavaVersion()).isNotNull();
  }

  @Test
  public void get_file_content() {
    assertThat(context.getFileContent()).isEqualTo("content");
  }

  @Test
  public void get_file() {
    assertThat(context.getFile()).isEqualTo(JAVA_INPUT_FILE.file());
  }

  @Test
  public void add_issue_with_file() {
    context.addIssue(JAVA_FILE, CHECK, 1, "msg");

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getInputComponent()).isEqualTo(JAVA_INPUT_FILE);
  }

  @Test
  public void get_file_lines() {
    assertThat(context.getFileLines()).isEmpty();
  }

  @Test
  public void get_semantic_model() {
    assertThat(context.getSemanticModel()).isNull();
  }

  @Test
  public void add_issue_on_file() {
    context.addIssueOnFile(CHECK, "file");

    assertThat(reportedMessage.getMessage()).isEqualTo("file");
    assertThat(reportedMessage.getInputComponent()).isEqualTo(JAVA_INPUT_FILE);
  }

  @Test
  public void add_issue_on_project() {
    context.addIssueOnProject(CHECK, "msg");

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getInputComponent()).isEqualTo(PROJECT_BASE_DIR);
  }

  @Test
  public void add_issue_no_file() {
    context.addIssue(10, CHECK, "msg2");

    assertThat(reportedMessage.getMessage()).isEqualTo("msg2");
    assertThat(reportedMessage.getInputComponent()).isEqualTo(JAVA_INPUT_FILE);
  }

  @Test
  public void add_issue_no_file_with_cost() {
    context.addIssue(10, CHECK, "msg3", 2);

    assertThat(reportedMessage.getMessage()).isEqualTo("msg3");
    assertThat(reportedMessage.getInputComponent()).isEqualTo(JAVA_INPUT_FILE);
    assertThat(reportedMessage.getCost()).isEqualTo(2);
  }

  @Test
  public void report_issue_on_tree() {
    context.reportIssue(CHECK, compilationUnitTree, "msg");

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isNull();
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 1, 0, 4, 1);
  }

  @Test
  public void working_directory() {
    assertThat(context.getWorkingDirectory()).isNotNull();
    assertThat(context.getBaseDirectory()).isNotNull();
  }

  @Test
  public void report_issue_with_message() {
    AnalyzerMessage message = context.createAnalyzerMessage(CHECK, compilationUnitTree, "msg");

    context.reportIssue(message);

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isNull();
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 1, 0, 4, 1);
  }

  @Test
  public void report_issue_on_tree_with_cross_file_scanner_throws() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> context.reportIssue(END_OF_ANALYSIS_CHECK, compilationUnitTree, "msg"))
        .withMessage("EndOfAnalysisCheck must only call reportIssue with AnalyzerMessage and must never pass a Tree reference.");
  }

  @Test
  public void report_issue_on_tree_with_no_secondary() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);

    context.reportIssue(CHECK, tree.simpleName(), "msg", new ArrayList<>(), null);

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isNull();
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 1, 6, 1, 7);
  }

  @Test
  public void report_issue_on_tree_with_cost() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);

    context.reportIssue(CHECK, tree.simpleName(), "msg", new ArrayList<>(), COST);

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isEqualTo(COST);
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 1, 6, 1, 7);
  }

  @Test
  public void report_issue_on_tree_with_secondary() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);
    Tree firstMember = tree.members().get(0);
    Tree secondMember = tree.members().get(1);

    ArrayList<Location> secondary = new ArrayList<>();
    secondary.add(new JavaFileScannerContext.Location("secondary", firstMember));
    secondary.add(new JavaFileScannerContext.Location("secondary", secondMember));

    context.reportIssue(CHECK, tree.simpleName(), "msg", secondary, null);

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isNull();
    assertThat(reportedMessage.flows).hasSize(2);

    assertMessagePosition(reportedMessage, 1, 6, 1, 7);
    List<AnalyzerMessage> secondaries = reportedMessage.flows.stream().map(flow -> flow.get(0)).collect(Collectors.toList());
    assertThat(secondaries).hasSize(2);
    assertMessagePosition(secondaries.get(0), 2, 2, 2, 13);
    assertMessagePosition(secondaries.get(1), 3, 2, 3, 15);
  }

  @Test
  public void report_issue_between_two_trees() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);
    VariableTree firstMember = (VariableTree) tree.members().get(0);
    VariableTree secondMember = (VariableTree) tree.members().get(1);

    context.reportIssue(CHECK, firstMember.simpleName(), secondMember.equalToken(), "msg");

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isNull();
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 2, 6, 3, 10);
  }

  private static void assertMessagePosition(AnalyzerMessage message, int startLine, int startColumn, int endLine, int endColumn) {
    TextSpan location = message.primaryLocation();
    assertThat(location.startLine).isEqualTo(startLine);
    assertThat(location.startCharacter).isEqualTo(startColumn);
    assertThat(location.endLine).isEqualTo(endLine);
    assertThat(location.endCharacter).isEqualTo(endColumn);
  }

  private SonarComponents createSonarComponentsMock() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    doAnswer(invocation -> {
      reportedMessage = (AnalyzerMessage) invocation.getArguments()[0];
      return null;
    }).when(sonarComponents).reportIssue(any(AnalyzerMessage.class));

    doAnswer(invocation -> {
      Integer cost = invocation.getArgument(4);
      reportedMessage = new AnalyzerMessage(invocation.getArgument(1),
        JAVA_INPUT_FILE,
        null,
        invocation.getArgument(3),
        cost != null ? cost : 0);
      return null;
    }).when(sonarComponents).addIssue(any(File.class), any(JavaCheck.class), anyInt(), anyString(), any());

    doAnswer(invocation -> {
      Integer cost = invocation.getArgument(4);
      reportedMessage = new AnalyzerMessage(invocation.getArgument(1),
        invocation.getArgument(0),
        null,
        invocation.getArgument(3),
        cost != null ? cost : 0);
      return null;
    }).when(sonarComponents).addIssue(any(InputComponent.class), any(JavaCheck.class), anyInt(), anyString(), any());

    when(sonarComponents.fileLines(any(InputFile.class))).thenReturn(Collections.emptyList());
    when(sonarComponents.inputFileContents(any(InputFile.class))).thenReturn("content");
    when(sonarComponents.baseDir()).thenReturn(BASE_DIR);
    when(sonarComponents.workDir()).thenReturn(WORK_DIR);
    when(sonarComponents.project()).thenReturn(PROJECT_BASE_DIR);

    return sonarComponents;
  }
}
