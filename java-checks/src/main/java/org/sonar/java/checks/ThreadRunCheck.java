/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
