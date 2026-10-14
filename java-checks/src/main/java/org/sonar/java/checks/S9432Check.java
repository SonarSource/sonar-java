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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9432")
public class S9432Check extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Use \"isBlank()\" on the original string instead.";
  private static final String TRIM_MESSAGE = "Use \"isBlank()\" on the original string instead, "
    + "if Unicode whitespace should be treated as blank and control characters as non-blank.";
  private static final String STRING_TYPE = "java.lang.String";

  private static final MethodMatchers WHITESPACE_REMOVAL_METHODS = MethodMatchers.create()
    .ofTypes(STRING_TYPE)
    .names("trim", "strip")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers BLANKNESS_METHODS = MethodMatchers.create()
    .ofTypes(STRING_TYPE)
    .names("isEmpty", "isBlank")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers LENGTH_METHOD = MethodMatchers.create()
    .ofTypes(STRING_TYPE)
    .names("length")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.asInt() >= 11;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    MethodInvocationTree removalInvocation = whitespaceRemovalInvocation(invocation);
    if (removalInvocation == null) {
      return;
    }
    if (BLANKNESS_METHODS.matches(invocation) || (LENGTH_METHOD.matches(invocation) && isEmptinessCheck(invocation))) {
      String message = "trim".equals(removalInvocation.methodSymbol().name()) ? TRIM_MESSAGE : MESSAGE;
      reportIssue(removalInvocation, message);
    }
  }

  @Nullable
  private static MethodInvocationTree whitespaceRemovalInvocation(MethodInvocationTree invocation) {
    ExpressionTree receiver = receiver(invocation);
    if (receiver instanceof MethodInvocationTree receiverInvocation && WHITESPACE_REMOVAL_METHODS.matches(receiverInvocation)) {
      return receiverInvocation;
    }
    return null;
  }

  @Nullable
  private static ExpressionTree receiver(MethodInvocationTree invocation) {
    if (invocation.methodSelect() instanceof MemberSelectExpressionTree memberSelect) {
      return ExpressionUtils.skipParentheses(memberSelect.expression());
    }
    return null;
  }

  private static boolean isEmptinessCheck(MethodInvocationTree lengthInvocation) {
    Tree parent = lengthInvocation.parent();
    while (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      parent = parent.parent();
    }
    if (!(parent instanceof BinaryExpressionTree binaryExpression)) {
      return false;
    }
    ExpressionTree left = ExpressionUtils.skipParentheses(binaryExpression.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(binaryExpression.rightOperand());
    if (left == lengthInvocation) {
      return comparisonWithLiteralOnRight(binaryExpression, right);
    }
    if (right == lengthInvocation) {
      return comparisonWithLiteralOnLeft(binaryExpression, left);
    }
    return false;
  }

  private static boolean comparisonWithLiteralOnRight(BinaryExpressionTree binaryExpression, ExpressionTree literal) {
    return (binaryExpression.is(Tree.Kind.EQUAL_TO, Tree.Kind.LESS_THAN_OR_EQUAL_TO) && LiteralUtils.isZero(literal))
      || (binaryExpression.is(Tree.Kind.NOT_EQUAL_TO, Tree.Kind.GREATER_THAN) && LiteralUtils.isZero(literal))
      || (binaryExpression.is(Tree.Kind.LESS_THAN) && LiteralUtils.isOne(literal))
      || (binaryExpression.is(Tree.Kind.GREATER_THAN_OR_EQUAL_TO) && LiteralUtils.isOne(literal));
  }

  private static boolean comparisonWithLiteralOnLeft(BinaryExpressionTree binaryExpression, ExpressionTree literal) {
    return (LiteralUtils.isZero(literal) && binaryExpression.is(Tree.Kind.EQUAL_TO, Tree.Kind.GREATER_THAN_OR_EQUAL_TO))
      || (LiteralUtils.isZero(literal) && binaryExpression.is(Tree.Kind.NOT_EQUAL_TO, Tree.Kind.LESS_THAN))
      || (LiteralUtils.isOne(literal) && binaryExpression.is(Tree.Kind.GREATER_THAN))
      || (LiteralUtils.isOne(literal) && binaryExpression.is(Tree.Kind.LESS_THAN_OR_EQUAL_TO));
  }
}
