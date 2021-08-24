/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S1125")
public class BooleanLiteralCheck extends IssuableSubscriptionVisitor {

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
      QuickFixHelper.newIssue(context)
        .forRule(this)
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
        addEditsConditionalBothLiterals(tree, edits, left, right);
      } else {
        String operator;
        if (left) {
          // cond() ? true : expr --> cond() || expr
          operator = "||";
        } else {
          // cond() ? false : expr --> !cond() && expr
          operator = "&&";
          edits.add(JavaTextEdit.insertBeforeTree(tree.condition(), "!"));
        }
        edits.add(JavaTextEdit.replaceBetweenTree(tree.questionToken(), tree.colonToken(), operator));
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

  private static void addEditsConditionalBothLiterals(ConditionalExpressionTree tree, List<JavaTextEdit> edits, Boolean left, Boolean right) {
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
  }

  private static List<JavaTextEdit> editsForEquality(BinaryExpressionTree tree, boolean equalTo) {
    List<JavaTextEdit> edits = new ArrayList<>();

    ExpressionTree leftOperand = tree.leftOperand();
    ExpressionTree rightOperand = tree.rightOperand();
    Boolean left = getBooleanValue(leftOperand);
    Boolean right = getBooleanValue(rightOperand);

    if (left != null) {
      if (right != null) {
        addEqualityWhenBothLiterals(tree, edits, left, right, equalTo);
      } else {
        // Presence of "!" is deducted from the inverse of the operator value.
        if (!left) {
          // false == expr -> !expr, false != expr --> expr
          equalTo = !equalTo;
        }
        edits.add(JavaTextEdit.replaceTextSpan(textSpanBetween(leftOperand, true, rightOperand, false), equalTo ? "" : "!"));
      }
    } else if (right != null) {
      // Defensive programming, if we reached this point, right must be a boolean literal
      addForEqualityWhenRightIsLiteral(edits, right, leftOperand, rightOperand, equalTo);
    }

    return edits;
  }

  private static void addEqualityWhenBothLiterals(BinaryExpressionTree tree, List<JavaTextEdit> edits, Boolean left, Boolean right, boolean equalTo) {
    if (!left.equals(right)) {
      // left and right are not the same, simplification is the inverse of the operator value.
      // true == false --> false, false == true --> false, true != false --> true, false != true --> true
      equalTo = !equalTo;
    }
    // left and right are the same, simplification can be deducted thanks to the operator value.
    edits.add(JavaTextEdit.replaceTree(tree, equalTo ? TRUE_LITERAL : FALSE_LITERAL));
  }

  private static void addForEqualityWhenRightIsLiteral(List<JavaTextEdit> edits, Boolean right, ExpressionTree leftOperand, ExpressionTree rightOperand, boolean equalTo) {
    // Right operand is a literal
    if (!right.equals(equalTo)) {
      // expr == false or expr != true --> !expr
      edits.add(JavaTextEdit.insertBeforeTree(leftOperand, "!"));
    }
    edits.add(JavaTextEdit.removeTextSpan(textSpanBetween(leftOperand, false, rightOperand, true)));
  }


  private static List<JavaTextEdit> editsForConditional(BinaryExpressionTree tree, boolean conditionalOr) {
    List<JavaTextEdit> edits = new ArrayList<>();

    ExpressionTree leftOperand = tree.leftOperand();
    ExpressionTree rightOperand = tree.rightOperand();
    Boolean left = getBooleanValue(leftOperand);
    Boolean right = getBooleanValue(rightOperand);

    if (left != null) {
      if (right != null) {
        addEditsConditionalWhenBothLiterals(tree, edits, left, right, conditionalOr);
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
      addForConditionalWhenRightIsLiteral(edits, right, leftOperand, rightOperand, conditionalOr);
    }

    return edits;
  }

  private static void addEditsConditionalWhenBothLiterals(BinaryExpressionTree tree, List<JavaTextEdit> edits, Boolean left, Boolean right, boolean conditionalOr) {
    boolean replacement =
      // true || true or true || false or false || true --> true
      (conditionalOr && (left || right))
        // true && true --> true
        || (!conditionalOr && left && right);

    edits.add(JavaTextEdit.replaceTree(tree, replacement ? TRUE_LITERAL : FALSE_LITERAL));
  }

  private static void addForConditionalWhenRightIsLiteral(List<JavaTextEdit> edits, Boolean right, ExpressionTree leftOperand, ExpressionTree rightOperand, boolean conditionalOr) {
    AnalyzerMessage.TextSpan textSpanToRemove;
    if (right.equals(conditionalOr)) {
      // var || true --> true or var && false --> false
      if (mayHaveSideEffect(leftOperand)) {
        // Can not remove a tree that could have side effect. We do not suggest to extract the side effect.
        return;
      }
      textSpanToRemove = textSpanBetween(leftOperand, true, rightOperand, false);
    } else {
      // var || false or var && true --> var
      textSpanToRemove = textSpanBetween(leftOperand, false, rightOperand, true);
    }
    edits.add(JavaTextEdit.removeTextSpan(textSpanToRemove));
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

  @Nullable
  private static Boolean getBooleanValue(Tree expression) {
    if (expression.is(Kind.BOOLEAN_LITERAL)) {
      return Boolean.parseBoolean(((LiteralTree) expression).value());
    }
    return null;
  }

}
