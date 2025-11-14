/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1217")
public class ThreadRunCheck extends AbstractMethodDetection {

  private static final MethodMatchers THREAD_RUN_METHOD_MATCHER = MethodMatchers.create()
    .ofSubTypes("java.lang.Thread")
    .names("run")
    .withAnyParameters()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return THREAD_RUN_METHOD_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    while (parent != null && !parent.is(Tree.Kind.METHOD)) {
      parent = parent.parent();
    }
    if (parent != null && THREAD_RUN_METHOD_MATCHER.matches((MethodTree) parent)) {
      return;
    }

    IdentifierTree methodName = ExpressionUtils.methodName(mit);
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(methodName)
      .withMessage("Call the method Thread.start() to execute the content of the run() method in a dedicated thread.")
      .withQuickFix(() -> computeQuickFix(methodName))
      .report();
  }

  private static JavaQuickFix computeQuickFix(IdentifierTree methodName) {
    return JavaQuickFix.newQuickFix("Replace run() with start()")
      .addTextEdit(JavaTextEdit.replaceTree(methodName, "start"))
      .build();
  }
}
