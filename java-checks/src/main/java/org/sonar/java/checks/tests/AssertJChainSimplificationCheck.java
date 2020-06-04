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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5838")
public class AssertJChainSimplificationCheck extends AbstractMethodDetection {
  private static final String ABSTRACT_ASSERT = "org.assertj.core.api.AbstractAssert";
  private static final List<String> ASSERTION_MESSAGE_NAMES = Arrays.asList("as", "describedAs", "withFailMessage", "overridingErrorMessage");

  private static final MethodMatchers ASSERTION_PREDICATES = MethodMatchers.create().ofSubTypes(ABSTRACT_ASSERT)
    .name(name -> !ASSERTION_MESSAGE_NAMES.contains(name)).withAnyParameters().build();

  // TODO: Support more assertion subject methods / from different classes
  private static final MethodMatchers ASSERTIONS_SUBJECT_METHODS = MethodMatchers.create().ofTypes(
    "org.assertj.core.api.Assertions",
    "org.assertj.core.api.AssertionsForInterfaceTypes",
    "org.assertj.core.api.AssertionsForClassTypes")
    .names("assertThat").withAnyParameters().build();

  /**
   * @see AssertJChainSimplificationIndex#CONTEXT_FREE_SIMPLIFIERS
   */
  private static final Map<String, List<SimplifierWithoutContext>> CONTEXT_FREE_SIMPLIFIERS = AssertJChainSimplificationIndex.CONTEXT_FREE_SIMPLIFIERS;

  /**
   * @see AssertJChainSimplificationIndex#SIMPLIFIERS_WITH_CONTEXT
   */
  private static final Map<String, List<SimplifierWithContext>> SIMPLIFIERS_WITH_CONTEXT = AssertJChainSimplificationIndex.SIMPLIFIERS_WITH_CONTEXT;

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return ASSERTIONS_SUBJECT_METHODS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree subjectMit) {
    List<MethodInvocationTree> predicates = new ArrayList<>();
    Optional<MethodInvocationTree> nextPredicate = MethodTreeUtils.subsequentMethodInvocation(subjectMit, ASSERTION_PREDICATES);

    while (nextPredicate.isPresent()) {
      predicates.add(nextPredicate.get());
      nextPredicate = MethodTreeUtils.subsequentMethodInvocation(nextPredicate.get(), ASSERTION_PREDICATES);
    }

    boolean wasIssueRaised = checkPredicatesForSimplification(
      predicates, CONTEXT_FREE_SIMPLIFIERS, SimplifierWithoutContext::simplify,
      (predicate, replacement) -> reportIssue(ExpressionUtils.methodName(predicate),
        String.format("Use %s instead", replacement)));

    // We do not continue when we have already raised an issue to avoid potentially conflicting issue reports. If we
    // have more than one predicate we also avoid continuing to avoid FP on cases such as:
    // assertThat(Integer.valueOf(1).compareTo(2)).isGreaterThan(1).isLessThan(10)
    if (wasIssueRaised || predicates.size() > 1) {
      return;
    }

    checkPredicatesForSimplification(
      predicates, SIMPLIFIERS_WITH_CONTEXT, (simplifier, predicate) -> simplifier.simplify(subjectMit, predicate),
      (predicate, replacement) -> reportIssue(ExpressionUtils.methodName(predicate),
        String.format("Use %s instead", replacement),
        Collections.singletonList(
          new JavaFileScannerContext.Location("", subjectMit)),
        null));
  }

  /**
   * @return {@code true} when an issue was reported, {@code false} otherwise.
   */
  private static <T> boolean checkPredicatesForSimplification(
    List<MethodInvocationTree> predicates,
    Map<String, List<T>> simplifiers,
    BiFunction<T, MethodInvocationTree, Optional<String>> simplificationMethod,
    BiConsumer<MethodInvocationTree, String> reportingMethod) {

    AssertJChainSimplificationHelper.BooleanFlag issueRaised = new AssertJChainSimplificationHelper.BooleanFlag();
    predicates.forEach(predicate -> {
      String predicateName = ExpressionUtils.methodName(predicate).name();
      if (simplifiers.containsKey(predicateName)) {
        simplifiers.get(predicateName).stream()
          .map(simplifier -> simplificationMethod.apply(simplifier, predicate))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .findFirst()
          .ifPresent(replacement -> {
            reportingMethod.accept(predicate, replacement);
            issueRaised.setTrue();
          });
      }
    });
    return issueRaised.value();
  }

  interface SimplifierWithoutContext {
    Optional<String> simplify(MethodInvocationTree predicate);
  }

  interface SimplifierWithContext {
    Optional<String> simplify(MethodInvocationTree subject, MethodInvocationTree predicate);
  }
}
