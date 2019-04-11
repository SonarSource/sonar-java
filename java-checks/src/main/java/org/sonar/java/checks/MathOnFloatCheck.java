/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

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
    Tree parent = tree.parent();
    while (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      parent = parent.parent();
    }
    return parent.is(Tree.Kind.PLUS) && ((BinaryExpressionTree) parent).symbolType().is("java.lang.String");
  }

}
