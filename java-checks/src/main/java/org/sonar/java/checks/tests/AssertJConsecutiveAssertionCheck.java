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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.UnitTestUtils.hasTestAnnotation;

@Rule(key = "S5853")
public class AssertJConsecutiveAssertionCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers ASSERT_THAT_MATCHER = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.Assertions", "org.assertj.core.api.Assert")
    .names("assertThat")
    .addParametersMatcher(MethodMatchers.ANY)
    .build();

  public static final MethodMatchers ASSERTJ_SET_CONTEXT_METHODS = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.AbstractAssert")
    .name(name -> name.startsWith("extracting") || name.startsWith("using") || name.startsWith("filtered")
      || "flatExtracting".equals(name) || "map".equals(name) || "flatMap".equals(name))
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (hasTestAnnotation(methodTree)) {
      BlockTree block = methodTree.block();
      if (block != null) {
        reportConsecutiveAssertions(block.body());
      }
    }
  }

  private void reportConsecutiveAssertions(List<StatementTree> statements) {
    AssertSubject currentSubject = null;
    List<AssertSubject> equivalentInvocations = new ArrayList<>();

    for (StatementTree statement : statements) {
      Optional<AssertSubject> assertThatInvocation = getSimpleAssertSubject(statement);

      if (assertThatInvocation.isPresent()) {
        AssertSubject assertSubject = assertThatInvocation.get();
        if (currentSubject == null) {
          currentSubject = assertSubject;
        } else if (currentSubject.hasEquivalentArgument(assertSubject)) {
          equivalentInvocations.add(assertSubject);
        } else {
          reportIssueIfMultipleCalls(currentSubject, equivalentInvocations);
          currentSubject = assertSubject;
          equivalentInvocations.clear();
        }
      } else {
        // We have something else than an assertion subject or a subject returning different values between two calls
        reportIssueIfMultipleCalls(currentSubject, equivalentInvocations);
        currentSubject = null;
        equivalentInvocations.clear();
      }
    }

    reportIssueIfMultipleCalls(currentSubject, equivalentInvocations);
  }

  /**
   * A "simple" assertion subject is coming from an assertion chain containing only one assertion predicate
   * and the assertion subject argument always returning the same value when called multiple times.
   */
  private static Optional<AssertSubject> getSimpleAssertSubject(StatementTree statement) {
    if (statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      ExpressionTree expression = ((ExpressionStatementTree) statement).expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        // First method invocation should be an assertion predicate, if not (incomplete assertion), we will not find anything
        return getSimpleAssertSubject(((MethodInvocationTree) expression).methodSelect());
      }
    }
    return Optional.empty();
  }

  private static Optional<AssertSubject> getSimpleAssertSubject(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree memberSelectExpression = ((MemberSelectExpressionTree) expressionTree).expression();
      if (memberSelectExpression.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) memberSelectExpression;
        if (ASSERT_THAT_MATCHER.matches(mit)) {
          ExpressionTree arg = mit.arguments().get(0);
          if (ExpressionsHelper.alwaysReturnSameValue(arg)) {
            return Optional.of(new AssertSubject(mit, arg));
          }
        } else if (ASSERTJ_SET_CONTEXT_METHODS.matches(mit)) {
          return Optional.empty();
        } else {
          return getSimpleAssertSubject(mit.methodSelect());
        }
      }
    }
    return Optional.empty();
  }

  private void reportIssueIfMultipleCalls(@Nullable AssertSubject assertSubject, List<AssertSubject> equivalentAssertions) {
    if (assertSubject != null && !equivalentAssertions.isEmpty()) {
      reportIssue(assertSubject.methodName(),
        "Join these multiple assertions subject to one assertion chain.",
        equivalentAssertions.stream().map(AssertSubject::toSecondaryLocation).collect(Collectors.toList()),
        null);
    }
  }

  private static class AssertSubject {
    final MethodInvocationTree mit;
    final Type assertionType;
    final ExpressionTree arg;

    AssertSubject(MethodInvocationTree mit, ExpressionTree arg) {
      this.mit = mit;
      this.assertionType = mit.symbolType().erasure();
      this.arg = arg;
    }

    boolean hasEquivalentArgument(AssertSubject other) {
      return SyntacticEquivalence.areEquivalent(arg, other.arg)
        && (other.assertionType.isSubtypeOf(assertionType) || couldBeChained(other));
    }

    boolean couldBeChained(AssertSubject other) {
      return MethodTreeUtils.consecutiveMethodInvocation(other.mit)
        .map(chainedNextMethod -> chainedNextMethod.methodSymbol().owner().type().erasure())
        .map(mit.methodSymbol().owner().type().erasure()::isSubtypeOf)
        .orElse(false);
    }

    IdentifierTree methodName() {
      return ExpressionUtils.methodName(mit);
    }

    JavaFileScannerContext.Location toSecondaryLocation() {
      return new JavaFileScannerContext.Location("Other assertThat", methodName());
    }
  }

}
