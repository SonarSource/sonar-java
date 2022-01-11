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
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;

@Rule(key = "S5786")
public class JUnit5DefaultPackageClassAndMethodCheck extends AbstractJUnit5NotCompliantModifierChecker {

  @Override
  protected boolean isNonCompliantModifier(Modifier modifier, boolean isMethod) {
    // All visibility modifiers except 'private' handled by S5810
    return modifier == Modifier.PUBLIC || modifier == Modifier.PROTECTED;
  }

  @Override
  protected void raiseIssueOnNonCompliantReturnType(MethodTree methodTree) {
    // Handled by S5810
  }

  @Override
  protected void raiseIssueOnNonCompliantModifier(ModifierKeywordTree modifier) {
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(modifier)
      .withMessage(WRONG_MODIFIER_ISSUE_MESSAGE, modifier.keyword().text())
      .withQuickFix(() -> quickFix(modifier))
      .report();
  }

  private static JavaQuickFix quickFix(ModifierKeywordTree modifier) {
    return JavaQuickFix.newQuickFix("Remove \"%s\" modifier", modifier.keyword().text())
      .addTextEdit(JavaTextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(modifier, true, QuickFixHelper.nextToken(modifier), false)))
      .build();
  }
}
