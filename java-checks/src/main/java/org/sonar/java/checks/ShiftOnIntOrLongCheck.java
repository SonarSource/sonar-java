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
    boolean expectInt = expectedType.is("int") || expectedType.is("java.lang.Integer");
    int sign = shift.is(Kind.UNARY_MINUS) ? -1 : 1;

    if (shift.is(Kind.UNARY_MINUS, Kind.UNARY_PLUS)) {
      shift = ((UnaryExpressionTree) shift).expression();
    }

    if (shift.is(Kind.INT_LITERAL, Kind.LONG_LITERAL)) {
      String value = ((LiteralTree) shift).value();
      long numberBits = sign * Integer.decode(value);
      long reducedNumberBits = numberBits % (expectInt ? 32 : 64);
      String message = getMessage(numberBits, reducedNumberBits, expectInt, identifier);
      if (message != null) {
        addIssue(tree, message);
      }
    }
  }

  private String getMessage(long numberBits, long reducedNumberBits, boolean expectInt, String identifier) {
    if (reducedNumberBits == 0L) {
      return MessageFormat.format("Remove this useless shift (multiple of {0})", expectInt ? 32 : 64);
    } else if (tooManyBits(numberBits, expectInt)) {
      if (expectInt) {
        return MessageFormat.format(
          identifier == null ?
            "Either use a \"long\" or correct this shift to {0}" :
            "Either make \"{1}\" a \"long\" or correct this shift to {0}",
          reducedNumberBits, identifier);
      } else {
        return MessageFormat.format("Correct this shift to {0}", reducedNumberBits);
      }
    }
    return null;
  }

  private boolean tooManyBits(long numberBits, boolean expectInt) {
    long value = Math.abs(numberBits);
    return (expectInt && value >= 32) || (!expectInt && value >= 64);
  }

  private String getIdentifierName(ExpressionTree tree) {
    ExpressionTree expressionTree = tree;
    if (tree.is(Kind.ARRAY_ACCESS_EXPRESSION)) {
      expressionTree = ((ArrayAccessExpressionTree) tree).expression();
    }
    return expressionTree.is(Kind.IDENTIFIER) ? ((IdentifierTree) expressionTree).name() : null;
  }
}
