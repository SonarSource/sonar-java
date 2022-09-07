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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.ExtendedJavaIssueBuilder;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.CONTEXT_FREE_SIMPLIFIERS;
import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.SIMPLIFIERS_WITH_CONTEXT;

@Rule(key = "S5838")
public class AssertJChainSimplificationCheck extends AbstractMethodDetection {
  private static final String ISSUE_MESSAGE_FORMAT_STRING = "Use %s instead.";

  private static final MethodMatchers ASSERTION_MESSAGE_METHODS = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.AbstractAssert")
    .names("as", "describedAs", "withFailMessage", "overridingErrorMessage").withAnyParameters().build();

  private static final MethodMatchers ASSERTIONS_SUBJECT_METHODS = MethodMatchers.create().ofTypes(
    "org.assertj.core.api.Assertions",
    "org.assertj.core.api.AssertionsForInterfaceTypes",
    "org.assertj.core.api.AssertionsForClassTypes")
    .names("assertThat", "assertThatObject").addParametersMatcher(MethodMatchers.ANY).build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return ASSERTIONS_SUBJECT_METHODS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree subjectMit) {
    List<MethodInvocationTree> predicates = new ArrayList<>();
    Optional<MethodInvocationTree> nextPredicateOpt = MethodTreeUtils.consecutiveMethodInvocation(subjectMit);

    while (nextPredicateOpt.isPresent()) {
      MethodInvocationTree nextPredicate = nextPredicateOpt.get();
      if (!ASSERTION_MESSAGE_METHODS.matches(nextPredicate)) {
        predicates.add(nextPredicate);
      }
      nextPredicateOpt = MethodTreeUtils.consecutiveMethodInvocation(nextPredicate);
    }

    boolean wasIssueRaised = checkPredicatesForSimplification(
      predicates, CONTEXT_FREE_SIMPLIFIERS, SimplifierWithoutContext::simplify,
      (predicate, simplification) -> createIssueBuilder(predicate, simplification).report()
    );

    // We do not continue when we have already raised an issue to avoid potentially conflicting issue reports. If we
    // have more than one predicate we also avoid continuing to avoid FP on cases such as:
    // assertThat(Integer.valueOf(1).compareTo(2)).isGreaterThan(1).isLessThan(10)
    // We also want to ignore all assertion chains that contain methods besides predicates and messages, such as those
    // that change the assertion context, as that level of complexity is not handled by this rule. The extraction
    // function is such an example:
    // assertThat(frodo).extracting("name", as(InstanceOfAssertFactories.STRING)).startsWith("Fro");
    if (wasIssueRaised || predicates.size() > 1) {
      return;
    }

    checkPredicatesForSimplification(
      predicates, SIMPLIFIERS_WITH_CONTEXT, (simplifier, predicate) -> simplifier.simplify(subjectMit, predicate),
      (predicate, simplification) -> createIssueBuilder(predicate, simplification)
        .withSecondaries(new JavaFileScannerContext.Location("This can be simplified", subjectMit.arguments().get(0)))
        .report()
    );
  }

  private ExtendedJavaIssueBuilder createIssueBuilder(MethodInvocationTree predicate, AssertJChainSimplificationIndex.Simplification simplification) {
    ExtendedJavaIssueBuilder builder = QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(ExpressionUtils.methodName(predicate))
      .withMessage(ISSUE_MESSAGE_FORMAT_STRING, simplification.getReplacement());
    simplification.getQuickFix().ifPresent(builder::withQuickFixes);
    return builder;
  }

  /**
   * @return {@code true} when an issue was reported, {@code false} otherwise.
   */
  private static <T> boolean checkPredicatesForSimplification(
    List<MethodInvocationTree> predicates,
    Map<String, List<T>> simplifiers,
    BiFunction<T, MethodInvocationTree, Optional<AssertJChainSimplificationIndex.Simplification>> simplificationMethod,
    BiConsumer<MethodInvocationTree, AssertJChainSimplificationIndex.Simplification> reportingMethod) {

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
    Optional<AssertJChainSimplificationIndex.Simplification> simplify(MethodInvocationTree predicate);
  }

  interface SimplifierWithContext {
    Optional<AssertJChainSimplificationIndex.Simplification> simplify(MethodInvocationTree subject, MethodInvocationTree predicate);
  }
}
