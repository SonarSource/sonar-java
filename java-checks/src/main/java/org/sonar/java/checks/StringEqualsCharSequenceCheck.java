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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9394")
public class StringEqualsCharSequenceCheck extends AbstractMethodDetection {

  private static final MethodMatchers EQUALS_MATCHER = MethodMatchers.create()
    .ofSubTypes("java.lang.String")
    .names("equals")
    .addParametersMatcher("java.lang.Object")
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return EQUALS_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree argument = mit.arguments().get(0);
    Type argumentType = argument.symbolType();
    if (argumentType.isUnknown() || argumentType.isNullType() || argument.is(Tree.Kind.NULL_LITERAL)) {
      return;
    }
    if (argumentType.isSubtypeOf("java.lang.CharSequence") && !argumentType.isSubtypeOf("java.lang.String")) {
      IdentifierTree methodName = ExpressionUtils.methodName(mit);
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(methodName)
        .withMessage("Use \"contentEquals()\" instead of \"equals()\" to compare a \"String\" with a \"CharSequence\".")
        .withQuickFix(() -> JavaQuickFix.newQuickFix("Replace with \"contentEquals()\"")
          .addTextEdit(JavaTextEdit.replaceTree(methodName, "contentEquals"))
          .build())
        .report();
    }
  }
}
