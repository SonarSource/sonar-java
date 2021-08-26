/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.java.collections.MapBuilder;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import static org.sonar.java.checks.tests.AssertJChainSimplificationCheck.SimplifierWithContext;
import static org.sonar.java.checks.tests.AssertJChainSimplificationCheck.SimplifierWithoutContext;
import static org.sonar.java.checks.tests.AssertJChainSimplificationHelper.ArgumentHelper;
import static org.sonar.java.checks.tests.AssertJChainSimplificationHelper.hasMethodCallAsArg;
import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.PredicateSimplifierWithContext.methodCallInSubject;
import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.PredicateSimplifierWithContext.withSubjectArgumentCondition;
import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.WithContextSimplification.msgWithActual;
import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.WithContextSimplification.msgWithActualCustom;
import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.WithContextSimplification.msgWithActualExpected;
import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.WithContextSimplification.msgWithActualExpectedInPredicate;
import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.WithContextSimplification.msgWithActualExpectedInSubject;
import static org.sonar.java.model.ExpressionUtils.skipParentheses;

public class AssertJChainSimplificationIndex {

  private AssertJChainSimplificationIndex() {
    // Hide default constructor
  }

  static final String QUICK_FIX_MESSAGE_FORMAT_STRING = "Use \"%s\"";

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_MAP = "java.util.Map";
  private static final String JAVA_UTIL_COLLECTION = "java.util.Collection";
  private static final String JAVA_IO_FILE = "java.io.File";
  private static final String JAVA_NIO_FILE_PATH = "java.nio.file.Path";
  private static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";

  private static final String CONTAINS = "contains";
  private static final String CONTAINS_KEY = "containsKey";
  private static final String CONTAINS_VALUE = "containsValue";
  private static final String DOES_NOT_CONTAIN = "doesNotContain";
  private static final String DOES_NOT_START_WITH = "doesNotStartWith";
  private static final String HAS_SIZE = "hasSize";
  private static final String IS_BLANK = "isBlank";
  private static final String IS_EMPTY = "isEmpty";
  private static final String IS_EQUAL_TO = "isEqualTo";
  private static final String IS_EQUAL_TO_IGNORING_CASE = "isEqualToIgnoringCase";
  private static final String IS_FALSE = "isFalse";
  private static final String IS_GREATER_THAN = "isGreaterThan";
  private static final String IS_GREATER_THAN_OR_EQUAL_TO = "isGreaterThanOrEqualTo";
  private static final String IS_LESS_THAN = "isLessThan";
  private static final String IS_LESS_THAN_OR_EQUAL_TO = "isLessThanOrEqualTo";
  private static final String IS_NEGATIVE = "isNegative";
  private static final String IS_NOT_BLANK = "isNotBlank";
  private static final String IS_NOT_EMPTY = "isNotEmpty";
  private static final String IS_NOT_EQUAL_TO = "isNotEqualTo";
  private static final String IS_NOT_EQUAL_TO_IGNORING_CASE = "isNotEqualToIgnoringCase";
  private static final String IS_NOT_NEGATIVE = "isNotNegative";
  private static final String IS_NOT_NULL = "isNotNull";
  private static final String IS_NOT_POSITIVE = "isNotPositive";
  private static final String IS_NOT_PRESENT = "isNotPresent";
  private static final String IS_NOT_ZERO = "isNotZero";
  private static final String IS_POSITIVE = "isPositive";
  private static final String IS_PRESENT = "isPresent";
  private static final String IS_SAME_AS = "isSameAs";
  private static final String IS_TRUE = "isTrue";
  private static final String IS_ZERO = "isZero";
  private static final String IS_NULL = "isNull";
  private static final String STARTS_WITH = "startsWith";
  private static final String ENDS_WITH = "endsWith";
  private static final String HAS_SAME_SIZE_AS = "hasSameSizeAs";
  private static final String LENGTH = "length";

  private static final WithContextSimplification OPTIONAL_PRESENT_REPLACEMENT = msgWithActual(IS_PRESENT);
  private static final WithContextSimplification OPTIONAL_EMPTY_REPLACEMENT =
    new WithContextSimplification(String.format("assertThat(actual).%s() or assertThat(actual).%s()", IS_NOT_PRESENT, IS_EMPTY));

  /**
   * Stores multiple lists of simplifiers which are mapped to by a key. The key is the method name of the predicate
   * that this simplifier applies to. The simplifiers in this map are not provided with the subject argument.
   * <p>
   * For instance, if you have a key {@code hasSize} that maps to a list containing
   * {@code PredicateSimplifierWithoutContext.withSingleArg(arg -> isZero(arg), "isEmpty()")} then it can be read as:
   * "<b>{@code hasSize}</b> with an argument that is <b>zero</b> can be simplified to <b>{@code isEmpty()}</b>".
   */
  static final Map<String, List<SimplifierWithoutContext>> CONTEXT_FREE_SIMPLIFIERS = MapBuilder.<String, List<SimplifierWithoutContext>>newMap()
    .put(HAS_SIZE, Collections.singletonList(
      PredicateSimplifierWithoutContext.withSingleArg(LiteralUtils::isZero, "isEmpty()")))
    .put(IS_EQUAL_TO, Collections.singletonList(
      PredicateSimplifierWithoutContext.withSingleArg(ExpressionUtils::isNullLiteral, "isNull()")))
    .put(IS_GREATER_THAN, Arrays.asList(
      PredicateSimplifierWithoutContext.withSingleArg(AssertJChainSimplificationIndex::isNegOneIntOrLong, "isNotNegative()"),
      PredicateSimplifierWithoutContext.withSingleArg(AssertJChainSimplificationIndex::isZeroIntOrLong, "isPositive()")))
    .put(IS_GREATER_THAN_OR_EQUAL_TO, Arrays.asList(
      PredicateSimplifierWithoutContext.withSingleArg(AssertJChainSimplificationIndex::isZeroIntOrLong, "isNotNegative()"),
      PredicateSimplifierWithoutContext.withSingleArg(AssertJChainSimplificationIndex::isOneIntOrLong, "isPositive()")))
    .put(IS_LESS_THAN, Arrays.asList(
      PredicateSimplifierWithoutContext.withSingleArg(AssertJChainSimplificationIndex::isZeroIntOrLong, "isNegative()"),
      PredicateSimplifierWithoutContext.withSingleArg(AssertJChainSimplificationIndex::isOneIntOrLong, "isNotPositive()")))
    .put(IS_LESS_THAN_OR_EQUAL_TO, Arrays.asList(
      PredicateSimplifierWithoutContext.withSingleArg(AssertJChainSimplificationIndex::isNegOneIntOrLong, "isNegative()"),
      PredicateSimplifierWithoutContext.withSingleArg(AssertJChainSimplificationIndex::isZeroIntOrLong, "isNotPositive()")))
    .put(IS_NOT_EQUAL_TO, Collections.singletonList(
      PredicateSimplifierWithoutContext.withSingleArg(subject -> ExpressionUtils.isNullLiteral(subject) &&
        !UnitTestUtils.isInUnitTestRelatedToObjectMethods(subject), "isNotNull()")
      ))
    .build();

  /**
   * Stores multiple lists of simplifiers with context, similar to {@link #CONTEXT_FREE_SIMPLIFIERS}. The
   * simplifiers in this map, though, have access to the subject as well (i.e. the {@code assertThat(...)} method
   * and its argument).
   */
  static final Map<String, List<SimplifierWithContext>> SIMPLIFIERS_WITH_CONTEXT = MapBuilder.<String, List<AssertJChainSimplificationCheck.SimplifierWithContext>>newMap()
    .put(IS_EQUAL_TO, Arrays.asList(
      withSubjectArgumentCondition(LiteralUtils::isTrue, AssertJChainSimplificationIndex::isNotObject, "isTrue()"),
      withSubjectArgumentCondition(LiteralUtils::isFalse, AssertJChainSimplificationIndex::isNotObject, "isFalse()"),
      withSubjectArgumentCondition(LiteralUtils::isEmptyString, AssertJChainSimplificationIndex::isNotObject, "isEmpty()"),
      withSubjectArgumentCondition(AssertJChainSimplificationIndex::isZeroIntOrLong, AssertJChainSimplificationIndex::isNotObject, "isZero()"),
      methodCallInSubject(Matchers.TO_STRING, msgWithActualCustom("hasToString", "expectedString")),
      methodCallInSubject(predicateArg -> hasMethodCallAsArg(predicateArg, Matchers.HASH_CODE), Matchers.HASH_CODE, msgWithActualExpected("hasSameHashCodeAs")),
      compareToSimplifier(LiteralUtils::isZero, "isEqualByComparingTo"),
      methodCallInSubject(LiteralUtils::isZero, Matchers.COMPARE_TO_IGNORE_CASE, msgWithActualExpectedInSubject(IS_EQUAL_TO_IGNORING_CASE)),
      indexOfSimplifier(LiteralUtils::isZero, STARTS_WITH),
      indexOfSimplifier(LiteralUtils::isNegOne, DOES_NOT_CONTAIN),
      methodCallInSubject(LiteralUtils::isZero, Matchers.STRING_LENGTH, msgWithActual(IS_EMPTY)),
      methodCallInSubject(predicateArg -> hasMethodCallAsArg(predicateArg, Matchers.STRING_LENGTH), Matchers.STRING_LENGTH, msgWithActualExpected(HAS_SAME_SIZE_AS)),
      methodCallInSubject(predicateArg -> hasMethodCallAsArg(predicateArg, Matchers.COLLECTION_SIZE), Matchers.COLLECTION_SIZE, msgWithActualExpected(HAS_SAME_SIZE_AS)),
      withSubjectArgumentCondition(AssertJChainSimplificationIndex::isArrayLength, AssertJChainSimplificationIndex::isArrayLength, msgWithActualExpected(HAS_SAME_SIZE_AS)),
      arrayLengthSimplifier(msgWithActualExpectedInPredicate(HAS_SIZE)),
      methodCallInSubject(MethodMatchers.or(Matchers.STRING_LENGTH, Matchers.COLLECTION_SIZE), msgWithActualExpectedInPredicate(HAS_SIZE)),
      methodCallInSubject(Matchers.FILE_LENGTH, msgWithActualExpectedInPredicate(HAS_SIZE)),
      methodCallInSubject(Matchers.FILE_GET_NAME, msgWithActualExpectedInPredicate("hasName")),
      methodCallInSubject(Matchers.FILE_GET_PARENT_AND_PARENT_FILE, msgWithActualExpectedInPredicate("hasParent")),
      methodCallInSubject(Matchers.PATH_GET_PARENT_AND_PARENT_FILE, msgWithActualExpectedInPredicate("hasParentRaw")),
      methodCallInSubject(Matchers.MAP_GET, msgWithActualCustom("containsEntry", "key, value")),
      methodCallInSubject(Matchers.PATH_GET_PARENT_AND_PARENT_FILE, msgWithActualExpectedInPredicate("hasParentRaw")),
      withSubjectArgumentCondition(predicateArg -> hasMethodCallAsArg(predicateArg, Matchers.EMPTY),
        subjectArg -> subjectArg.symbolType().is(JAVA_UTIL_OPTIONAL), OPTIONAL_EMPTY_REPLACEMENT),
      methodCallInSubject(Matchers.GET, msgWithActualExpectedInPredicate(CONTAINS))))
    .put(IS_FALSE, Arrays.asList(
      withSubjectArgumentCondition(arg ->
        hasMethodCallAsArg(arg, Matchers.EQUALS_METHOD) && !UnitTestUtils.isInUnitTestRelatedToObjectMethods(arg), msgWithActualExpectedInSubject(IS_NOT_EQUAL_TO)),
      methodCallInSubject(Matchers.CONTENT_EQUALS, msgWithActualExpectedInSubject(IS_NOT_EQUAL_TO)),
      methodCallInSubject(Matchers.EQUALS_IGNORE_CASE, msgWithActualExpectedInSubject(IS_NOT_EQUAL_TO_IGNORING_CASE)),
      methodCallInSubject(Matchers.CONTAINS, msgWithActualExpectedInSubject(DOES_NOT_CONTAIN)),
      methodCallInSubject(Matchers.STARTS_WITH, msgWithActualExpectedInSubject(DOES_NOT_START_WITH)),
      methodCallInSubject(Matchers.ENDS_WITH, msgWithActualExpectedInSubject("doesNotEndWith")),
      methodCallInSubject(Matchers.MATCHES, msgWithActualExpectedInSubject("doesNotMatch")),
      withSubjectArgumentCondition(arg -> ArgumentHelper.equalsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual(IS_NOT_NULL)),
      withSubjectArgumentCondition(arg -> ArgumentHelper.notEqualsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual(IS_NULL)),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.EQUAL_TO), msgWithActualExpected("isNotSameAs")),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.NOT_EQUAL_TO), msgWithActualExpected(IS_SAME_AS)),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.INSTANCE_OF), msgWithActualCustom("isNotInstanceOf", "ExpectedClass.class")),
      methodCallInSubject(Matchers.IS_BLANK, msgWithActual(IS_NOT_BLANK)),
      methodCallInSubject(Matchers.IS_EMPTY_GENERIC, msgWithActual(IS_NOT_EMPTY)),
      methodCallInSubject(Matchers.FILE_EXISTS, msgWithActual("doesNotExist")),
      methodCallInSubject(Matchers.FILE_AND_PATH_IS_ABSOLUTE, msgWithActual("isRelative")),
      methodCallInSubject(Matchers.IS_PRESENT, OPTIONAL_EMPTY_REPLACEMENT),
      methodCallInSubject(Matchers.IS_EMPTY_OPTIONAL, OPTIONAL_PRESENT_REPLACEMENT)))
    .put(IS_NEGATIVE, Arrays.asList(
      compareToSimplifier(IS_LESS_THAN),
      indexOfSimplifier(DOES_NOT_CONTAIN)))
    .put(IS_EMPTY, Collections.singletonList(
      methodCallInSubject(Matchers.FILE_LIST_AND_LIST_FILE, msgWithActual("isEmptyDirectory"))))
    .put(IS_NOT_EMPTY, Arrays.asList(
      methodCallInSubject(Matchers.TRIM, msgWithActual(IS_NOT_BLANK)),
      methodCallInSubject(Matchers.FILE_LIST_AND_LIST_FILE, msgWithActual("isNotEmptyDirectory"))))
    .put(IS_NOT_EQUAL_TO, Arrays.asList(
      withSubjectArgumentCondition(AssertJChainSimplificationIndex::isZeroIntOrLong, AssertJChainSimplificationIndex::isNotObject, "isNotZero()"),
      compareToSimplifier(LiteralUtils::isZero, "isNotEqualByComparingTo"),
      methodCallInSubject(LiteralUtils::isZero, Matchers.COMPARE_TO_IGNORE_CASE, msgWithActualExpectedInSubject(IS_NOT_EQUAL_TO_IGNORING_CASE)),
      indexOfSimplifier(LiteralUtils::isZero, DOES_NOT_START_WITH),
      methodCallInSubject(LiteralUtils::isEmptyString, Matchers.TRIM, msgWithActual(IS_NOT_BLANK)),
      methodCallInSubject(Matchers.MAP_GET, msgWithActualCustom("doesNotContainEntry", "key, value")),
      methodCallInSubject(LiteralUtils::isEmptyString, Matchers.TRIM, msgWithActual(IS_NOT_BLANK)),
      withSubjectArgumentCondition(predicateArg -> hasMethodCallAsArg(predicateArg, Matchers.EMPTY),
        subjectArg -> subjectArg.symbolType().is(JAVA_UTIL_OPTIONAL), OPTIONAL_PRESENT_REPLACEMENT)))
    .put(IS_NOT_NEGATIVE, Arrays.asList(
      compareToSimplifier(IS_GREATER_THAN_OR_EQUAL_TO),
      indexOfSimplifier(CONTAINS)))
    .put(IS_NOT_NULL, Collections.singletonList(
      withSubjectArgumentCondition(
        subjectArg -> hasMethodCallAsArg(subjectArg, Matchers.OR_ELSE) &&
          ExpressionUtils.isNullLiteral(((MethodInvocationTree) subjectArg).arguments().get(0)),
        OPTIONAL_PRESENT_REPLACEMENT)))
    .put(IS_NOT_POSITIVE, Arrays.asList(
      compareToSimplifier(IS_LESS_THAN_OR_EQUAL_TO),
      methodCallInSubject(Matchers.STRING_LENGTH, msgWithActual(IS_EMPTY))))
    .put(IS_NOT_ZERO, Arrays.asList(
      compareToSimplifier("isNotEqualByComparingTo"),
      methodCallInSubject(Matchers.COMPARE_TO_IGNORE_CASE, msgWithActualExpectedInSubject(IS_NOT_EQUAL_TO_IGNORING_CASE)),
      indexOfSimplifier(DOES_NOT_START_WITH),
      methodCallInSubject(Matchers.STRING_LENGTH, msgWithActual(IS_NOT_EMPTY)),
      methodCallInSubject(Matchers.FILE_LENGTH, msgWithActual(IS_NOT_EMPTY)),
      arrayLengthSimplifier(msgWithActual(IS_NOT_EMPTY))))
    .put(IS_POSITIVE, Arrays.asList(
      compareToSimplifier(IS_GREATER_THAN),
      methodCallInSubject(MethodMatchers.or(Matchers.STRING_LENGTH, Matchers.COLLECTION_SIZE), msgWithActual(IS_NOT_EMPTY)),
      methodCallInSubject(Matchers.FILE_LENGTH, msgWithActual(IS_NOT_EMPTY)),
      arrayLengthSimplifier(msgWithActual(IS_NOT_EMPTY))))
    .put(IS_SAME_AS, Collections.singletonList(
      methodCallInSubject(Matchers.GET, msgWithActualExpectedInPredicate("containsSame"))))
    .put(IS_TRUE, Arrays.asList(
      withSubjectArgumentCondition(arg ->
        hasMethodCallAsArg(arg, Matchers.EQUALS_METHOD) && !UnitTestUtils.isInUnitTestRelatedToObjectMethods(arg), msgWithActualExpectedInSubject(IS_EQUAL_TO)),
      methodCallInSubject(Matchers.CONTENT_EQUALS, msgWithActualExpectedInSubject(IS_EQUAL_TO)),
      methodCallInSubject(Matchers.EQUALS_IGNORE_CASE, msgWithActualExpectedInSubject(IS_EQUAL_TO_IGNORING_CASE)),
      methodCallInSubject(Matchers.CONTAINS, msgWithActualExpectedInSubject(CONTAINS)),
      methodCallInSubject(Matchers.COLLECTION_CONTAINS_ALL, msgWithActualExpectedInSubject("containsAll")),
      methodCallInSubject(Matchers.STARTS_WITH, msgWithActualExpectedInSubject(STARTS_WITH)),
      methodCallInSubject(Matchers.ENDS_WITH, msgWithActualExpectedInSubject(ENDS_WITH)),
      methodCallInSubject(Matchers.MATCHES, msgWithActualExpectedInSubject("matches")),
      withSubjectArgumentCondition(arg -> ArgumentHelper.equalsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual(IS_NULL)),
      withSubjectArgumentCondition(arg -> ArgumentHelper.notEqualsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual(IS_NOT_NULL)),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.EQUAL_TO), msgWithActualExpected(IS_SAME_AS)),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.NOT_EQUAL_TO), msgWithActualExpected("isNotSameAs")),
      withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.INSTANCE_OF), msgWithActualCustom("isInstanceOf", "ExpectedClass.class")),
      methodCallInSubject(Matchers.IS_BLANK, msgWithActual(IS_BLANK)),
      methodCallInSubject(Matchers.IS_EMPTY_GENERIC, msgWithActual(IS_EMPTY)),
      methodCallInSubject(Matchers.FILE_CAN_READ, msgWithActual("canRead")),
      methodCallInSubject(Matchers.FILE_CAN_WRITE, msgWithActual("canWrite")),
      methodCallInSubject(Matchers.FILE_EXISTS, msgWithActual("exists")),
      methodCallInSubject(Matchers.FILE_AND_PATH_IS_ABSOLUTE, msgWithActual("isAbsolute")),
      methodCallInSubject(Matchers.FILE_IS_DIRECTORY, msgWithActual("isDirectory")),
      methodCallInSubject(Matchers.FILE_IS_FILE, msgWithActual("isFile")),
      methodCallInSubject(Matchers.PATH_STARTS_WITH, msgWithActualExpectedInSubject("startsWithRaw")),
      methodCallInSubject(Matchers.PATH_ENDS_WITH, msgWithActualExpectedInSubject("endsWithRaw")),
      methodCallInSubject(Matchers.IS_EMPTY_GENERIC, msgWithActual(IS_EMPTY)),
      methodCallInSubject(Matchers.MAP_CONTAINS_KEY, msgWithActualExpectedInSubject(CONTAINS_KEY)),
      methodCallInSubject(Matchers.MAP_CONTAINS_VALUE, msgWithActualExpectedInSubject(CONTAINS_VALUE)),
      methodCallInSubject(Matchers.IS_PRESENT, OPTIONAL_PRESENT_REPLACEMENT),
      methodCallInSubject(Matchers.IS_EMPTY_OPTIONAL, OPTIONAL_EMPTY_REPLACEMENT)))
    .put(IS_ZERO, Arrays.asList(
      compareToSimplifier("isEqualByComparingTo"),
      methodCallInSubject(Matchers.COMPARE_TO_IGNORE_CASE, msgWithActualExpectedInSubject(IS_EQUAL_TO_IGNORING_CASE)),
      indexOfSimplifier(STARTS_WITH),
      methodCallInSubject(Matchers.STRING_LENGTH, msgWithActual(IS_EMPTY)),
      methodCallInSubject(MethodMatchers.or(Matchers.STRING_LENGTH, Matchers.COLLECTION_SIZE), msgWithActual(IS_EMPTY)),
      methodCallInSubject(Matchers.FILE_LENGTH, msgWithActual(IS_EMPTY)),
      arrayLengthSimplifier(msgWithActual(IS_EMPTY))))
    .put(IS_NULL, Arrays.asList(
      methodCallInSubject(Matchers.FILE_GET_PARENT_AND_PARENT_FILE, msgWithActual("hasNoParent")),
      methodCallInSubject(Matchers.PATH_GET_PARENT_AND_PARENT_FILE, msgWithActual("hasNoParentRaw")),
      withSubjectArgumentCondition(
        subjectArg -> hasMethodCallAsArg(subjectArg, Matchers.OR_ELSE) &&
          ExpressionUtils.isNullLiteral(((MethodInvocationTree) subjectArg).arguments().get(0)),
        OPTIONAL_EMPTY_REPLACEMENT)))
    .put(IS_LESS_THAN_OR_EQUAL_TO, Arrays.asList(
      methodCallInSubject(Matchers.COLLECTION_SIZE, msgWithActualExpectedInPredicate("hasSizeLessThanOrEqualTo")),
      arrayLengthSimplifier(msgWithActualExpectedInPredicate("hasSizeLessThanOrEqualTo"))))
    .put(IS_LESS_THAN, Arrays.asList(
      methodCallInSubject(Matchers.COLLECTION_SIZE, msgWithActualExpectedInPredicate("hasSizeLessThan")),
      arrayLengthSimplifier(msgWithActualExpectedInPredicate("hasSizeLessThan"))))
    .put(IS_GREATER_THAN_OR_EQUAL_TO, Arrays.asList(
      methodCallInSubject(Matchers.COLLECTION_SIZE, msgWithActualExpectedInPredicate("hasSizeGreaterThanOrEqualTo")),
      arrayLengthSimplifier(msgWithActualExpectedInPredicate("hasSizeGreaterThanOrEqualTo"))))
    .put(IS_GREATER_THAN, Arrays.asList(
      methodCallInSubject(Matchers.COLLECTION_SIZE, msgWithActualExpectedInPredicate("hasSizeGreaterThan")),
      arrayLengthSimplifier(msgWithActualExpectedInPredicate("hasSizeGreaterThan"))))
    .put(CONTAINS, Arrays.asList(
      methodCallInSubject(Matchers.MAP_KEY_SET, msgWithActualExpectedInPredicate(CONTAINS_KEY)),
      methodCallInSubject(Matchers.MAP_VALUES, msgWithActualExpectedInPredicate(CONTAINS_VALUE))))
    .put("containsOnly", Collections.singletonList(
      methodCallInSubject(Matchers.MAP_KEY_SET, msgWithActualExpectedInPredicate("containsOnlyKeys"))))
    .build();

  private static class Matchers {

    public static final MethodMatchers COMPARE_TO = MethodMatchers.create().ofSubTypes("java.lang.Comparable")
      .names("compareTo").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers COMPARE_TO_IGNORE_CASE = MethodMatchers.create().ofSubTypes(JAVA_LANG_STRING)
      .names("compareToIgnoreCase").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers CONTAINS = MethodMatchers.create().ofTypes(JAVA_LANG_STRING, JAVA_UTIL_COLLECTION)
      .names(AssertJChainSimplificationIndex.CONTAINS).addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers CONTENT_EQUALS = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("contentEquals").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers EMPTY = MethodMatchers.create().ofTypes(JAVA_UTIL_OPTIONAL)
      .names("empty").addWithoutParametersMatcher().build();
    public static final MethodMatchers ENDS_WITH = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names(AssertJChainSimplificationIndex.ENDS_WITH).addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers EQUALS_IGNORE_CASE = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("equalsIgnoreCase").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers EQUALS_METHOD = MethodMatchers.create().ofAnyType().names("equals")
      .addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers HASH_CODE = MethodMatchers.create().ofAnyType().names("hashCode")
      .addWithoutParametersMatcher().build();
    public static final MethodMatchers INDEX_OF_STRING = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("indexOf").addParametersMatcher(JAVA_LANG_STRING).build();
    public static final MethodMatchers IS_BLANK = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names(AssertJChainSimplificationIndex.IS_BLANK).addWithoutParametersMatcher().build();
    public static final MethodMatchers IS_EMPTY_GENERIC = MethodMatchers.create().ofTypes(JAVA_LANG_STRING, JAVA_UTIL_COLLECTION, JAVA_UTIL_MAP)
      .names(IS_EMPTY).addWithoutParametersMatcher().build();
    public static final MethodMatchers IS_EMPTY_OPTIONAL = MethodMatchers.create().ofTypes(JAVA_UTIL_OPTIONAL)
      .names(IS_EMPTY).addWithoutParametersMatcher().build();
    public static final MethodMatchers IS_PRESENT = MethodMatchers.create().ofTypes(JAVA_UTIL_OPTIONAL)
      .names(AssertJChainSimplificationIndex.IS_PRESENT).addWithoutParametersMatcher().build();
    public static final MethodMatchers STRING_LENGTH = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names(LENGTH).addWithoutParametersMatcher().build();
    public static final MethodMatchers FILE_LENGTH = MethodMatchers.create().ofTypes(JAVA_IO_FILE)
      .names(LENGTH).addWithoutParametersMatcher().build();
    public static final MethodMatchers MATCHES = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("matches").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers STARTS_WITH = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names(AssertJChainSimplificationIndex.STARTS_WITH).addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers TO_STRING = MethodMatchers.create().ofAnyType().names("toString")
      .addWithoutParametersMatcher().build();
    public static final MethodMatchers TRIM = MethodMatchers.create().ofTypes(JAVA_LANG_STRING)
      .names("trim").addWithoutParametersMatcher().build();
    public static final MethodMatchers FILE_CAN_READ = MethodMatchers.create().ofTypes(JAVA_IO_FILE)
      .names("canRead").addWithoutParametersMatcher().build();
    public static final MethodMatchers FILE_CAN_WRITE = MethodMatchers.create().ofTypes(JAVA_IO_FILE)
      .names("canWrite").addWithoutParametersMatcher().build();
    public static final MethodMatchers FILE_EXISTS = MethodMatchers.create().ofTypes(JAVA_IO_FILE)
      .names("exists").addWithoutParametersMatcher().build();
    public static final MethodMatchers FILE_GET_NAME = MethodMatchers.create().ofTypes(JAVA_IO_FILE)
      .names("getName").addWithoutParametersMatcher().build();
    public static final MethodMatchers FILE_GET_PARENT_AND_PARENT_FILE = MethodMatchers.create().ofTypes(JAVA_IO_FILE)
      .names("getParent", "getParentFile").addWithoutParametersMatcher().build();
    public static final MethodMatchers FILE_AND_PATH_IS_ABSOLUTE = MethodMatchers.create().ofTypes(JAVA_IO_FILE, JAVA_NIO_FILE_PATH)
      .names("isAbsolute").addWithoutParametersMatcher().build();
    public static final MethodMatchers FILE_IS_DIRECTORY = MethodMatchers.create().ofTypes(JAVA_IO_FILE)
      .names("isDirectory").addWithoutParametersMatcher().build();
    public static final MethodMatchers FILE_IS_FILE = MethodMatchers.create().ofTypes(JAVA_IO_FILE)
      .names("isFile").addWithoutParametersMatcher().build();
    public static final MethodMatchers FILE_LIST_AND_LIST_FILE = MethodMatchers.create().ofTypes(JAVA_IO_FILE)
      .names("list", "listFiles").addWithoutParametersMatcher().build();
    public static final MethodMatchers GET = MethodMatchers.create().ofTypes(JAVA_UTIL_OPTIONAL)
      .names("get").addWithoutParametersMatcher().build();
    public static final MethodMatchers OR_ELSE = MethodMatchers.create().ofTypes(JAVA_UTIL_OPTIONAL)
      .names("orElse").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers PATH_GET_PARENT_AND_PARENT_FILE = MethodMatchers.create().ofTypes(JAVA_NIO_FILE_PATH)
      .names("getParent").addWithoutParametersMatcher().build();
    public static final MethodMatchers PATH_STARTS_WITH = MethodMatchers.create().ofTypes(JAVA_NIO_FILE_PATH)
      .names(AssertJChainSimplificationIndex.STARTS_WITH).addParametersMatcher(JAVA_LANG_STRING).build();
    public static final MethodMatchers PATH_ENDS_WITH = MethodMatchers.create().ofTypes(JAVA_NIO_FILE_PATH)
      .names(AssertJChainSimplificationIndex.ENDS_WITH).addParametersMatcher(JAVA_LANG_STRING).build();
    public static final MethodMatchers COLLECTION_SIZE = MethodMatchers.create().ofTypes(JAVA_UTIL_COLLECTION, JAVA_UTIL_MAP)
      .names("size").addWithoutParametersMatcher().build();
    public static final MethodMatchers COLLECTION_CONTAINS_ALL = MethodMatchers.create().ofTypes(JAVA_UTIL_COLLECTION)
      .names("containsAll").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers MAP_CONTAINS_KEY = MethodMatchers.create().ofTypes(JAVA_UTIL_MAP)
      .names(CONTAINS_KEY).addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers MAP_CONTAINS_VALUE = MethodMatchers.create().ofTypes(JAVA_UTIL_MAP)
      .names(CONTAINS_VALUE).addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers MAP_GET = MethodMatchers.create().ofTypes(JAVA_UTIL_MAP)
      .names("get").addParametersMatcher(MethodMatchers.ANY).build();
    public static final MethodMatchers MAP_KEY_SET = MethodMatchers.create().ofTypes(JAVA_UTIL_MAP)
      .names("keySet").addWithoutParametersMatcher().build();
    public static final MethodMatchers MAP_VALUES = MethodMatchers.create().ofTypes(JAVA_UTIL_MAP)
      .names("values").addWithoutParametersMatcher().build();
  }

  private static PredicateSimplifierWithContext compareToSimplifier(Predicate<ExpressionTree> predicateArgCondition, String simplification) {
    // Compare to can not be quick fixed easily because assertJ related method requires a Comparable and not any Object.
    return PredicateSimplifierWithContext.methodCallInSubject(predicateArgCondition, Matchers.COMPARE_TO, msgWithActualExpectedInSubject(simplification));
  }

  private static PredicateSimplifierWithContext compareToSimplifier(String simplification) {
    // Compare to can not be quick fixed easily because assertJ related method requires a Comparable and not any Object.
    return PredicateSimplifierWithContext.methodCallInSubject(Matchers.COMPARE_TO, msgWithActualExpectedInSubject(simplification));
  }

  private static PredicateSimplifierWithContext indexOfSimplifier(Predicate<ExpressionTree> predicateArgCondition, String simplification) {
    return PredicateSimplifierWithContext.methodCallInSubject(predicateArgCondition, Matchers.INDEX_OF_STRING, msgWithActualExpectedInSubject(simplification));
  }

  private static PredicateSimplifierWithContext indexOfSimplifier(String simplification) {
    return PredicateSimplifierWithContext.methodCallInSubject(Matchers.INDEX_OF_STRING, msgWithActualExpectedInSubject(simplification));
  }

  private static PredicateSimplifierWithContext arrayLengthSimplifier(WithContextSimplification simplification) {
    return PredicateSimplifierWithContext.withSubjectArgumentCondition(AssertJChainSimplificationIndex::isArrayLength, simplification);
  }



  public static boolean isZeroIntOrLong(ExpressionTree tree) {
    if (tree.is(Tree.Kind.LONG_LITERAL)) {
      String value = ((LiteralTree) tree).value();
      return "0L".equals(value) || "0l".equals(value);
    }
    return LiteralUtils.isZero(tree);
  }

  public static boolean isOneIntOrLong(ExpressionTree tree) {
    if (tree.is(Tree.Kind.LONG_LITERAL)) {
      String value = ((LiteralTree) tree).value();
      return "1L".equals(value) || "1l".equals(value);
    }
    return LiteralUtils.isOne(tree);
  }

  public static boolean isNegOneIntOrLong(ExpressionTree tree) {
    return tree.is(Tree.Kind.UNARY_MINUS) && isOneIntOrLong(((UnaryExpressionTree) tree).expression());
  }

  private static boolean isArrayLength(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) expression;
      return memberSelectExpressionTree.expression().symbolType().isArray() && LENGTH.equals(memberSelectExpressionTree.identifier().name());
    }
    return false;
  }

  private static boolean isNotObject(ExpressionTree expression) {
    return !expression.symbolType().is("java.lang.Object");
  }

  private static class PredicateSimplifierWithoutContext implements SimplifierWithoutContext {
    private final Predicate<MethodInvocationTree> mitPredicate;
    private final ContextFreeSimplification simplification;

    public PredicateSimplifierWithoutContext(
      Predicate<MethodInvocationTree> mitPredicate,
      String simplification) {

      this.mitPredicate = mitPredicate;
      this.simplification = new ContextFreeSimplification(simplification);
    }

    public static PredicateSimplifierWithoutContext withSingleArg(Predicate<ExpressionTree> argumentPredicate, String simplified) {
      return new PredicateSimplifierWithoutContext(mit -> {
        Arguments arguments = mit.arguments();
        return arguments.size() == 1 && argumentPredicate.test(arguments.get(0));
      }, simplified);
    }

    @Override
    public Optional<Simplification> simplify(MethodInvocationTree predicate) {
      if (mitPredicate.test(predicate)) {
        simplification.buildQuickFix(predicate);
        return Optional.of(simplification);
      } else {
        return Optional.empty();
      }
    }
  }

  static class PredicateSimplifierWithContext implements SimplifierWithContext {
    private final Predicate<MethodInvocationTree> predicateCondition;
    private final Predicate<MethodInvocationTree> subjectCondition;
    private final WithContextSimplification simplification;

    public PredicateSimplifierWithContext(
      Predicate<MethodInvocationTree> predicateCondition,
      Predicate<MethodInvocationTree> subjectCondition,
      WithContextSimplification simplification) {
      this.predicateCondition = predicateCondition;
      this.subjectCondition = subjectCondition;
      this.simplification = simplification;
    }

    public static PredicateSimplifierWithContext withSubjectArgumentCondition(
      Predicate<ExpressionTree> predicateArgumentCondition, Predicate<ExpressionTree> subjectArgumentCondition,
      String simplification) {

      return withSubjectArgumentCondition(predicateArgumentCondition, subjectArgumentCondition,
        // Same quick fix as context free
        new WithContextSimplification(simplification, new ContextFreeQuickFix(simplification)));
    }

    public static PredicateSimplifierWithContext withSubjectArgumentCondition(
      Predicate<ExpressionTree> predicateArgumentCondition, Predicate<ExpressionTree> subjectArgumentCondition,
      WithContextSimplification simplification) {
      return new PredicateSimplifierWithContext(
        predicateMit -> predicateMit.arguments().size() == 1 && predicateArgumentCondition.test(skipParentheses(predicateMit.arguments().get(0))),
        subjectMit -> subjectMit.arguments().size() == 1 && subjectArgumentCondition.test(skipParentheses(subjectMit.arguments().get(0))),
        simplification);
    }

    public static PredicateSimplifierWithContext withSubjectArgumentCondition(
      Predicate<ExpressionTree> subjectArgumentCondition, WithContextSimplification simplification) {
      return new PredicateSimplifierWithContext(x -> true, subjectMit -> subjectMit.arguments().size() == 1 &&
        subjectArgumentCondition.test(skipParentheses(subjectMit.arguments().get(0))),
        simplification);
    }

    public static PredicateSimplifierWithContext methodCallInSubject(
      MethodMatchers methodCallMatcher,
      WithContextSimplification simplification) {
      return withSubjectArgumentCondition(arg -> hasMethodCallAsArg(arg, methodCallMatcher), simplification);
    }

    public static PredicateSimplifierWithContext methodCallInSubject(
      Predicate<ExpressionTree> predicateArgumentCondition,
      MethodMatchers methodCallMatcher,
      WithContextSimplification simplification) {
      return withSubjectArgumentCondition(predicateArgumentCondition, arg -> hasMethodCallAsArg(arg, methodCallMatcher),
        simplification);
    }

    @Override
    public Optional<Simplification> simplify(MethodInvocationTree subject, MethodInvocationTree predicate) {
      if (predicateCondition.test(predicate) && subjectCondition.test(subject)) {
        simplification.buildQuickFix(subject, predicate);
        return Optional.of(simplification);
      } else {
        return Optional.empty();
      }
    }
  }

  abstract static class Simplification {
    String replacement;
    Supplier<List<JavaQuickFix>> quickFix = null;

    Simplification(String replacement) {
      this.replacement = replacement;
    }

    Optional<Supplier<List<JavaQuickFix>>> getQuickFix() {
      return Optional.ofNullable(quickFix);
    }

    String getReplacement() {
      return replacement;
    }
  }

  static class ContextFreeSimplification extends Simplification {
    ContextFreeSimplification(String replacement) {
      super(replacement);
    }

    void buildQuickFix(MethodInvocationTree predicate) {
      this.quickFix = new ContextFreeQuickFix(replacement).apply(predicate);
    }
  }

  static class WithContextSimplification extends Simplification {
    private static final String MESSAGE_ACTUAL_EXPECTED = "assertThat(actual).%s(expected)";
    AssertJChainSimplificationQuickFix buildQuickFix;

    WithContextSimplification(String replacement) {
      super(replacement);
      this.buildQuickFix = new NoQuickFix();
    }

    WithContextSimplification(String replacement, AssertJChainSimplificationQuickFix buildQuickFix) {
      super(replacement);
      this.buildQuickFix = buildQuickFix;
    }

    public void buildQuickFix(MethodInvocationTree subject, MethodInvocationTree predicate) {
      this.quickFix = buildQuickFix.apply(subject, predicate);
    }

    /**
     * Message of the form: assertThat(actual).predicateName()
     * Quick fix of the form: assertThat(x.y(a)).z(b); or assertThat(x.y).z(b); --> assertThat(x).predicateName();
     */
    static WithContextSimplification msgWithActual(String predicateName) {
      String replacement = String.format("assertThat(actual).%s()", predicateName);
      return new WithContextSimplification(replacement, new ActualExpectedInPredicateQuickFix(replacement, predicateName, false));
    }

    /**
     * Message of the form: assertThat(actual).predicateName(expected)
     * No quick fix for now, should be done case by case
     */
    static WithContextSimplification msgWithActualExpected(String predicateName) {
      return new WithContextSimplification(String.format(MESSAGE_ACTUAL_EXPECTED, predicateName));
    }

    /**
     * Message of the form: assertThat(actual).predicateName(expected)
     * Quick fix of the form: assertThat(x.y(a)).y(); --> by assertThat(x).predicateName(a);
     */
    static WithContextSimplification msgWithActualExpectedInSubject(String predicateName) {
      String replacement = String.format(MESSAGE_ACTUAL_EXPECTED, predicateName);
      return new WithContextSimplification(replacement, new ActualExpectedInSubjectQuickFix(replacement, predicateName));
    }

    /**
     * Message of the form: assertThat(actual).predicateName(expected)
     * Quick fix of the form: assertThat(x.y(a)).z(b); or assertThat(x.y).z(b); --> assertThat(x).predicateName(b);
     */
    static WithContextSimplification msgWithActualExpectedInPredicate(String predicateName) {
      String replacement = String.format(MESSAGE_ACTUAL_EXPECTED, predicateName);
      return new WithContextSimplification(replacement, new ActualExpectedInPredicateQuickFix(replacement, predicateName, true));
    }

    /**
     * Message of the form: assertThat(actual).predicateName(predicateArg)
     * No quick fix.
     */
    static WithContextSimplification msgWithActualCustom(String predicateName, String predicateArg) {
      // Providing quick fixes for such issues should be done in case by case, it is an important effort for little value, it seems reasonable to not suggest them.
      return new WithContextSimplification(String.format("assertThat(actual).%s(%s)", predicateName, predicateArg));
    }
  }
}
