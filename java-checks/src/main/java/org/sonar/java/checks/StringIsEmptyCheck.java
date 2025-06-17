/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

/**
 * Implement rule that <pre>CharSequence.length() == 0</pre> should be replaced with
 * <pre>CharSequence.isEmpty()</pre>.
 */
@Rule(key = "S7158")
public class StringIsEmptyCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final MethodMatchers STRING_LENGTH_METHOD = MethodMatchers.create()
    .ofTypes("java.lang.String")
    .names("length")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers CHARSEQUENCE_LENGTH_METHOD = MethodMatchers.create()
    .ofSubTypes("java.lang.CharSequence")
    .names("length")
    .addWithoutParametersMatcher()
    .build();

  private static final Kind[] TARGETED_BINARY_OPERATOR_TREES = {
    Kind.EQUAL_TO,
    Kind.NOT_EQUAL_TO,
    Kind.LESS_THAN,
    Kind.LESS_THAN_OR_EQUAL_TO,
    Kind.GREATER_THAN,
    Kind.GREATER_THAN_OR_EQUAL_TO
  };

  private enum ComparisonType {
    // for example, `s.length() == 0`
    IS_EMPTY,
    // for example, `s.length() > 0`
    IS_NOT_EMPTY,
    // for example, `s.length() > 80`
    OTHER
  }

  private static boolean isEmptinessCheck(ComparisonType comparisonType) {
    return comparisonType == ComparisonType.IS_EMPTY || comparisonType == ComparisonType.IS_NOT_EMPTY;
  }


  // `String.isEmpty()` is available since Java 6, but `CharSequence.isEmpty()` since Java 15
  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava6Compatible();
  }


  @Override
  public List<Kind> nodesToVisit() {
    return List.of(TARGETED_BINARY_OPERATOR_TREES);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree bet = (BinaryExpressionTree) tree;
    ExpressionTree left = ExpressionUtils.skipParentheses(bet.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(bet.rightOperand());

    boolean leftIsZero = LiteralUtils.isZero(left);
    boolean leftIsOne = LiteralUtils.isOne(left);

    boolean rightIsZero = LiteralUtils.isZero(right);
    boolean rightIsOne = LiteralUtils.isOne(right);

    final MethodInvocationTree lengthCall;
    final ComparisonType comparisonType;

    // First try `s.length() OPERATOR VALUE`, then try `VALUE OPERATOR s.length()`.
    MethodInvocationTree mit = getLengthCall(left);
    if (mit != null) {
      lengthCall = mit;
      comparisonType = getComparisonOnRight(bet, rightIsZero, rightIsOne);
    } else {
      lengthCall = getLengthCall(right);
      comparisonType = lengthCall != null ? getComparisonOnLeft(leftIsZero, leftIsOne, bet) : null;
    }

    if (lengthCall != null && isEmptinessCheck(comparisonType)) {
      QuickFixHelper
        .newIssue(context)
        .forRule(this)
        .onTree(tree)
        .withMessage("Use \"isEmpty()\" to check whether a \""+lengthCall.methodSymbol().owner().name()+"\" is empty or not.")
        .withQuickFix(() -> getQuickFix(tree, lengthCall, comparisonType))
        .report();
    }
  }

  private static JavaQuickFix getQuickFix(Tree comparisonExpression, MethodInvocationTree lengthInvocation, ComparisonType comparisonType) {
    // There are two cases to deal with:
    //   s.length() OP CONST
    //   CONST OP s.length()
    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix("Replace with \"isEmpty()\"");

    // Replace "[CONST OP]" with "!"/"".
    AnalyzerMessage.TextSpan prefixSpan = AnalyzerMessage.textSpanBetween(comparisonExpression, true, lengthInvocation, false);
    String prefixReplacement = comparisonType == ComparisonType.IS_NOT_EMPTY ? "!" : "";
    if (!prefixReplacement.isEmpty() || !prefixSpan.isEmpty()) {
      builder.addTextEdit(JavaTextEdit.replaceTextSpan(prefixSpan, prefixReplacement));
    }

    // Replace "length() [OP CONST]" with "isEmpty()".
    IdentifierTree lengthIdentifier = ExpressionUtils.methodName(lengthInvocation);
    builder.addTextEdit(JavaTextEdit.replaceBetweenTree(
      lengthIdentifier,
      true,
      comparisonExpression,
      true,
      "isEmpty()"
    ));

    return builder.build();
  }

  @Nullable
  private MethodInvocationTree getLengthCall(ExpressionTree tree) {
    if (tree instanceof MethodInvocationTree mit) {
      if (STRING_LENGTH_METHOD.matches(mit) || (context.getJavaVersion().isJava15Compatible() && CHARSEQUENCE_LENGTH_METHOD.matches(mit))) {
        return mit;
      }
    }
    return null;
  }

  /**
   * Check the comparison for <pre>length() OP VALUE</pre>.
   */
  private static ComparisonType getComparisonOnRight(BinaryExpressionTree tree, boolean rightIsZero, boolean rightIsOne) {
    if (tree.is(Kind.EQUAL_TO, Kind.LESS_THAN_OR_EQUAL_TO) && rightIsZero) {
      return ComparisonType.IS_EMPTY;
    }
    if (tree.is(Kind.NOT_EQUAL_TO, Kind.GREATER_THAN) && rightIsZero) {
      return ComparisonType.IS_NOT_EMPTY;
    }
    if (tree.is(Kind.LESS_THAN) && rightIsOne) {
      return ComparisonType.IS_EMPTY;
    }
    if (tree.is(Kind.GREATER_THAN_OR_EQUAL_TO) && rightIsOne) {
      return ComparisonType.IS_NOT_EMPTY;
    }
    return ComparisonType.OTHER;
  }

  /**
   * Check the comparison for <pre>VALUE OP length()</pre>.
   */
  private static ComparisonType getComparisonOnLeft(boolean leftIsZero, boolean leftIsOne, BinaryExpressionTree tree) {
    if (leftIsZero && tree.is(Kind.EQUAL_TO, Kind.GREATER_THAN_OR_EQUAL_TO)) {
      return ComparisonType.IS_EMPTY;
    }
    if (leftIsZero && tree.is(Kind.NOT_EQUAL_TO, Kind.LESS_THAN)) {
      return ComparisonType.IS_NOT_EMPTY;
    }
    if (leftIsOne && tree.is(Kind.GREATER_THAN)) {
      return ComparisonType.IS_EMPTY;
    }
    if (leftIsOne && tree.is(Kind.LESS_THAN_OR_EQUAL_TO)) {
      return ComparisonType.IS_NOT_EMPTY;
    }
    return ComparisonType.OTHER;
  }
}
