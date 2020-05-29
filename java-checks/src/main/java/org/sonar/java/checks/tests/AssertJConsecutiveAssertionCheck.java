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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.UnitTestUtils.hasTestAnnotation;

@Rule(key = "S5853")
public class AssertJConsecutiveAssertionCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers ASSERT_THAT_MATCHER = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.Assertions").names("assertThat").addParametersMatcher(MethodMatchers.ANY).build();

  public static final MethodMatchers ASSERTJ_SET_CONTEXT_METHODS = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.AbstractAssert")
    .name(name ->
      name.startsWith("extracting") || name.startsWith("using") || name.startsWith("filtered")
    ).withAnyParameters()
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
    ExpressionTree currentArgument = null;
    MethodInvocationTree currentMit = null;
    List<MethodInvocationTree> equivalentInvocations = new ArrayList<>();

    for (StatementTree statement : statements) {
      Optional<MethodInvocationTree> assertThatInvocation = getSimpleAssertSubjectInvocation(statement);

      if (assertThatInvocation.isPresent()) {
        MethodInvocationTree mit = assertThatInvocation.get();
        ExpressionTree arg = mit.arguments().get(0);
        if (currentArgument == null) {
          currentMit = mit;
          currentArgument = arg;
        } else if (areEquivalent(currentArgument, arg)) {
          equivalentInvocations.add(mit);
        }
      } else {
        // We have something else than an assertion subject between two calls
        reportIssueIfMultipleCalls(currentMit, equivalentInvocations);

        currentArgument = null;
        currentMit = null;
        equivalentInvocations.clear();
      }
    }

    reportIssueIfMultipleCalls(currentMit, equivalentInvocations);
  }

  private static Optional<MethodInvocationTree> getSimpleAssertSubjectInvocation(StatementTree statement) {
    if (statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      ExpressionTree expression = ((ExpressionStatementTree) statement).expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        // First method invocation should be an assertion predicate, if not (incomplete assertion), we will not find anything
        return getSimpleAssertSubjectInvocation(((MethodInvocationTree) expression).methodSelect());
      }
    }
    return Optional.empty();
  }

  private static Optional<MethodInvocationTree> getSimpleAssertSubjectInvocation(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree memberSelectExpression = ((MemberSelectExpressionTree) expressionTree).expression();
      if (memberSelectExpression.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) memberSelectExpression;
        if (ASSERT_THAT_MATCHER.matches(mit)) {
          return Optional.of(mit);
        } else if (ASSERTJ_SET_CONTEXT_METHODS.matches(mit)) {
          return Optional.empty();
        } else {
          return getSimpleAssertSubjectInvocation(mit.methodSelect());
        }
      }
    }
    return Optional.empty();
  }

  private static boolean areEquivalent(ExpressionTree e1, ExpressionTree e2) {
    if (e1.is(Tree.Kind.METHOD_INVOCATION)) {
      // Two method invocation can return different values.
      return false;
    }

    return SyntacticEquivalence.areEquivalent(e1, e2);
  }

  private void reportIssueIfMultipleCalls(@Nullable MethodInvocationTree mainLocation, List<MethodInvocationTree> equivalentAssertions) {
    if (mainLocation != null && !equivalentAssertions.isEmpty()) {
      reportIssue(ExpressionUtils.methodName(mainLocation),
        "Join these multiple assertions subject to one assertion chain.",
        equivalentAssertions.stream().map(mit -> new JavaFileScannerContext.Location("Other assertThat", ExpressionUtils.methodName(mit))).collect(Collectors.toList()),
        null);
    }
  }

}
