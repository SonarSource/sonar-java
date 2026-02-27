/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8465")
public class ScopedValueStableReferenceCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Consider using a stable reference for ScopedValue instances.";

  private static final MethodMatchers WHERE_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.ScopedValue", "java.lang.ScopedValue$Carrier")
    .names("where")
    .withAnyParameters()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return WHERE_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodInvocation) {
    var finder = new NewInstanceInvocationFinder();
    methodInvocation.arguments()
      .get(0)
      .accept(finder);
    MethodInvocationTree newInstanceInvocation = finder.invocation;
    if (newInstanceInvocation != null) {
      reportIssue(newInstanceInvocation, MESSAGE);
    }
  }

  private static class NewInstanceInvocationFinder extends BaseTreeVisitor {

    private static final MethodMatchers NEW_INSTANCE_MATCHER = MethodMatchers.create()
      .ofTypes("java.lang.ScopedValue")
      .names("newInstance")
      .addWithoutParametersMatcher()
      .build();

    private MethodInvocationTree invocation = null;

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      Tree parent = nearestNonParenthesizedParent(methodInvocation);
      if (NEW_INSTANCE_MATCHER.matches(methodInvocation.methodSymbol()) && parent != null && !parent.is(Tree.Kind.ASSIGNMENT)) {
        invocation = methodInvocation;
        return;
      }
      super.visitMethodInvocation(methodInvocation);
    }

    private Tree nearestNonParenthesizedParent(Tree tree) {
      Tree parent = tree.parent();
      while (parent != null && parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
        parent = parent.parent();
      }
      return parent;
    }

  }

}
