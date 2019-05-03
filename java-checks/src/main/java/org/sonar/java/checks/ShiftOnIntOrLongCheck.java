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
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Rule(key = "S2183")
public class ShiftOnIntOrLongCheck extends IssuableSubscriptionVisitor {

  private List<Tree> shiftTrees = new ArrayList<>();

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.LEFT_SHIFT, Kind.LEFT_SHIFT_ASSIGNMENT, Kind.RIGHT_SHIFT, Kind.RIGHT_SHIFT_ASSIGNMENT);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    shiftTrees.clear();
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    for (int i = 0; i < shiftTrees.size(); i++) {
      checkShiftTree(shiftTrees.get(i), i);
    }
  }

  @Override
  public void visitNode(Tree tree) {
    shiftTrees.add(tree);
  }

  private void checkShiftTree(Tree tree, int treeIndex) {
    String identifier;
    ExpressionTree shift;
    SyntaxToken operatorToken;

    if (tree.is(Kind.LEFT_SHIFT, Kind.RIGHT_SHIFT)) {
      BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
      if (isZeroMaskShift(binaryExpressionTree)) {
        // No issue should be reported for "1 << 0" or "1 >> 0"
        return;
      }
      identifier = getIdentifierName(binaryExpressionTree.leftOperand());
      shift = binaryExpressionTree.rightOperand();
      operatorToken = binaryExpressionTree.operatorToken();
    } else {
      AssignmentExpressionTree assignmentExpressionTree = (AssignmentExpressionTree) tree;
      identifier = getIdentifierName(assignmentExpressionTree.variable());
      shift = assignmentExpressionTree.expression();
      operatorToken = assignmentExpressionTree.operatorToken();
    }

    checkShift((ExpressionTree) tree, shift, identifier, operatorToken, treeIndex);
  }

  private void checkShift(ExpressionTree tree, ExpressionTree shift, @Nullable String identifier, SyntaxToken operatorToken,
                          int treeIndex) {
    Long literalValue = LiteralUtils.longLiteralValue(shift);
    if (literalValue != null) {
      int numericalBase = getNumericalBase(tree);
      long reducedNumberBits = literalValue % numericalBase;
      if (isInvalidShift(reducedNumberBits, literalValue, numericalBase, operatorToken, treeIndex)) {
        reportIssue(operatorToken, getMessage(reducedNumberBits, numericalBase, identifier));
      }
    }
  }

  private boolean isInvalidShift(long reducedNumberBits, long numberBits, int base, SyntaxToken operatorToken, int treeIndex) {
    return (reducedNumberBits == 0L && !aligned(operatorToken, treeIndex)) || tooManyBits(numberBits, base);
  }

  private boolean aligned(SyntaxToken operatorToken, int treeIndex) {
    return (treeIndex > 0 && isAlignedWith(operatorToken, shiftTrees.get(treeIndex - 1)))
      || (treeIndex + 1 < shiftTrees.size() && isAlignedWith(operatorToken, shiftTrees.get(treeIndex + 1)));
  }

  private static boolean isAlignedWith(SyntaxToken operatorToken, Tree other) {
    SyntaxToken otherOperator = operatorToken(other);
    return otherOperator.text().equals(operatorToken.text())
      && operatorToken.column() == otherOperator.column()
      // less than 2 lines distance
      && Math.abs(operatorToken.line() - otherOperator.line()) < 2;
  }

  private static SyntaxToken operatorToken(Tree tree) {
    if (tree instanceof BinaryExpressionTree) {
      return ((BinaryExpressionTree) tree).operatorToken();
    }
    return ((AssignmentExpressionTree) tree).operatorToken();
  }

  private static boolean isZeroMaskShift(BinaryExpressionTree binaryExpressionTree) {
    return isLiteralValue(binaryExpressionTree.leftOperand(), 1L) && isLiteralValue(binaryExpressionTree.rightOperand(), 0L);
  }

  private static boolean isLiteralValue(ExpressionTree tree, long value) {
    Long evaluatedValue = LiteralUtils.longLiteralValue(tree);
    return evaluatedValue != null && evaluatedValue == value;
  }

  private static String getMessage(long reducedNumberBits, int base, @Nullable String identifier) {
    if (reducedNumberBits == 0L) {
      return "Remove this useless shift";
    } else if (base == 32) {
      return MessageFormat.format(
        identifier == null ?
          "Either use a \"long\" or correct this shift to {0}" :
          "Either make \"{1}\" a \"long\" or correct this shift to {0}",
        reducedNumberBits, identifier);
    } else {
      return MessageFormat.format("Correct this shift to {0}", reducedNumberBits);
    }
  }

  private static int getNumericalBase(ExpressionTree tree) {
    if (tree.symbolType().is("int")) {
      return 32;
    }
    return 64;
  }

  private static boolean tooManyBits(long numberBits, int base) {
    return Math.abs(numberBits) >= base;
  }

  @CheckForNull
  private static String getIdentifierName(ExpressionTree tree) {
    if (tree.is(Kind.ARRAY_ACCESS_EXPRESSION)) {
      return getIdentifierName(((ArrayAccessExpressionTree) tree).expression());
    } else if (tree.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).name();
    }
    return null;
  }
}
