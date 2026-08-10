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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9148")
public class FloatingPointComparisonCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Use \"Double.compare\" or \"Float.compare\" to compare floating-point values.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      if (isCompareToMethod(methodTree) || isCompareMethod(methodTree)) {
        methodTree.block().accept(new FloatingPointComparisonVisitor());
      }
    } else {
      LambdaExpressionTree lambda = (LambdaExpressionTree) tree;
      if (lambda.symbolType().isSubtypeOf("java.util.Comparator")) {
        lambda.body().accept(new FloatingPointComparisonVisitor());
      }
    }
  }

  private static boolean isCompareToMethod(MethodTree tree) {
    return "compareTo".equals(tree.simpleName().name())
      && returnsInt(tree)
      && hasOneNonPrimitiveParameter(tree);
  }

  private static boolean isCompareMethod(MethodTree tree) {
    return "compare".equals(tree.simpleName().name())
      && returnsInt(tree)
      && tree.parameters().size() == 2;
  }

  private static boolean returnsInt(MethodTree tree) {
    TypeTree returnType = tree.returnType();
    return returnType != null && returnType.symbolType().isPrimitive(Type.Primitives.INT);
  }

  private static boolean hasOneNonPrimitiveParameter(MethodTree tree) {
    List<VariableTree> parameters = tree.parameters();
    return parameters.size() == 1 && !parameters.get(0).type().symbolType().isPrimitive();
  }

  private static boolean hasFloatingType(ExpressionTree tree) {
    return tree.symbolType().isPrimitive(Type.Primitives.FLOAT)
      || tree.symbolType().isPrimitive(Type.Primitives.DOUBLE);
  }

  private class FloatingPointComparisonVisitor extends BaseTreeVisitor {

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.MINUS) && hasFloatingOperand(tree)) {
        reportIssue(tree.operatorToken(), MESSAGE);
      } else if (tree.is(Tree.Kind.LESS_THAN, Tree.Kind.GREATER_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO, Tree.Kind.GREATER_THAN_OR_EQUAL_TO)
        && hasFloatingOperand(tree)) {
        reportIssue(tree.operatorToken(), MESSAGE);
      }
      super.visitBinaryExpression(tree);
    }

    private boolean hasFloatingOperand(BinaryExpressionTree tree) {
      return hasFloatingType(tree.leftOperand()) || hasFloatingType(tree.rightOperand());
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
}
