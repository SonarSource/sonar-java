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
package org.sonar.java.testing;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.FluentReporting;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaFileScannerContextForTestsTest {

  private static final JavaCheck CHECK = new DummyRule();
  private static JavaFileScannerContextForTests context;
  private static ClassTree classA;
  private static ClassTree classB;
  private static InputFile inputFile;

  @BeforeAll
  static void setup() {
    JavaTree.CompilationUnitTreeImpl cut = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(
      "package orf.foo;\n"
      + "class A {}\n"
      + "class B {}\n");
    classA = (ClassTree) cut.types().get(0);
    classB = (ClassTree) cut.types().get(1);

    inputFile = TestUtils.emptyInputFile("");
    JavaVersionImpl javaVersion = new JavaVersionImpl();
    SensorContextTester sensorContext = SensorContextTester.create(new File("."));
    DefaultFileSystem fileSystem = sensorContext.fileSystem();
    fileSystem.add(inputFile);

    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, null, null, null, null);

    context = new JavaFileScannerContextForTests(cut, inputFile, cut.sema, sonarComponents, javaVersion, false, false, null);
  }

  @BeforeEach
  void reset() {
    context.getIssues().clear();
  }

  @Test
  void newIssue() {
    assertThat(context.newIssue()).isInstanceOf(JavaIssueBuilderForTests.class);
  }

  @Test
  void test_issue_can_be_reported_only_once() {
    FluentReporting.JavaIssueBuilder builder = context.newIssue()
      .forRule(CHECK)
      .onTree(classA)
      .withMessage("msg");
    builder.report();

    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.report());
    assertThat(exception).hasMessage("Can only be reported once.");
  }

  @Test
  void reportIssue_on_tree() {
    context.reportIssue(CHECK, classA, "issue on A");

    Set<AnalyzerMessage> issues = context.getIssues();
    assertThat(issues).hasSize(1);
    AnalyzerMessage issue = issues.iterator().next();

    assertThat(issue.getCheck()).isInstanceOf(DummyRule.class);
    assertThat(issue.getMessage()).isEqualTo("issue on A");
    assertThat(issue.getInputComponent()).isEqualTo(inputFile);
    assertThat(issue.getLine()).isEqualTo(2);
    assertThat(issue.getCost()).isNull();
    assertThat(issue.flows).isEmpty();
    assertPosition(issue.primaryLocation(), 2, 0, 2, 10);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void reportIssue_on_tree_with_secondaries(boolean withSecondariesAndCost) {
    JavaFileScannerContext.Location location1 = new JavaFileScannerContext.Location("secondary message on }", classA.closeBraceToken());
    JavaFileScannerContext.Location location2 = new JavaFileScannerContext.Location("secondary message on {", classA.openBraceToken());
    List<JavaFileScannerContext.Location> secondaries = withSecondariesAndCost ? Arrays.asList(location1, location2) : Collections.emptyList();
    Integer cost = withSecondariesAndCost ? 42 : null;

    context.reportIssue(CHECK, classA, "issue on A", secondaries, cost);

    Set<AnalyzerMessage> issues = context.getIssues();
    assertThat(issues).hasSize(1);
    AnalyzerMessage issue = issues.iterator().next();

    assertThat(issue.getCheck()).isInstanceOf(DummyRule.class);
    assertThat(issue.getMessage()).isEqualTo("issue on A");
    assertThat(issue.getInputComponent()).isEqualTo(inputFile);
    assertThat(issue.getLine()).isEqualTo(2);

     if (withSecondariesAndCost) {
       assertThat(issue.getCost()).isEqualTo(42);
       assertThat(issue.flows)
         .hasSize(2)
         .allMatch(secondary -> secondary.size() == 1);
       AnalyzerMessage secondary = issue.flows.get(1).get(0);
       assertThat(secondary.getMessage()).isEqualTo("secondary message on {");
     } else {
       assertThat(issue.getCost()).isNull();
       assertThat(issue.flows).isEmpty();
     }
     assertPosition(issue.primaryLocation(), 2, 0, 2, 10);
  }

  @Test
  void reportIssue_between_trees() {
    context.reportIssue(CHECK, classA, classB, "issue on A and B");

    Set<AnalyzerMessage> issues = context.getIssues();
    assertThat(issues).hasSize(1);
    AnalyzerMessage issue = issues.iterator().next();

    assertThat(issue.getCheck()).isInstanceOf(DummyRule.class);
    assertThat(issue.getMessage()).isEqualTo("issue on A and B");
    assertThat(issue.getInputComponent()).isEqualTo(inputFile);
    assertThat(issue.getLine()).isEqualTo(2);
    assertThat(issue.getCost()).isNull();
    assertThat(issue.flows).isEmpty();
    assertPosition(issue.primaryLocation(), 2, 0, 3, 10);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void reportIssue_between_trees_with_secondaries(boolean withSecondariesAndCost) {
    JavaFileScannerContext.Location location1 = new JavaFileScannerContext.Location("secondary message on }", classA.closeBraceToken());
    JavaFileScannerContext.Location location2 = new JavaFileScannerContext.Location("secondary message on {", classA.openBraceToken());
    List<JavaFileScannerContext.Location> secondaries = withSecondariesAndCost ? Arrays.asList(location1, location2) : Collections.emptyList();
    Integer cost = withSecondariesAndCost ? 42 : null;

    context.reportIssue(CHECK, classA, classB, "issue on A and B", secondaries, cost);

    Set<AnalyzerMessage> issues = context.getIssues();
    assertThat(issues).hasSize(1);
    AnalyzerMessage issue = issues.iterator().next();

    assertThat(issue.getCheck()).isInstanceOf(DummyRule.class);
    assertThat(issue.getMessage()).isEqualTo("issue on A and B");
    assertThat(issue.getInputComponent()).isEqualTo(inputFile);
    assertThat(issue.getLine()).isEqualTo(2);

    if (withSecondariesAndCost) {
      assertThat(issue.getCost()).isEqualTo(42);
      assertThat(issue.flows)
        .hasSize(2)
        .allMatch(secondary -> secondary.size() == 1);
      AnalyzerMessage secondary = issue.flows.get(1).get(0);
      assertThat(secondary.getMessage()).isEqualTo("secondary message on {");
    } else {
      assertThat(issue.getCost()).isNull();
      assertThat(issue.flows).isEmpty();
    }
    assertPosition(issue.primaryLocation(), 2, 0, 3, 10);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void reportIssue_between_trees_with_flows(boolean withFlowsAndCost) {
    JavaFileScannerContext.Location location1 = new JavaFileScannerContext.Location("secondary message on }", classA.closeBraceToken());
    JavaFileScannerContext.Location location2 = new JavaFileScannerContext.Location("secondary message on {", classA.openBraceToken());
    Iterable<List<JavaFileScannerContext.Location>> flows = withFlowsAndCost ? Collections.singletonList(Arrays.asList(location1, location2)) : Collections.emptyList();
    Integer cost = withFlowsAndCost ? 42 : null;

    context.reportIssueWithFlow(CHECK, classA, "issue on A", flows, cost);

    Set<AnalyzerMessage> issues = context.getIssues();
    assertThat(issues).hasSize(1);
    AnalyzerMessage issue = issues.iterator().next();

    assertThat(issue.getCheck()).isInstanceOf(DummyRule.class);
    assertThat(issue.getMessage()).isEqualTo("issue on A");
    assertThat(issue.getInputComponent()).isEqualTo(inputFile);
    assertThat(issue.getLine()).isEqualTo(2);

    if (withFlowsAndCost) {
      assertThat(issue.getCost()).isEqualTo(42);
      assertThat(issue.flows)
        .hasSize(1)
        .allMatch(secondary -> secondary.size() == 2);
      AnalyzerMessage secondary = issue.flows.get(0).get(0);
      assertThat(secondary.getMessage()).isEqualTo("secondary message on }");
    } else {
      assertThat(issue.getCost()).isNull();
      assertThat(issue.flows).isEmpty();
    }
    assertPosition(issue.primaryLocation(), 2, 0, 2, 10);
  }

  @Test
  void test_quick_fixes_collected_in_context() {
    JavaQuickFix quickFix = JavaQuickFix
      .newQuickFix("desc")
      .build();

    ((JavaIssueBuilderForTests) context.newIssue())
      .forRule(CHECK)
      .onTree(classA)
      .withMessage("message")
      .withQuickFix(() -> quickFix)
      .report();

    Map<AnalyzerMessage.TextSpan, List<JavaQuickFix>> quickFixes = context.getQuickFixes();
    assertThat(quickFixes).hasSize(1);

    assertThat(quickFixes.values().iterator().next()).containsExactly(quickFix);
    assertThat(quickFixes.keySet().iterator().next()).hasToString("(2:0)-(2:10)");
  }

  private static void assertPosition(AnalyzerMessage.TextSpan location, int startLine, int startColumn, int endLine, int endColumn) {
    assertThat(location).isNotNull();
    assertThat(location.startLine).isEqualTo(startLine);
    assertThat(location.startCharacter).isEqualTo(startColumn);
    assertThat(location.endLine).isEqualTo(endLine);
    assertThat(location.endCharacter).isEqualTo(endColumn);
  }

  private static class DummyRule implements JavaCheck { }
}
