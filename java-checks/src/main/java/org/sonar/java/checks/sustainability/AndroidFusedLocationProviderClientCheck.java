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
package org.sonar.java.checks.sustainability;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6914")
public class AndroidFusedLocationProviderClientCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofSubTypes("android.content.Context")
      .names("getSystemService")
      .addParametersMatcher("java.lang.String")
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    var nameArg = tree.arguments().get(0);
    if ("location".equals(ExpressionUtils.resolveAsConstant(nameArg))) {
      reportIfInAndroidContext(nameArg);
    }
  }

  private void reportIfInAndroidContext(Tree tree) {
    if (context.inAndroidContext()) {
      reportIssue(tree, "Use \"FusedLocationProviderClient\" instead of \"LocationManager\".");
    }
  }
}
