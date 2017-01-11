/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.List;

@Rule(key = "S1872")
public class ClassComparedByNameCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(MethodMatcher.create().typeDefinition("java.lang.String").name("equals").withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    List<ExpressionTree> expressionsToCheck = new ArrayList<>(2);
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      expressionsToCheck.add(((MemberSelectExpressionTree) mit.methodSelect()).expression());
    }
    expressionsToCheck.add(mit.arguments().get(0));

    ClassGetNameDetector visitor = new ClassGetNameDetector();
    for (ExpressionTree expression : expressionsToCheck) {
      expression.accept(visitor);
    }
    if (visitor.useClassGetName && !visitor.useStackTraceElementGetClassName) {
      reportIssue(mit, "Use an \"instanceof\" comparison instead.");
    }
  }

  private static class ClassGetNameDetector extends BaseTreeVisitor {

    private boolean useClassGetName = false;
    private boolean useStackTraceElementGetClassName = false;

    private final MethodMatcherCollection methodMatchers = MethodMatcherCollection.create(
      MethodMatcher.create().typeDefinition("java.lang.Class").name("getName").withoutParameter(),
      MethodMatcher.create().typeDefinition("java.lang.Class").name("getSimpleName").withoutParameter());

    private final MethodMatcher stackTraceElementMatcher = MethodMatcher.create()
        .typeDefinition("java.lang.StackTraceElement").name("getClassName").withoutParameter();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (methodMatchers.anyMatch(tree)) {
        useClassGetName = true;
      } else if (stackTraceElementMatcher.matches(tree)) {
        useStackTraceElementGetClassName = true;
      }
      scan(tree.methodSelect());
    }
  }
}
