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
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;

import java.text.MessageFormat;
import java.util.List;

@Rule(
  key = "S2183",
  name = "Ints and longs should not be shifted by more than their number of bits-1",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class ShiftOnIntOrLongCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.LEFT_SHIFT, Kind.LEFT_SHIFT_ASSIGNMENT, Kind.RIGHT_SHIFT, Kind.RIGHT_SHIFT_ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    String identifier;
    ExpressionTree shift;

    if (tree.is(Kind.LEFT_SHIFT, Kind.RIGHT_SHIFT)) {
      BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
      if (isZeroMaskShift(binaryExpressionTree)) {
        return;
      }
      identifier = getIdentifierName(binaryExpressionTree.leftOperand());
      shift = binaryExpressionTree.rightOperand();
    } else {
      AssignmentExpressionTree assignmentExpressionTree = (AssignmentExpressionTree) tree;
      identifier = getIdentifierName(assignmentExpressionTree.variable());
      shift = assignmentExpressionTree.expression();
    }

    checkShiftValidity((ExpressionTree) tree, identifier, shift);
  }

  private void checkShiftValidity(ExpressionTree tree, String identifier, ExpressionTree shift) {
    int base = getBase(tree);
    Long numberBits = LiteralUtils.longLiteralValue(shift);
    if (numberBits != null) {
      long reducedNumberBits = numberBits % base;
      String message = getMessage(numberBits, reducedNumberBits, base, identifier);
      if (message != null) {
        addIssue(tree, message);
      }
    }
  }

  private boolean isZeroMaskShift(BinaryExpressionTree binaryExpressionTree) {
    return hasValue(binaryExpressionTree.leftOperand(), 1) && hasValue(binaryExpressionTree.rightOperand(), 0);
  }

  private boolean hasValue(ExpressionTree tree, long value) {
    Long evaluatedValue = LiteralUtils.longLiteralValue(tree);
    return evaluatedValue != null && evaluatedValue.longValue() == value;
  }

  @CheckForNull
  private String getMessage(long numberBits, long reducedNumberBits, int base, String identifier) {
    if (reducedNumberBits == 0L) {
      return "Remove this useless shift";
    } else if (tooManyBits(numberBits, base)) {
      if (base == 32) {
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

  private int getBase(ExpressionTree tree) {
    if (tree.symbolType().is("int")) {
      return 32;
    }
    return 64;
  }

  private boolean tooManyBits(long numberBits, int base) {
    return Math.abs(numberBits) >= base;
  }

  @CheckForNull
  private String getIdentifierName(ExpressionTree tree) {
    if (tree.is(Kind.ARRAY_ACCESS_EXPRESSION)) {
      return getIdentifierName(((ArrayAccessExpressionTree) tree).expression());
    } else if (tree.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).name();
    }
    return null;
  }
}
