/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import java.util.List;

@Rule(key = "S1764")
public class IdenticalOperandOnBinaryExpressionCheck extends IssuableSubscriptionVisitor {

  /**
   * symetric operators : a OP b is equivalent to b OP a
   */
  private static final List<Tree.Kind> SYMMETRIC_OPERATORS = ImmutableList.<Tree.Kind>builder()
    .add(Tree.Kind.EQUAL_TO)
    .add(Tree.Kind.NOT_EQUAL_TO)
    .add(Tree.Kind.AND)
    .add(Tree.Kind.XOR)
    .add(Tree.Kind.OR)
    .add(Tree.Kind.CONDITIONAL_AND)
    .add(Tree.Kind.CONDITIONAL_OR)
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.<Tree.Kind>builder()
      .add(Tree.Kind.DIVIDE)
      .add(Tree.Kind.REMAINDER)
      .add(Tree.Kind.MINUS)
      .add(Tree.Kind.LEFT_SHIFT)
      .add(Tree.Kind.RIGHT_SHIFT)
      .add(Tree.Kind.UNSIGNED_RIGHT_SHIFT)
      .add(Tree.Kind.LESS_THAN)
      .add(Tree.Kind.GREATER_THAN)
      .add(Tree.Kind.LESS_THAN_OR_EQUAL_TO)
      .add(Tree.Kind.GREATER_THAN_OR_EQUAL_TO)
      .add(Tree.Kind.EQUAL_TO)
      .add(Tree.Kind.NOT_EQUAL_TO)
      .add(Tree.Kind.AND)
      .add(Tree.Kind.XOR)
      .add(Tree.Kind.OR)
      .add(Tree.Kind.CONDITIONAL_AND)
      .add(Tree.Kind.CONDITIONAL_OR)
      .build();
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
    ExpressionTree rightOperand = binaryExpressionTree.rightOperand();
    ExpressionTree equivalentOperand = equivalentOperand(binaryExpressionTree, rightOperand);
    if (equivalentOperand != null) {
      reportIssue(
        rightOperand,
        "Identical sub-expressions on both sides of operator \"" + binaryExpressionTree.operatorToken().text() + "\"",
        ImmutableList.of(new JavaFileScannerContext.Location("", equivalentOperand)),
        null);
    }
  }

  @CheckForNull
  public static ExpressionTree equivalentOperand(BinaryExpressionTree tree, ExpressionTree rightOperand) {
    if (isNanTest(tree) || isLeftShiftOnOne(tree)) {
      return null;
    }
    return equivalentOperand(tree.leftOperand(), rightOperand, tree.kind());
  }

  public static ExpressionTree equivalentOperand(ExpressionTree left, ExpressionTree right, Tree.Kind binaryKind) {
    if (SyntacticEquivalence.areEquivalent(left, right)) {
      return left;
    }
    // Check other operands if operator is symmetric.
    if (SYMMETRIC_OPERATORS.contains(binaryKind) && left.is(binaryKind)) {
      ExpressionTree equivalent = equivalentOperand(((BinaryExpressionTree) left).leftOperand(), right, binaryKind);
      if (equivalent != null) {
        return equivalent;
      }
      return equivalentOperand(((BinaryExpressionTree) left).rightOperand(), right, binaryKind);
    }
    return null;
  }

  private static boolean isNanTest(BinaryExpressionTree tree) {
    Type leftOperandType = tree.leftOperand().symbolType();
    if (tree.is(Tree.Kind.NOT_EQUAL_TO) && (leftOperandType.isPrimitive(Type.Primitives.FLOAT) || leftOperandType.isPrimitive(Type.Primitives.DOUBLE))) {
      return true;
    }
    return false;
  }

  private static boolean isLeftShiftOnOne(BinaryExpressionTree tree) {
    // 1 << 1 is used for bit masks construction and should be excluded.
    if (tree.is(Tree.Kind.LEFT_SHIFT) && tree.leftOperand().is(Tree.Kind.INT_LITERAL) && tree.rightOperand().is(Tree.Kind.INT_LITERAL)) {
      String left = ((LiteralTree) tree.leftOperand()).value();
      String right = ((LiteralTree) tree.rightOperand()).value();
      if ("1".equals(right) && "1".equals(left)) {
        return true;
      }
    }
    return false;
  }

}
