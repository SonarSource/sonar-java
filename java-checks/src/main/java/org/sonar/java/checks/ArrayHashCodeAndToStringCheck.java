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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2116")
public class ArrayHashCodeAndToStringCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofType(Type::isArray)
      .names("toString", "hashCode")
      .addWithoutParametersMatcher()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String methodName = mit.methodSymbol().name();
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(ExpressionUtils.methodName(mit))
      .withMessage("Use \"Arrays." + methodName + "(array)\" instead.")
      .withQuickFixes(() -> getQuickFix(mit))
      .report();
  }

  private static List<JavaQuickFix> getQuickFix(MethodInvocationTree tree) {
    String methodName = ExpressionUtils.methodName(tree).name();
    if ("toString".equals(methodName)) {
      JavaQuickFix toStringQuickFix = JavaQuickFix.newQuickFix("Use \"Arrays.toString(array)\" instead.")
        .addTextEdit(JavaTextEdit.replaceTree(tree, "Arrays.toString(" + tree.firstToken().text() + ")"))
        .build();
      return Collections.singletonList(toStringQuickFix);
    } else {
      JavaQuickFix hashCodeQuickFix = JavaQuickFix.newQuickFix("Use \"Arrays.hashCode(array)\" instead.")
        .addTextEdit(JavaTextEdit.replaceTree(tree, "Arrays.hashCode(" + tree.firstToken().text() + ")"))
        .build();
      return Collections.singletonList(hashCodeQuickFix);
    }
  }

}
