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
package org.sonar.java.checks.tests;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import static org.sonar.plugins.java.api.semantic.Type.Primitives.DOUBLE;
import static org.sonar.plugins.java.api.semantic.Type.Primitives.FLOAT;

@Rule(key = "S2698")
public class AssertionsWithoutMessageCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Add a message to this assertion.";
  private static final String MESSAGE_FEST_LIKE = "Add a message to this assertion before calling this method.";
  private static final String ASSERT = "assert";

  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final String FEST_GENERIC_ASSERT = "org.fest.assertions.GenericAssert";
  private static final MethodMatchers FEST_MESSAGE_METHODS = MethodMatchers.create()
    .ofSubTypes(FEST_GENERIC_ASSERT).names("as", "describedAs", "overridingErrorMessage")
    .addParametersMatcher(types -> matchFirstParameterWithAnyOf(types, JAVA_LANG_STRING, "org.fest.assertions.Description")).build();

  private static final String ASSERTJ_ABSTRACT_ASSERT = "org.assertj.core.api.AbstractAssert";
  private static final MethodMatchers ASSERTJ_MESSAGE_METHODS = MethodMatchers.create()
    .ofSubTypes(ASSERTJ_ABSTRACT_ASSERT).names("as", "describedAs", "withFailMessage", "overridingErrorMessage")
    .addParametersMatcher(types -> matchFirstParameterWithAnyOf(types, JAVA_LANG_STRING, "org.assertj.core.description.Description"))
    .build();

  private static final Set<String> ASSERT_METHODS_WITH_ONE_PARAM = ImmutableSet.of("assertNull", "assertNotNull");
  private static final Set<String> ASSERT_METHODS_WITH_TWO_PARAMS = ImmutableSet.of("assertEquals", "assertSame", "assertNotSame", "assertThat");
  private static final Set<String> JUNIT5_ASSERT_METHODS_IGNORED = ImmutableSet.of("assertAll", "assertLinesMatch");
  private static final Set<String> JUNIT5_ASSERT_METHODS_WITH_ONE_PARAM = ImmutableSet.of("assertTrue", "assertFalse", "assertNull", "assertNotNull", "assertDoesNotThrow");
  private static final Set<String> JUNIT5_ASSERT_METHODS_WITH_DELTA = ImmutableSet.of("assertArrayEquals", "assertEquals");

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes("org.junit.jupiter.api.Assertions", "org.junit.Assert", "junit.framework.Assert", "org.fest.assertions.Fail",
          "org.assertj.core.api.Fail")
        .name(name -> name.startsWith(ASSERT) || name.equals("fail")).withAnyParameters().build(),
      MethodMatchers.create().ofSubTypes(FEST_GENERIC_ASSERT, ASSERTJ_ABSTRACT_ASSERT).anyName().withAnyParameters().build());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Symbol symbol = mit.symbol();
    Type type = symbol.owner().type();

    if (FEST_MESSAGE_METHODS.matches(mit) || ASSERTJ_MESSAGE_METHODS.matches(mit)) {
      // If we can establish that the currently tested method is the one adding a message,
      // we have very easily shown that this rule does not apply.
      return;
    }

    IdentifierTree reportLocation = ExpressionUtils.methodName(mit);

    if (type.isSubtypeOf(FEST_GENERIC_ASSERT)) {
      checkFestLikeAssertion(mit, symbol, FEST_MESSAGE_METHODS, reportLocation);
    } else if (type.isSubtypeOf(ASSERTJ_ABSTRACT_ASSERT)) {
      checkFestLikeAssertion(mit, symbol, ASSERTJ_MESSAGE_METHODS, reportLocation);
    } else if (type.is("org.junit.jupiter.api.Assertions")) {
      checkJUnit5(mit, reportLocation);
    } else if (mit.arguments().isEmpty() || !isString(mit.arguments().get(0)) || isAssertingOnStringWithNoMessage(mit)) {
      reportIssue(reportLocation, MESSAGE);
    }
  }

  private void checkFestLikeAssertion(MethodInvocationTree mit, Symbol symbol, MethodMatchers messageMethods, IdentifierTree reportLocation) {
    if (isConstructor(symbol)) {
      return;
    }
    FestLikeVisitor visitor = new FestLikeVisitor(messageMethods);
    mit.methodSelect().accept(visitor);
    if (!visitor.useDescription) {
      reportIssue(reportLocation, MESSAGE_FEST_LIKE);
    }
  }

  private void checkJUnit5(MethodInvocationTree mit, IdentifierTree reportLocation) {
    String methodName = mit.symbol().name();
    if (JUNIT5_ASSERT_METHODS_IGNORED.contains(methodName)) {
      return;
    }

    if (mit.arguments().isEmpty()) {
      reportIssue(reportLocation, MESSAGE);
    } else if (methodName.equals("fail")) {
      if (mit.arguments().size() == 1 && mit.arguments().get(0).symbolType().isSubtypeOf("java.lang.Throwable")) {
        reportIssue(reportLocation, MESSAGE);
      }
    } else {
      checkJUnit5Assertions(mit, reportLocation);
    }
  }

  private void checkJUnit5Assertions(MethodInvocationTree mit, IdentifierTree reportLocation) {
    String methodName = mit.symbol().name();
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
    return ASSERT_METHODS_WITH_ONE_PARAM.contains(mit.symbol().name()) && mit.arguments().size() == 1;
  }

  private static boolean isAssertWithTwoParams(MethodInvocationTree mit) {
    return ASSERT_METHODS_WITH_TWO_PARAMS.contains(mit.symbol().name()) && mit.arguments().size() == 2;
  }

  private static boolean isString(ExpressionTree expressionTree) {
    return expressionTree.symbolType().is(JAVA_LANG_STRING);
  }

  private static class FestLikeVisitor extends BaseTreeVisitor {
    boolean useDescription = false;
    private final MethodMatchers descriptionMethodMatcher;

    public FestLikeVisitor(MethodMatchers descriptionMethodMatcher) {
      this.descriptionMethodMatcher = descriptionMethodMatcher;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      useDescription |= descriptionMethodMatcher.matches(tree);
      super.visitMethodInvocation(tree);
    }
  }
}
