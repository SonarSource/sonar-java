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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6885")
public class MathClampMethodsCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CONDITIONAL_EXPRESSION, Tree.Kind.METHOD_INVOCATION, Tree.Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
      checkConditionalExpression((ConditionalExpressionTree) tree);
    }
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      checkMethodInvocation(tree);
    }
    if (tree.is(Tree.Kind.IF_STATEMENT)) {
      checkIfStatement((IfStatementTree) tree);
    }
  }

  private void checkIfStatement(IfStatementTree tree) {
    if (isGreaterThanOrLessThan(tree.condition())
      && (tree.elseStatement() != null && tree.elseStatement().is(Tree.Kind.IF_STATEMENT))) {
      IfStatementTree elseIfStatement = (IfStatementTree) tree.elseStatement();
      if (isGreaterThanOrLessThan(elseIfStatement.condition())) {
        reportIssue(tree, "Use \"Math.clamp\" instead of a conditional expression.");
      }
    }
  }

  private void checkMethodInvocation(Tree tree) {
    MethodInvocationTree mit = getMethodInvocationTree(tree);
    if (mit != null) {
      boolean isArgMathMinMax = mit.arguments().stream().anyMatch(arg -> getMethodInvocationTree(arg) != null);
      if (isArgMathMinMax) {
        reportIssue(mit, "Use \"Math.clamp\" instead of \"Math.min\" or \"Math.max\".");
      }
    }
  }

  private void checkConditionalExpression(ConditionalExpressionTree tree) {
    if (isGreaterThanOrLessThan(tree.condition())
      && (tree.trueExpression().is(Tree.Kind.CONDITIONAL_EXPRESSION) || tree.falseExpression().is(Tree.Kind.CONDITIONAL_EXPRESSION))) {
      reportIssue(tree, "Use \"Math.clamp\" instead of a conditional expression.");
    }
  }

  private static MethodInvocationTree getMethodInvocationTree(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) mit.methodSelect();
        if (memberSelectExpressionTree.expression().symbolType().is("java.lang.Math")
          && ("min".equals(memberSelectExpressionTree.identifier().name()) || "max".equals(memberSelectExpressionTree.identifier().name()))) {
          return mit;
        }
      }
    }
    return null;
  }

  private static boolean isGreaterThanOrLessThan(Tree tree) {
    return tree.is(Tree.Kind.GREATER_THAN) || tree.is(Tree.Kind.LESS_THAN);
  }

}
