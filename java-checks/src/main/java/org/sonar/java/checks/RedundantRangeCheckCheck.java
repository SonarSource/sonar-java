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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S9392")
public class RedundantRangeCheckCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this redundant range check; it is implied by the \"%s\" check.";

  @Override
  public List<Kind> nodesToVisit() {
    return List.of(Kind.CONDITIONAL_AND);
  }

  @Override
  public void visitNode(Tree tree) {
    Tree parent = ExpressionUtils.skipParenthesesUpwards(tree.parent());
    if (parent != null && parent.is(Kind.CONDITIONAL_AND)) {
      return;
    }

    Map<Symbol, List<Comparison>> comparisonsByVariable = new LinkedHashMap<>();
    collectComparisons((BinaryExpressionTree) tree, comparisonsByVariable);

    for (List<Comparison> comparisons : comparisonsByVariable.values()) {
      for (int i = 0; i < comparisons.size(); i++) {
        Comparison redundant = comparisons.get(i);
        for (int j = 0; j < comparisons.size(); j++) {
          if (i != j && comparisons.get(j).implies(redundant)) {
            if (redundant.implies(comparisons.get(j)) && j <= i) {
              continue;
            }
            reportIssue(redundant.tree, MESSAGE.formatted(comparisons.get(j).toString()));
            break;
          }
        }
      }
    }
  }

  private void collectComparisons(BinaryExpressionTree andTree, Map<Symbol, List<Comparison>> comparisonsByVariable) {
    ExpressionTree left = ExpressionUtils.skipParentheses(andTree.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(andTree.rightOperand());

    if (left.is(Kind.CONDITIONAL_AND)) {
      collectComparisons((BinaryExpressionTree) left, comparisonsByVariable);
    } else {
      tryAddComparison(left, comparisonsByVariable);
    }

    if (right.is(Kind.CONDITIONAL_AND)) {
      collectComparisons((BinaryExpressionTree) right, comparisonsByVariable);
    } else {
      tryAddComparison(right, comparisonsByVariable);
    }
  }

  private void tryAddComparison(ExpressionTree expr, Map<Symbol, List<Comparison>> comparisonsByVariable) {
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

    Symbol variable = null;
    Long constant = null;
    String operator = comparison.operatorToken().text();

    if (left.is(Kind.IDENTIFIER) && right.is(Kind.INT_LITERAL, Kind.LONG_LITERAL)) {
      variable = ((IdentifierTree) left).symbol();
      constant = extractConstantValue(right);
    } else if (right.is(Kind.IDENTIFIER) && left.is(Kind.INT_LITERAL, Kind.LONG_LITERAL)) {
      variable = ((IdentifierTree) right).symbol();
      constant = extractConstantValue(left);
      operator = flipOperator(operator);
    }

    if (variable != null && !variable.isUnknown() && constant != null) {
      return new Comparison(variable, operator, constant, comparison);
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
        return switch (this.operator) {
          case ">=" -> this.constant >= other.constant;
          case "<=" -> this.constant <= other.constant;
          case ">" -> this.constant >= other.constant;
          case "<" -> this.constant <= other.constant;
          default -> false;
        };
      }
      if (this.operator.equals(">") && other.operator.equals(">=")) {
        return this.constant >= other.constant;
      }
      if (this.operator.equals(">=") && other.operator.equals(">")) {
        return this.constant > other.constant;
      }
      if (this.operator.equals("<") && other.operator.equals("<=")) {
        return this.constant <= other.constant;
      }
      if (this.operator.equals("<=") && other.operator.equals("<")) {
        return this.constant < other.constant;
      }
      return false;
    }

    @Override
    public String toString() {
      return variable.name() + " " + operator + " " + constant;
    }
  }
}
