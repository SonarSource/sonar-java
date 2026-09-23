/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S9412")
public class CollectionsSortCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Replace this \"Collections.sort()\" with \"List.sort()\".";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.util.Collections")
      .names("sort")
      .addParametersMatcher("java.util.List")
      .addParametersMatcher("java.util.List", "java.util.Comparator")
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree listArgument = mit.arguments().get(0);
    boolean hasTwoArgs = mit.arguments().size() == 2;

    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(ExpressionUtils.methodName(mit))
      .withMessage(MESSAGE)
      .withQuickFix(() -> buildQuickFix(mit, listArgument, hasTwoArgs))
      .report();
  }

  private JavaQuickFix buildQuickFix(MethodInvocationTree mit, ExpressionTree listArgument, boolean hasTwoArgs) {
    String listText = QuickFixHelper.contentForTree(listArgument, context);
    String replacement;
    if (hasTwoArgs) {
      String comparatorText = QuickFixHelper.contentForTree(mit.arguments().get(1), context);
      replacement = listText + ".sort(" + comparatorText + ")";
    } else {
      replacement = listText + ".sort(null)";
    }
    return JavaQuickFix.newQuickFix("Use \"%s\" instead", replacement)
      .addTextEdit(JavaTextEdit.replaceTree(mit, replacement))
      .build();
  }
}
