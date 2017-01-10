/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.AnalyzerMessage.TextSpan;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class DefaultJavaFileScannerContextTest {

  private static final File JAVA_FILE = new File("src/test/files/api/JavaFileScannerContext.java");
  private static final int COST = 42;
  private static final JavaCheck CHECK = new JavaCheck() {
  };
  private SonarComponents sonarComponents;
  private CompilationUnitTree compilationUnitTree;
  private DefaultJavaFileScannerContext context;
  private static AnalyzerMessage reportedMessage;

  @Before
  public void setup() {
    sonarComponents = createSonarComponentsMock();
    compilationUnitTree = (CompilationUnitTree) JavaParser.createParser(StandardCharsets.UTF_8).parse(JAVA_FILE);
    context = new DefaultJavaFileScannerContext(compilationUnitTree, JAVA_FILE, null, sonarComponents, null, true);
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

    context.reportIssue(CHECK, firstMember.simpleName(), ((VariableTreeImpl) secondMember).equalToken(), "msg");

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

  private static SonarComponents createSonarComponentsMock() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    doAnswer(invocation -> {
      reportedMessage = (AnalyzerMessage) invocation.getArguments()[0];
      return null;
    }).when(sonarComponents).reportIssue(any(AnalyzerMessage.class));

    return sonarComponents;
  }
}
