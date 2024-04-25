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

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonarsource.analyzer.commons.quickfixes.QuickFix;
import org.sonarsource.analyzer.commons.quickfixes.TextEdit;

@Rule(key = "S2116")
public class ArrayHashCodeAndToStringCheck extends AbstractMethodDetection {

  private static final String ARRAYS = "java.util.Arrays";

  private QuickFixHelper.ImportSupplier importSupplier;

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
    IdentifierTree methodTree = ExpressionUtils.methodName(mit);
    String methodName = methodTree.name();
    String methodCallee = QuickFixHelper.contentForTree(((MemberSelectExpressionTreeImpl) mit.methodSelect()).expression(), context);
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(methodTree)
      .withMessage("Use \"Arrays." + methodName + "(array)\" instead.")
      .withQuickFix(() -> getQuickFix(mit, methodName, methodCallee))
      .report();
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    importSupplier = null;
  }

  private QuickFix getQuickFix(MethodInvocationTree tree, String methodName, String methodCallee) {
    List<TextEdit> edits = new ArrayList<>();
    getImportSupplier()
      .newImportEdit(ARRAYS)
      .ifPresent(edits::add);

    if ("toString".equals(methodName)) {
      edits.add(AnalyzerMessage.replaceTree(tree, "Arrays.toString(" + methodCallee + ")"));
      return QuickFix.newQuickFix("Use \"Arrays.toString(array)\" instead")
        .addTextEdits(edits)
        .build();
    } else {
      edits.add(AnalyzerMessage.replaceTree(tree, "Arrays.hashCode(" + methodCallee + ")"));
      return QuickFix.newQuickFix("Use \"Arrays.hashCode(array)\" instead")
        .addTextEdits(edits)
        .build();
    }
  }

  private QuickFixHelper.ImportSupplier getImportSupplier() {
    if (importSupplier == null) {
      importSupplier = QuickFixHelper.newImportSupplier(context);
    }
    return importSupplier;
  }

}
