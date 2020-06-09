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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.tests.AssertJChainSimplificationCheck.SimplifierWithContext;
import static org.sonar.java.checks.tests.AssertJChainSimplificationCheck.SimplifierWithoutContext;
import static org.sonar.java.checks.tests.AssertJChainSimplificationHelper.ArgumentHelper;
import static org.sonar.java.checks.tests.AssertJChainSimplificationHelper.hasMethodCallAsArg;
import static org.sonar.java.checks.tests.AssertJChainSimplificationHelper.msgWithActual;
import static org.sonar.java.checks.tests.AssertJChainSimplificationHelper.msgWithActualCustom;
import static org.sonar.java.checks.tests.AssertJChainSimplificationHelper.msgWithActualExpected;
import static org.sonar.java.checks.tests.AssertJChainSimplificationHelper.unwrap;
import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.PredicateSimplifierWithContext.methodCallInSubject;
import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.PredicateSimplifierWithContext.withSubjectArgumentCondition;

public class AssertJChainSimplificationIndex {

  private AssertJChainSimplificationIndex() {
    // Hide default constructor
  }

  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final String CONTAINS = "contains";
  private static final String DOES_NOT_CONTAIN = "doesNotContain";
  private static final String DOES_NOT_START_WITH = "doesNotStartWith";
  private static final String HAS_SIZE = "hasSize";
  private static final String IS_EMPTY = "isEmpty";
  private static final String IS_EQUAL_TO = "isEqualTo";
  private static final String IS_EQUAL_TO_IGNORING_CASE = "isEqualToIgnoringCase";
  private static final String IS_FALSE = "isFalse";
  private static final String IS_GREATER_THAN = "isGreaterThan";
  private static final String IS_GREATER_THAN_OR_EQUAL_TO = "isGreaterThanOrEqualTo";
  private static final String IS_LESS_THAN = "isLessThan";
  private static final String IS_LESS_THAN_OR_EQUAL_TO = "isLessThanOrEqualTo";
  private static final String IS_NEGATIVE = "isNegative";
  private static final String IS_NOT_EMPTY = "isNotEmpty";
  private static final String IS_NOT_EQUAL_TO = "isNotEqualTo";
  private static final String IS_NOT_EQUAL_TO_IGNORING_CASE = "isNotEqualToIgnoringCase";
  private static final String IS_NOT_NEGATIVE = "isNotNegative";
  private static final String IS_NOT_POSITIVE = "isNotPositive";
  private static final String IS_NOT_ZERO = "isNotZero";
  private static final String IS_POSITIVE = "isPositive";
  private static final String IS_TRUE = "isTrue";
  private static final String IS_ZERO = "isZero";
  private static final String STARTS_WITH = "startsWith";

  /**
   * Stores multiple lists of simplifiers which are mapped to by a key. The key is the method name of the predicate
   * that this simplifier applies to. The simplifiers in this map are not provided with the subject argument.
   * <p>
   * For instance, if you have a key {@code hasSize} that maps to a list containing
   * {@code PredicateSimplifierWithoutContext.withSingleArg(arg -> isZero(arg), "isEmpty()")} then it can be read as:
   * "<b>{@code hasSize}</b> with an argument that is <b>zero</b> can be simplified to <b>{@code isEmpty()}</b>".
   */
  static final Map<String, List<SimplifierWithoutContext>> CONTEXT_FREE_SIMPLIFIERS = ImmutableMap.<String, List<SimplifierWithoutContext>>builder()
    .put(HAS_SIZE, Collections.singletonList(
      PredicateSimplifierWithoutContext.withSingleArg(ArgumentHelper::isZero, "isEmpty()")))
    .put(IS_EQUAL_TO, ImmutableList.of(
      PredicateSimplifierWithoutContext.withSingleArg(ExpressionUtils::isNullLiteral, "isNull()"),
      PredicateSimplifierWithoutContext.withSingleArg(LiteralUtils::isTrue, "isTrue()"),
      PredicateSimplifierWithoutContext.withSingleArg(LiteralUtils::isFalse, "isFalse()"),
      PredicateSimplifierWithoutContext.withSingleArg(LiteralUtils::isEmptyString, "isEmpty()")))
    .put(IS_NOT_EQUAL_TO, Collections.singletonList(
      PredicateSimplifierWithoutContext.withSingleArg(ExpressionUtils::isNullLiteral, "isNotNull()")))
    .build();

  /**
   * Stores multiple lists of simplifiers with context, similar to {@link #CONTEXT_FREE_SIMPLIFIERS}. The
   * simplifiers in this map, though, have access to the subject as well (i.e. the {@code assertThat(...)} method
   * and its argument).
   */
  static final Map<String, List<SimplifierWithContext>> SIMPLIFIERS_WITH_CONTEXT = ImmutableMap.<String, List<AssertJChainSimplificationCheck.SimplifierWithContext>>builder()
    .put(IS_EQUAL_TO, ImmutableList.of(
      methodCallInSubject(Matchers.TO_STRING, msgWithActualCustom("hasToString", "expectedString")),
      methodCallInSubject(predicateArg -> hasMethodCallAsArg(predicateArg, Matchers.HASH_CODE),
         Matchers.HASH_CODE, msgWithActualExpected("hasSameHashCodeAs")),
      compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected("isEqualByComparingTo")),
    methodCallInSubject(ArgumentHelper::isZero, Matchers.COMPARE_TO_IGNORE_CASE, msgWithActualExpected(IS_EQUAL_TO_IGNORING_CASE)),
      indexOfSimplifier(ArgumentHelper::isZero, STARTS_WITH),
      indexOfSimplifier(ArgumentHelper::isNegOne, DOES_NOT_CONTAIN),
      methodCallInSubject(ArgumentHelper::isZero, Matchers.LENGTH, msgWithActual(IS_EMPTY)),
      methodCallInSubject(predicateArg -> hasMethodCallAsArg(predicateArg, Matchers.LENGTH), Matchers.LENGTH, msgWithActualExpected("hasSameSizeAs")),
      methodCallInSubject(Matchers.LENGTH, msgWithActualExpected(HAS_SIZE)))).put(IS_FALSE, ImmutableList.of(
      methodCallInSubject(Matchers.EQUALS_METHOD, msgWithActualExpected(IS_NOT_EQUAL_TO)),
      methodCallInSubject(Matchers.CONTENT_EQUALS, msgWithActualExpected(IS_NOT_EQUAL_TO)),
      methodCallInSubject(Matchers.EQUALS_IGNORE_CASE, msgWithActualExpected(IS_NOT_EQUAL_TO_IGNORING_CASE)),
      methodCallInSubject(Matchers.CONTAINS, msgWithActualExpected(DOES_NOT_CONTAIN)),
      methodCallInSubject(Matchers.STARTS_WITH, msgWithActualExpected(DOES_NOT_START_WITH)),
      methodCallInSubject(Matchers.ENDS_WITH, msgWithActualExpected("doesNotEndWith")),
      methodCallInSubject(Matchers.MATCHES, msgWithActualExpected("doesNotMatch")),withSubjectArgumentCondition(arg -> ArgumentHelper.equalsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNotNull")),
      withSubjectArgumentCondition(arg -> ArgumentHelper.notEqualsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNull")),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.EQUAL_TO), msgWithActualExpected("isNotSameAs")),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.NOT_EQUAL_TO), msgWithActualExpected("isSameAs")),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.INSTANCE_OF), msgWithActualCustom("isNotInstanceOf", "ExpectedClass.class")),
    methodCallInSubject(Matchers.IS_EMPTY, msgWithActual(IS_NOT_EMPTY)))).put(IS_GREATER_THAN, ImmutableList.of(
      compareToSimplifier(ArgumentHelper::isNegOne, msgWithActualExpected(IS_GREATER_THAN_OR_EQUAL_TO)),
      compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected(IS_GREATER_THAN)),
    indexOfSimplifier(ArgumentHelper::isNegOne, CONTAINS))).put(IS_GREATER_THAN_OR_EQUAL_TO, ImmutableList.of(
      compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected(IS_GREATER_THAN_OR_EQUAL_TO)),
      compareToSimplifier(ArgumentHelper::isOne, msgWithActualExpected(IS_GREATER_THAN)),
      indexOfSimplifier(ArgumentHelper::isZero, CONTAINS)))
    .put(IS_LESS_THAN, ImmutableList.of(
      compareToSimplifier(ArgumentHelper::isOne, msgWithActualExpected(IS_LESS_THAN_OR_EQUAL_TO)),
      compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected(IS_LESS_THAN)),
    indexOfSimplifier(ArgumentHelper::isZero, DOES_NOT_CONTAIN),
      methodCallInSubject(ArgumentHelper::isOne, Matchers.LENGTH, msgWithActual(IS_EMPTY)))).put(IS_LESS_THAN_OR_EQUAL_TO, ImmutableList.of(
      compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected(IS_LESS_THAN_OR_EQUAL_TO)),
      compareToSimplifier(ArgumentHelper::isNegOne, msgWithActualExpected(IS_LESS_THAN)),
    indexOfSimplifier(ArgumentHelper::isNegOne, DOES_NOT_CONTAIN),
      methodCallInSubject(ArgumentHelper::isZero, Matchers.LENGTH, msgWithActual(IS_EMPTY))))
    .put(IS_NEGATIVE, ImmutableList.of(
      compareToSimplifier(msgWithActualExpected(IS_LESS_THAN)),
    indexOfSimplifier(DOES_NOT_CONTAIN)))
    .put(IS_NOT_EMPTY, Collections.singletonList(
      methodCallInSubject(Matchers.TRIM, msgWithActual("isNotBlank"))))
    .put(IS_NOT_EQUAL_TO, ImmutableList.of(
      compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected("isNotEqualByComparingTo")),
    methodCallInSubject(ArgumentHelper::isZero, Matchers.COMPARE_TO_IGNORE_CASE, msgWithActualExpected(IS_NOT_EQUAL_TO_IGNORING_CASE)),
      indexOfSimplifier(ArgumentHelper::isZero, DOES_NOT_START_WITH),
      methodCallInSubject(LiteralUtils::isEmptyString, Matchers.TRIM, msgWithActual("isNotBlank"))))
    .put(IS_NOT_NEGATIVE, ImmutableList.of(
      compareToSimplifier(msgWithActualExpected(IS_GREATER_THAN_OR_EQUAL_TO)),
    indexOfSimplifier(CONTAINS))).put(IS_NOT_POSITIVE, Collections.singletonList(
      compareToSimplifier(msgWithActualExpected(IS_LESS_THAN_OR_EQUAL_TO))))
    .put(IS_NOT_ZERO, ImmutableList.of(
      compareToSimplifier(msgWithActualExpected("isNotEqualByComparingTo")),
    methodCallInSubject(Matchers.COMPARE_TO_IGNORE_CASE, msgWithActualExpected(IS_NOT_EQUAL_TO_IGNORING_CASE)),
      indexOfSimplifier(DOES_NOT_START_WITH))).put(IS_POSITIVE, Collections.singletonList(
      compareToSimplifier(msgWithActualExpected(IS_GREATER_THAN))))
    .put(IS_TRUE, ImmutableList.of(
      methodCallInSubject(Matchers.EQUALS_METHOD, msgWithActualExpected(IS_EQUAL_TO)),
      methodCallInSubject(Matchers.CONTENT_EQUALS, msgWithActualExpected(IS_EQUAL_TO)),
      methodCallInSubject(Matchers.EQUALS_IGNORE_CASE, msgWithActualExpected(IS_EQUAL_TO_IGNORING_CASE)),
      methodCallInSubject(Matchers.CONTAINS, msgWithActualExpected(CONTAINS)),
      methodCallInSubject(Matchers.STARTS_WITH, msgWithActualExpected(STARTS_WITH)),
      methodCallInSubject(Matchers.ENDS_WITH, msgWithActualExpected("endsWith")),
      methodCallInSubject(Matchers.MATCHES, msgWithActualExpected("matches")),withSubjectArgumentCondition(arg -> ArgumentHelper.equalsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNull")),
      withSubjectArgumentCondition(arg -> ArgumentHelper.notEqualsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNotNull")),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.EQUAL_TO), msgWithActualExpected("isSameAs")),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.NOT_EQUAL_TO), msgWithActualExpected("isNotSameAs")),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.INSTANCE_OF), msgWithActualCustom("isInstanceOf", "ExpectedClass.class")),
    methodCallInSubject(Matchers.IS_EMPTY, msgWithActual(IS_EMPTY))))
    .put(IS_ZERO, ImmutableList.of(
      compareToSimplifier(msgWithActualExpected("isEqualByComparingTo")),
      methodCallInSubject(Matchers.COMPARE_TO_IGNORE_CASE, msgWithActualExpected(IS_EQUAL_TO_IGNORING_CASE)),
      indexOfSimplifier(STARTS_WITH),
      methodCallInSubject(Matchers.LENGTH, msgWithActual(IS_EMPTY))))
    .build();

  private static class Matchers {
    public static final MethodMatchers COMPARE_TO = MethodMatchers.create().ofSubTypes("java.lang.Comparable")
      .names("compareTo").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers COMPARE_TO_IGNORE_CASE = MethodMatchers.create().ofSubTypes(JAVA_LANG_STRING)
      .names("compareToIgnoreCase").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers CONTAINS = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names(AssertJChainSimplificationIndex.CONTAINS).addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers CONTENT_EQUALS = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("contentEquals").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers ENDS_WITH = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("endsWith").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers EQUALS_IGNORE_CASE = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("equalsIgnoreCase").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers EQUALS_METHOD = MethodMatchers.create().ofAnyType().names("equals")
      .addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers HASH_CODE = MethodMatchers.create().ofAnyType().names("hashCode")
      .addWithoutParametersMatcher().build();
    public static final MethodMatchers INDEX_OF_STRING = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("indexOf").addParametersMatcher(JAVA_LANG_STRING).build();
    public static final MethodMatchers IS_EMPTY = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names(AssertJChainSimplificationIndex.IS_EMPTY).addWithoutParametersMatcher().build();
    public static final MethodMatchers LENGTH = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("length").addWithoutParametersMatcher().build();
    public static final MethodMatchers MATCHES = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("matches").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers STARTS_WITH = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names(AssertJChainSimplificationIndex.STARTS_WITH).addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers TO_STRING = MethodMatchers.create().ofAnyType().names("toString")
      .addWithoutParametersMatcher().build();
    public static final MethodMatchers TRIM = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("trim").addWithoutParametersMatcher().build();
  }

  private static PredicateSimplifierWithContext compareToSimplifier(Predicate<ExpressionTree> predicateArgCondition, String simplification) {
    return PredicateSimplifierWithContext.methodCallInSubject(predicateArgCondition, Matchers.COMPARE_TO, simplification);
  }

  private static PredicateSimplifierWithContext compareToSimplifier(String simplification) {
    return PredicateSimplifierWithContext.methodCallInSubject(Matchers.COMPARE_TO, simplification);
  }

  private static PredicateSimplifierWithContext indexOfSimplifier(Predicate<ExpressionTree> predicateArgCondition, String simplification) {
    return PredicateSimplifierWithContext.methodCallInSubject(predicateArgCondition, Matchers.INDEX_OF_STRING, msgWithActualExpected(simplification));
  }

  private static PredicateSimplifierWithContext indexOfSimplifier(String simplification) {
    return PredicateSimplifierWithContext.methodCallInSubject(Matchers.INDEX_OF_STRING, msgWithActualExpected(simplification));
  }

  private static class PredicateSimplifierWithoutContext implements SimplifierWithoutContext {
    private final Predicate<MethodInvocationTree> mitPredicate;
    private final String simplification;

    public PredicateSimplifierWithoutContext(
      Predicate<MethodInvocationTree> mitPredicate,
      String simplification) {

      this.mitPredicate = mitPredicate;
      this.simplification = simplification;
    }

    public static PredicateSimplifierWithoutContext withSingleArg(Predicate<ExpressionTree> argumentPredicate, String simplified) {
      return new PredicateSimplifierWithoutContext(mit -> {
        Arguments arguments = mit.arguments();
        return arguments.size() == 1 && argumentPredicate.test(arguments.get(0));
      }, simplified);
    }

    @Override
    public Optional<String> simplify(MethodInvocationTree predicate) {
      if (mitPredicate.test(predicate)) {
        return Optional.of(simplification);
      } else {
        return Optional.empty();
      }
    }
  }

  static class PredicateSimplifierWithContext implements SimplifierWithContext {
    private final Predicate<MethodInvocationTree> predicateCondition;
    private final Predicate<MethodInvocationTree> subjectCondition;
    private final String simplification;

    public PredicateSimplifierWithContext(
      Predicate<MethodInvocationTree> predicateCondition,
      Predicate<MethodInvocationTree> subjectCondition,
      String simplification) {
      this.predicateCondition = predicateCondition;
      this.subjectCondition = subjectCondition;
      this.simplification = simplification;
    }

    public static PredicateSimplifierWithContext withSubjectArgumentCondition(
      Predicate<ExpressionTree> predicateArgumentCondition, Predicate<ExpressionTree> subjectArgumentCondition,
      String simplification) {
      return new PredicateSimplifierWithContext(
        predicateMit -> predicateMit.arguments().size() == 1 && predicateArgumentCondition.test(unwrap(predicateMit.arguments().get(0))),
        subjectMit -> subjectMit.arguments().size() == 1 && subjectArgumentCondition.test(unwrap(subjectMit.arguments().get(0))),
        simplification);
    }

    public static PredicateSimplifierWithContext withSubjectArgumentCondition(
      Predicate<ExpressionTree> subjectArgumentCondition, String simplification) {
      return new PredicateSimplifierWithContext(x -> true, subjectMit -> subjectMit.arguments().size() == 1 && subjectArgumentCondition.test(unwrap(subjectMit.arguments().get(0))),
        simplification);
    }

    public static PredicateSimplifierWithContext methodCallInSubject(
      MethodMatchers methodCallMatcher,
      String simplification) {
      return withSubjectArgumentCondition(arg -> hasMethodCallAsArg(arg, methodCallMatcher), simplification);
    }

    public static PredicateSimplifierWithContext methodCallInSubject(
      Predicate<ExpressionTree> predicateArgumentCondition,
      MethodMatchers methodCallMatcher,
      String simplification) {
      return withSubjectArgumentCondition(predicateArgumentCondition, arg -> hasMethodCallAsArg(arg, methodCallMatcher),
        simplification);
    }

    public static PredicateSimplifierWithContext withSingleArgument(
      Predicate<ExpressionTree> predicateArgsCondition, Predicate<ExpressionTree> subjectArgsCondition,
      String simplification) {

      return new PredicateSimplifierWithContext(predicateMit -> predicateMit.arguments().size() == 1 && predicateArgsCondition.test(unwrap(predicateMit.arguments().get(0))),
        subjectMit -> subjectMit.arguments().size() == 1 && subjectArgsCondition.test(unwrap(subjectMit.arguments().get(0))),
        simplification);
    }

    @Override
    public Optional<String> simplify(MethodInvocationTree subject, MethodInvocationTree predicate) {
      if (predicateCondition.test(predicate) && subjectCondition.test(subject)) {
        return Optional.of(simplification);
      } else {
        return Optional.empty();
      }
    }
  }
}
