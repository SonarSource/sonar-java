/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9354")
public class IntegerSubtractionInComparisonCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Subtracting numeric values in %s can overflow; use %s instead.";

  private static final MethodMatchers COMPARE_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("java.lang.Comparable")
      .names("compareTo")
      .addParametersMatcher(MethodMatchers.ANY)
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.Comparator")
      .names("compare")
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
      .build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null) {
      return;
    }
    if (tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      if (COMPARE_METHODS.matches(methodTree) && methodTree.block() != null) {
        methodTree.block().accept(new SubtractionInComparisonVisitor(methodTree.simpleName().name()));
      }
    } else {
      LambdaExpressionTree lambda = (LambdaExpressionTree) tree;
      if (lambda.symbolType().isSubtypeOf("java.util.Comparator")) {
        lambda.body().accept(new SubtractionInComparisonVisitor("compare"));
      }
    }
  }

  private class SubtractionInComparisonVisitor extends BaseTreeVisitor {

    private final String enclosingMethodName;

    private SubtractionInComparisonVisitor(String enclosingMethodName) {
      this.enclosingMethodName = enclosingMethodName;
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.MINUS)) {
        String replacement = replacementFor(tree);
        if (replacement != null) {
          reportIssue(tree.operatorToken(), String.format(MESSAGE, enclosingMethodName, replacement));
        }
      }
      super.visitBinaryExpression(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Do not visit inner classes
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree tree) {
      // Do not visit nested lambdas
    }
  }

  private static String replacementFor(BinaryExpressionTree tree) {
    Type left = primitiveOrSelf(tree.leftOperand());
    Type right = primitiveOrSelf(tree.rightOperand());
    if (left.isUnknown() || right.isUnknown() || isFloating(left) || isFloating(right)) {
      return null;
    }
    if (isLong(left) || isLong(right)) {
      return "Long.compare";
    }
    if (isInt(left) || isInt(right)) {
      return "Integer.compare";
    }
    return null;
  }

  private static Type primitiveOrSelf(ExpressionTree tree) {
    Type type = tree.symbolType();
    if (type.isPrimitive()) {
      return type;
    }
    Type primitive = type.primitiveType();
    return primitive != null ? primitive : type;
  }

  private static boolean isFloating(Type type) {
    return type.isPrimitive(Type.Primitives.FLOAT) || type.isPrimitive(Type.Primitives.DOUBLE);
  }

  private static boolean isLong(Type type) {
    return type.isPrimitive(Type.Primitives.LONG);
  }

  private static boolean isInt(Type type) {
    return type.isPrimitive(Type.Primitives.INT);
  }

}
