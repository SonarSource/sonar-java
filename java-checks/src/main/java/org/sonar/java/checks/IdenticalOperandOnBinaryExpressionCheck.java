/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1764")
public class IdenticalOperandOnBinaryExpressionCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private static final String SECONDARY_MESSAGE = "Identical sub-expression.";

  private static final MethodMatchers EQUALS_MATCHER = MethodMatchers.create()
    .ofAnyType()
    .names("equals")
    .addParametersMatcher(JAVA_LANG_OBJECT)
    .build();

  private static final MethodMatchers OBJECTS_EQUALS_MATCHER = MethodMatchers.create()
    .ofTypes("java.util.Objects")
    .names("equals", "deepEquals")
    .addParametersMatcher(JAVA_LANG_OBJECT, JAVA_LANG_OBJECT)
    .build();

  /**
   * symetric operators : a OP b is equivalent to b OP a
   */
  private static final Set<Tree.Kind> SYMMETRIC_OPERATORS = SetUtils.immutableSetOf(
    Tree.Kind.EQUAL_TO,
    Tree.Kind.NOT_EQUAL_TO,
    Tree.Kind.AND,
    Tree.Kind.XOR,
    Tree.Kind.OR,
    Tree.Kind.CONDITIONAL_AND,
    Tree.Kind.CONDITIONAL_OR);

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.DIVIDE,
      Tree.Kind.REMAINDER,
      Tree.Kind.MINUS,
      Tree.Kind.LEFT_SHIFT,
      Tree.Kind.RIGHT_SHIFT,
      Tree.Kind.UNSIGNED_RIGHT_SHIFT,
      Tree.Kind.LESS_THAN,
      Tree.Kind.GREATER_THAN,
      Tree.Kind.LESS_THAN_OR_EQUAL_TO,
      Tree.Kind.GREATER_THAN_OR_EQUAL_TO,
      Tree.Kind.EQUAL_TO,
      Tree.Kind.NOT_EQUAL_TO,
      Tree.Kind.AND,
      Tree.Kind.XOR,
      Tree.Kind.OR,
      Tree.Kind.CONDITIONAL_AND,
      Tree.Kind.CONDITIONAL_OR,
      Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      checkEqualsMethods(mit);
      return;
    }
    BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
    ExpressionTree rightOperand = binaryExpressionTree.rightOperand();
    ExpressionTree equivalentOperand = equivalentOperand(binaryExpressionTree, rightOperand);
    if (equivalentOperand != null) {
      reportIssue(
        rightOperand,
        "Correct one of the identical sub-expressions on both sides of operator \"" + binaryExpressionTree.operatorToken().text() + "\"",
        Collections.singletonList(new JavaFileScannerContext.Location(SECONDARY_MESSAGE, equivalentOperand)),
        null);
    }
  }

  private void checkEqualsMethods(MethodInvocationTree mit) {
    if(EQUALS_MATCHER.matches(mit)) {
      if(mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        ExpressionTree leftOp = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
        ExpressionTree rightOp = mit.arguments().get(0);
        if(SyntacticEquivalence.areEquivalent(leftOp, rightOp)) {
          reportIssue(
            rightOp,
            "Correct one of the identical sub-expressions on both sides of equals.",
            Collections.singletonList(new JavaFileScannerContext.Location(SECONDARY_MESSAGE, leftOp)),
            null);
        }
      }
    } else if (OBJECTS_EQUALS_MATCHER.matches(mit)) {
      ExpressionTree leftOp = mit.arguments().get(0);
      ExpressionTree rightOp = mit.arguments().get(1);
      if(SyntacticEquivalence.areEquivalent(leftOp, rightOp)) {
        reportIssue(
          rightOp,
          "Correct one of the identical argument sub-expressions.",
          Collections.singletonList(new JavaFileScannerContext.Location(SECONDARY_MESSAGE, leftOp)),
          null);
      }
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
    return tree.is(Tree.Kind.NOT_EQUAL_TO) && (leftOperandType.isPrimitive(Type.Primitives.FLOAT) || leftOperandType.isPrimitive(Type.Primitives.DOUBLE));
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
