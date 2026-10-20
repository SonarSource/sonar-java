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
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S9435")
public class NegatedTernaryConditionCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Invert this negated condition and swap the two branches of the ternary.";
  private static final String QUICK_FIX = "Invert the condition and swap the branches";

  private static final Tree.Kind[] COMPARISONS = {
    Tree.Kind.EQUAL_TO,
    Tree.Kind.NOT_EQUAL_TO,
    Tree.Kind.LESS_THAN,
    Tree.Kind.GREATER_THAN,
    Tree.Kind.LESS_THAN_OR_EQUAL_TO,
    Tree.Kind.GREATER_THAN_OR_EQUAL_TO
  };

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    ConditionalExpressionTree ternary = (ConditionalExpressionTree) tree;
    ExpressionTree condition = ternary.condition();
    if (isNegated(ExpressionUtils.skipParentheses(condition))) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(condition)
        .withMessage(MESSAGE)
        .withQuickFix(() -> invertAndSwap(ternary))
        .report();
    }
  }

  private JavaQuickFix invertAndSwap(ConditionalExpressionTree ternary) {
    ExpressionTree condition = ternary.condition();
    ExpressionTree stripped = ExpressionUtils.skipParentheses(condition);
    JavaQuickFix.Builder fix = JavaQuickFix.newQuickFix(QUICK_FIX);
    if (stripped.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      ExpressionTree positive = ((UnaryExpressionTree) stripped).expression();
      String replacement = withSeparator(condition, QuickFixHelper.contentForTree(positive, context));
      fix.addTextEdit(JavaTextEdit.replaceTree(condition, replacement));
    } else {
      fix.addTextEdit(JavaTextEdit.replaceTree(((BinaryExpressionTree) stripped).operatorToken(), "=="));
    }
    fix.addTextEdit(JavaTextEdit.replaceTree(ternary.trueExpression(), QuickFixHelper.contentForTree(ternary.falseExpression(), context)));
    fix.addTextEdit(JavaTextEdit.replaceTree(ternary.falseExpression(), falseBranchText(ternary.trueExpression())));
    return fix.build();
  }

  /**
   * The third operand of {@code ?:} cannot be an assignment, so an assignment moved out of the true branch
   * has to be parenthesized or the rewritten expression does not compile.
   */
  private String withSeparator(Tree replaced, String replacement) {
    if (replacement.isEmpty() || Character.isWhitespace(replacement.charAt(0))) {
      return replacement;
    }
    int column = Position.startOf(replaced).columnOffset();
    if (column == 0) {
      return replacement;
    }
    String line = context.getFileLines().get(Position.startOf(replaced).lineOffset());
    if (Character.isJavaIdentifierPart(line.charAt(column - 1))) {
      return " " + replacement;
    }
    return replacement;
  }

  private String falseBranchText(ExpressionTree formerTrueBranch) {
    String text = QuickFixHelper.contentForTree(formerTrueBranch, context);
    if (formerTrueBranch instanceof AssignmentExpressionTree) {
      return "(" + text + ")";
    }
    return text;
  }

  private static boolean isNegated(ExpressionTree condition) {
    if (condition.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      ExpressionTree negated = ExpressionUtils.skipParentheses(((UnaryExpressionTree) condition).expression());
      // A negated comparison such as "!(count > 0)" is already reported by S1940.
      return !negated.is(COMPARISONS);
    }
    return condition.is(Tree.Kind.NOT_EQUAL_TO) && !isNullComparison(condition);
  }

  private static boolean isNullComparison(ExpressionTree expression) {
    BinaryExpressionTree comparison = (BinaryExpressionTree) expression;
    return isNullLiteral(comparison.leftOperand()) || isNullLiteral(comparison.rightOperand());
  }

  private static boolean isNullLiteral(ExpressionTree expression) {
    return ExpressionUtils.skipParentheses(expression).is(Tree.Kind.NULL_LITERAL);
  }
}
