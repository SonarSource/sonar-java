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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ComparisonMethodUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9148")
public class FloatingPointComparisonCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Use \"Double.compare\" or \"Float.compare\" to compare floating-point values.";

  private static final MethodMatchers FLOAT_DOUBLE_COMPARE = MethodMatchers.or(
    MethodMatchers.create().ofTypes("java.lang.Float").names("compare").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("java.lang.Double").names("compare").withAnyParameters().build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ComparisonMethodUtils.nodesToVisit();
  }

  @Override
  public void visitNode(Tree tree) {
    ComparisonMethodUtils.visitComparisonNode(context, tree,
      methodTree -> {
        Set<Tree> suppressedSubtractions = collectSuppressedSubtractions(methodTree.block());
        methodTree.block().accept(new FloatingPointComparisonVisitor(suppressedSubtractions));
      },
      lambda -> {
        Set<Tree> suppressedSubtractions = collectSuppressedSubtractions(lambda.body());
        lambda.body().accept(new FloatingPointComparisonVisitor(suppressedSubtractions));
      });
  }

  private static Set<Tree> collectSuppressedSubtractions(Tree tree) {
    var collector = new CompareArgumentCollector();
    tree.accept(collector);
    return collector.suppressedSubtractions;
  }

  private static boolean hasFloatingType(ExpressionTree tree) {
    return tree.symbolType().isPrimitive(Type.Primitives.FLOAT)
      || tree.symbolType().isPrimitive(Type.Primitives.DOUBLE);
  }

  private static class CompareArgumentCollector extends ComparisonMethodUtils.SkipNestedTypesVisitor {
    final Set<Tree> suppressedSubtractions = new HashSet<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (FLOAT_DOUBLE_COMPARE.matches(tree)) {
        for (ExpressionTree argument : tree.arguments()) {
          collectSubtractionFromArgument(argument);
        }
      }
      super.visitMethodInvocation(tree);
    }

    private void collectSubtractionFromArgument(ExpressionTree argument) {
      if (argument.is(Tree.Kind.MINUS)) {
        suppressedSubtractions.add(argument);
      } else if (argument.is(Tree.Kind.IDENTIFIER)) {
        Tree initializer = getVariableInitializer((IdentifierTree) argument);
        if (initializer != null && initializer.is(Tree.Kind.MINUS)) {
          suppressedSubtractions.add(initializer);
        }
      }
    }

    private static Tree getVariableInitializer(IdentifierTree identifier) {
      Symbol symbol = identifier.symbol();
      if (symbol.isVariableSymbol()) {
        Tree declaration = symbol.declaration();
        if (declaration instanceof VariableTree variableTree) {
          return variableTree.initializer();
        }
      }
      return null;
    }
  }

  private class FloatingPointComparisonVisitor extends ComparisonMethodUtils.SkipNestedTypesVisitor {

    private final Set<Tree> suppressedSubtractions;

    FloatingPointComparisonVisitor(Set<Tree> suppressedSubtractions) {
      this.suppressedSubtractions = suppressedSubtractions;
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (isProblematicExpression(tree) && hasFloatingOperand(tree)) {
        reportIssue(tree.operatorToken(), MESSAGE);
      }
      super.visitBinaryExpression(tree);
    }

    private boolean isProblematicExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.LESS_THAN, Tree.Kind.GREATER_THAN,
        Tree.Kind.LESS_THAN_OR_EQUAL_TO, Tree.Kind.GREATER_THAN_OR_EQUAL_TO)) {
        return true;
      }
      return tree.is(Tree.Kind.MINUS) && !suppressedSubtractions.contains(tree);
    }

    private boolean hasFloatingOperand(BinaryExpressionTree tree) {
      return hasFloatingType(tree.leftOperand()) || hasFloatingType(tree.rightOperand());
    }
  }
}
