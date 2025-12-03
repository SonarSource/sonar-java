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
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2658")
public class DynamicClassLoadCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofSubTypes("java.lang.Class").names("forName").withAnyParameters().build(),
      MethodMatchers.create().ofSubTypes("java.lang.ClassLoader").names("loadClass").withAnyParameters().build());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String stringConstant = ExpressionsHelper.getConstantValueAsString(mit.arguments().get(0)).value();
    if (stringConstant == null) {
      reportIssue(ExpressionUtils.methodName(mit), "Remove this use of dynamic class loading.");
    }
  }

}
