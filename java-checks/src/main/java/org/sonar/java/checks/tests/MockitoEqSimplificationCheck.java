/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6068")
public class MockitoEqSimplificationCheck extends AbstractMockitoArgumentChecker {
  private static final MethodMatchers MOCKITO_EQ = MethodMatchers.create()
    .ofTypes("org.mockito.ArgumentMatchers", "org.mockito.Matchers", "org.mockito.Mockito")
    .names("eq").withAnyParameters().build();


  @Override
  protected void visitArguments(Arguments arguments) {
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
