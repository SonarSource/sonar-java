/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.UnitTestUtils.FAIL_METHOD_MATCHER;

@Rule(key = "S3658")
public class AssertionFailInCatchBlockCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return FAIL_METHOD_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (isInCatch(mit)) {
      reportIssue(mit, "Remove this failure assertion and simply add the exception type to the method signature.");
    }
  }

  private static boolean isInCatch(Tree tree) {
    Tree parent = tree.parent();
    return parent != null && !parent.is(Tree.Kind.METHOD) && (parent.is(Tree.Kind.CATCH) || isInCatch(parent));
  }
}
