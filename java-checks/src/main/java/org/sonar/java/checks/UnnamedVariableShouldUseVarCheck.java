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
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VarTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Use `var` instead of a type with unnamed variable (`_`) in foreach and try-with-resources.
 */
@Rule(key = "S7466")
public class UnnamedVariableShouldUseVarCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.FOR_EACH_STATEMENT, Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof ForEachStatement forEachTree) {
      checkVariable(forEachTree.variable());
    } else if (tree instanceof TryStatementTree tryTree) {
      for (Tree resource: tryTree.resourceList()) {
        if (resource instanceof VariableTree variable)  {
          checkVariable(variable);
        }
      }
    }
  }

  void checkVariable(VariableTree variable) {
    TypeTree type = variable.type();
    if (variable.simpleName().isUnnamedVariable() && !isVarDeclaration(type)) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(type)
        .withMessage("Use `var` instead of a type with unnamed variable _")
        .withQuickFix(() -> getQuickFix(type))
        .report();
    }
  }

  private static boolean isVarDeclaration(TypeTree typeTree) {
    return typeTree instanceof VarTypeTree;
  }

  private static JavaQuickFix getQuickFix(TypeTree tree) {
    return JavaQuickFix.newQuickFix("Replace the type with \"var\"")
      .addTextEdit(JavaTextEdit.replaceTree(tree, "var"))
      .build();
  }
}
