/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.RegexParserTestUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonar.java.reporting.FluentReporting;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.SourceMap;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.regex.ast.CurlyBraceQuantifier;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.RepetitionTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultJavaFileScannerContextTest extends DefaultInputFileScannerContextTest {

  @Test
  void get_complexity_nodes() {
    assertThat(context.getComplexityNodes(compilationUnitTree)).isEmpty();
  }

  @Test
  void get_tree() {
    assertThat(context.getTree()).isEqualTo(compilationUnitTree);
  }

  @Test
  void get_file_parsed() {
    assertThat(context.fileParsed()).isTrue();
  }

  @Test
  void get_java_version() {
    assertThat(context.getJavaVersion()).isNotNull();
  }

  @Test
  void in_android_context_false_by_default() {
    assertThat(context.inAndroidContext()).isFalse();
  }

  @Test
  void get_file_content() {
    assertThat(context.getFileContent())
      .isEqualTo("content")
      .isSameAs(context.getFileContent());
  }

  @Test
  void get_file_lines() {
    List<String> lines = context.getFileLines();
    assertThat(lines)
      .hasSize(2)
      .isSameAs(context.getFileLines())
      .noneMatch(line -> line.endsWith("\n"));

    assertThatThrownBy(() -> lines.add("new line")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void get_semantic_model() {
    assertThat(context.getSemanticModel()).isNull();
  }

  @Test
  void add_issue_on_file() {
    context.addIssueOnFile(CHECK, "file");

    assertThat(reportedMessage.getMessage()).isEqualTo("file");
    assertThat(reportedMessage.getInputComponent()).isEqualTo(JAVA_INPUT_FILE);
  }

  @Test
  void add_issue_on_project() {
    context.addIssueOnProject(CHECK, "msg");

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getInputComponent()).isEqualTo(PROJECT_BASE_DIR);
  }

  @Test
  void add_issue_no_file() {
    context.addIssue(10, CHECK, "msg2");

    assertThat(reportedMessage.getMessage()).isEqualTo("msg2");
    assertThat(reportedMessage.getInputComponent()).isEqualTo(JAVA_INPUT_FILE);
  }

  @Test
  void add_issue_no_file_with_cost() {
    context.addIssue(10, CHECK, "msg3", 2);

    assertThat(reportedMessage.getMessage()).isEqualTo("msg3");
    assertThat(reportedMessage.getInputComponent()).isEqualTo(JAVA_INPUT_FILE);
    assertThat(reportedMessage.getCost()).isEqualTo(2);
  }

  @Test
  void report_issue_on_tree() {
    context.reportIssue(CHECK, compilationUnitTree, "msg");

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isNull();
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 1, 0, 6, 1);
  }

  @Test
  void working_directory() {
    assertThat(context.getWorkingDirectory()).isNotNull();
  }

  @Test
  void report_issue_with_message() {
    AnalyzerMessage message = context.createAnalyzerMessage(CHECK, compilationUnitTree, "msg");

    context.reportIssue(message);

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isNull();
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 1, 0, 6, 1);
  }

  @Test
  void report_issue_on_tree_with_cross_file_scanner_throws() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
      .isThrownBy(() -> context.reportIssue(END_OF_ANALYSIS_CHECK, compilationUnitTree, "msg"))
      .withMessage("EndOfAnalysisCheck must only call reportIssue with AnalyzerMessage and must never pass a Tree reference.");
  }

  @Test
  void report_issue_on_tree_with_no_secondary() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);

    context.reportIssue(CHECK, tree.simpleName(), "msg", new ArrayList<>(), null);

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isNull();
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 3, 6, 3, 7);
  }

  @Test
  void report_issue_on_tree_with_cost() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);

    context.reportIssue(CHECK, tree.simpleName(), "msg", new ArrayList<>(), COST);

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isEqualTo(COST);
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 3, 6, 3, 7);
  }

  @Test
  void report_issue_on_tree_with_secondary() {
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

    assertMessagePosition(reportedMessage, 3, 6, 3, 7);
    List<AnalyzerMessage> secondaries = reportedMessage.flows.stream().map(flow -> flow.get(0)).toList();
    assertThat(secondaries).hasSize(2);
    assertMessagePosition(secondaries.get(0), 4, 2, 4, 13);
    assertMessagePosition(secondaries.get(1), 5, 2, 5, 15);
  }

  @Test
  void report_issue_between_two_trees() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);
    VariableTree firstMember = (VariableTree) tree.members().get(0);
    VariableTree secondMember = (VariableTree) tree.members().get(1);

    context.reportIssue(CHECK, firstMember.simpleName(), secondMember.equalToken(), "msg");

    assertThat(reportedMessage.getMessage()).isEqualTo("msg");
    assertThat(reportedMessage.getCost()).isNull();
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 4, 6, 5, 10);
  }

  @Test
  void report_issue_on_regex_tree() {
    RegexCheck regexCheck = new RegexCheck() {
    };
    String regex = "x{42}|y{23}";
    RegexTree regexTree = RegexParserTestUtils.assertSuccessfulParse(regex);
    DisjunctionTree disjunctionTree = (DisjunctionTree) regexTree;
    RepetitionTree y23 = (RepetitionTree) disjunctionTree.getAlternatives().get(1);
    CurlyBraceQuantifier rep23 = (CurlyBraceQuantifier) y23.getQuantifier();

    int cost = 42;

    context.reportIssue(regexCheck, rep23, "regexMsg", cost, Collections.emptyList());

    assertThat(reportedMessage.getMessage()).isEqualTo("regexMsg");
    assertThat(reportedMessage.getCost()).isEqualTo(Double.valueOf(cost));
    assertThat(reportedMessage.flows).isEmpty();

    assertMessagePosition(reportedMessage, 3, 8, 3, 12);
  }

  @Test
  void report_issue_on_regex_tree_with_secondary() {
    RegexCheck regexCheck = new RegexCheck() {
    };
    String regex = "x{42}|y{23}";
    RegexTree regexTree = RegexParserTestUtils.assertSuccessfulParse(regex);
    DisjunctionTree disjunctionTree = (DisjunctionTree) regexTree;

    RepetitionTree x42 = (RepetitionTree) disjunctionTree.getAlternatives().get(0);
    CurlyBraceQuantifier rep42 = (CurlyBraceQuantifier) x42.getQuantifier();

    RepetitionTree y23 = (RepetitionTree) disjunctionTree.getAlternatives().get(1);
    CurlyBraceQuantifier rep23 = (CurlyBraceQuantifier) y23.getQuantifier();

    RegexCheck.RegexIssueLocation secondary = new RegexCheck.RegexIssueLocation(rep42, "regexSecondary");
    context.reportIssue(regexCheck, rep23, "regexMsg", null, Collections.singletonList(secondary));

    assertThat(reportedMessage.getMessage()).isEqualTo("regexMsg");
    assertThat(reportedMessage.getCost()).isNull();
    assertMessagePosition(reportedMessage, 3, 8, 3, 12);

    assertThat(reportedMessage.flows).hasSize(1);
    List<AnalyzerMessage> reportedSecondaries = reportedMessage.flows.get(0);
    assertThat(reportedSecondaries).hasSize(1);

    AnalyzerMessage reportedSecondary = reportedSecondaries.get(0);
    assertThat(reportedSecondary.getMessage()).isEqualTo("regexSecondary");
    assertThat(reportedSecondary.getCost()).isNull();
    assertMessagePosition(reportedSecondary, 3, 2, 3, 6);
  }

  @Test
  void test_source_map() {
    GeneratedFile file = mock(GeneratedFile.class);
    SourceMap sourceMap = mock(SourceMap.class);
    when(file.sourceMap()).thenReturn(sourceMap);
    DefaultJavaFileScannerContext ctx = new DefaultJavaFileScannerContext(compilationUnitTree, file, null, sonarComponents, new JavaVersionImpl(), true, false);
    assertThat(ctx.sourceMap()).containsSame(sourceMap);

    ctx = new DefaultJavaFileScannerContext(compilationUnitTree, JAVA_INPUT_FILE, null, sonarComponents, new JavaVersionImpl(), true, false);
    assertThat(ctx.sourceMap()).isEmpty();
  }

  @Test
  void test_new_issue_return_a_builder() {
    assertThat(context.newIssue()).isInstanceOf(FluentReporting.JavaIssueBuilder.class);
  }

  @Test
  void test_getCacheContext_returns_created_cacheContext() {
    var sensorContext = mock(SensorContext.class);
    doAnswer(invocation -> true).when(sensorContext).isCacheEnabled();
    doAnswer(invocation -> null).when(sensorContext).previousCache();
    doAnswer(invocation -> null).when(sensorContext).nextCache();
    doAnswer(invocation -> sensorContext).when(sonarComponents).context();

    DefaultJavaFileScannerContext ctx = new DefaultJavaFileScannerContext(
      compilationUnitTree, JAVA_INPUT_FILE, null, sonarComponents, new JavaVersionImpl(), true, false);

    var cc = ctx.getCacheContext();
    assertThat(cc.isCacheEnabled()).isTrue();
    assertThat(cc.getReadCache()).isNotNull();
    assertThat(cc.getWriteCache()).isNotNull();
  }

  private static void assertMessagePosition(AnalyzerMessage message, int startLine, int startColumn, int endLine, int endColumn) {
    TextSpan location = message.primaryLocation();
    assertThat(location.startLine).isEqualTo(startLine);
    assertThat(location.startCharacter).isEqualTo(startColumn);
    assertThat(location.endLine).isEqualTo(endLine);
    assertThat(location.endCharacter).isEqualTo(endColumn);
  }
}
