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
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S5841")
public class AssertJTestForEmptinessCheck extends AbstractMethodDetection {

  private static final MethodMatchers ASSERTJ_NOT_TESTING_SIZE = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.AbstractAssert")
    .name(name ->
      "as".equals(name) || "describedAs".equals(name) || "withFailMessage".equals(name) || "overridingErrorMessage".equals(name)
        || "isNotNull".equals(name) || "asList".equals(name) || name.contains("InstanceOf")|| name.startsWith("using")
        || name.startsWith("extracting") || name.startsWith("filtered") || name.startsWith("doesNotContain") || name.startsWith("all")
    ).withAnyParameters()
    .build();

  private static final MethodMatchers ASSERT_THAT_MATCHER = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.Assertions", "org.assertj.core.api.AssertionsForInterfaceTypes", "org.assertj.core.api.AssertionsForClassTypes")
    .names("assertThat", "assertThatObject").addParametersMatcher(MethodMatchers.ANY).build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofSubTypes("org.assertj.core.api.AbstractIterableAssert")
        .names("allMatch", "allSatisfy", "doesNotContainSequence", "doesNotContainSubsequence", "doesNotContainAnyElementsOf")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes("org.assertj.core.api.AbstractIterableAssert")
        .names("doesNotContain")
        .addParametersMatcher(MethodMatchers.ANY)
        .build()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (consecutiveInvocationTestSize(mit)) {
      return;
    }

    getSubjectArgumentNotTestedForSize(mit)
      .filter(AssertJTestForEmptinessCheck::isNotUsedSomewhereElse)
      .ifPresent(arg -> reportIssueWithSecondary(mit, arg));
  }

  private static boolean consecutiveInvocationTestSize(MethodInvocationTree mit) {
    Optional<MethodInvocationTree> consecutiveMethodInvocation = MethodTreeUtils.consecutiveMethodInvocation(mit);
    if (consecutiveMethodInvocation.isPresent()) {
      MethodInvocationTree consecutiveInvocation = consecutiveMethodInvocation.get();
      if (ASSERTJ_NOT_TESTING_SIZE.matches(consecutiveInvocation)) {
        return consecutiveInvocationTestSize(consecutiveInvocation);
      } else {
        // To avoid FP, only every others methods not explicitly listed as not testing size, will be considered as testing size
        return true;
      }
    }
    return false;
  }

  private static Optional<ExpressionTree> getSubjectArgumentNotTestedForSize(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree memberSelectExpression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (memberSelectExpression.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree childMit = (MethodInvocationTree) memberSelectExpression;
        if (ASSERT_THAT_MATCHER.matches(childMit)) {
          return Optional.of(childMit.arguments().get(0));
        } else if (ASSERTJ_NOT_TESTING_SIZE.matches(childMit)) {
          return getSubjectArgumentNotTestedForSize(childMit);
        }
      }
    }
    return Optional.empty();
  }

  private static boolean isNotUsedSomewhereElse(ExpressionTree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      return isNotUsedSomewhereElse(((MethodInvocationTree) tree).methodSelect());
    } else if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) tree;
      if (!ExpressionUtils.isSelectOnThisOrSuper(memberSelectExpressionTree)) {
        return isNotUsedSomewhereElse(((MemberSelectExpressionTree) tree).expression());
      }
    } else if (tree.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      return isNotUsedSomewhereElse(((ParenthesizedTree) tree).expression());
    } else if (tree.is(Tree.Kind.TYPE_CAST)) {
      return isNotUsedSomewhereElse(((TypeCastTree) tree).expression());
    } else if (tree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).symbol().usages().size() == 1;
    }
    return false;
  }

  private void reportIssueWithSecondary(MethodInvocationTree mit, ExpressionTree argument) {
    reportIssue(ExpressionUtils.methodName(mit),
      "Test the emptiness of the list before calling this assertion predicate.",
      Collections.singletonList(new JavaFileScannerContext.Location("", argument)),
      null);
  }

}
