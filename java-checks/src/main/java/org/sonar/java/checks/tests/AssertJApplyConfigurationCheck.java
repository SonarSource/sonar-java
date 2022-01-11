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

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5831")
public class AssertJApplyConfigurationCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("org.assertj.core.configuration.Configuration").constructor().addWithoutParametersMatcher().build();
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Tree parent = newClassTree.parent();
    if (parent != null && parent.is(Tree.Kind.VARIABLE)) {
      VariableTree variableTree = ((VariableTree) parent);
      if (variableTree.symbol().usages().stream().noneMatch(AssertJApplyConfigurationCheck::canApplyConfiguration)) {
        reportIssue(variableTree.simpleName(), "Apply this configuration with apply() or applyAndDisplay().");
      }
    }
  }

  private static boolean canApplyConfiguration(IdentifierTree identifier) {
    Tree parent = identifier.parent();
    if (parent == null) {
      return false;
    }

    if (parent.is(Tree.Kind.MEMBER_SELECT)) {
      String methodName = ((MemberSelectExpressionTree) parent).identifier().name();
      return "apply".equals(methodName) || "applyAndDisplay".equals(methodName);
    }
    // The configuration can be applied when passed as argument.
    return parent.is(Tree.Kind.ARGUMENTS);
  }

}
