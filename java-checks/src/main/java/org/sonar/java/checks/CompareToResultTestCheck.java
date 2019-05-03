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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Rule(key = "S2200")
public class CompareToResultTestCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher COMPARE_TO = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("java.lang.Comparable"))
    .name("compareTo")
    .parameters(TypeCriteria.anyType());

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpression = (BinaryExpressionTree) tree;
    if (isInvalidTest(binaryExpression.leftOperand(), binaryExpression.rightOperand())) {
      reportIssue(binaryExpression.operatorToken(), "Only the sign of the result should be examined.");
    }
  }

  private boolean isInvalidTest(ExpressionTree operand1, ExpressionTree operand2) {
    return (isNonZeroInt(operand1) && isCompareToResult(operand2))
      || (isNonZeroInt(operand2) && isCompareToResult(operand1));
  }

  private boolean isCompareToResult(ExpressionTree expression) {
    if (hasSemantic()) {
      if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        return COMPARE_TO.matches((MethodInvocationTree) expression);
      }
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        return isIdentifierContainingCompareToResult((IdentifierTree) expression);
      }
    }
    return false;
  }

  private static boolean isIdentifierContainingCompareToResult(IdentifierTree identifier) {
    Symbol variableSymbol = identifier.symbol();
    if (!variableSymbol.isVariableSymbol()) {
      return false;
    }
    VariableTree variableDefinition = ((Symbol.VariableSymbol) variableSymbol).declaration();
    if (variableDefinition != null) {
      ExpressionTree initializer = variableDefinition.initializer();
      if (initializer != null && initializer.is(Tree.Kind.METHOD_INVOCATION) && variableSymbol.owner().isMethodSymbol()) {
        MethodTree method = ((Symbol.MethodSymbol) variableSymbol.owner()).declaration();
        return method != null && COMPARE_TO.matches((MethodInvocationTree) initializer) && !isReassigned(variableSymbol, method);
      }
    }
    return false;
  }

  private static boolean isNonZeroInt(ExpressionTree expression) {
    return isNonZeroIntLiteral(expression)
      || (expression.is(Tree.Kind.UNARY_MINUS) && isNonZeroIntLiteral(((UnaryExpressionTree) expression).expression()));
  }

  private static boolean isNonZeroIntLiteral(ExpressionTree expression) {
    return expression.is(Tree.Kind.INT_LITERAL) && !"0".equals(((LiteralTree) expression).value());
  }

  private static boolean isReassigned(Symbol variableSymbol, Tree method) {
    Collection<IdentifierTree> usages = variableSymbol.usages();
    ReAssignmentFinder reAssignmentFinder = new ReAssignmentFinder(usages);
    method.accept(reAssignmentFinder);
    return reAssignmentFinder.foundReAssignment;
  }

  private static class ReAssignmentFinder extends BaseTreeVisitor {

    private final Collection<IdentifierTree> usages;
    private boolean foundReAssignment = false;

    public ReAssignmentFinder(Collection<IdentifierTree> usages) {
      this.usages = usages;
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree unaryExp) {
      if (unaryExp.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.PREFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT)) {
        checkReAssignment(unaryExp.expression());
      }
      super.visitUnaryExpression(unaryExp);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree assignmentExpression) {
      checkReAssignment(assignmentExpression.variable());
      super.visitAssignmentExpression(assignmentExpression);
    }

    private void checkReAssignment(ExpressionTree expression) {
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) expression;
        if (usages.contains(identifier)) {
          foundReAssignment = true;
        }
      }
    }
  }

}
