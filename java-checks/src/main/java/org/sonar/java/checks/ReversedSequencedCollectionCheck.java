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
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(key = "S6876")
public class ReversedSequencedCollectionCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  public static final String ISSUE_MESSAGE = "Use the \"reversed()\" method instead of manually iterating the list in reverse.";

  private static final MethodMatchers LIST_ITERATOR_MATCHER = MethodMatchers.create()
    .ofTypes("java.util.List")
    .names("listIterator")
    .addParametersMatcher("int")
    .build();

  private static final MethodMatchers LIST_SIZE_MATCHER = MethodMatchers.create()
    .ofTypes("java.util.List")
    .names("size")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers LIST_HAS_PREVIOUS_MATCHER = MethodMatchers.create()
    .ofTypes("java.util.ListIterator")
    .names("hasPrevious")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.FOR_STATEMENT, Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.FOR_STATEMENT)) {
      var forStatement = (ForStatementTree) tree;
      var condition = forStatement.condition();

      if (condition == null) {
        return;
      }

      if (condition.is(Tree.Kind.METHOD_INVOCATION) && LIST_HAS_PREVIOUS_MATCHER.matches((MethodInvocationTree) condition)) {
        checkHasPreviousCondition(forStatement);
      } else if (condition.is(Tree.Kind.GREATER_THAN_OR_EQUAL_TO) && ((BinaryExpressionTree) condition).leftOperand().is(Tree.Kind.IDENTIFIER)) {
        checkGreaterThanOrEqualCondition(forStatement);
      }

    } else if (tree.is(Tree.Kind.WHILE_STATEMENT)) {
      var whileStatement = (WhileStatementTree) tree;
      var condition = whileStatement.condition();

      if (condition.is(Tree.Kind.METHOD_INVOCATION) && LIST_HAS_PREVIOUS_MATCHER.matches((MethodInvocationTree) condition)) {
        reportIssue(whileStatement, ISSUE_MESSAGE);
      }
    }

  }

  private void checkGreaterThanOrEqualCondition(ForStatementTree forStatement) {
    if (forStatement.update().size() != 1) {
      return;
    }
    var update = forStatement.update().get(0);
    if (update.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      var expression = ((ExpressionStatementTree) update).expression();
      if (expression.is(Tree.Kind.POSTFIX_DECREMENT) || isMinusOneAssignment(expression)) {
        checkInitializer(forStatement);
      }
    }
  }

  private void checkInitializer(ForStatementTree forStatement) {
    if (forStatement.initializer().size() != 1) {
      return;
    }
    var initializer = forStatement.initializer().get(0);
    if (initializer.is(Tree.Kind.VARIABLE) && ((VariableTree) initializer).initializer() != null && ((VariableTree) initializer).initializer().is(Tree.Kind.MINUS)) {
      var minusOperation = (BinaryExpressionTree) ((VariableTree) initializer).initializer();
      if (minusOperation.leftOperand().is(Tree.Kind.METHOD_INVOCATION) && LIST_SIZE_MATCHER.matches((MethodInvocationTree) minusOperation.leftOperand())
        && (minusOperation.rightOperand().is(Tree.Kind.INT_LITERAL) && "1".equals((minusOperation.rightOperand()).firstToken().text()))) {
        reportIssue(forStatement, ISSUE_MESSAGE);
      }
    }
  }

  private static boolean isMinusOneAssignment(Tree tree) {
    if (tree.is(Tree.Kind.ASSIGNMENT)) {
      var assignment = (AssignmentExpressionTree) tree;
      if (assignment.expression().is(Tree.Kind.MINUS)) {
        var minusOperation = (BinaryExpressionTree) assignment.expression();
        return "1".equals(minusOperation.rightOperand().firstToken().text());
      }
    } else if (tree.is(Tree.Kind.MINUS_ASSIGNMENT)) {
      var minusOperation = (AssignmentExpressionTree) tree;
      return "1".equals(minusOperation.expression().firstToken().text());
    }
    return false;
  }

  private void checkHasPreviousCondition(ForStatementTree forStatement) {
    if (forStatement.initializer().size() != 1) {
      return;
    }
    var initializer = forStatement.initializer().get(0);
    if (initializer.is(Tree.Kind.VARIABLE)) {
      var variable = (VariableTree) initializer;
      var variableInitializer = variable.initializer();
      if (variableInitializer != null && variableInitializer.is(Tree.Kind.METHOD_INVOCATION) && LIST_ITERATOR_MATCHER.matches((MethodInvocationTree) variableInitializer)) {
        var arg = ((MethodInvocationTree) variableInitializer).arguments().get(0);
        if (arg.is(Tree.Kind.METHOD_INVOCATION) && LIST_SIZE_MATCHER.matches((MethodInvocationTree) arg)) {
          reportIssue(forStatement, ISSUE_MESSAGE);
        }
      }
    }
  }

}
