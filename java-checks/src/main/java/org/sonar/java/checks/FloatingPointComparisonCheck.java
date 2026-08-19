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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9148")
public class FloatingPointComparisonCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Use \"Double.compare\" or \"Float.compare\" to compare floating-point values.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ComparisonMethodUtils.nodesToVisit();
  }

  @Override
  public void visitNode(Tree tree) {
    ComparisonMethodUtils.visitComparisonNode(context, tree,
      methodTree -> methodTree.block().accept(new FloatingPointComparisonVisitor()),
      lambda -> lambda.body().accept(new FloatingPointComparisonVisitor()));
  }

  private static boolean hasFloatingType(ExpressionTree tree) {
    return tree.symbolType().isPrimitive(Type.Primitives.FLOAT)
      || tree.symbolType().isPrimitive(Type.Primitives.DOUBLE);
  }

  private class FloatingPointComparisonVisitor extends ComparisonMethodUtils.SkipNestedTypesVisitor {

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.MINUS, Tree.Kind.LESS_THAN, Tree.Kind.GREATER_THAN,
        Tree.Kind.LESS_THAN_OR_EQUAL_TO, Tree.Kind.GREATER_THAN_OR_EQUAL_TO)
        && hasFloatingOperand(tree)) {
        reportIssue(tree.operatorToken(), MESSAGE);
      }
      super.visitBinaryExpression(tree);
    }

    private boolean hasFloatingOperand(BinaryExpressionTree tree) {
      return hasFloatingType(tree.leftOperand()) || hasFloatingType(tree.rightOperand());
    }
  }
}
