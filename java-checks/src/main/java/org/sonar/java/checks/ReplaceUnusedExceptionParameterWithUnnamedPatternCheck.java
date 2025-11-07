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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S7467")
public class ReplaceUnusedExceptionParameterWithUnnamedPatternCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    CatchTree catchTree = (CatchTree) tree;
    VariableTree v = catchTree.parameter();
    IdentifierTree ident = v.simpleName();

    if (!ident.isUnnamedVariable() && v.symbol().usages().isEmpty()) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(ident)
        .withMessage(String.format("Replace \"%s\" with an unnamed pattern.", ident.name()))
        .withQuickFix(() -> getQuickFix(ident))
        .report();
    }
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava22Compatible();
  }

  private static JavaQuickFix getQuickFix(IdentifierTree ident) {
    return JavaQuickFix.newQuickFix(String.format("Replace \"%s\" with unnamed pattern \"_\"", ident.name()))
      .addTextEdit(JavaTextEdit.replaceTree(ident, "_"))
      .build();
  }
}
