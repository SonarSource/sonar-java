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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S9392")
public class RedundantRangeCheckCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this redundant range check.";

  @Override
  public List<Kind> nodesToVisit() {
    return List.of(Kind.CONDITIONAL_AND);
  }

  @Override
  public void visitNode(Tree tree) {
    if (isNestedConditionalAnd(tree)) {
      return;
    }
    Map<Symbol, List<Comparison>> comparisonsByVariable = new LinkedHashMap<>();
    if (collectOperands((BinaryExpressionTree) tree, comparisonsByVariable)) {
      reportRedundantComparisons(comparisonsByVariable);
    }
  }

  private static boolean isNestedConditionalAnd(Tree tree) {
    Tree parent = ExpressionUtils.skipParenthesesUpwards(tree.parent());
    return parent != null && parent.is(Kind.CONDITIONAL_AND);
  }

  private void reportRedundantComparisons(Map<Symbol, List<Comparison>> comparisonsByVariable) {
    for (List<Comparison> comparisons : comparisonsByVariable.values()) {
      reportRedundantInGroup(comparisons);
    }
  }

  private void reportRedundantInGroup(List<Comparison> comparisons) {
    for (int i = 0; i < comparisons.size(); i++) {
      findAndReportImplyingComparison(comparisons, i);
    }
  }

  private void findAndReportImplyingComparison(List<Comparison> comparisons, int redundantIndex) {
    Comparison redundant = comparisons.get(redundantIndex);
    for (int j = 0; j < comparisons.size(); j++) {
      if (redundantIndex == j) {
        continue;
      }
      Comparison implying = comparisons.get(j);
      if (implying.implies(redundant) && !shouldSkipMutualImplication(redundant, implying, redundantIndex, j)) {
        var secondary = new JavaFileScannerContext.Location("Implying check", implying.tree);
        reportIssue(redundant.tree, MESSAGE, Collections.singletonList(secondary), null);
        return;
      }
    }
  }

  private static boolean shouldSkipMutualImplication(Comparison a, Comparison b, int indexA, int indexB) {
    if (!a.implies(b)) {
      return false;
    }
    if (SyntacticEquivalence.areEquivalent(a.tree, b.tree)) {
      return true;
    }
    return indexB <= indexA;
  }

  private static boolean collectOperands(BinaryExpressionTree andTree, Map<Symbol, List<Comparison>> comparisonsByVariable) {
    ExpressionTree left = ExpressionUtils.skipParentheses(andTree.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(andTree.rightOperand());

    if (!collectOperand(left, comparisonsByVariable) || !collectOperand(right, comparisonsByVariable)) {
      return false;
    }
    return true;
  }

  private static boolean collectOperand(ExpressionTree operand, Map<Symbol, List<Comparison>> comparisonsByVariable) {
    if (operand.is(Kind.CONDITIONAL_AND)) {
      return collectOperands((BinaryExpressionTree) operand, comparisonsByVariable);
    }
    if (hasPotentialSideEffects(operand)) {
      comparisonsByVariable.clear();
      return false;
    }
    tryAddComparison(operand, comparisonsByVariable);
    return true;
  }

  private static boolean hasPotentialSideEffects(ExpressionTree expr) {
    AtomicBoolean found = new AtomicBoolean(false);
    expr.accept(new BaseTreeVisitor() {
      @Override
      public void visitMethodInvocation(MethodInvocationTree tree) {
        found.set(true);
      }

      @Override
      public void visitMethodReference(MethodReferenceTree tree) {
        found.set(true);
      }

      @Override
      public void visitNewClass(NewClassTree tree) {
        found.set(true);
      }

      @Override
      public void visitUnaryExpression(UnaryExpressionTree tree) {
        if (tree.is(Kind.PREFIX_INCREMENT, Kind.PREFIX_DECREMENT, Kind.POSTFIX_INCREMENT, Kind.POSTFIX_DECREMENT)) {
          found.set(true);
        }
        super.visitUnaryExpression(tree);
      }

      @Override
      public void visitAssignmentExpression(AssignmentExpressionTree tree) {
        found.set(true);
      }
    });
    return found.get();
  }

  private static void tryAddComparison(ExpressionTree expr, Map<Symbol, List<Comparison>> comparisonsByVariable) {
    if (expr.is(Kind.GREATER_THAN, Kind.GREATER_THAN_OR_EQUAL_TO, Kind.LESS_THAN, Kind.LESS_THAN_OR_EQUAL_TO)) {
      Comparison comparisonObj = extractComparison((BinaryExpressionTree) expr);
      if (comparisonObj != null) {
        comparisonsByVariable
          .computeIfAbsent(comparisonObj.variable, k -> new ArrayList<>())
          .add(comparisonObj);
      }
    }
  }

  @Nullable
  private static Comparison extractComparison(BinaryExpressionTree comparison) {
    ExpressionTree left = ExpressionUtils.skipParentheses(comparison.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(comparison.rightOperand());

    String operator = comparison.operatorToken().text();

    if (left.is(Kind.IDENTIFIER) && right.is(Kind.INT_LITERAL, Kind.LONG_LITERAL)) {
      Symbol variable = ((IdentifierTree) left).symbol();
      Long constant = extractConstantValue(right);
      return createComparison(variable, operator, constant, comparison);
    } else if (right.is(Kind.IDENTIFIER) && left.is(Kind.INT_LITERAL, Kind.LONG_LITERAL)) {
      Symbol variable = ((IdentifierTree) right).symbol();
      Long constant = extractConstantValue(left);
      return createComparison(variable, flipOperator(operator), constant, comparison);
    }
    return null;
  }

  @Nullable
  private static Comparison createComparison(Symbol variable, String operator, @Nullable Long constant, BinaryExpressionTree tree) {
    if (!variable.isUnknown() && constant != null) {
      return new Comparison(variable, operator, constant, tree);
    }
    return null;
  }

  private static String flipOperator(String operator) {
    return switch (operator) {
      case "<" -> ">";
      case "<=" -> ">=";
      case ">" -> "<";
      case ">=" -> "<=";
      default -> operator;
    };
  }

  @Nullable
  private static Long extractConstantValue(ExpressionTree tree) {
    Integer intValue = LiteralUtils.intLiteralValue(tree);
    if (intValue != null) {
      return intValue.longValue();
    }
    return LiteralUtils.longLiteralValue(tree);
  }

  private static class Comparison {
    final Symbol variable;
    final String operator;
    final long constant;
    final Tree tree;

    Comparison(Symbol variable, String operator, long constant, Tree tree) {
      this.variable = variable;
      this.operator = operator;
      this.constant = constant;
      this.tree = tree;
    }

    boolean implies(Comparison other) {
      if (this.operator.equals(other.operator)) {
        return impliesSameOperator(other);
      }
      return impliesDifferentOperator(other);
    }

    private boolean impliesSameOperator(Comparison other) {
      return switch (this.operator) {
        case ">=" -> this.constant >= other.constant;
        case "<=" -> this.constant <= other.constant;
        case ">" -> this.constant >= other.constant;
        case "<" -> this.constant <= other.constant;
        default -> false;
      };
    }

    private boolean impliesDifferentOperator(Comparison other) {
      if (">".equals(this.operator) && ">=".equals(other.operator)) {
        return this.constant >= other.constant;
      }
      if (">=".equals(this.operator) && ">".equals(other.operator)) {
        return this.constant > other.constant;
      }
      if ("<".equals(this.operator) && "<=".equals(other.operator)) {
        return this.constant <= other.constant;
      }
      if ("<=".equals(this.operator) && "<".equals(other.operator)) {
        return this.constant < other.constant;
      }
      return false;
    }
  }
}
