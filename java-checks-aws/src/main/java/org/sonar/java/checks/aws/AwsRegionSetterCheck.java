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
package org.sonar.java.checks.aws;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6262")
public class AwsRegionSetterCheck extends AbstractMethodDetection {

  private static final String STRING_TYPE = "java.lang.String";
  private static final String MESSAGE = "Give the enum value for this region instead.";

  private static final MethodMatchers REGION_SETTER_MATCHER = MethodMatchers.create()
    .ofSubTypes("com.amazonaws.client.builder.AwsClientBuilder")
    .names("withRegion", "setRegion")
    .addParametersMatcher(STRING_TYPE)
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return REGION_SETTER_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    // The methodmatcher ensures that there is one and only one argument.
    ExpressionTree argument = tree.arguments().get(0);
    if (argument.is(Tree.Kind.STRING_LITERAL)) {
      reportIssue(argument, MESSAGE);
    }
  }
}
