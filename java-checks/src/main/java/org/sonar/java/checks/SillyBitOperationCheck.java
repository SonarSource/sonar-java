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
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S2437",
  name = "Silly bit operations should not be performed",
  tags = {"bug"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class SillyBitOperationCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(
      Kind.XOR,
      Kind.XOR_ASSIGNMENT,
      Kind.AND,
      Kind.AND_ASSIGNMENT,
      Kind.OR,
      Kind.OR_ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    Long identityElement = getBitwiseOperationIdentityElement(tree);
    Long evaluatedExpression = evaluateExpression(getExpression(tree));

    if (evaluatedExpression != null && identityElement.equals(evaluatedExpression)) {
      addIssue(tree, "Remove this silly bit operation.");
    }
  }

  private Long getBitwiseOperationIdentityElement(Tree tree) {
    Long identityElement = 0L;
    if (tree.is(Kind.AND, Kind.AND_ASSIGNMENT)) {
      identityElement = -1L;
    }
    return identityElement;
  }

  private ExpressionTree getExpression(Tree tree) {
    ExpressionTree expression;
    if (tree.is(Kind.OR, Kind.XOR, Kind.AND)) {
      expression = ((BinaryExpressionTree) tree).rightOperand();
    } else {
      expression = ((AssignmentExpressionTree) tree).expression();
    }
    return expression;
  }

  @Nullable
  private Long evaluateExpression(ExpressionTree tree) {
    ExpressionTree expression = tree;

    int sign = expression.is(Kind.UNARY_MINUS) ? -1 : 1;
    if (expression.is(Kind.UNARY_MINUS, Kind.UNARY_PLUS)) {
      expression = ((UnaryExpressionTree) expression).expression();
    }

    if (expression.is(Kind.INT_LITERAL, Kind.LONG_LITERAL)) {
      return sign * Long.decode(LiteralUtils.trimLongSuffix(((LiteralTree) expression).value()));
    }
    return null;
  }
}
