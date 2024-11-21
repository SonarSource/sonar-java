/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.tests;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5833")
public class AssertJContextBeforeAssertionCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofSubTypes("org.assertj.core.api.AbstractAssert")
      .name(name ->
        "as".equals(name) || "describedAs".equals(name) || "withFailMessage".equals(name)
          || "overridingErrorMessage".equals(name) || "extracting".equals(name)
          || name.startsWith("using") || name.startsWith("filtered")
      ).withAnyParameters()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (isLastCallInStatement(mit)) {
      // Report only when it's the last call, if we have anything after, it's either:
      // - an assertion: compliant solution
      // - a method setting context: we will report on this other one instead (if problematic), to avoid reporting multiple times.
      // - not in a statement, meaning that it can be asserted somewhere else (assertion returned, assigned, ...)
      reportIssue(ExpressionUtils.methodName(mit), "Add an assertion predicate after calling this method.");
    }
  }

  private static boolean isLastCallInStatement(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    return parent != null && parent.is(Tree.Kind.EXPRESSION_STATEMENT);
  }

}

