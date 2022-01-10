/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.ExpressionUtils.methodName;

@Rule(key = "S5863")
public class AssertionCompareToSelfCheck extends IssuableSubscriptionVisitor {

  private static final String ASSERT_ARRAY_EQUALS = "assertArrayEquals";
  private static final String ASSERT_EQUALS = "assertEquals";
  private static final String IS_EQUAL_TO = "isEqualTo";

  private static final MethodMatchers ASSERTJ_AND_FEST_ASSERT_SUBJECT_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("org.assertj.core.api.Assertions",
        "org.assertj.core.api.AssertionsForInterfaceTypes",
        "org.assertj.core.api.AssertionsForClassTypes")
      .names("assertThat", "assertThatObject")
      .addParametersMatcher(MethodMatchers.ANY)
      .build(),
    MethodMatchers.create()
      .ofTypes("org.fest.assertions.Assertions")
      .names("assertThat")
      .addParametersMatcher(MethodMatchers.ANY)
      .build());

  private static final MethodMatchers JUNIT5_ASSERTIONS = MethodMatchers.create()
    .ofTypes("org.junit.jupiter.api.Assertions")
    .names(ASSERT_ARRAY_EQUALS, ASSERT_EQUALS, "assertIterableEquals", "assertLinesMatch")
    .addParametersMatcher(parameters -> parameters.size() >= 2)
    .build();

  private static final MethodMatchers JUNIT4_ASSERTIONS_WITHOUT_MESSAGE = MethodMatchers.create()
    .ofTypes("org.junit.Assert")
    .names(ASSERT_ARRAY_EQUALS, ASSERT_EQUALS)
    .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
    .build();

  private static final MethodMatchers JUNIT4_ASSERTIONS_WITH_MESSAGE = MethodMatchers.create()
    .ofTypes("org.junit.Assert")
    .names(ASSERT_ARRAY_EQUALS, ASSERT_EQUALS)
    .addParametersMatcher("java.lang.String", MethodMatchers.ANY, MethodMatchers.ANY)
    .build();

  private static final MethodMatchers ASSERTJ_AND_FEST_ASSERT_MESSAGE_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("org.assertj.core.api.AbstractAssert")
      .names("as", "describedAs", "withFailMessage", "overridingErrorMessage").withAnyParameters().build(),
    MethodMatchers.create()
      .ofSubTypes("org.fest.assertions.GenericAssert")
      .names("as", "describedAs", "overridingErrorMessage").withAnyParameters().build());

  private static final MethodMatchers ASSERTJ_AND_FEST_ASSERT_PREDICATES = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("org.assertj.core.api.AbstractAssert")
      .names("contains", "containsAll", "containsAllEntriesOf", "containsAnyElementOf", "containsAnyOf",
        "containsExactly", "containsExactlyElementsOf", "containsExactlyEntriesOf", "containsExactlyInAnyOrder",
        "containsExactlyInAnyOrderEntriesOf", "containsIgnoringCase", "containsOnly", "containsOnlyElementsOf",
        "containsSequence", "containsSubsequence", "doesNotContain", "endsWith",
        "hasSameClassAs", "hasSameElementsAs", "hasSameHashCodeAs", "hasSameSizeAs", IS_EQUAL_TO,
        "isEqualToIgnoringCase", "isSameAs", "startsWith")
      .addParametersMatcher(MethodMatchers.ANY)
      .build(),
    MethodMatchers.create()
      .ofSubTypes("org.fest.assertions.GenericAssert")
      .names("contains", "containsExactly", "containsIgnoringCase", "containsOnly", "doesNotContain", "endsWith",
        IS_EQUAL_TO, "isEqualToIgnoringCase", "isSameAs", "startsWith")
      .addParametersMatcher(MethodMatchers.ANY)
      .build());

  private static final Set<String> EQUALS_HASH_CODE_METHODS = SetUtils.immutableSetOf(ASSERT_EQUALS, IS_EQUAL_TO, "hasSameHashCodeAs");

  private static final String MESSAGE = "Replace this assertion to not have the same actual and expected expression.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (JUNIT4_ASSERTIONS_WITH_MESSAGE.matches(mit)) {
      checkActualAndExpectedExpression(methodName(mit).name(), mit.arguments().get(2), mit.arguments().get(1));
    } else if (JUNIT4_ASSERTIONS_WITHOUT_MESSAGE.matches(mit) || JUNIT5_ASSERTIONS.matches(mit)) {
      checkActualAndExpectedExpression(methodName(mit).name(), mit.arguments().get(1), mit.arguments().get(0));
    } else if (ASSERTJ_AND_FEST_ASSERT_SUBJECT_METHODS.matches(mit)) {
      ExpressionTree actualExpression = mit.arguments().get(0);
      consecutiveMethodIgnoringMessageDescription(mit)
        .filter(predicate -> predicate.arguments().size() == 1 && ASSERTJ_AND_FEST_ASSERT_PREDICATES.matches(predicate))
        .ifPresent(predicate -> checkActualAndExpectedExpression(methodName(predicate).name(), actualExpression, predicate.arguments().get(0)));
    }
  }

  private void checkActualAndExpectedExpression(String predicateMethodName, ExpressionTree actualExpression, ExpressionTree expectedExpression) {
    if (ExpressionsHelper.alwaysReturnSameValue(actualExpression) &&
      SyntacticEquivalence.areEquivalent(actualExpression, expectedExpression) &&
      !isLegitimateSelfComparison(predicateMethodName, actualExpression)) {
      List<Location> secondaryLocations = Collections.singletonList(new Location("actual", actualExpression));
      reportIssue(expectedExpression, MESSAGE, secondaryLocations, null);
    }
  }

  private static boolean isLegitimateSelfComparison(String comparisonMethodName, ExpressionTree actualExpression) {
    // In a unit test validating "equals" and "hashCode" methods, it's legitimate to compare an object to itself.
    // In other kinds of tests, it's a bug. But it's complicated to know if a unit test is about validating the
    // "equals" method or not. The following trade-off allows the self-comparison of an object if the unit test
    // name contains a keyword like "equals", "hash_code", ...
    Type actualExpressionType = actualExpression.symbolType();
    return EQUALS_HASH_CODE_METHODS.contains(comparisonMethodName) &&
      !isPrimitiveOrNull(actualExpressionType) &&
      UnitTestUtils.isInUnitTestRelatedToObjectMethods(actualExpression);
  }

  private static boolean isPrimitiveOrNull(Type actualExpressionType) {
    return actualExpressionType.isPrimitive() || "null".equals(actualExpressionType.symbol().name());
  }

  private static Optional<MethodInvocationTree> consecutiveMethodIgnoringMessageDescription(MethodInvocationTree mit) {
    Optional<MethodInvocationTree> consecutiveMethod = MethodTreeUtils.consecutiveMethodInvocation(mit);
    if (consecutiveMethod.isPresent() && ASSERTJ_AND_FEST_ASSERT_MESSAGE_METHODS.matches(consecutiveMethod.get())) {
      return consecutiveMethodIgnoringMessageDescription(consecutiveMethod.get());
    }
    return consecutiveMethod;
  }

}
