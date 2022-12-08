/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.ExtendedIssueBuilderSubscriptionVisitor;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S1125")
public class BooleanLiteralCheck extends ExtendedIssueBuilderSubscriptionVisitor {

  private static final String FALSE_LITERAL = "false";
  private static final String TRUE_LITERAL = "true";

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO, Kind.CONDITIONAL_AND, Kind.CONDITIONAL_OR,
      Kind.LOGICAL_COMPLEMENT, Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    List<LiteralTree> literalList;
    if (tree.is(Kind.LOGICAL_COMPLEMENT)) {
      literalList = getBooleanLiterals(((UnaryExpressionTree) tree).expression());
    } else if (tree.is(Kind.CONDITIONAL_EXPRESSION)) {
      ConditionalExpressionTree expression = (ConditionalExpressionTree) tree;
      literalList = getBooleanLiterals(expression.trueExpression(), expression.falseExpression());
    } else {
      BinaryExpressionTree expression = (BinaryExpressionTree) tree;
      literalList = getBooleanLiterals(expression.leftOperand(), expression.rightOperand());
    }

    int nLiterals = literalList.size();
    if (nLiterals > 0) {
      newIssue()
        .onTree(literalList.get(0))
        .withMessage("Remove the unnecessary boolean literal%s.", nLiterals > 1 ? "s" : "")
        .withSecondaries(literalList.stream().skip(1).map(lit -> new JavaFileScannerContext.Location("", lit)).collect(Collectors.toList()))
        .withQuickFixes(() -> getQuickFix(tree))
        .report();
    }
  }

  private static List<LiteralTree> getBooleanLiterals(Tree... trees) {
    List<LiteralTree> booleanLiterals = new ArrayList<>();
    for (Tree t : trees) {
      if (t.is(Kind.NULL_LITERAL)) {
        return Collections.emptyList();
      } else if (t.is(Kind.BOOLEAN_LITERAL)) {
        booleanLiterals.add((LiteralTree) t);
      }
    }
    return booleanLiterals;
  }

  private static List<JavaQuickFix> getQuickFix(Tree tree) {
    List<JavaTextEdit> edits;
    if (tree.is(Kind.CONDITIONAL_EXPRESSION)) {
      edits = editsForConditionalExpression((ConditionalExpressionTree) tree);
    } else if (tree.is(Kind.LOGICAL_COMPLEMENT)) {
      String booleanValue = ((LiteralTree) ((UnaryExpressionTree) tree).expression()).value();
      edits = new ArrayList<>();
      edits.add(JavaTextEdit.replaceTree(tree, TRUE_LITERAL.equals(booleanValue) ? FALSE_LITERAL : TRUE_LITERAL));
    } else if (tree.is(Kind.EQUAL_TO)) {
      edits = editsForEquality((BinaryExpressionTree) tree, true);
    } else if (tree.is(Kind.NOT_EQUAL_TO)) {
      edits = editsForEquality((BinaryExpressionTree) tree, false);
    } else if (tree.is(Kind.CONDITIONAL_OR)) {
      edits = editsForConditional((BinaryExpressionTree) tree, true);
    } else {
      // Kind.CONDITIONAL_AND
      edits = editsForConditional((BinaryExpressionTree) tree, false);
    }

    if (edits.isEmpty()) {
      return Collections.emptyList();
    }
    return Collections.singletonList(JavaQuickFix.newQuickFix("Simplify the expression").addTextEdits(edits).build());
  }

  private static List<JavaTextEdit> editsForConditionalExpression(ConditionalExpressionTree tree) {
    List<JavaTextEdit> edits = new ArrayList<>();
    Boolean left = getBooleanValue(tree.trueExpression());
    Boolean right = getBooleanValue(tree.falseExpression());

    if (left != null) {
      if (right != null) {
        edits = editsForConditionalBothLiterals(tree, left, right);
      } else {
        if (left) {
          // cond() ? true : expr --> cond() || expr
          edits.add(JavaTextEdit.replaceBetweenTree(tree.questionToken(), tree.colonToken(), "||"));
        } else {
          // cond() ? false : expr --> !cond() && expr
          edits.add(JavaTextEdit.replaceBetweenTree(tree.questionToken(), tree.colonToken(), "&&"));
          List<JavaTextEdit> collection = computeNegatingTextEdits(tree.condition(), true);
          edits.addAll(collection);
        }
      }
    } else if (right != null) {
      // Defensive programming, if we reached this point, right must be a boolean literal
      edits.add(JavaTextEdit.removeTextSpan(textSpanBetween(tree.trueExpression(), false, tree.falseExpression(), true)));
      String operator;
      if (right) {
        // cond() ? expr : true --> !cond() || expr
        operator = "||";
        edits.add(JavaTextEdit.insertBeforeTree(tree.condition(), "!"));
      } else {
        // cond() ? expr : false --> cond() && expr
        operator = "&&";
      }
      edits.add(JavaTextEdit.replaceTree(tree.questionToken(), operator));
    }
    return edits;
  }

  private static List<JavaTextEdit> computeNegatingTextEdits(ExpressionTree tree, boolean followedByConjunction) {
    List<JavaTextEdit> edits = new ArrayList<>();

    if (tree.is(Kind.PARENTHESIZED_EXPRESSION)) {
      ParenthesizedTree expression = (ParenthesizedTree) tree;
      edits.addAll(computeNegatingTextEdits(expression.expression(), false));
    } else if (tree.is(Kind.EQUAL_TO)) {
      BinaryExpressionTree condition = (BinaryExpressionTree) tree;
      edits.add(JavaTextEdit.replaceTree(condition.operatorToken(), "!="));
    } else if (tree.is(Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree condition = (BinaryExpressionTree) tree;
      edits.add(JavaTextEdit.replaceTree(condition.operatorToken(), "=="));
    } else if (tree.is(Kind.CONDITIONAL_AND)) {
      BinaryExpressionTree condition = (BinaryExpressionTree) tree;
      if (followedByConjunction) {
        edits.add(JavaTextEdit.insertAfterTree(tree, ")"));
      }
      edits.addAll(computeNegatingTextEdits(condition.rightOperand(), followedByConjunction));
      edits.add(JavaTextEdit.replaceTree(condition.operatorToken(), "||"));
      edits.addAll(computeNegatingTextEdits(condition.leftOperand(), false));
      if (followedByConjunction) {
        edits.add(JavaTextEdit.insertBeforeTree(tree, "("));
      }
    } else if (tree.is(Kind.CONDITIONAL_OR)) {
      BinaryExpressionTree condition = (BinaryExpressionTree) tree;
      edits.addAll(computeNegatingTextEdits(condition.rightOperand(), followedByConjunction));
      edits.add(JavaTextEdit.replaceTree(condition.operatorToken(), "&&"));
      edits.addAll(computeNegatingTextEdits(condition.leftOperand(), true));
    } else {
      edits.add(JavaTextEdit.insertBeforeTree(tree, "!"));
    }

    return edits;
  }

  private static List<JavaTextEdit> editsForConditionalBothLiterals(ConditionalExpressionTree tree, Boolean left, Boolean right) {
    List<JavaTextEdit> edits = new ArrayList<>();
    // Both side are literals.
    JavaTextEdit editRemoveExpressions = JavaTextEdit.removeTextSpan(textSpanBetween(tree.condition(), false, tree.falseExpression(), true));
    if (left && !right) {
      // cond() ? true : false --> cond()
      edits.add(editRemoveExpressions);
    } else if (!left && right) {
      // cond() ? false : true --> !cond()
      edits.add(editRemoveExpressions);
      edits.add(JavaTextEdit.insertBeforeTree(tree, "!"));
    }
    // In case of "true : true" or "false : false", we do not add a quick fix as it looks like a bug (see S3923).
    return edits;
  }

  private static List<JavaTextEdit> editsForEquality(BinaryExpressionTree tree, boolean equalToOperator) {
    List<JavaTextEdit> edits = new ArrayList<>();

    ExpressionTree leftOperand = tree.leftOperand();
    ExpressionTree rightOperand = tree.rightOperand();
    Boolean left = getBooleanValue(leftOperand);
    Boolean right = getBooleanValue(rightOperand);

    if (left != null) {
      if (right != null) {
        edits.add(editForEqualityWhenBothLiterals(tree, left, right, equalToOperator));
      } else {
        // Presence of "!" is deducted from the inverse of the operator value.
        if (!left) {
          // false == expr -> !expr, false != expr --> expr
          equalToOperator = !equalToOperator;
        }
        edits.add(JavaTextEdit.replaceTextSpan(textSpanBetween(leftOperand, true, rightOperand, false), equalToOperator ? "" : "!"));
      }
    } else if (right != null) {
      // Defensive programming, if we reached this point, right must be a boolean literal
      edits = editsForEqualityWhenRightIsLiteral(right, leftOperand, rightOperand, equalToOperator);
    }

    return edits;
  }

  private static JavaTextEdit editForEqualityWhenBothLiterals(BinaryExpressionTree tree, Boolean left, Boolean right, boolean equalToOperator) {
    if (!left.equals(right)) {
      // left and right are not the same, simplification is the inverse of the operator value.
      // true == false --> false, false == true --> false, true != false --> true, false != true --> true
      equalToOperator = !equalToOperator;
    }
    // left and right are the same, simplification can be deducted thanks to the operator value.
    return JavaTextEdit.replaceTree(tree, equalToOperator ? TRUE_LITERAL : FALSE_LITERAL);
  }

  private static List<JavaTextEdit> editsForEqualityWhenRightIsLiteral(Boolean right, ExpressionTree leftOperand, ExpressionTree rightOperand, boolean equalToOperator) {
    List<JavaTextEdit> edits = new ArrayList<>();
    // Right operand is a literal
    if (!right.equals(equalToOperator)) {
      // expr == false or expr != true --> !expr
      edits.add(JavaTextEdit.insertBeforeTree(leftOperand, "!"));
    }
    edits.add(JavaTextEdit.removeTextSpan(textSpanBetween(leftOperand, false, rightOperand, true)));
    return edits;
  }


  private static List<JavaTextEdit> editsForConditional(BinaryExpressionTree tree, boolean conditionalOr) {
    List<JavaTextEdit> edits = new ArrayList<>();

    ExpressionTree leftOperand = tree.leftOperand();
    ExpressionTree rightOperand = tree.rightOperand();
    Boolean left = getBooleanValue(leftOperand);
    Boolean right = getBooleanValue(rightOperand);

    if (left != null) {
      if (right != null) {
        edits.add(editForConditionalWhenBothLiterals(tree, left, right, conditionalOr));
      } else {
        AnalyzerMessage.TextSpan textSpanToRemove;
        if (conditionalOr == left) {
          // true || var --> true or false && var --> false
          textSpanToRemove = textSpanBetween(leftOperand, false, rightOperand, true);
        } else {
          // false || var --> var or true && var --> var
          textSpanToRemove = textSpanBetween(leftOperand, true, rightOperand, false);
        }
        edits.add(JavaTextEdit.removeTextSpan(textSpanToRemove));
      }
    } else if (right != null) {
      // Defensive programming, if we reached this point, right must be a boolean literal
      editForConditionalWhenRightIsLiteral(right, leftOperand, rightOperand, conditionalOr)
        .ifPresent(edits::add);
    }

    return edits;
  }

  @Nullable
  private static Boolean getBooleanValue(Tree expression) {
    if (expression.is(Kind.BOOLEAN_LITERAL)) {
      return Boolean.parseBoolean(((LiteralTree) expression).value());
    }
    return null;
  }

  private static JavaTextEdit editForConditionalWhenBothLiterals(BinaryExpressionTree tree, Boolean left, Boolean right, boolean conditionalOr) {
    boolean conditionalAnd = !conditionalOr;
    boolean simplification =
      // true || true or true || false or false || true --> true
      (conditionalOr && (left || right))
        // true && true --> true
        || (conditionalAnd && left && right);

    return JavaTextEdit.replaceTree(tree, simplification ? TRUE_LITERAL : FALSE_LITERAL);
  }

  private static Optional<JavaTextEdit> editForConditionalWhenRightIsLiteral(Boolean right, ExpressionTree leftOperand, ExpressionTree rightOperand, boolean conditionalOr) {
    AnalyzerMessage.TextSpan textSpanToRemove;
    if (right.equals(conditionalOr)) {
      // var || true --> true or var && false --> false
      if (mayHaveSideEffect(leftOperand)) {
        // Can not remove a tree that could have side effect. We do not suggest to extract the side effect.
        return Optional.empty();
      }
      textSpanToRemove = textSpanBetween(leftOperand, true, rightOperand, false);
    } else {
      // var || false or var && true --> var
      textSpanToRemove = textSpanBetween(leftOperand, false, rightOperand, true);
    }
    return Optional.of(JavaTextEdit.removeTextSpan(textSpanToRemove));
  }

  private static boolean mayHaveSideEffect(Tree tree) {
    MethodInvocationFinder methodInvocationFinder = new MethodInvocationFinder();
    tree.accept(methodInvocationFinder);
    return methodInvocationFinder.found;
  }

  private static class MethodInvocationFinder extends BaseTreeVisitor {

    boolean found = false;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      found = true;
    }
  }

}
