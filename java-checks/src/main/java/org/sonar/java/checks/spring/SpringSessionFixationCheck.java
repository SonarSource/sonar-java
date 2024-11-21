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
package org.sonar.java.checks.spring;


import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;

@Rule(key = "S5876")
public class SpringSessionFixationCheck extends AbstractMethodDetection {

  private static final String ISSUE_MSG = "Create a new session during user authentication to prevent session fixation attacks.";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer$SessionFixationConfigurer")
      .names("none")
      .addWithoutParametersMatcher()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodInvocation) {
    reportIssue(ExpressionUtils.methodName(methodInvocation), ISSUE_MSG);
  }

  @Override
  protected void onMethodReferenceFound(MethodReferenceTree methodReferenceTree) {
    reportIssue(methodReferenceTree.method(), ISSUE_MSG);
  }

}
