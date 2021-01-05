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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6068")
public class MockitoEqSimplificationCheck extends IssuableSubscriptionVisitor {
  private static final String MOCKITO = "org.mockito.Mockito";

  private static final MethodMatchers METHODS_USING_EQ_IN_ARGUMENTS = MethodMatchers.or(
    MethodMatchers.create().ofTypes(MOCKITO).names("when")
      .addParametersMatcher(MethodMatchers.ANY).build(),
    MethodMatchers.create().ofTypes("org.mockito.BDDMockito").names("given")
      .addParametersMatcher(MethodMatchers.ANY).build()
  );

  private static final MethodMatchers METHODS_USING_EQ_IN_CONSECUTIVE_CALL = MethodMatchers.or(
    MethodMatchers.create().ofTypes(MOCKITO).names("verify").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("org.mockito.InOrder").names("verify").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("org.mockito.stubbing.Stubber").names("when").withAnyParameters().build()
  );

  private static final MethodMatchers MOCKITO_EQ = MethodMatchers.create()
    .ofTypes("org.mockito.Matchers")
    .names("eq").withAnyParameters().build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;

    if (METHODS_USING_EQ_IN_ARGUMENTS.matches(mit)) {
      ExpressionTree argument = mit.arguments().get(0);
      argument = ExpressionUtils.skipParentheses(argument);
      if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
        reportUselessEqUsage(((MethodInvocationTree) argument).arguments());
      }
    } else if (METHODS_USING_EQ_IN_CONSECUTIVE_CALL.matches(mit)) {
      MethodTreeUtils.consecutiveMethodInvocation(mit).ifPresent(m -> reportUselessEqUsage(m.arguments()));
    }
  }

  private void reportUselessEqUsage(Arguments arguments) {
    List<MethodInvocationTree> eqs = new ArrayList<>();

    for (ExpressionTree arg : arguments) {
      arg = ExpressionUtils.skipParentheses(arg);
      if (arg.is(Tree.Kind.METHOD_INVOCATION) && MOCKITO_EQ.matches((MethodInvocationTree) arg)) {
        eqs.add((MethodInvocationTree) arg);
      } else {
        // If arguments contain anything else than a call to eq(...), we do not report an issue
        return;
      }
    }

    if (!eqs.isEmpty()) {
      reportIssue(eqs.get(0).methodSelect(), String.format(
        "Remove this%s useless \"eq(...)\" invocation; pass the values directly.", eqs.size() == 1 ? "" : " and every subsequent"),
        eqs.stream()
          .skip(1)
          .map(eq -> new JavaFileScannerContext.Location("", eq.methodSelect()))
          .collect(Collectors.toList()),
        null);
    }
  }

}
