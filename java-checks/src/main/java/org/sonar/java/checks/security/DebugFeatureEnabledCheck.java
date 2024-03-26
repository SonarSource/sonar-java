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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4507")
public class DebugFeatureEnabledCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make sure this debug feature is deactivated before delivering the code in production.";

  private static final MethodMatchers PRINT_STACK_TRACE_MATCHER = MethodMatchers.create()
    .ofSubTypes("java.lang.Throwable").names("printStackTrace").addWithoutParametersMatcher().build();

  private static final MethodMatchers SET_WEB_CONTENTS_DEBUGGING_ENABLED = MethodMatchers.create()
      .ofSubTypes("android.webkit.WebView", "android.webkit.WebViewFactoryProvider$Statics")
      .names("setWebContentsDebuggingEnabled").addParametersMatcher("boolean").build();

  private static final MethodMatchers DEBUG_MATCHER = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.config.annotation.web.builders.WebSecurity")
    .names("debug").addParametersMatcher("boolean").build();

  private final Deque<Symbol.TypeSymbol> enclosingClass = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.ANNOTATION, Tree.Kind.CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case ANNOTATION:
        checkAnnotation((AnnotationTree) tree);
        break;
      case METHOD_INVOCATION:
        checkMethodInvocation((MethodInvocationTree) tree);
        break;
      default:
        ClassTree classTree = (ClassTree) tree;
        enclosingClass.push(classTree.symbol());
        break;
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree instanceof ClassTree) {
      enclosingClass.pop();
    }
  }

  private void checkMethodInvocation(MethodInvocationTree mit) {
    if (isPrintStackTraceIllegalUsage(mit) || isSetWebContentsDebuggingEnabled(mit) || isDebugWithTrueArgument(mit)) {
      reportIssue(ExpressionUtils.methodName(mit), MESSAGE);
    }
  }

  private boolean isPrintStackTraceIllegalUsage(MethodInvocationTree mit) {
    return !enclosingClassExtendsThrowable() && PRINT_STACK_TRACE_MATCHER.matches(mit);
  }

  private static boolean isSetWebContentsDebuggingEnabled(MethodInvocationTree mit) {
    return SET_WEB_CONTENTS_DEBUGGING_ENABLED.matches(mit) &&
      Boolean.TRUE.equals(ExpressionUtils.resolveAsConstant(mit.arguments().get(0)));
  }

  private static boolean isDebugWithTrueArgument(MethodInvocationTree mit){
    if (!DEBUG_MATCHER.matches(mit.methodSymbol())){
      return false;
    }
    var cstArg = mit.arguments().get(0).asConstant();
    return cstArg.isPresent() && cstArg.get().equals(true);
  }

  private void checkAnnotation(AnnotationTree annotation) {
    if (annotation.symbolType().is("org.springframework.security.config.annotation.web.configuration.EnableWebSecurity")) {
      annotation.arguments().stream()
        .map(DebugFeatureEnabledCheck::getDebugArgument)
        .filter(Objects::nonNull)
        .findFirst()
        .filter(assignment -> Boolean.TRUE.equals(ExpressionsHelper.getConstantValueAsBoolean(assignment.expression()).value()))
        .ifPresent(assignment -> reportIssue(assignment, MESSAGE));
    }
  }

  @CheckForNull
  private static AssignmentExpressionTree getDebugArgument(ExpressionTree expression) {
    if (expression.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
      if (assignment.variable().is(Tree.Kind.IDENTIFIER) &&
        "debug".equals(((IdentifierTree) assignment.variable()).name())) {
        return assignment;
      }
    }
    return null;
  }

  private boolean enclosingClassExtendsThrowable() {
    return enclosingClass.peek() != null && enclosingClass.peek().type().isSubtypeOf("java.lang.Throwable");
  }

}
