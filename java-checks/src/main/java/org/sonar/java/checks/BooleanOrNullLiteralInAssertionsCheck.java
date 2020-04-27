/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Rule(key = "S2701")
public class BooleanOrNullLiteralInAssertionsCheck extends AbstractMethodDetection {
  private static final String DEFAULT_MESSAGE = "Remove or correct this assertion.";
  private static final String MESSAGE_WITH_ALTERNATIVE = "Use %s instead.";
  private static final String ASSERT = "assert";
  private static final String IS = "is";

  private static final MethodMatchers FEST_ASSERT_THAT = MethodMatchers.create()
    .ofTypes("org.fest.assertions.Assertions")
    .names("assertThat")
    .addParametersMatcher(MethodMatchers.ANY)
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes(
          "org.junit.Assert",
          "org.junit.jupiter.api.Assertions",
          "junit.framework.Assert",
          "junit.framework.TestCase")
        .name(name -> name.startsWith(ASSERT))
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes("org.fest.assertions.GenericAssert")
        .name(name -> name.startsWith(IS))
        .withAnyParameters()
        .build()
      );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    switch (mit.symbol().name()) {
      case "assertEquals":
      case "assertSame":
        checkEqualityAsserts(mit, false);
        break;

      case "assertNotEquals":
      case "assertNotSame":
        checkEqualityAsserts(mit, true);
        break;

      case "isEqualTo":
      case "isSameAs":
        checkFestEqualityAsserts(mit, false);
        break;

      case "isNotEqualTo":
      case "isNotSameAs":
        checkFestEqualityAsserts(mit, true);
        break;

      default:
        checkOtherAsserts(mit);
        break;
    }
  }

  private void checkEqualityAsserts(MethodInvocationTree mit, boolean flipped) {
    List<ExpressionTree> literals = mit.arguments().stream()
      .filter(BooleanOrNullLiteralInAssertionsCheck::isBoolOrNullLiteral)
      .collect(Collectors.toList());
    if (literals.size() > 1) {
      reportIssue(mit, DEFAULT_MESSAGE);
    } else if (literals.size() == 1) {
      checkEqualityAssertWithOneLiteral(mit, literals.get(0), flipped, ASSERT);
    }
  }

  private void checkFestEqualityAsserts(MethodInvocationTree mit, boolean flipped) {
    if (mit.arguments().isEmpty()) {
      return;
    }
    ExpressionTree expected = mit.arguments().get(0);
    ExpressionTree actual = findActualValueForFest(mit);
    boolean actualIsLiteral = actual != null && isBoolOrNullLiteral(actual);
    if (isBoolOrNullLiteral(expected) && actualIsLiteral) {
      reportIssue(mit, DEFAULT_MESSAGE);
    } else if (isBoolOrNullLiteral(expected)) {
      checkEqualityAssertWithOneLiteral(mit, expected, flipped, IS);
    } else if (actualIsLiteral) {
      checkEqualityAssertWithOneLiteral(mit, actual, flipped, IS);
    }
  }

  private static ExpressionTree findActualValueForFest(MethodInvocationTree mit) {
    if (FEST_ASSERT_THAT.matches(mit)) {
      return mit.arguments().get(0);
    }
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree member = (MemberSelectExpressionTree) mit.methodSelect();
      if (member.expression().is(Tree.Kind.METHOD_INVOCATION)) {
        return findActualValueForFest((MethodInvocationTree) member.expression());
      }
    }
    return null;
  }

  private void checkEqualityAssertWithOneLiteral(MethodInvocationTree mit, ExpressionTree expr, boolean flipped, String assertOrIs) {
    String predicate;
    if (isNullLiteral(expr)) {
      predicate = flipped ? "NotNull" : "Null";
    } else {
      Optional<Boolean> value = expr.asConstant(Boolean.class);
      if (!value.isPresent()) {
        return;
      }
      if (Boolean.TRUE.equals(value.get())) {
        predicate = flipped ? "False" : "True";
      } else {
        predicate = flipped ? "True" : "False";
      }
    }
    String recommendedAssertMethod = assertOrIs + predicate;
    reportIssue(mit, String.format(MESSAGE_WITH_ALTERNATIVE, recommendedAssertMethod));
  }

  private static boolean isNullLiteral(ExpressionTree expr) {
    // Also recognize null literals inside a cast because null literals often need to be cast to avoid
    // overloading ambiguities.
    if (expr.is(Tree.Kind.TYPE_CAST)) {
      return isNullLiteral(((TypeCastTree) expr).expression());
    } else {
      return expr.is(Tree.Kind.NULL_LITERAL);
    }
  }

  private static boolean isBoolOrNullLiteral(ExpressionTree expr) {
    return expr.is(Tree.Kind.BOOLEAN_LITERAL) || isNullLiteral(expr);
  }

  private void checkOtherAsserts(MethodInvocationTree mit) {
    for (ExpressionTree arg : mit.arguments()) {
      if (isBoolOrNullLiteral(arg)) {
        reportIssue(mit, DEFAULT_MESSAGE);
        return;
      }
    }
    ExpressionTree festActual = findActualValueForFest(mit);
    if (festActual != null && isBoolOrNullLiteral(festActual)) {
      reportIssue(mit, DEFAULT_MESSAGE);
    }
  }
}
