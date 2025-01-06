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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;

@Rule(key = "S1844")
public class WaitOnConditionCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofSubTypes("java.util.concurrent.locks.Condition")
      .names("wait")
      .addWithoutParametersMatcher()
      .addParametersMatcher("long")
      .addParametersMatcher("long", "int")
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(ExpressionUtils.methodName(mit), "The \"Condition.await(...)\" method should be used instead of \"Object.wait(...)\"");
  }

  @Override
  protected void onMethodReferenceFound(MethodReferenceTree methodReferenceTree) {
    reportIssue(methodReferenceTree.method(), "The \"Condition.await(...)\" method should be used instead of \"Object.wait(...)\"");
  }
}
