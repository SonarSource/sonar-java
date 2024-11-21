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

import java.util.Objects;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2925")
public class ThreadSleepInTestsCheck extends AbstractMethodDetection {
  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String name = Objects.requireNonNull(mit.methodSymbol().owner()).type().name();
    reportIssue(ExpressionUtils.methodName(mit), String.format("Remove this use of \"%s.sleep()\".", name));
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(MethodMatchers.create().ofTypes("java.lang.Thread").names("sleep").withAnyParameters().build(),
      MethodMatchers.create().ofTypes("java.util.concurrent.TimeUnit").names("sleep").withAnyParameters().build());
  }
}
