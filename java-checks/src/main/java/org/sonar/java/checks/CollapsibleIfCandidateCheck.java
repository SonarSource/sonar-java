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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1066")
public class CollapsibleIfCandidateCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private Deque<IfStatementTree> outerIf = new ArrayDeque<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
    outerIf.clear();
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {

    if (!outerIf.isEmpty() && !hasElseClause(tree)) {
      IfStatementTree outerIfStatement = outerIf.peek();
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(tree.ifKeyword())
        .withMessage("Merge this if statement with the enclosing one.")
        .withSecondaries(Collections.singletonList(new JavaFileScannerContext.Location("", outerIfStatement.ifKeyword())))
        .withQuickFix(() -> computeQuickFix(tree, outerIfStatement))
        .report();
    }

    if (!hasElseClause(tree) && hasBodySingleIfStatement(tree.thenStatement())) {
      // children of this if statement are eligible for issues
      outerIf.push(tree);
      // recurse into sub-tree
      super.visitIfStatement(tree);
      if (!outerIf.isEmpty()) {
        outerIf.pop();
      }
    } else {
      // direct children of this if statement not eligible for issues. Reset nesting count
      outerIf.clear();
      super.visitIfStatement(tree);
    }
  }

  private static boolean hasElseClause(IfStatementTree tree) {
    return tree.elseStatement() != null;
  }

  private static boolean hasBodySingleIfStatement(StatementTree thenStatement) {

    if (thenStatement.is(Tree.Kind.BLOCK)) {
      // thenStatement has curly braces. Let's see what's inside...
      BlockTree block = (BlockTree) thenStatement;
      return block.body().size() == 1 && block.body().get(0).is(Tree.Kind.IF_STATEMENT);
    } else if (thenStatement.is(Tree.Kind.IF_STATEMENT)) {
      // no curlys on thenStatement; it's a bare if statement
      return true;
    }

    return false;
  }

  private static JavaQuickFix computeQuickFix(IfStatementTree ifStatement, IfStatementTree outerIf) {
    var quickFixBuilder = JavaQuickFix.newQuickFix("Merge this if statement with the enclosing one");
    StatementTree containingStatement = outerIf.thenStatement();
    if (containingStatement.is(Tree.Kind.BLOCK)) {
      StatementTree thenStatement = ifStatement.thenStatement();
      if (thenStatement.is(Tree.Kind.BLOCK)) {
        SyntaxToken closingBrace = ((BlockTree) thenStatement).closeBraceToken();
        quickFixBuilder.addTextEdit(JavaTextEdit.removeTree(closingBrace));
      } else {
        quickFixBuilder.addTextEdit(JavaTextEdit.insertBeforeTree(ifStatement.thenStatement(), "{"));
      }
    }
    quickFixBuilder.addTextEdit(JavaTextEdit.replaceBetweenTree(outerIf.closeParenToken(), ifStatement.ifKeyword(), " && "));
    return quickFixBuilder.build();
  }
}
