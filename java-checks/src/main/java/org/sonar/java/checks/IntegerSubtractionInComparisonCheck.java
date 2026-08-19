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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ComparisonMethodUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S9354")
public class IntegerSubtractionInComparisonCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Subtracting numeric values in %s can overflow; use %s instead.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ComparisonMethodUtils.nodesToVisit();
  }

  @Override
  public void visitNode(Tree tree) {
    ComparisonMethodUtils.visitComparisonNode(context, tree,
      methodTree -> methodTree.block().accept(new ComparisonResultVisitor(methodTree.simpleName().name())),
      lambda -> {
        Tree body = lambda.body();
        ComparisonResultVisitor visitor = new ComparisonResultVisitor("compare");
        if (body.is(Tree.Kind.BLOCK)) {
          body.accept(visitor);
        } else {
          visitor.checkComparisonResult((ExpressionTree) body);
        }
      });
  }

  private class ComparisonResultVisitor extends ComparisonMethodUtils.SkipNestedTypesVisitor {

    private final String enclosingMethodName;

    private ComparisonResultVisitor(String enclosingMethodName) {
      this.enclosingMethodName = enclosingMethodName;
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      ExpressionTree expression = tree.expression();
      if (expression != null) {
        checkComparisonResult(expression);
      }
    }

    private void checkComparisonResult(ExpressionTree expression) {
      ExpressionTree unwrapped = skipParenthesesAndIntCasts(expression);
      if (!unwrapped.is(Tree.Kind.MINUS)) {
        return;
      }
      String replacement = replacementFor((BinaryExpressionTree) unwrapped);
      if (replacement != null) {
        reportIssue(((BinaryExpressionTree) unwrapped).operatorToken(), String.format(MESSAGE, enclosingMethodName, replacement));
      }
    }
  }

  private static ExpressionTree skipParenthesesAndIntCasts(ExpressionTree expression) {
    ExpressionTree current = ExpressionUtils.skipParentheses(expression);
    while (current.is(Tree.Kind.TYPE_CAST)) {
      TypeCastTree cast = (TypeCastTree) current;
      if (!cast.type().symbolType().isPrimitive(Type.Primitives.INT)) {
        return current;
      }
      current = ExpressionUtils.skipParentheses(cast.expression());
    }
    return current;
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
