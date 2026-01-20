/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks.tests;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.Version;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.semantic.Type.Primitives.DOUBLE;
import static org.sonar.plugins.java.api.semantic.Type.Primitives.FLOAT;

@Rule(key = "S2698")
public class AssertionsWithoutMessageCheck extends AbstractMethodDetection implements DependencyVersionAware {

  private static final String MESSAGE = "Add a message to this assertion.";
  private static final String MESSAGE_FEST_LIKE = "Add a message to this assertion chain before the predicate method.";

  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final String FEST_GENERIC_ASSERT = "org.fest.assertions.GenericAssert";
  private static final String ASSERTJ_ABSTRACT_ASSERT = "org.assertj.core.api.AbstractAssert";

  private static final Set<String> ASSERT_METHODS_WITH_ONE_PARAM = SetUtils.immutableSetOf("assertNull", "assertNotNull");
  private static final Set<String> ASSERT_METHODS_WITH_TWO_PARAMS = SetUtils.immutableSetOf("assertEquals", "assertSame", "assertNotSame", "assertThat");
  private static final Set<String> JUNIT5_ASSERT_METHODS_IGNORED = SetUtils.immutableSetOf("assertAll", "assertLinesMatch");
  private static final Set<String> JUNIT5_ASSERT_METHODS_WITH_ONE_PARAM = SetUtils.immutableSetOf("assertTrue", "assertFalse", "assertNull", "assertNotNull", "assertDoesNotThrow");
  private static final Set<String> JUNIT5_ASSERT_METHODS_WITH_DELTA = SetUtils.immutableSetOf("assertArrayEquals", "assertEquals");
  private static final Set<String> TESTNG_THROWS_METHODS = SetUtils.immutableSetOf("assertThrows", "expectThrows");

  private static final Predicate<String> INVOCATION_PREDICATE = name ->
    name.startsWith("assert") || name.startsWith("expect") || "fail".equals(name);

  private static final MethodMatchers FEST_LIKE_ABSTRACT_ASSERT = MethodMatchers.create()
    .ofSubTypes(FEST_GENERIC_ASSERT, ASSERTJ_ABSTRACT_ASSERT).anyName().withAnyParameters().build();
  private static final MethodMatchers FEST_LIKE_MESSAGE_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes(FEST_GENERIC_ASSERT).names("as", "describedAs", "overridingErrorMessage")
      .addParametersMatcher(types -> matchFirstParameterWithAnyOf(types, JAVA_LANG_STRING, "org.fest.assertions.Description")).build(),
    MethodMatchers.create()
      .ofSubTypes(ASSERTJ_ABSTRACT_ASSERT).names("as", "describedAs", "withFailMessage", "overridingErrorMessage")
      .addParametersMatcher(types -> matchFirstParameterWithAnyOf(types, JAVA_LANG_STRING, "org.assertj.core.description.Description"))
      .build()
  );
  private static final MethodMatchers ASSERT_THAT_MATCHER = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.Assertions",
      "org.assertj.core.api.AssertionsForInterfaceTypes",
      "org.assertj.core.api.AssertionsForClassTypes",
      "org.fest.assertions.Assertions")
    .names("assertThat", "assertThatObject").withAnyParameters().build();
  private static final MethodMatchers ASSERT_SETTING_CONTEXT = MethodMatchers.create()
    .ofSubTypes(ASSERTJ_ABSTRACT_ASSERT)
      .name(name -> name.startsWith("extracting") || name.startsWith("using") || name.startsWith("filtered"))
      .withAnyParameters()
      .build();

  private boolean isTestng77orLater = false;

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes("org.junit.jupiter.api.Assertions", "org.junit.Assert", "junit.framework.Assert", "org.fest.assertions.Fail",
          "org.assertj.core.api.Fail", "org.testng.Assert", "org.testng.AssertJUnit")
        .name(INVOCATION_PREDICATE).withAnyParameters().build(),
      FEST_LIKE_ABSTRACT_ASSERT
      );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Symbol symbol = mit.methodSymbol();
    Type type = symbol.owner().type();

    if (FEST_LIKE_MESSAGE_METHODS.matches(mit) || ASSERT_SETTING_CONTEXT.matches(mit)) {
      // If we can establish that the currently tested method is the one adding a message or not an assertion predicate,
      // we have very easily shown that this rule does not apply.
      return;
    }

    IdentifierTree reportLocation = ExpressionUtils.methodName(mit);

    if (type.isSubtypeOf(FEST_GENERIC_ASSERT) || type.isSubtypeOf(ASSERTJ_ABSTRACT_ASSERT)) {
      checkFestLikeAssertion(mit, symbol, reportLocation);
    } else if (type.is("org.junit.jupiter.api.Assertions")) {
      checkJUnit5(mit, reportLocation);
    } else if (mit.arguments().isEmpty() || !hasMessageArg(mit, type) || isAssertingOnStringWithNoMessage(mit)) {
      reportIssue(reportLocation, MESSAGE);
    }
  }

  /**
   * True if the call has a message argument. Such an argument is a string
   * and it is the first of the last argument (depending on the assertion library).
   */
  private boolean hasMessageArg(MethodInvocationTree mit, Type type) {
    List<ExpressionTree> args = mit.arguments();

    // `fail` needs one argument. Assume it is a string.
    if ("fail".equals(mit.methodSymbol().name())) {
      return !args.isEmpty();
    }

    // In TestNG, the message is the last argument.
    if (type.is("org.testng.Assert") || type.is("org.testng.AssertJUnit")) {
      // Depending on the version of TestNG, assertThrows /expectThrows may or may not have the variant with a message.
      boolean isThrowsMethod = TESTNG_THROWS_METHODS.contains(mit.methodSymbol().name());
      if (!isTestng77orLater && isThrowsMethod) {
        return true;
      }
      int expectedMessageArgIndex = isThrowsMethod ? 0 : (args.size() - 1);
      return expectedMessageArgIndex >= 0 && isString(args.get(expectedMessageArgIndex));
    }

    // In JUnit and others, the message is the first argument.
    return !args.isEmpty() && isString(args.get(0));
  }

  private void checkFestLikeAssertion(MethodInvocationTree mit, Symbol symbol, IdentifierTree reportLocation) {
    if (isConstructor(symbol)) {
      return;
    }
    if (isFirstAssertingPredicateAfterAssertThat(mit)) {
      // If we have anything between the current assertion predicate and the assertion subject, it's either
      // - another assertion predicate: the issue will be raised on this one (if problematic)
      // - a message: compliant solution
      reportIssue(reportLocation, MESSAGE_FEST_LIKE);
    }
  }

  private static boolean isFirstAssertingPredicateAfterAssertThat(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree childMit = (MethodInvocationTree) expression;
        if (ASSERT_THAT_MATCHER.matches(childMit)) {
          return true;
        } else if (ASSERT_SETTING_CONTEXT.matches(childMit)) {
          return isFirstAssertingPredicateAfterAssertThat(childMit);
        }
      }
    }
    return false;
  }

  private void checkJUnit5(MethodInvocationTree mit, IdentifierTree reportLocation) {
    String methodName = mit.methodSymbol().name();
    if (JUNIT5_ASSERT_METHODS_IGNORED.contains(methodName)) {
      return;
    }

    if (mit.arguments().isEmpty()) {
      reportIssue(reportLocation, MESSAGE);
    } else if ("fail".equals(methodName)) {
      if (mit.arguments().size() == 1 && mit.arguments().get(0).symbolType().isSubtypeOf("java.lang.Throwable")) {
        reportIssue(reportLocation, MESSAGE);
      }
    } else {
      checkJUnit5Assertions(mit, reportLocation);
    }
  }

  private void checkJUnit5Assertions(MethodInvocationTree mit, IdentifierTree reportLocation) {
    String methodName = mit.methodSymbol().name();
    if (JUNIT5_ASSERT_METHODS_WITH_ONE_PARAM.contains(methodName)) {
      if (mit.arguments().size() == 1) {
        reportIssue(reportLocation, MESSAGE);
      }
    } else if (mit.arguments().size() == 2) {
      reportIssue(reportLocation, MESSAGE);
    } else if (JUNIT5_ASSERT_METHODS_WITH_DELTA.contains(methodName) && mit.arguments().size() == 3) {
      Type thirdArgumentType = mit.arguments().get(2).symbolType();
      if (thirdArgumentType.isPrimitive(DOUBLE) || thirdArgumentType.isPrimitive(FLOAT)) {
        reportIssue(reportLocation, MESSAGE);
      }
    }
  }

  private static Boolean matchFirstParameterWithAnyOf(List<Type> parameterTypes, String... acceptableTypes) {
    if (!parameterTypes.isEmpty()) {
      Type firstParamType = parameterTypes.get(0);
      for (String acceptableType : acceptableTypes) {
        if (firstParamType.is(acceptableType)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isConstructor(Symbol symbol) {
    return "<init>".equals(symbol.name());
  }

  private static boolean isAssertingOnStringWithNoMessage(MethodInvocationTree mit) {
    return isAssertWithTwoParams(mit) || isAssertWithOneParam(mit);
  }

  private static boolean isAssertWithOneParam(MethodInvocationTree mit) {
    return ASSERT_METHODS_WITH_ONE_PARAM.contains(mit.methodSymbol().name()) && mit.arguments().size() == 1;
  }

  private static boolean isAssertWithTwoParams(MethodInvocationTree mit) {
    return ASSERT_METHODS_WITH_TWO_PARAMS.contains(mit.methodSymbol().name()) && mit.arguments().size() == 2;
  }

  private static boolean isString(ExpressionTree expressionTree) {
    return expressionTree.symbolType().is(JAVA_LANG_STRING);
  }

  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    Optional<Version> testngVersion = dependencyFinder.apply("testng");
    isTestng77orLater = testngVersion.map(v -> v.isGreaterThanOrEqualTo("7.7")).orElse(false);
    return true;
  }
}
