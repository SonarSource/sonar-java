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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.List;

@Rule(
  key = "S2183",
  priority = Priority.CRITICAL,
  tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class ShiftOnIntOrLongCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.LEFT_SHIFT, Kind.LEFT_SHIFT_ASSIGNMENT, Kind.RIGHT_SHIFT, Kind.RIGHT_SHIFT_ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree target = null;
    ExpressionTree shift = null;

    if (tree.is(Kind.LEFT_SHIFT, Kind.RIGHT_SHIFT)) {
      BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
      target = binaryExpressionTree.leftOperand();
      shift = binaryExpressionTree.rightOperand();
    } else {
      AssignmentExpressionTreeImpl assignmentExpressionTree = (AssignmentExpressionTreeImpl) tree;
      target = assignmentExpressionTree.variable();
      shift = assignmentExpressionTree.expression();
    }

    String identifier = getIdentifierName(target);
    boolean expectLong = treeTypeIsLong(target);
    boolean expectInt = treeTypeIsInt(target);
    boolean shiftIsMinus = shift.is(Kind.UNARY_MINUS);

    if (shift.is(Kind.UNARY_MINUS, Kind.UNARY_PLUS)) {
      shift = ((UnaryExpressionTree) shift).expression();
    }

    if (isShiftEvaluable(shift, expectInt || expectLong)) {
      String value = ((LiteralTreeImpl) shift).value();
      long numberBits = (shiftIsMinus ? -1 : 1) * (expectInt ? Integer.parseInt(value) : Long.parseLong(value));
      long reducedNumberBits = numberBits % (expectLong ? 64 : 32);
      insertIssue(tree, numberBits, reducedNumberBits, expectInt, identifier);
    }
  }

  private void insertIssue(Tree tree, long numberBits, long reducedNumberBits, boolean expectInt, String identifier) {
    if (reducedNumberBits == 0L) {
      addIssue(tree, new StringBuilder("Remove this useless shift (multiple of ").append(expectInt ? "32" : "64").append(")").toString());
    } else if (tooManyBits(numberBits, expectInt)) {
      if (expectInt) {
        String identifierMarker = identifier == null ? "use" : new StringBuilder("make \"").append(identifier).append("\"").toString();
        addIssue(tree, new StringBuilder("Either ").append(identifierMarker).append(" a \"long\" or correct this shift to ").append(reducedNumberBits).toString());
      } else {
        addIssue(tree, new StringBuilder("Correct this shift to ").append(reducedNumberBits).toString());
      }
    }
  }

  private boolean tooManyBits(long numberBits, boolean isInt) {
    long value = Math.abs(numberBits);
    return (isInt && value >= 32) || (!isInt && value >= 64);
  }

  private boolean treeTypeIsInt(ExpressionTree tree) {
    return treeTypeIs(tree, "int") || treeTypeIs(tree, "java.lang.Integer");
  }

  private boolean treeTypeIsLong(ExpressionTree tree) {
    return treeTypeIs(tree, "long") || treeTypeIs(tree, "java.lang.Long");
  }

  private boolean treeTypeIs(ExpressionTree tree, String fullyQualifiedName) {
    return (tree.is(Kind.IDENTIFIER) && ((IdentifierTreeImpl) tree).getSymbolType().is(fullyQualifiedName)) ||
      (tree.is(Kind.ARRAY_ACCESS_EXPRESSION) && ((ArrayAccessExpressionTreeImpl) tree).getSymbolType().is(fullyQualifiedName)) ||
      (tree.is(Kind.PARENTHESIZED_EXPRESSION) && ((ParenthesizedTreeImpl) tree).getSymbolType().is(fullyQualifiedName)) ||
      (tree.is(Kind.METHOD_INVOCATION) && ((MethodInvocationTreeImpl) tree).getSymbolType().is(fullyQualifiedName));
  }

  private boolean isShiftEvaluable(ExpressionTree tree, boolean intOrLong) {
    return intOrLong && tree.is(Kind.INT_LITERAL, Kind.LONG_LITERAL);
  }

  private String getIdentifierName(ExpressionTree tree) {
    ExpressionTree expressionTree = tree;
    if (tree.is(Kind.ARRAY_ACCESS_EXPRESSION)) {
      expressionTree = ((ArrayAccessExpressionTree) tree).expression();
    }
    return expressionTree.is(Kind.IDENTIFIER) ? ((IdentifierTree) expressionTree).name() : null;
  }
}
