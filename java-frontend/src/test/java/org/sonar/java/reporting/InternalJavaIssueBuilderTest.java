/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.reporting;


import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalJavaIssueBuilderTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private static final File JAVA_FILE = new File("src/test/files/api/JavaFileInternalJavaIssueBuilderTest.java");
  private static final JavaCheck CHECK = new JavaCheck() {
  };
  private static final int COST = 42;
  private static final String REPOSITORY_KEY = "test";
  private static final RuleKey RULE_KEY = RuleKey.of(REPOSITORY_KEY, "key");

  private SensorContextTester sensorContextTester;
  private InternalJavaIssueBuilder builder;
  private CompilationUnitTree compilationUnitTree;
  private InputFile inputFile;

  @BeforeEach
  public void setup() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    sensorContextTester = SensorContextTester.create(new File(""));
    when(sonarComponents.context()).thenReturn(sensorContextTester);
    when(sonarComponents.getRuleKey(any())).thenReturn(Optional.of(RULE_KEY));

    inputFile = TestUtils.inputFile("src/test/files/api/JavaFileInternalJavaIssueBuilderTest.java");
    builder = new InternalJavaIssueBuilder(inputFile, sonarComponents);

    compilationUnitTree = JParserTestUtils.parse(JAVA_FILE);
  }

  @Test
  void test_build_issue_with_cost() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);

    builder.forRule(CHECK)
      .onTree(tree.simpleName())
      .withMessage("msg")
      .withCost(COST)
      .build();

    Collection<Issue> issues = sensorContextTester.allIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = (DefaultIssue) issues.iterator().next();

    assertThat(issue.ruleKey()).isSameAs(RULE_KEY);
    assertThat(issue.gap()).isEqualTo(COST);
    assertThat(issue.flows()).isEmpty();

    IssueLocation primaryLocation = issue.primaryLocation();
    assertThat(primaryLocation.message()).isEqualTo("msg");
    assertThat(primaryLocation.inputComponent()).isEqualTo(inputFile);
    assertPosition(primaryLocation.textRange(), 3, 6, 3, 7);
  }

  @Test
  void test_build_issue_with_formated_message() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);

    builder.forRule(CHECK)
      .onTree(tree.simpleName())
      .withMessage("msg %d %s %d", 42, "yolo", -1)
      .withCost(COST)
      .build();

    Collection<Issue> issues = sensorContextTester.allIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = (DefaultIssue) issues.iterator().next();

    assertThat(issue.ruleKey()).isSameAs(RULE_KEY);
    assertThat(issue.gap()).isEqualTo(COST);
    assertThat(issue.flows()).isEmpty();

    IssueLocation primaryLocation = issue.primaryLocation();
    assertThat(primaryLocation.message()).isEqualTo("msg 42 yolo -1");
    assertThat(primaryLocation.inputComponent()).isEqualTo(inputFile);
    assertPosition(primaryLocation.textRange(), 3, 6, 3, 7);
  }

  @Test
  void test_build_issue_with_secondary() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);
    Tree firstMember = tree.members().get(0);
    Tree secondMember = tree.members().get(1);

    builder.forRule(CHECK)
      .onTree(tree.simpleName())
      .withMessage("msg")
      .withSecondaries(new JavaFileScannerContext.Location("secondary1", firstMember),
        new JavaFileScannerContext.Location("secondary2", secondMember))
      .build();

    Collection<Issue> issues = sensorContextTester.allIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = (DefaultIssue) issues.iterator().next();
    assertThat(issue.gap()).isZero();
    List<Issue.Flow> flows = issue.flows();

    assertThat(flows).hasSize(2);

    IssueLocation secondary1 = flows.get(0).locations().get(0);
    IssueLocation secondary2 = flows.get(1).locations().get(0);
    assertPosition(secondary1.textRange(), 4, 2, 4, 13);
    assertThat(secondary1.message()).isEqualTo("secondary1");
    assertThat(secondary1.inputComponent()).isEqualTo(inputFile);
    assertPosition(secondary2.textRange(), 5, 2, 5, 15);
    assertThat(secondary2.message()).isEqualTo("secondary2");
    assertThat(secondary2.inputComponent()).isEqualTo(inputFile);
  }

  @Test
  void test_build_issue_with_flows() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);
    Tree firstMember = tree.members().get(0);
    Tree secondMember = tree.members().get(1);

    builder.forRule(CHECK)
      .onTree(tree.simpleName())
      .withMessage("msg")
      .withFlows(
        Collections.singletonList(
          Arrays.asList(
            new JavaFileScannerContext.Location("location1", firstMember),
            new JavaFileScannerContext.Location("location2", secondMember)
          )
        )
      ).build();

    Collection<Issue> issues = sensorContextTester.allIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = (DefaultIssue) issues.iterator().next();
    List<Issue.Flow> flows = issue.flows();

    assertThat(flows).hasSize(1);

    List<IssueLocation> locations = flows.get(0).locations();
    assertThat(locations).hasSize(2);

    IssueLocation location1 = locations.get(0);
    IssueLocation location2 = locations.get(1);
    assertPosition(location1.textRange(), 4, 2, 4, 13);
    assertThat(location1.message()).isEqualTo("location1");
    assertThat(location1.inputComponent()).isEqualTo(inputFile);
    assertPosition(location2.textRange(), 5, 2, 5, 15);
    assertThat(location2.message()).isEqualTo("location2");
    assertThat(location2.inputComponent()).isEqualTo(inputFile);
  }

  @Test
  void test_build_issue_on_range() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);
    Tree member = tree.members().get(0);
    builder.forRule(CHECK)
      .onRange(member.firstToken(), member.lastToken())
      .withMessage("msg")
      .build();

    Collection<Issue> issues = sensorContextTester.allIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = (DefaultIssue) issues.iterator().next();

    IssueLocation primaryLocation = issue.primaryLocation();
    assertPosition(primaryLocation.textRange(), 4, 2, 4, 13);
  }

  @Test
  void test_fields_default_values() {
    assertThat(builder.rule()).isNull();
    assertThat(builder.inputFile()).isEqualTo(inputFile);
    assertThat(builder.message()).isNull();
    assertThat(builder.textSpan()).isNull();
    assertThat(builder.cost()).isEmpty();
    assertThat(builder.secondaries()).isEmpty();
    assertThat(builder.flows()).isEmpty();
  }

  @Test
  void test_must_set_a_rule_first() {
    Tree tree = compilationUnitTree.types().get(0);
    assertThatThrownBy(() -> builder.onTree(tree))
      .hasMessage("A rule must be set first.")
      .isOfAnyClassIn(IllegalStateException.class);
  }

  @Test
  void test_must_set_a_position_first() {
    assertThatThrownBy(() -> builder.withMessage(""))
      .hasMessage("A position must be set first.")
      .isOfAnyClassIn(IllegalStateException.class);
  }

  @Test
  void test_must_set_a_message_first() {
    assertThatThrownBy(() -> builder.withCost(COST))
      .hasMessage("A message must be set first.")
      .isOfAnyClassIn(IllegalStateException.class);
  }

  @Test
  void test_cannot_set_position_multiple_times() {
    Tree tree = compilationUnitTree.types().get(0);
    builder = builder
      .forRule(CHECK)
      .onTree(tree);
    assertThatThrownBy(() -> builder.onRange(tree, tree))
      .hasMessage("Cannot set position multiple times.")
      .isOfAnyClassIn(IllegalStateException.class);
  }

  @Test
  void test_sonar_component_is_null() {
    InternalJavaIssueBuilder builder = new InternalJavaIssueBuilder(inputFile, null);
    builder.forRule(CHECK)
      .onTree(compilationUnitTree.types().get(0))
      .withMessage("msg")
      .build();

    assertThat(logTester.logs(LoggerLevel.TRACE)).containsExactly("SonarComponents is not set - discarding issue");
  }

  @Test
  void test_sonar_rule_key_not_registered() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.getRuleKey(any())).thenReturn(Optional.empty());
    InternalJavaIssueBuilder builder = new InternalJavaIssueBuilder(inputFile, sonarComponents);
    builder.forRule(CHECK)
      .onTree(compilationUnitTree.types().get(0))
      .withMessage("msg")
      .build();

    assertThat(logTester.logs(LoggerLevel.TRACE)).containsExactly("Rule not enabled - discarding issue");
  }

  @Test
  void test_cannot_set_flow_after_secondary() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);
    Tree firstMember = tree.members().get(0);
    JavaFileScannerContext.Location location = new JavaFileScannerContext.Location("location1", firstMember);

    builder = builder.forRule(CHECK)
      .onTree(tree.simpleName())
      .withMessage("msg")
      .withSecondaries(location);
    List<List<JavaFileScannerContext.Location>> flows = Collections.singletonList(Collections.singletonList(location));
    assertThatThrownBy(() -> builder.withFlows(flows))
      .hasMessage("Cannot set flows and secondaries at the same time.")
      .isOfAnyClassIn(IllegalStateException.class);
  }

  @Test
  void test_cannot_set_secondary_after_flow() {
    ClassTree tree = (ClassTree) compilationUnitTree.types().get(0);
    Tree firstMember = tree.members().get(0);
    JavaFileScannerContext.Location location = new JavaFileScannerContext.Location("location1", firstMember);

    builder = builder.forRule(CHECK)
      .onTree(tree.simpleName())
      .withMessage("msg")
      .withFlows(
        Collections.singletonList(Collections.singletonList(location))
      );

    assertThatThrownBy(() -> builder.withSecondaries(location))
      .hasMessage("Cannot set flows and secondaries at the same time.")
      .isOfAnyClassIn(IllegalStateException.class);
  }

  private static void assertPosition(TextRange location, int startLine, int startColumn, int endLine, int endColumn) {
    TextPointer start = location.start();
    TextPointer end = location.end();
    assertThat(start.line()).isEqualTo(startLine);
    assertThat(start.lineOffset()).isEqualTo(startColumn);
    assertThat(end.line()).isEqualTo(endLine);
    assertThat(end.lineOffset()).isEqualTo(endColumn);
  }

}
