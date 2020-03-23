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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2701")
public class BooleanLiteralInAssertionsCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes(
          "org.junit.Assert",
          "org.junit.jupiter.api.Assertions",
          "junit.framework.Assert",
          "junit.framework.TestCase").name(name -> name.startsWith("assert"))
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofTypes("org.fest.assertions.Assertions")
        .names("assertThat")
        .addParametersMatcher("boolean")
        .build()
      );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    int arity = mit.arguments().size();
    for (int i = 0; i < arity; i++) {
      ExpressionTree booleanArg = mit.arguments().get(i);
      if (booleanArg.is(Tree.Kind.BOOLEAN_LITERAL)) {
        reportIssue(booleanArg, "Remove or correct this assertion.");
        break;
      }
    }
  }

}
