/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2097",
  name = "\"equals(Object obj)\" should test argument type",
  tags = {"bug"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class EqualsArgumentTypeCheck extends SubscriptionBaseVisitor {

  private static final MethodInvocationMatcher EQUALS_MATCHER = MethodInvocationMatcher.create()
    .name("equals")
    .addParameter("java.lang.Object");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    if (methodTree.block() != null && "equals".equals(methodTree.symbol().name()) && methodTree.parameters().size() == 1) {
      Symbol parameterSymbol = methodTree.parameters().get(0).symbol();
      if (parameterSymbol.type().is("java.lang.Object")) {
        ExpressionVisitor visitor = new ExpressionVisitor(parameterSymbol);
        methodTree.accept(visitor);
        if (!visitor.typeChecked) {
          addIssue(tree, "Add a type test to this method.");
        }
      }
    }
  }

  private static class ExpressionVisitor extends BaseTreeVisitor {
    private final Symbol parameterSymbol;
    private boolean typeChecked;

    ExpressionVisitor(Symbol parameterSymbol) {
      this.parameterSymbol = parameterSymbol;
    }

    @Override
    public void visitInstanceOf(InstanceOfTree tree) {
      if (isArgument(tree.expression())) {
        typeChecked = true;
      }
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
        if (isGetClassOnArgument(tree.leftOperand()) || isGetClassOnArgument(tree.rightOperand())) {
          typeChecked = true;
        } else if (isExplicitComparison(tree.leftOperand(), tree.rightOperand())) {
          typeChecked = true;
        }
      } else {
        super.visitBinaryExpression(tree);
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (EQUALS_MATCHER.matches(tree)) {
        typeChecked = true;
      }
    }

    private boolean isArgument(ExpressionTree tree) {
      ExpressionTree expressionTree = removeParenthesis(tree);
      return expressionTree.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) expressionTree).symbol().equals(parameterSymbol);
    }

    private boolean isExplicitComparison(ExpressionTree operand1, ExpressionTree operand2) {
      return (isArgument(operand1) && isThis(operand2)) || (isArgument(operand2) && isThis(operand1));
    }

    private boolean isGetClassOnArgument(ExpressionTree tree) {
      ExpressionTree expressionTree = removeParenthesis(tree);
      if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
        ExpressionTree methodSelect = ((MethodInvocationTree) expressionTree).methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
          return isArgument(expression);
        }
      }
      return false;
    }

    private boolean isThis(ExpressionTree tree) {
      ExpressionTree expressionTree = removeParenthesis(tree);
      return expressionTree.is(Tree.Kind.IDENTIFIER) && "this".equals(((IdentifierTree) expressionTree).identifierToken().text());
    }

    private ExpressionTree removeParenthesis(ExpressionTree tree) {
      ExpressionTree result = tree;
      while (true) {
        if (result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
          result = ((ParenthesizedTree) result).expression();
        } else {
          return result;
        }
      }
    }
  }

}
