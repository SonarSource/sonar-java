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
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * A visitor that targets the arguments of Mockito methods `given`, `verify`, and `when`.
 * It also targets the arguments of method calls embedded or chained to `verify` and `when`.
 */
public abstract class AbstractMockitoArgumentChecker extends IssuableSubscriptionVisitor {

  private static final MethodMatchers METHODS_USING_ARGUMENT_MATCHER_IN_ARGUMENTS = MethodMatchers.or(
    MethodMatchers.create().ofTypes("org.mockito.Mockito").names("when")
      .addParametersMatcher(MethodMatchers.ANY).build(),
    MethodMatchers.create().ofTypes("org.mockito.BDDMockito").names("given")
      .addParametersMatcher(MethodMatchers.ANY).build()
  );

  private static final MethodMatchers METHODS_USING_ARGUMENT_MATCHER_IN_CONSECUTIVE_CALL = MethodMatchers.or(
    MethodMatchers.create().ofTypes("org.mockito.Mockito").names("verify").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("org.mockito.InOrder").names("verify").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("org.mockito.stubbing.Stubber").names("when").withAnyParameters().build()
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (METHODS_USING_ARGUMENT_MATCHER_IN_ARGUMENTS.matches(invocation)) {
      ExpressionTree argument = invocation.arguments().get(0);
      argument = ExpressionUtils.skipParentheses(argument);
      if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
        visitArguments(((MethodInvocationTree) argument).arguments());
      }
    } else if (METHODS_USING_ARGUMENT_MATCHER_IN_CONSECUTIVE_CALL.matches(invocation)) {
      MethodTreeUtils.consecutiveMethodInvocation(invocation).ifPresent(m -> visitArguments(m.arguments()));
    }
  }

  /**
   * Visits the argument of a target method and reports an issue if infringing on implemented rule.
   * @param arguments List of arguments of the method
   */
  protected abstract void visitArguments(Arguments arguments);
}
