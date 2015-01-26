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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.text.MessageFormat;
import java.util.List;

@Rule(
  key = "S2183",
  priority = Priority.CRITICAL,
  tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class ShiftOnIntOrLongCheck extends SubscriptionBaseVisitor {

  private static final String USELESS_SHIFT_MESSAGE = "Remove this useless shift (multiple of {0})";
  private static final String INTEGER_NO_ID_MESSAGE = "Either use a \"long\" or correct this shift to {0}";
  private static final String INTEGER_ID_MESSAGE = "Either make \"{1}\" a \"long\" or correct this shift to {0}";
  private static final String LONG_MESSAGE = "Correct this shift to {0}";

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
      AssignmentExpressionTree assignmentExpressionTree = (AssignmentExpressionTree) tree;
      target = assignmentExpressionTree.variable();
      shift = assignmentExpressionTree.expression();
    }

    String identifier = getIdentifierName(target);
    Type expectedType = ((AbstractTypedTree) target).getSymbolType();
    boolean expectLong = expectedType.is("long") || expectedType.is("java.lang.Long");
    boolean expectInt = expectedType.is("int") || expectedType.is("java.lang.Integer");
    boolean shiftIsMinus = shift.is(Kind.UNARY_MINUS);

    if (shift.is(Kind.UNARY_MINUS, Kind.UNARY_PLUS)) {
      shift = ((UnaryExpressionTree) shift).expression();
    }

    if ((expectInt || expectLong) && isShiftValueEvaluable(shift)) {
      String value = ((LiteralTree) shift).value();
      long numberBits = (shiftIsMinus ? -1 : 1) * Long.parseLong(value);
      long reducedNumberBits = numberBits % (expectLong ? 64 : 32);
      insertIssue(tree, numberBits, reducedNumberBits, expectInt, identifier);
    }
  }

  private void insertIssue(Tree tree, long numberBits, long reducedNumberBits, boolean expectInt, String identifier) {
    if (reducedNumberBits == 0L) {
      addIssue(tree, MessageFormat.format(USELESS_SHIFT_MESSAGE, expectInt ? 32 : 64));
    } else if (tooManyBits(numberBits, expectInt)) {
      if (expectInt) {
        String message = MessageFormat.format(identifier == null ? INTEGER_NO_ID_MESSAGE : INTEGER_ID_MESSAGE, reducedNumberBits, identifier);
        addIssue(tree, message);
      } else {
        addIssue(tree, MessageFormat.format(LONG_MESSAGE, reducedNumberBits));
      }
    }
  }

  private boolean tooManyBits(long numberBits, boolean isInt) {
    long value = Math.abs(numberBits);
    return (isInt && value >= 32) || (!isInt && value >= 64);
  }

  private boolean isShiftValueEvaluable(ExpressionTree tree) {
    return tree.is(Kind.INT_LITERAL, Kind.LONG_LITERAL);
  }

  private String getIdentifierName(ExpressionTree tree) {
    ExpressionTree expressionTree = tree;
    if (tree.is(Kind.ARRAY_ACCESS_EXPRESSION)) {
      expressionTree = ((ArrayAccessExpressionTree) tree).expression();
    }
    return expressionTree.is(Kind.IDENTIFIER) ? ((IdentifierTree) expressionTree).name() : null;
  }
}
