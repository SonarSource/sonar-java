/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.symexecengine;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.fest.assertions.Assertions;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataFlowVisitorTest {

  @Test
  public void test() {
    StringBuilder expectedIssuesOutput = new StringBuilder();
    StringBuilder raisedIssuesOutput = new StringBuilder();
    File file = new File("src/test/files/symexecengine/DataFlowVisitor.java");
    CompilationUnitTree compilationUnit = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(file);
    SemanticModel.createFor(compilationUnit, ImmutableList.<File>of());
    ClassTree classTree = (ClassTree) compilationUnit.types().get(0);
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD)) {
        MethodTree method = (MethodTree) member;
        Check check = new Check();
        DataFlowVisitor.analyze(method, check);
        Set<Integer> expectedIssues = getExpectedIssues(method);
        if (!check.raisedIssues.equals(expectedIssues)) {
          String methodName = method.simpleName().identifierToken().text();
          expectedIssuesOutput.append("in method ").append(methodName).append(" ").append(expectedIssues).append("\n");
          raisedIssuesOutput.append("in method ").append(methodName).append(" ").append(check.raisedIssues).append("\n");
        }
      }
    }
    Assertions.assertThat(raisedIssuesOutput.toString()).isEqualTo(expectedIssuesOutput.toString());
  }

  private Set<Integer> getExpectedIssues(MethodTree tree) {
    Set<Integer> result = new HashSet<>();
    SymbolMetadata methodMetadata = tree.symbol().metadata();
    if (methodMetadata.isAnnotatedWith("ExpectedIssues")) {
      AnnotationValue annotation = methodMetadata.valuesForAnnotation("ExpectedIssues").get(0);
      for (ExpressionTree expressionTree : ((NewArrayTree) annotation.value()).initializers()) {
        result.add(Integer.parseInt(((LiteralTree) expressionTree).value()));
      }
    }
    return result;
  }

  protected static class Check extends SymbolicExecutionCheck {
    private final Set<Integer> raisedIssues = new HashSet<>();

    @Override
    protected void onExecutableElementInvocation(ExecutionState executionState, Tree tree, List<ExpressionTree> arguments) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
      MemberSelectExpressionTree memberSelectTree = (MemberSelectExpressionTree) methodInvocation.methodSelect();
      IdentifierTree expressionTree = (IdentifierTree) memberSelectTree.expression();
      int value = Integer.parseInt(((LiteralTree) methodInvocation.arguments().get(0)).value());
      executionState.markValueAs(expressionTree.symbol(), new TestState(tree, ImmutableSet.of(value)));
    }

    @Override
    protected void onValueUnreachable(ExecutionState executionState, State state) {
      if (state instanceof TestState) {
        raisedIssues.addAll(((TestState) state).values);
      }
    }
  }

  private static class TestState extends State {
    private final Set<Integer> values;

    private TestState(Tree tree, Set<Integer> values) {
      super(tree);
      this.values = values;
    }

    private TestState(List<Tree> trees, Set<Integer> values) {
      super(trees);
      this.values = values;
    }

    @Override
    public State merge(State s) {
      Set<Integer> concat = new HashSet<>();
      concat.addAll(values);
      concat.addAll(((TestState) s).values);
      return new TestState(reportingTrees(), concat);
    }
  }

}
