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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.sonar.java.checks.tests.AssertJChainSimplificationHelper.*;
import static org.sonar.java.checks.tests.AssertJChainSimplificationCheck.SimplifierWithContext;
import static org.sonar.java.checks.tests.AssertJChainSimplificationCheck.SimplifierWithoutContext;

public class AssertJChainSimplificationIndex {

  private AssertJChainSimplificationIndex() {
    // Hide default constructor
  }

  private static final String HAS_SIZE = "hasSize";
  private static final String IS_EQUAL_TO = "isEqualTo";
  private static final String IS_FALSE = "isFalse";
  private static final String IS_GREATER_THAN = "isGreaterThan";
  private static final String IS_GREATER_THAN_OR_EQUAL_TO = "isGreaterThanOrEqualTo";
  private static final String IS_LESS_THAN = "isLessThan";
  private static final String IS_LESS_THAN_OR_EQUAL_TO = "isLessThanOrEqualTo";
  private static final String IS_NEGATIVE = "isNegative";
  private static final String IS_NOT_EQUAL_TO = "isNotEqualTo";
  private static final String IS_NOT_NEGATIVE = "isNotNegative";
  private static final String IS_NOT_POSITIVE = "isNotPositive";
  private static final String IS_NOT_ZERO = "isNotZero";
  private static final String IS_POSITIVE = "isPositive";
  private static final String IS_TRUE = "isTrue";
  private static final String IS_ZERO = "isZero";

  /**
   * Stores multiple lists of simplifiers which are mapped to by a key. The key is the method name of the predicate
   * that this simplifier applies to. The simplifiers in this map are not provided with the subject argument.
   * <p>
   * For instance, if you have a key {@code hasSize} that maps to a list containing
   * {@code PredicateSimplifierWithoutContext.withSingleArg(arg -> isZero(arg), "isEmpty()")} then it can be read as:
   * "<b>{@code hasSize}</b> with an argument that is <b>zero</b> can be simplified to <b>{@code isEmpty()}</b>".
   */
  static final Map<String, List<SimplifierWithoutContext>> CONTEXT_FREE_SIMPLIFIERS =
    ImmutableMap.<String, List<SimplifierWithoutContext>>builder()
      .put(HAS_SIZE, Collections.singletonList(
        PredicateSimplifierWithoutContext.withSingleArg(ArgumentHelper::isZero, "isEmpty()")))
      .put(IS_EQUAL_TO, ImmutableList.of(
        PredicateSimplifierWithoutContext.withSingleArg(ExpressionUtils::isNullLiteral, "isNull()"),
        PredicateSimplifierWithoutContext.withSingleArg(ArgumentHelper::isTrue, "isTrue()"),
        PredicateSimplifierWithoutContext.withSingleArg(ArgumentHelper::isFalse, "isFalse()")))
      .put(IS_NOT_EQUAL_TO, Collections.singletonList(
        PredicateSimplifierWithoutContext.withSingleArg(ExpressionUtils::isNullLiteral, "isNotNull()")))
      .build();

  /**
   * Stores multiple lists of simplifiers with context, similar to {@link #CONTEXT_FREE_SIMPLIFIERS}. The
   * simplifiers in this map, though, have access to the subject as well (i.e. the {@code assertThat(...)} method
   * and its argument).
   */
  static final Map<String, List<SimplifierWithContext>> SIMPLIFIERS_WITH_CONTEXT =
    ImmutableMap.<String, List<AssertJChainSimplificationCheck.SimplifierWithContext>>builder()
      .put(IS_EQUAL_TO, ImmutableList.of(
        PredicateSimplifierWithContext.methodCallInSubject(Matchers.TO_STRING, msgWithActualCustom("hasToString", "expectedString")),
        PredicateSimplifierWithContext.withSingleArgument(predicateArg -> hasMethodCallAsArg(predicateArg, Matchers.HASH_CODE),
                                                                                                    subjectArg -> hasMethodCallAsArg(subjectArg, Matchers.HASH_CODE), msgWithActualExpected("hasSameHashCodeAs")),
        compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected("isEqualByComparingTo"))))
      .put(IS_FALSE, ImmutableList.of(
        PredicateSimplifierWithContext.methodCallInSubject(Matchers.EQUALS_METHOD, msgWithActualExpected(IS_NOT_EQUAL_TO)),
        PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> ArgumentHelper.equalsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNotNull")),
        PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> ArgumentHelper.notEqualsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNull")),
        PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.EQUAL_TO), msgWithActualExpected("isNotSameAs")),
        PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.NOT_EQUAL_TO), msgWithActualExpected("isSameAs")),
        PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.INSTANCE_OF), msgWithActualCustom("isNotInstanceOf", "ExpectedClass.class"))))
      .put(IS_GREATER_THAN, ImmutableList.of(
        compareToSimplifier(ArgumentHelper::isNegOne, msgWithActualExpected(IS_GREATER_THAN_OR_EQUAL_TO)),
        compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected(IS_GREATER_THAN))))
      .put(IS_GREATER_THAN_OR_EQUAL_TO, ImmutableList.of(
        compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected(IS_GREATER_THAN_OR_EQUAL_TO)),
        compareToSimplifier(ArgumentHelper::isOne, msgWithActualExpected(IS_GREATER_THAN))))
      .put(IS_LESS_THAN, ImmutableList.of(
        compareToSimplifier(ArgumentHelper::isOne, msgWithActualExpected(IS_LESS_THAN_OR_EQUAL_TO)),
        compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected(IS_LESS_THAN))))
      .put(IS_LESS_THAN_OR_EQUAL_TO, ImmutableList.of(
        compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected(IS_LESS_THAN_OR_EQUAL_TO)),
        compareToSimplifier(ArgumentHelper::isNegOne, msgWithActualExpected(IS_LESS_THAN))))
      .put(IS_NEGATIVE, Collections.singletonList(
        compareToSimplifier(msgWithActualExpected(IS_LESS_THAN))))
      .put(IS_NOT_EQUAL_TO, Collections.singletonList(
        compareToSimplifier(ArgumentHelper::isZero, msgWithActualExpected("isNotEqualByComparingTo"))))
      .put(IS_NOT_NEGATIVE, Collections.singletonList(
        compareToSimplifier(msgWithActualExpected(IS_GREATER_THAN_OR_EQUAL_TO))))
      .put(IS_NOT_POSITIVE, Collections.singletonList(
        compareToSimplifier(msgWithActualExpected(IS_LESS_THAN_OR_EQUAL_TO))))
      .put(IS_NOT_ZERO, Collections.singletonList(
        compareToSimplifier(msgWithActualExpected("isNotEqualByComparingTo"))))
      .put(IS_POSITIVE, Collections.singletonList(
        compareToSimplifier(msgWithActualExpected(IS_GREATER_THAN))))
      .put(IS_TRUE, ImmutableList.of(
        PredicateSimplifierWithContext.methodCallInSubject(Matchers.EQUALS_METHOD, msgWithActualExpected(IS_EQUAL_TO)),
        PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> ArgumentHelper.equalsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNull")),
        PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> ArgumentHelper.notEqualsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNotNull")),
        PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.EQUAL_TO), msgWithActualExpected("isSameAs")),
        PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.NOT_EQUAL_TO), msgWithActualExpected("isNotSameAs")),
        PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.INSTANCE_OF), msgWithActualCustom("isInstanceOf", "ExpectedClass.class"))))
      .put(IS_ZERO, Collections.singletonList(
        compareToSimplifier(msgWithActualExpected("isEqualByComparingTo"))))
      .build();

  private static class Matchers {
    public static final MethodMatchers EQUALS_METHOD = MethodMatchers.create().ofAnyType().names("equals")
                                                         .addParametersMatcher(parameters -> parameters.size() == 1).build();
    public static final MethodMatchers TO_STRING = MethodMatchers.create().ofAnyType().names("toString")
                                                     .addWithoutParametersMatcher().build();
    public static final MethodMatchers HASH_CODE = MethodMatchers.create().ofAnyType().names("hashCode")
                                                     .addWithoutParametersMatcher().build();
    public static final MethodMatchers COMPARE_TO = MethodMatchers.create().ofSubTypes("java.lang.Comparable")
                                                      .names("compareTo").addParametersMatcher(parameters -> parameters.size() == 1).build();
  }

  private static PredicateSimplifierWithContext compareToSimplifier(Predicate<ExpressionTree> predicateArgCondition, String simplification) {
    return PredicateSimplifierWithContext.withSingleArgument(predicateArgCondition,
                                                             arg -> hasMethodCallAsArg(arg, Matchers.COMPARE_TO), simplification);
  }

  private static PredicateSimplifierWithContext compareToSimplifier(String simplification) {
    return PredicateSimplifierWithContext.methodCallInSubject(Matchers.COMPARE_TO, simplification);
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

  private static class PredicateSimplifierWithContext implements SimplifierWithContext {
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
      Predicate<ExpressionTree> subjectArgumentCondition, String simplification) {
      return new PredicateSimplifierWithContext(x -> true, subjectMit -> subjectMit.arguments().size() == 1 && subjectArgumentCondition.test(unwrap(subjectMit.arguments().get(0))),
                                                simplification);
    }

    public static PredicateSimplifierWithContext methodCallInSubject(
      MethodMatchers methodCallMatcher,
      String simplification) {

      return withSubjectArgumentCondition(arg -> hasMethodCallAsArg(arg, methodCallMatcher), simplification);
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
