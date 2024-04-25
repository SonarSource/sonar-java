/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.quickfixes.TextEdit;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

interface AssertJChainSimplificationQuickFix extends BiFunction<MethodInvocationTree, MethodInvocationTree, Supplier<List<JavaQuickFix>>> {
  String QUICK_FIX_MESSAGE_FORMAT_STRING = "Use \"%s\"";

  @Override
  Supplier<List<JavaQuickFix>> apply(MethodInvocationTree subject, MethodInvocationTree predicate);
}

class NoQuickFix implements AssertJChainSimplificationQuickFix {

  @Override
  public Supplier<List<JavaQuickFix>> apply(MethodInvocationTree subject, MethodInvocationTree predicate) {
    return null;
  }
}

/**
 * Replace assertThat(x.y(a)).z(b); or assertThat(x.y).z(b); by:
 * <p>
 * - keepPredicateArgument = true
 * assertThat(x).replacement(b);
 * <p>
 * - keepPredicateArgument = false
 * assertThat(x).replacement();
 */
class ActualExpectedInPredicateQuickFix implements AssertJChainSimplificationQuickFix {
  private final boolean keepPredicateArgument;
  final String replacement;
  final String predicateName;

  ActualExpectedInPredicateQuickFix(String replacement, String predicateName, boolean keepPredicateArgument) {
    this.replacement = replacement;
    this.predicateName = predicateName;
    this.keepPredicateArgument = keepPredicateArgument;
  }

  @Override
  public Supplier<List<JavaQuickFix>> apply(MethodInvocationTree subject, MethodInvocationTree predicate) {
    return () -> {
      Arguments arguments = subject.arguments();
      if (arguments.size() == 1) {
        ExpressionTree argument = arguments.get(0);
        Optional<MemberSelectExpressionTree> memberSelectInSubject = getMemberSelectInSubject(argument);
        if (memberSelectInSubject.isPresent()) {
          MemberSelectExpressionTree memberSelect = memberSelectInSubject.get();
          return Collections.singletonList(getJavaQuick(predicate, argument, memberSelect));
        }
      }
      return Collections.emptyList();
    };
  }

  private JavaQuickFix getJavaQuick(MethodInvocationTree predicate, ExpressionTree argument, MemberSelectExpressionTree memberSelect) {
    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE_FORMAT_STRING, replacement);
    // assertThat(x.y()).z() --> assertThat(x).z()
    builder.addTextEdit(TextEdit.removeTextSpan(textSpanBetween(memberSelect.expression(), false, argument, true)));
    if (keepPredicateArgument) {
      // assertThat(x).z(a) --> assertThat(x).predicateName(a)
      builder.addTextEdit(AnalyzerMessage.replaceTree(ExpressionUtils.methodName(predicate), predicateName));
    } else {
      // assertThat(x).z(a) --> assertThat(x).predicateName()
      builder.addTextEdit(AnalyzerMessage.replaceBetweenTree(ExpressionUtils.methodName(predicate), predicate, predicateName + "()"));
    }
    return builder.build();
  }

  private static Optional<MemberSelectExpressionTree> getMemberSelectInSubject(ExpressionTree tree) {
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      return Optional.of((MemberSelectExpressionTree) tree);
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      return getMemberSelectInSubject(((MethodInvocationTree) tree).methodSelect());
    }
    return Optional.empty();
  }
}

/**
 * Replace assertThat(x.y(a)).y(b); by assertThat(x).replacement(a);
 */
class ActualExpectedInSubjectQuickFix implements AssertJChainSimplificationQuickFix {
  final String replacement;
  final String predicateName;

  ActualExpectedInSubjectQuickFix(String replacement, String predicateName) {
    this.replacement = replacement;
    this.predicateName = predicateName;
  }

  @Override
  public Supplier<List<JavaQuickFix>> apply(MethodInvocationTree subject, MethodInvocationTree predicate) {
    return () -> {
      Optional<MethodInvocationTree> methodInvocationInArguments = getMethodInvocationInArguments(subject.arguments());
      if (methodInvocationInArguments.isPresent()) {
        MethodInvocationTree invocationTree = methodInvocationInArguments.get();
        ExpressionTree methodSelect = invocationTree.methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) methodSelect;
          return Collections.singletonList(getJavaQuickFix(predicate, memberSelect, invocationTree));
        }
      }
      return Collections.emptyList();
    };
  }

  private static Optional<MethodInvocationTree> getMethodInvocationInArguments(Arguments arguments) {
    if (arguments.size() == 1) {
      ExpressionTree argument = arguments.get(0);
      if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
        return Optional.of((MethodInvocationTree) argument);
      }
    }
    return Optional.empty();
  }

  private JavaQuickFix getJavaQuickFix(MethodInvocationTree predicate, MemberSelectExpressionTree memberSelect, MethodInvocationTree invocationTree) {
    return JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE_FORMAT_STRING, replacement)
      .addTextEdit(
        // assertThat(x.y(a)).z() --> assertThat(x).predicateName(a)).z()
        TextEdit.replaceTextSpan(textSpanBetween(memberSelect.expression(), false, invocationTree.arguments().get(0), false),
          String.format(").%s(", predicateName)),
        // assertThat(x).predicateName(a)).z() --> assertThat(x).predicateName(a)
        TextEdit.removeTextSpan(textSpanBetween(invocationTree.arguments(), false, predicate, true))
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
  public Supplier<List<JavaQuickFix>> apply(MethodInvocationTree subject, MethodInvocationTree predicate) {
    return apply(predicate);
  }

  public Supplier<List<JavaQuickFix>> apply(MethodInvocationTree predicate) {
    return () -> Collections.singletonList(JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE_FORMAT_STRING, replacement)
      .addTextEdit(AnalyzerMessage.replaceBetweenTree(ExpressionUtils.methodName(predicate), predicate, replacement))
      .build());
  }
}
