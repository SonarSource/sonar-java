/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S2200")
public class CompareToResultTestCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers COMPARE_TO = MethodMatchers.create()
    .ofSubTypes("java.lang.Comparable")
    .names("compareTo")
    .addParametersMatcher(ANY)
    .build();

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpression = (BinaryExpressionTree) tree;
    ExpressionTree operand1 = ExpressionUtils.skipParentheses(binaryExpression.leftOperand());
    ExpressionTree operand2 = ExpressionUtils.skipParentheses(binaryExpression.rightOperand());
    if (isCompareToResult(operand1)) {
      checkCompareToOperand(binaryExpression, operand2, true);
    } else if (isCompareToResult(operand2)) {
      checkCompareToOperand(binaryExpression, operand1, false);
    }
  }

  public void checkCompareToOperand(BinaryExpressionTree binaryExpression, ExpressionTree operand, boolean compareToIsLeft) {
    Object resolvedOperandValue = ExpressionUtils.resolveAsConstant(operand);
    if (resolvedOperandValue instanceof Number number) {
      long operandValue = number.longValue();
      if (operandValue != 0) {
        reportIssue(binaryExpression, operandValue, compareToIsLeft);
      }
    }
  }

  private void reportIssue(BinaryExpressionTree binaryExpression, long operandValue, boolean compareToIsLeft) {
    InternalJavaIssueBuilder builder = QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(binaryExpression.operatorToken())
      .withMessage("Only the sign of the result should be examined.");
    if (binaryExpression.is(Kind.EQUAL_TO)) {
      // For !=, even if we could in theory replace by <=/>= 0, we do not suggest quick fixes and let the user figure out what was his intent
      builder.withQuickFix(() -> getQuickFix(binaryExpression, operandValue, compareToIsLeft));
    }
    builder.report();
  }

  private static JavaQuickFix getQuickFix(BinaryExpressionTree binaryExpression, long operandValue, boolean compareToIsLeft) {
    AnalyzerMessage.TextSpan textSpan;
    String newComparison;

    SyntaxToken operatorToken = binaryExpression.operatorToken();
    if (compareToIsLeft) {
      newComparison = operandValue < 0 ? "< 0" : "> 0";
      textSpan = AnalyzerMessage.textSpanBetween(operatorToken, true, binaryExpression.rightOperand(), true);
    } else {
      newComparison = operandValue < 0 ? "0 >" : "0 <";
      textSpan = AnalyzerMessage.textSpanBetween(binaryExpression.leftOperand(), true, operatorToken, true);
    }

    return JavaQuickFix.newQuickFix("Replace with \"%s\"", newComparison)
      .addTextEdit(JavaTextEdit.replaceTextSpan(textSpan, newComparison))
      .build();
  }

  private static boolean isCompareToResult(ExpressionTree expression) {
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      return COMPARE_TO.matches((MethodInvocationTree) expression);
    }
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      return isIdentifierContainingCompareToResult((IdentifierTree) expression);
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
      expression = ExpressionUtils.skipParentheses(expression);
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) expression;
        if (usages.contains(identifier)) {
          foundReAssignment = true;
        }
      }
    }
  }

}
