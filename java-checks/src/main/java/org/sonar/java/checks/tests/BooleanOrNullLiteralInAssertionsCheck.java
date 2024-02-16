/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

import java.util.List;
import java.util.Optional;

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
    switch (mit.methodSymbol().name()) {
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
    List<LiteralTree> literals = findLiterals(mit.arguments());
    IdentifierTree methodName = ExpressionUtils.methodName(mit);
    if (literals.size() > 1) {
      reportDefaultMessage(methodName, literals);
    } else if (literals.size() == 1) {
      checkEqualityAssertWithOneLiteral(methodName, literals.get(0), flipped, ASSERT);
    }
  }

  private void checkFestEqualityAsserts(MethodInvocationTree mit, boolean flipped) {
    if (mit.arguments().isEmpty()) {
      return;
    }
    Optional<LiteralTree> expectedLiteral = getBoolOrNullLiteral(mit.arguments().get(0));
    Optional<LiteralTree> actualLiteral = findActualLiteralForFest(mit);
    IdentifierTree methodName = ExpressionUtils.methodName(mit);
    if (expectedLiteral.isPresent() && actualLiteral.isPresent()) {
      reportDefaultMessage(methodName, Arrays.asList(expectedLiteral.get(), actualLiteral.get()));
    } else {
      expectedLiteral.ifPresent(literal -> checkEqualityAssertWithOneLiteral(methodName, literal, flipped, IS));
      actualLiteral.ifPresent(literal -> checkEqualityAssertWithOneLiteral(methodName, literal, flipped, IS));
    }
  }

  private void checkEqualityAssertWithOneLiteral(IdentifierTree methodName, LiteralTree literal, boolean flipped, String assertOrIs) {
    String predicate;
    if (literal.is(Tree.Kind.NULL_LITERAL)) {
      predicate = flipped ? "NotNull" : "Null";
    } else {
      Optional<Boolean> value = literal.asConstant(Boolean.class);
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
    List<JavaFileScannerContext.Location> secondaryLocation = Collections.singletonList(
      new JavaFileScannerContext.Location("This literal can be avoided by using a different assertion method.", literal)
    );
    String mainMessage = String.format(MESSAGE_WITH_ALTERNATIVE, recommendedAssertMethod);
    reportIssue(methodName, mainMessage, secondaryLocation, null);
  }

  private void checkOtherAsserts(MethodInvocationTree mit) {
    List<LiteralTree> literals = findLiterals(mit.arguments());
    Optional<LiteralTree> festActualLiteral = findActualLiteralForFest(mit);
    festActualLiteral.ifPresent(literals::add);
    if (!literals.isEmpty()) {
      reportDefaultMessage(ExpressionUtils.methodName(mit), literals);
    }
  }

  private static List<LiteralTree> findLiterals(List<ExpressionTree> expressions) {
    List<LiteralTree> result = new ArrayList<>();
    for (ExpressionTree expression : expressions) {
      getBoolOrNullLiteral(expression).ifPresent(result::add);
    }
    return result;
  }

  private static Optional<LiteralTree> findActualLiteralForFest(MethodInvocationTree mit) {
    if (FEST_ASSERT_THAT.matches(mit)) {
      return getBoolOrNullLiteral(mit.arguments().get(0));
    }
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree member = (MemberSelectExpressionTree) mit.methodSelect();
      if (member.expression().is(Tree.Kind.METHOD_INVOCATION)) {
        return findActualLiteralForFest((MethodInvocationTree) member.expression());
      }
    }
    return Optional.empty();
  }

  /**
   * Tests whether an expression is a boolean or null literal, possibly embedded in a sequence of casts (because null
   * literals often need to be cast to avoid overloading ambiguities), and return the null literal if so.
   */
  private static Optional<LiteralTree> getBoolOrNullLiteral(ExpressionTree expr) {
    if (expr.is(Tree.Kind.TYPE_CAST)) {
      return getBoolOrNullLiteral(((TypeCastTree) expr).expression());
    } else if (expr.is(Tree.Kind.NULL_LITERAL) || expr.is(Tree.Kind.BOOLEAN_LITERAL)) {
      return Optional.of((LiteralTree) expr);
    } else {
      return Optional.empty();
    }
  }

  private void reportDefaultMessage(IdentifierTree methodName, List<LiteralTree> literals) {
    List<JavaFileScannerContext.Location> literalLocations = literals.stream()
      .map(literal -> new JavaFileScannerContext.Location("There does not seem to be a reason to use a literal here.", literal))
      .toList();
    reportIssue(methodName, DEFAULT_MESSAGE, literalLocations, null);
  }
}
