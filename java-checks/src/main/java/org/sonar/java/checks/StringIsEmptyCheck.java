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
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

/**
 * Implement rule that <pre>CharSequence.length() == 0</pre> should be replaced with
 * <pre>CharSequence.isEmpty()</pre>, and <pre>String.strip().length() == 0</pre> or
 * <pre>String.trim().length() == 0</pre> with <pre>String.isBlank()</pre>.
 */
@Rule(key = "S7158")
public class StringIsEmptyCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";

  // `String.isBlank()` is available since Java 11
  private static final int JAVA_11 = 11;

  private static final MethodMatchers STRING_LENGTH_METHOD = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("length")
    .addWithoutParametersMatcher()
    .build();

  private static final String TRIM_NAME = "trim";

  private static final MethodMatchers STRING_WHITESPACE_REMOVAL_METHODS = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("strip", TRIM_NAME)
    .addWithoutParametersMatcher()
    .build();

  /**
   * <pre>trim()</pre> removes every character up to and including <pre>U+0020</pre>, while <pre>isBlank()</pre> relies on
   * <pre>Character.isWhitespace</pre>. They disagree on control characters below <pre>U+0020</pre> and on Unicode white
   * space above it, so replacing one with the other can change behaviour.
   */
  private static final String TRIM_CAVEAT =
    "Use this if Unicode whitespace should be treated as blank and control characters as non-blank.";

  private static final String TRIM_FIX_CAVEAT = " (Unicode whitespace is treated as blank, control characters as non-blank)";

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
      MethodInvocationTree whitespaceRemovalCall = getWhitespaceRemovalCall(lengthCall);

      QuickFixHelper
        .newIssue(context)
        .forRule(this)
        .onTree(tree)
        .withMessage(getMessage(lengthCall, whitespaceRemovalCall))
        .withQuickFix(() -> getQuickFix(tree, lengthCall, whitespaceRemovalCall, comparisonType))
        .report();
    }
  }

  private static String getMessage(MethodInvocationTree lengthCall, @Nullable MethodInvocationTree whitespaceRemovalCall) {
    String adjective = whitespaceRemovalCall != null ? "blank" : "empty";
    String caveat = isTrimBased(whitespaceRemovalCall) ? (" " + TRIM_CAVEAT) : "";

    return "Use \"" + replacementMethodName(whitespaceRemovalCall) + "\" to check whether a \""
      + ownerName(lengthCall) + "\" is " + adjective + " or not." + caveat;
  }

  private static JavaQuickFix getQuickFix(Tree comparisonExpression, MethodInvocationTree lengthInvocation,
    @Nullable MethodInvocationTree whitespaceRemovalCall, ComparisonType comparisonType) {
    String replacedMethod = replacementMethodName(whitespaceRemovalCall);
    MethodInvocationTree replacedInvocation = whitespaceRemovalCall != null ? whitespaceRemovalCall : lengthInvocation;
    String fixDescription = "Replace with \"" + replacedMethod + "\"" + (isTrimBased(whitespaceRemovalCall) ? TRIM_FIX_CAVEAT : "");

    // There are two cases to deal with:
    //   s.length() OP CONST
    //   CONST OP s.length()
    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix(fixDescription);

    // Replace "[CONST OP]" with "!"/"".
    AnalyzerMessage.TextSpan prefixSpan = AnalyzerMessage.textSpanBetween(comparisonExpression, true, lengthInvocation, false);
    String prefixReplacement = comparisonType == ComparisonType.IS_NOT_EMPTY ? "!" : "";
    if (!prefixReplacement.isEmpty() || !prefixSpan.isEmpty()) {
      builder.addTextEdit(JavaTextEdit.replaceTextSpan(prefixSpan, prefixReplacement));
    }

    // Replace "length() [OP CONST]" with "isEmpty()", or "strip()/trim().length() [OP CONST]" with "isBlank()".
    IdentifierTree replacedIdentifier = ExpressionUtils.methodName(replacedInvocation);
    builder.addTextEdit(JavaTextEdit.replaceBetweenTree(
      replacedIdentifier,
      true,
      comparisonExpression,
      true,
      replacedMethod
    ));

    return builder.build();
  }

  private static String replacementMethodName(@Nullable MethodInvocationTree whitespaceRemovalCall) {
    return whitespaceRemovalCall != null ? "isBlank()" : "isEmpty()";
  }

  private static boolean isTrimBased(@Nullable MethodInvocationTree whitespaceRemovalCall) {
    return whitespaceRemovalCall != null && TRIM_NAME.equals(ExpressionUtils.methodName(whitespaceRemovalCall).name());
  }

  private static String ownerName(MethodInvocationTree lengthCall) {
    if (lengthCall.methodSelect() instanceof MemberSelectExpressionTree sel) {
      return sel.expression().symbolType().name();
    } else {
      return lengthCall.methodSymbol().owner().name();
    }
  }

  /**
   * @return the <pre>strip()</pre> or <pre>trim()</pre> invocation the given <pre>length()</pre> call is applied to, or
   *         null when there is none, when it is not directly chained, or when <pre>String.isBlank()</pre> is not available.
   */
  @Nullable
  private MethodInvocationTree getWhitespaceRemovalCall(MethodInvocationTree lengthCall) {
    if (context.getJavaVersion().asInt() >= JAVA_11
      && lengthCall.methodSelect() instanceof MemberSelectExpressionTree sel
      && sel.expression() instanceof MethodInvocationTree mit
      && STRING_WHITESPACE_REMOVAL_METHODS.matches(mit)) {
      return mit;
    }
    return null;
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
