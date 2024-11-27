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
package org.sonar.java.checks.sustainability;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6891")
public class AndroidExactAlarmCheck extends AbstractMethodDetection {

  private static final String SET_EXACT_NAME = "setExact";

  private static final String SET_EXACT_AND_ALLOW_WHILE_IDLE_NAME = "setExactAndAllowWhileIdle";

  private static final String SET_WINDOW_NAME = "setWindow";

  private static final long SUGGESTED_MIN_LENGTH_MILLIS = 10L * 60L * 1000L;

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofSubTypes("android.app.AlarmManager")
      .names(SET_EXACT_NAME, SET_EXACT_AND_ALLOW_WHILE_IDLE_NAME, SET_WINDOW_NAME)
      .withAnyParameters()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    if (SET_WINDOW_NAME.equals(tree.methodSymbol().name())) {
      onSetWindowInvocation(tree);
    } else {
      onSetExactInvocation(tree);
    }
  }

  private void onSetExactInvocation(MethodInvocationTree tree) {
    var identifier = ExpressionUtils.methodName(tree);
    var methodName = identifier.name();
    var replacementName = methodName.replace("Exact", "");
    reportIfInAndroidContext(identifier, String.format("Use \"%s\" instead of \"%s\".", replacementName, methodName));
  }

  private void onSetWindowInvocation(MethodInvocationTree tree) {
    if (tree.arguments().size() < 3) return;
    var windowLengthMillisArg = tree.arguments().get(2);
    var windowLengthMillis = ExpressionUtils.resolveAsConstant(windowLengthMillisArg);

    if (windowLengthMillis instanceof Number num && num.longValue() < SUGGESTED_MIN_LENGTH_MILLIS) {
      reportIfInAndroidContext(windowLengthMillisArg, "Use alarm windows of 10 minutes or more instead.");
    }
  }

  private void reportIfInAndroidContext(Tree tree, String message) {
    if (context.inAndroidContext()) {
      reportIssue(tree, message);
    }
  }
}
