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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5845")
public class AssertionTypesCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";

  private static final String JUNIT4_ASSERTIONS = "org.junit.Assert";
  private static final String JUNIT5_ASSERTIONS = "org.junit.jupiter.api.Assertions";
  private static final String ASSERT_NULL = "assertNull";
  private static final String ASSERT_NOT_NULL = "assertNotNull";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String ASSERT_EQUALS = "assertEquals";
  private static final String ASSERT_NOT_EQUALS = "assertNotEquals";

  private static final MethodMatchers ASSERT_NULLABLE_FIRST_ARGUMENT = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JUNIT4_ASSERTIONS)
      .names(ASSERT_NULL, ASSERT_NOT_NULL)
      .addParametersMatcher(MethodMatchers.ANY)
      .build(),
    MethodMatchers.create()
      .ofTypes(JUNIT5_ASSERTIONS)
      .names(ASSERT_NULL, ASSERT_NOT_NULL)
      .addParametersMatcher(MethodMatchers.ANY)
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
      .build());

  private static final MethodMatchers ASSERT_NULLABLE_SECOND_ARGUMENT = MethodMatchers.create()
    .ofTypes(JUNIT4_ASSERTIONS)
    .names(ASSERT_NULL, ASSERT_NOT_NULL)
    .addParametersMatcher(JAVA_LANG_STRING, MethodMatchers.ANY)
    .build();

  private static final MethodMatchers ASSERT_EQUALS_FIRST_AND_SECOND_ARGUMENTS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JUNIT4_ASSERTIONS)
      .names(ASSERT_EQUALS)
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
      .build(),
    MethodMatchers.create()
      .ofTypes(JUNIT5_ASSERTIONS)
      .names(ASSERT_EQUALS)
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY, MethodMatchers.ANY)
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY, MethodMatchers.ANY, MethodMatchers.ANY)
      .build());

  private static final MethodMatchers ASSERT_EQUALS_SECOND_AND_THIRD_ARGUMENTS = MethodMatchers.create()
    .ofTypes(JUNIT4_ASSERTIONS)
    .names(ASSERT_EQUALS)
    .addParametersMatcher(JAVA_LANG_STRING, MethodMatchers.ANY, MethodMatchers.ANY)
    .addParametersMatcher(JAVA_LANG_STRING, MethodMatchers.ANY, MethodMatchers.ANY, MethodMatchers.ANY)
    .build();

  private static final MethodMatchers ASSERT_NOT_EQUALS_FIRST_AND_SECOND_ARGUMENTS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JUNIT4_ASSERTIONS)
      .names(ASSERT_NOT_EQUALS)
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
      .build(),
    MethodMatchers.create()
      .ofTypes(JUNIT5_ASSERTIONS)
      .names(ASSERT_NOT_EQUALS)
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY, MethodMatchers.ANY)
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY, MethodMatchers.ANY, MethodMatchers.ANY)
      .build());

  private static final MethodMatchers ASSERT_NOT_EQUALS_SECOND_AND_THIRD_ARGUMENTS = MethodMatchers.create()
    .ofTypes(JUNIT4_ASSERTIONS)
    .names(ASSERT_NOT_EQUALS)
    .addParametersMatcher(JAVA_LANG_STRING, MethodMatchers.ANY, MethodMatchers.ANY)
    .addParametersMatcher(JAVA_LANG_STRING, MethodMatchers.ANY, MethodMatchers.ANY, MethodMatchers.ANY)
    .build();

  private static final MethodMatchers ASSERTJ_ASSERT_THAT = MethodMatchers.create()
    .ofTypes(
      "org.assertj.core.api.Assertions",
      "org.assertj.core.api.AssertionsForInterfaceTypes",
      "org.assertj.core.api.AssertionsForClassTypes")
    .names("assertThat", "assertThatObject")
    .addParametersMatcher(MethodMatchers.ANY)
    .build();

  private static final MethodMatchers.NameBuilder MATCHER_ANY_TYPE = MethodMatchers.create().ofAnyType();

  private static final MethodMatchers ASSERTJ_NULL_AND_NOT_NULL = MATCHER_ANY_TYPE
    .names("isNull", "isNotNull")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers ASSERTJ_EQUAL_TO_PREDICATE = MATCHER_ANY_TYPE
    .names("isEqualTo")
    .addParametersMatcher(MethodMatchers.ANY)
    .build();

  private static final MethodMatchers ASSERTJ_IS_SAME_AS_PREDICATE = MATCHER_ANY_TYPE
    .names("isSameAs")
    .addParametersMatcher(MethodMatchers.ANY)
    .build();

  private static final MethodMatchers ASSERTJ_IS_NOT_EQUAL_TO_PREDICATE = MATCHER_ANY_TYPE
    .names("isNotEqualTo")
    .addParametersMatcher(MethodMatchers.ANY)
    .build();

  private static final MethodMatchers ASSERTJ_IS_NOT_SAME_AS_PREDICATE = MATCHER_ANY_TYPE
    .names("isNotSameAs")
    .addParametersMatcher(MethodMatchers.ANY)
    .build();

  private static final MethodMatchers ASSERTJ_CONFIGURATION = MATCHER_ANY_TYPE
    .names("as", "describedAs", "withFailMessage", "overridingErrorMessage")
    .withAnyParameters()
    .build();

  private static final List<String> ASSERTJ_EXCEPTIONS = Arrays.asList(
    "org.assertj.core.api.AbstractTemporalAssert",
    "org.assertj.core.api.AbstractDateAssert",
    "org.assertj.core.api.AbstractBigIntegerAssert",
    "org.assertj.core.api.AbstractBigDecimalAssert");

  private enum Option {
    ACCEPT_DISSIMILAR_INTERFACE,
    REJECT_DISSIMILAR_INTERFACE,
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (ASSERT_NULLABLE_FIRST_ARGUMENT.matches(mit)) {
      checkNullableAssertion(new Argument(mit, 0));
    } else if (ASSERT_NULLABLE_SECOND_ARGUMENT.matches(mit)) {
      checkNullableAssertion(new Argument(mit, 1));
    } else if (ASSERT_EQUALS_FIRST_AND_SECOND_ARGUMENTS.matches(mit)) {
      checkCompatibleTypes(mit, new Argument(mit, 1), new Argument(mit, 0), Option.ACCEPT_DISSIMILAR_INTERFACE);
    } else if (ASSERT_EQUALS_SECOND_AND_THIRD_ARGUMENTS.matches(mit)) {
      checkCompatibleTypes(mit, new Argument(mit, 2), new Argument(mit, 1), Option.ACCEPT_DISSIMILAR_INTERFACE);
    } else if (ASSERT_NOT_EQUALS_FIRST_AND_SECOND_ARGUMENTS.matches(mit)) {
      checkCompatibleTypes(mit, new Argument(mit, 1), new Argument(mit, 0), Option.REJECT_DISSIMILAR_INTERFACE);
    } else if (ASSERT_NOT_EQUALS_SECOND_AND_THIRD_ARGUMENTS.matches(mit)) {
      checkCompatibleTypes(mit, new Argument(mit, 2), new Argument(mit, 1), Option.REJECT_DISSIMILAR_INTERFACE);
    } else if (ASSERTJ_ASSERT_THAT.matches(mit)) {
      checkSubsequentAssertJPredicateCompatibleTypes(new Argument(mit, 0), mit);
    }
  }

  private void checkSubsequentAssertJPredicateCompatibleTypes(Argument actual, MethodInvocationTree previousMethod) {
    MethodTreeUtils.consecutiveMethodInvocation(previousMethod)
      .ifPresent(mit -> {
        boolean checkFollowingMethod = true;
        if (ASSERTJ_NULL_AND_NOT_NULL.matches(mit)) {
          checkNullableAssertion(ExpressionUtils.methodName(mit), actual);
        } else if (ASSERTJ_EQUAL_TO_PREDICATE.matches(mit)) {
          checkCompatibleAssertJEqualTypes(mit, actual, new Argument(mit, 0), Option.ACCEPT_DISSIMILAR_INTERFACE);
        } else if (ASSERTJ_IS_SAME_AS_PREDICATE.matches(mit)) {
          checkCompatibleTypes(mit, actual, new Argument(mit, 0), Option.ACCEPT_DISSIMILAR_INTERFACE);
        } else if (ASSERTJ_IS_NOT_EQUAL_TO_PREDICATE.matches(mit)) {
          checkCompatibleAssertJEqualTypes(mit, actual, new Argument(mit, 0), Option.REJECT_DISSIMILAR_INTERFACE);
        } else if (ASSERTJ_IS_NOT_SAME_AS_PREDICATE.matches(mit)) {
          checkCompatibleTypes(mit, actual, new Argument(mit, 0), Option.REJECT_DISSIMILAR_INTERFACE);
        } else if (!ASSERTJ_CONFIGURATION.matches(mit)) {
          // stop checking when methods like: extracting, using*, filtered*
          checkFollowingMethod = false;
        }
        if (checkFollowingMethod) {
          checkSubsequentAssertJPredicateCompatibleTypes(actual, mit);
        }
      });
  }

  private void checkNullableAssertion(Argument actual) {
    checkNullableAssertion(actual.expression, actual);
  }

  private void checkNullableAssertion(Tree issueLocation, Argument actual) {
    if (actual.isPrimitive()) {
      reportIssue(issueLocation, "Change the assertion arguments to not compare a primitive value with null.");
    }
  }

  private void checkCompatibleAssertJEqualTypes(MethodInvocationTree mit, Argument actual, Argument expected, Option option) {
    Type type = mit.symbolType();
    if (ASSERTJ_EXCEPTIONS.stream().anyMatch(type::isSubtypeOf)) {
      // AssertJ supports Date/Temporal and BigInteger/BigDecimal comparison with String.
      return;
    }
    checkCompatibleTypes(mit, actual, expected, option);
  }

  private void checkCompatibleTypes(MethodInvocationTree mit, Argument actual, Argument expected, Option option) {
    if (areNotCompatibleTypes(actual, expected, option) && !isNotEqualsInTestRelatedToEquals(mit)) {
      createIssue(actual, expected);
    }
  }

  private static boolean isNotEqualsInTestRelatedToEquals(MethodInvocationTree mit) {
    String methodName = ExpressionUtils.methodName(mit).name();
    return (ASSERT_NOT_EQUALS.equals(methodName) || "isNotEqualTo".equals(methodName)) &&
      UnitTestUtils.isInUnitTestRelatedToObjectMethods(mit);
  }

  private static boolean areNotCompatibleTypes(Argument actual, Argument expected, Option option) {
    return isNullAndPrimitive(actual, expected) ||
      isNotCompatibleArray(actual, expected, option) ||
      isArrayAndNotArray(actual, expected) ||
      isNotCompatibleClass(actual, expected, option);
  }

  private static boolean isNullAndPrimitive(Argument actual, Argument expected) {
    return (actual.isNullLiteral() && expected.isPrimitive()) ||
      (actual.isPrimitive() && expected.isNullLiteral());
  }

  private static boolean isArrayAndNotArray(Argument actual, Argument expected) {
    return (actual.isArray() && expected.isNotArray()) ||
      (actual.isNotArray() && expected.isArray());
  }

  private static boolean isNotCompatibleArray(Argument actual, Argument expected, Option option) {
    if (!actual.isArray() || !expected.isArray()) {
      return false;
    }
    Type actualElementType = ((Type.ArrayType) actual.type).elementType().erasure();
    Type expectedElementType = ((Type.ArrayType) expected.type).elementType().erasure();
    if (actualElementType.isUnknown() || expectedElementType.isUnknown()) {
      return false;
    }
    if (actualElementType.isPrimitive() || expectedElementType.isPrimitive()) {
      return !actualElementType.name().equals(expectedElementType.name());
    }
    return areNotCompatibleTypes(
      new Argument(actual.expression, actualElementType),
      new Argument(expected.expression, expectedElementType),
      option);
  }

  private static boolean isNotCompatibleClass(Argument actual, Argument expected, Option option) {
    return isNotInstanceOf(actual, expected, option) && isNotInstanceOf(expected, actual, option);
  }

  private static boolean isNotInstanceOf(Argument argumentA, Argument argumentB, Option option) {
    if (argumentA.type.isPrimitive() && argumentB.type.isPrimitive()) {
      return false;
    }
    Type typeA = wrapperType(argumentA.type);
    Type typeB = wrapperType(argumentB.type);
    if (typeA.isUnknown() || typeB.isUnknown() || !typeA.isClass() || !typeB.isClass()) {
      return false;
    } else if (typeA.symbol().isInterface() && typeB.symbol().isInterface()) {
      return option == Option.REJECT_DISSIMILAR_INTERFACE && !typeA.isSubtypeOf(typeB);
    } else if (typeB.symbol().isInterface()) {
      // typeA is not an interface
      return (option == Option.REJECT_DISSIMILAR_INTERFACE || typeA.symbol().isFinal()) &&
        !typeA.isSubtypeOf(typeB);
    } else if (typeA.symbol().isInterface()) {
      // typeB is not an interface
      return true;
    } else {
      // typeA and typeB are not interfaces
      return !typeA.isSubtypeOf(typeB);
    }
  }

  private void createIssue(Argument actual, Argument expected) {
    reportIssue(
      expected.expression,
      "Change the assertion arguments to not compare dissimilar types.",
      Collections.singletonList(new JavaFileScannerContext.Location("Actual", actual.expression)),
      null);
  }

  static class Argument {

    final ExpressionTree expression;

    final Type expressionType;

    final Type type;

    Argument(MethodInvocationTree mit, int argumentIndex) {
      expression = mit.arguments().get(argumentIndex);
      expressionType = expression.symbolType().erasure();
      Type expectedType = expectedArgumentType(mit, argumentIndex);
      if (expectedType.isUnknown()) {
        type = expressionType;
      } else if (expectedType.isPrimitive()) {
        type = expectedType;
      } else {
        type = wrapperType(expressionType);
      }
    }

    Argument(ExpressionTree expression, Type type) {
      this.expression = expression;
      this.expressionType = type;
      this.type = type;
    }

    boolean isArray() {
      return type.isArray();
    }

    boolean isNotArray() {
      return !type.isUnknown() && !type.isArray() && !type.is(JAVA_LANG_OBJECT) && !isNullLiteral();
    }

    boolean isNullLiteral() {
      return expression.kind() == Tree.Kind.NULL_LITERAL;
    }

    boolean isPrimitive() {
      return expressionType.isPrimitive();
    }

    static Type expectedArgumentType(MethodInvocationTree mit, int argumentIndex) {
      if (mit.methodSymbol().isUnknown()) {
        return Symbols.unknownType;
      }
      List<Type> parameterTypes = mit.methodSymbol().parameterTypes();
      if (argumentIndex >= parameterTypes.size()) {
        return Symbols.unknownType;
      }
      return parameterTypes.get(argumentIndex).erasure();
    }
  }

  static Type wrapperType(Type type) {
    if (type.isPrimitive()) {
      Type wrapperType = type.primitiveWrapperType();
      return wrapperType != null ? wrapperType : type;
    }
    return type;
  }

}
