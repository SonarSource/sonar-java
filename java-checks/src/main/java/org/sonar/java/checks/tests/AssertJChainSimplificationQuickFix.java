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

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.tests.AssertJChainSimplificationIndex.QUICK_FIX_MESSAGE_FORMAT_STRING;
import static org.sonar.java.model.ExpressionUtils.skipParentheses;
import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

interface AssertJChainSimplificationQuickFix extends BiFunction<MethodInvocationTree, MethodInvocationTree, Supplier<JavaQuickFix>> {

  @Override
  Supplier<JavaQuickFix> apply(MethodInvocationTree subject, MethodInvocationTree predicate);

  default Optional<MethodInvocationTree> getMethodInvocationInArguments(Arguments arguments) {
    if (arguments.size() == 1) {
      ExpressionTree argument = skipParentheses(arguments.get(0));
      if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
        return Optional.of((MethodInvocationTree) argument);
      }
    }
    return Optional.empty();
  }
}

class NoQuickFix implements AssertJChainSimplificationQuickFix {

  @Override
  public Supplier<JavaQuickFix> apply(MethodInvocationTree subject, MethodInvocationTree predicate) {
    return null;
  }
}

abstract class ActualExpectedQuickFix implements AssertJChainSimplificationQuickFix {
  final String replacement;
  final String predicateName;

  ActualExpectedQuickFix(String replacement, String predicateName) {
    this.replacement = replacement;
    this.predicateName = predicateName;
  }

  @Override
  public Supplier<JavaQuickFix> apply(MethodInvocationTree subject, MethodInvocationTree predicate) {
    Optional<MethodInvocationTree> methodInvocationInArguments = getMethodInvocationInArguments(subject.arguments());
    if (methodInvocationInArguments.isPresent()) {
      MethodInvocationTree invocationTree = methodInvocationInArguments.get();
      ExpressionTree methodSelect = invocationTree.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) methodSelect;
        return buildQuickFix(predicate, memberSelect, invocationTree);
      }
    }
    return null;
  }

  abstract Supplier<JavaQuickFix> buildQuickFix(MethodInvocationTree predicate, MemberSelectExpressionTree memberSelect, MethodInvocationTree invocationTree);
}

/**
 * Replace assertThat(x.y(a)).y(b); by:
 *
 * - keepPredicateArgument = true
 * assertThat(x).replacement(b);
 *
 * - keepPredicateArgument = false
 * assertThat(x).replacement();
 */
class ActualExpectedInPredicateQuickFix extends ActualExpectedQuickFix {
  private final boolean keepPredicateArgument;

  ActualExpectedInPredicateQuickFix(String replacement, String predicateName, boolean keepPredicateArgument) {
    super(replacement, predicateName);
    this.keepPredicateArgument = keepPredicateArgument;
  }

  Supplier<JavaQuickFix> buildQuickFix(MethodInvocationTree predicate, MemberSelectExpressionTree memberSelect, MethodInvocationTree invocationTree) {
    return () -> {
      JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE_FORMAT_STRING, replacement);
      // assertThat(x.y()).z() --> assertThat(x).z()
      builder.addTextEdit(JavaTextEdit.removeTextSpan(textSpanBetween(memberSelect.expression(), false, invocationTree, true)));
      if (keepPredicateArgument) {
        // assertThat(x).z(a) --> assertThat(x).predicateName(a)
        builder.addTextEdit(JavaTextEdit.replaceTree(ExpressionUtils.methodName(predicate), predicateName));
      } else {
        // assertThat(x).z(a) --> assertThat(x).predicateName()
        builder.addTextEdit(JavaTextEdit.replaceBetweenTree(ExpressionUtils.methodName(predicate), predicate, predicateName + "()"));
      }
      return builder.build();
    };
  }
}

/**
 * Replace assertThat(x.y(a)).y(b); by assertThat(x).replacement(a);
 */
class ActualExpectedInSubjectQuickFix extends ActualExpectedQuickFix {

  ActualExpectedInSubjectQuickFix(String replacement, String predicateName) {
    super(replacement, predicateName);
  }

  @Override
  Supplier<JavaQuickFix> buildQuickFix(MethodInvocationTree predicate, MemberSelectExpressionTree memberSelect, MethodInvocationTree invocationTree) {
    return () -> JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE_FORMAT_STRING, replacement)
      .addTextEdit(
        // assertThat(x.y(a)).z() --> assertThat(x).predicateName(a)).z()
        JavaTextEdit.replaceTextSpan(textSpanBetween(memberSelect.expression(), false, invocationTree.arguments().get(0), false),
          String.format(").%s(", predicateName)),
        // assertThat(x).predicateName(a)).z() --> assertThat(x).predicateName(a)
        JavaTextEdit.removeTextSpan(textSpanBetween(invocationTree.arguments(), false, predicate, true))
      ).build();
  }
}

/**
 * Replace assertThat(x).y(a); by assertThat(x).replacement();
 */
class ContextFreeQuickFix implements AssertJChainSimplificationQuickFix {
  private final String replacement;

  ContextFreeQuickFix(String replacement) {
    this.replacement = replacement;
  }

  @Override
  public Supplier<JavaQuickFix> apply(MethodInvocationTree subject, MethodInvocationTree predicate) {
    return apply(predicate);
  }

  public Supplier<JavaQuickFix> apply(MethodInvocationTree predicate) {
    return () -> JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE_FORMAT_STRING, replacement)
      .addTextEdit(JavaTextEdit.replaceBetweenTree(ExpressionUtils.methodName(predicate), predicate, replacement))
      .build();
  }
}
