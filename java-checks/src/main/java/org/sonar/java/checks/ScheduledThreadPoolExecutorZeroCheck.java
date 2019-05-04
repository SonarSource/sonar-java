/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S2122")
public class ScheduledThreadPoolExecutorZeroCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Increase the \"corePoolSize\".";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.util.concurrent.ThreadPoolExecutor")).name("setCorePoolSize").addParameter("int"),
      MethodMatcher.create().typeDefinition("java.util.concurrent.ScheduledThreadPoolExecutor").name("<init>").addParameter("int")
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree arg = mit.arguments().get(0);
    if (isZeroIntLiteral(arg)) {
      reportIssue(arg, MESSAGE);
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    ExpressionTree arg = newClassTree.arguments().get(0);
    if (isZeroIntLiteral(arg)) {
      reportIssue(arg, MESSAGE);
    }
  }

  private static boolean isZeroIntLiteral(ExpressionTree arg) {
    return arg.is(Tree.Kind.INT_LITERAL) && "0".equals(((LiteralTree) arg).value());
  }
}
