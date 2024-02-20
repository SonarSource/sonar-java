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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6901")
public class VirtualThreadUnsupportedMethodsCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String issueMessage = "Method '%s' is not supported on virtual threads.";

  private static final MethodMatchers VIRTUAL_THREAD_BUILDER_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.lang.Thread$Builder$OfVirtual")
      .names("unstarted", "start")
      .addParametersMatcher("java.lang.Runnable").build(),
    MethodMatchers.create()
      .ofTypes("java.lang.Thread")
      .names("startVirtualThread")
      .addParametersMatcher("java.lang.Runnable").build()
  );

  private static final MethodMatchers VIRTUAL_THREAD_UNSUPPORTED_METHODS = MethodMatchers.create()
    .ofTypes("java.lang.Thread")
    .names("setDaemon", "setPriority", "getThreadGroup")
    .withAnyParameters()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (VIRTUAL_THREAD_UNSUPPORTED_METHODS.matches(mit)) {
      checkMethodCalledOnVirtualThread(mit);
    }
  }

  private void checkMethodCalledOnVirtualThread(MethodInvocationTree methodInvocation) {
    var memberSelect = (MemberSelectExpressionTree) methodInvocation.methodSelect();
    var expression = memberSelect.expression();
    if (isIdentifierAndVirtualThread(expression) || isMethodInvocationAndReturningVirtualThread(expression)) {
      reportIssue(memberSelect.identifier(), String.format(issueMessage, memberSelect.identifier().name()));
    }
  }

  private boolean isIdentifierAndVirtualThread(ExpressionTree expression) {
    return expression instanceof IdentifierTree identifier && isSymbolVirtualThread(identifier.symbol());
  }

  private boolean isMethodInvocationAndReturningVirtualThread(ExpressionTree expression) {
    return expression instanceof MethodInvocationTree mit && VIRTUAL_THREAD_BUILDER_METHODS.matches(mit);
  }

  private boolean isSymbolVirtualThread(Symbol symbol) {
    if (symbol.declaration() instanceof VariableTree variableTree) {
      return variableTree.initializer() instanceof MethodInvocationTree mit && VIRTUAL_THREAD_BUILDER_METHODS.matches(mit);
    }
    return false;
  }

}
