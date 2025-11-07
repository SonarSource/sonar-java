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
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.ExpressionUtils.skipParenthesesUpwards;

@Rule(key = "S2164")
public class MathOnFloatCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (tree.is(Tree.Kind.PLUS, Tree.Kind.MINUS, Tree.Kind.MULTIPLY, Tree.Kind.DIVIDE)) {
      if (withinStringConcatenation(tree)) {
        return;
      }
      if (tree.symbolType().is("float")) {
        context.reportIssue(this, tree, "Use a \"double\" or \"BigDecimal\" instead.");
        // do not look for other issues in sub-tree
        return;
      }
    }
    super.visitBinaryExpression(tree);
  }

  private static boolean withinStringConcatenation(BinaryExpressionTree tree) {
    Tree parent = skipParenthesesUpwards(tree.parent());
    return parent.is(Tree.Kind.PLUS) && ((BinaryExpressionTree) parent).symbolType().is("java.lang.String");
  }

}
