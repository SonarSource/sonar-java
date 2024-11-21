/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

/**
 * Implement rule that <pre>String.length() == 0</pre> should be replaced with
 * <pre>String.isEmpty()</pre>.
 */
@Rule(key = "S7158")
public class StringIsEmptyCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final MethodMatchers LENGTH_METHOD = MethodMatchers.create()
    .ofTypes("java.lang.String")
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

  // `String.isEmpty()` is available since Java 6.
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

    if ((isLengthCall(left) && isComparisonOnRight(bet, rightIsZero, rightIsOne))
      || (isLengthCall(right) && isComparisonOnLeft(leftIsZero, leftIsOne, bet))) {
      reportIssue(tree, "Use isEmpty() to check whether a string is empty or not.");
    }
  }

  private static boolean isLengthCall(ExpressionTree tree) {
    return tree instanceof MethodInvocationTree mit && LENGTH_METHOD.matches(mit);
  }

  /**
   * Check the comparison for <pre>length() OP VALUE</pre>.
   */
  private static boolean isComparisonOnRight(BinaryExpressionTree tree, boolean rightIsZero, boolean rightIsOne) {
    return (tree.is(Kind.EQUAL_TO, Kind.LESS_THAN_OR_EQUAL_TO, Kind.NOT_EQUAL_TO, Kind.GREATER_THAN) && rightIsZero)
      || (tree.is(Kind.LESS_THAN, Kind.GREATER_THAN_OR_EQUAL_TO) && rightIsOne);
  }

  /**
   * Check the comparison for <pre>VALUE OP length()</pre>.
   */
  private static boolean isComparisonOnLeft(boolean leftIsZero, boolean leftIsOne, BinaryExpressionTree tree) {
    return (leftIsZero && tree.is(Kind.EQUAL_TO, Kind.GREATER_THAN_OR_EQUAL_TO, Kind.NOT_EQUAL_TO, Kind.LESS_THAN))
      || (leftIsOne && tree.is(Kind.GREATER_THAN, Kind.LESS_THAN_OR_EQUAL_TO));
  }
}
