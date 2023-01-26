/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.tests;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S5810")
public class JUnit5SilentlyIgnoreClassAndMethodCheck extends AbstractJUnit5NotCompliantModifierChecker {

  @Override
  protected boolean isNonCompliantModifier(Modifier modifier, boolean isMethod) {
    return modifier == Modifier.PRIVATE || (isMethod && modifier == Modifier.STATIC);
  }

  @Override
  protected void raiseIssueOnNonCompliantReturnType(MethodTree methodTree) {
    TypeTree returnType = methodTree.returnType();
    // returnType of METHOD is never null (unlike CONSTRUCTOR)
    Type type = returnType.symbolType();
    boolean methodReturnAValue = !type.isUnknown() && !type.isVoid();
    if(methodReturnAValue && !methodTree.symbol().metadata().isAnnotatedWith("org.junit.jupiter.api.TestFactory")) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(methodTree.returnType())
        .withMessage("Replace the return type by void.")
        .withQuickFix(() ->
          JavaQuickFix.newQuickFix("Replace with void")
            .addTextEdit(JavaTextEdit.replaceTree(returnType, "void"))
            .build())
        .report();
    }
  }
}
