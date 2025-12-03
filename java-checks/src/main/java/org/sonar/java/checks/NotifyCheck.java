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
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2446")
public class NotifyCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create().ofAnyType().names("notify").addWithoutParametersMatcher().build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    IdentifierTree methodName = ExpressionUtils.methodName(mit);
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(methodName)
      .withMessage("\"notify\" may not wake up the appropriate thread.")
      .withQuickFix(() -> JavaQuickFix.newQuickFix("Replace with \"notifyAll()\"")
        .addTextEdit(JavaTextEdit.replaceTree(methodName, "notifyAll"))
        .build())
      .report();
  }
}
