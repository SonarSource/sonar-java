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
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import java.util.ArrayList;
import java.util.List;

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

  private JavaQuickFix getQuickFix(MethodInvocationTree tree, String methodName, String methodCallee) {
    List<JavaTextEdit> edits = new ArrayList<>();
    getImportSupplier()
      .newImportEdit(ARRAYS)
      .ifPresent(edits::add);

    if ("toString".equals(methodName)) {
      edits.add(JavaTextEdit.replaceTree(tree, "Arrays.toString(" + methodCallee + ")"));
      return JavaQuickFix.newQuickFix("Use \"Arrays.toString(array)\" instead")
        .addTextEdits(edits)
        .build();
    } else {
      edits.add(JavaTextEdit.replaceTree(tree, "Arrays.hashCode(" + methodCallee + ")"));
      return JavaQuickFix.newQuickFix("Use \"Arrays.hashCode(array)\" instead")
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
