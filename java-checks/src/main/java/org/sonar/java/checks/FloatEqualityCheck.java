/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1244")
public class FloatEqualityCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO, Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
    if (binaryExpressionTree.is(Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR) && isIndirectEquality(binaryExpressionTree)) {
      binaryExpressionTree = (BinaryExpressionTree) binaryExpressionTree.leftOperand();
    }
    if ((hasFloatingType(binaryExpressionTree.leftOperand()) || hasFloatingType(binaryExpressionTree.rightOperand())) && !isNanTest(binaryExpressionTree)) {
      reportIssue(binaryExpressionTree.operatorToken(), "Equality tests should not be made with floating point values.");
    }
  }

  private static boolean isIndirectEquality(BinaryExpressionTree binaryExpressionTree) {
    return isIndirectEquality(binaryExpressionTree, Tree.Kind.CONDITIONAL_AND, Tree.Kind.GREATER_THAN_OR_EQUAL_TO, Tree.Kind.LESS_THAN_OR_EQUAL_TO)
      || isIndirectEquality(binaryExpressionTree, Tree.Kind.CONDITIONAL_OR, Tree.Kind.GREATER_THAN, Tree.Kind.LESS_THAN);
  }

  private static boolean isIndirectEquality(BinaryExpressionTree binaryExpressionTree, Tree.Kind indirectOperator, Tree.Kind comparator1, Tree.Kind comparator2) {
    if (binaryExpressionTree.is(indirectOperator) && binaryExpressionTree.leftOperand().is(comparator1, comparator2)) {
      BinaryExpressionTree leftOp = (BinaryExpressionTree) binaryExpressionTree.leftOperand();
      if (binaryExpressionTree.rightOperand().is(comparator1, comparator2)) {
        BinaryExpressionTree rightOp = (BinaryExpressionTree) binaryExpressionTree.rightOperand();
        if (leftOp.kind().equals(rightOp.kind())) {
          //same operator
          return SyntacticEquivalence.areEquivalent(leftOp.leftOperand(), rightOp.rightOperand())
            && SyntacticEquivalence.areEquivalent(leftOp.rightOperand(), rightOp.leftOperand());
        } else {
          //different operator
          return SyntacticEquivalence.areEquivalent(leftOp.leftOperand(), rightOp.leftOperand())
            && SyntacticEquivalence.areEquivalent(leftOp.rightOperand(), rightOp.rightOperand());
        }
      }
    }
    return false;
  }


  private static boolean isNanTest(BinaryExpressionTree binaryExpressionTree) {
    return SyntacticEquivalence.areEquivalent(binaryExpressionTree.leftOperand(), binaryExpressionTree.rightOperand());
  }

  private static boolean hasFloatingType(ExpressionTree expressionTree) {
    return expressionTree.symbolType().isPrimitive(Type.Primitives.FLOAT) || expressionTree.symbolType().isPrimitive(Type.Primitives.DOUBLE);
  }

}
