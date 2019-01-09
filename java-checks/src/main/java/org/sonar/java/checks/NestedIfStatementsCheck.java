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
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Rule(key = "S134")
public class NestedIfStatementsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_MAX = 3;

  @RuleProperty(
    description = "Maximum allowed control flow statement nesting depth.",
    defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private JavaFileScannerContext context;
  private Deque<Tree> nestingLevel;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    this.nestingLevel = new ArrayDeque<>();
    scan(context.getTree());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    SyntaxToken ifKeyword = tree.ifKeyword();
    checkNesting(ifKeyword);
    nestingLevel.push(ifKeyword);
    visit(tree);
    nestingLevel.pop();
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    SyntaxToken forKeyword = tree.forKeyword();
    checkNesting(forKeyword);
    nestingLevel.push(forKeyword);
    super.visitForStatement(tree);
    nestingLevel.pop();
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    SyntaxToken forKeyword = tree.forKeyword();
    checkNesting(forKeyword);
    nestingLevel.push(forKeyword);
    super.visitForEachStatement(tree);
    nestingLevel.pop();
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    SyntaxToken whileKeyword = tree.whileKeyword();
    checkNesting(whileKeyword);
    nestingLevel.push(whileKeyword);
    super.visitWhileStatement(tree);
    nestingLevel.pop();
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    SyntaxToken doKeyword = tree.doKeyword();
    checkNesting(doKeyword);
    nestingLevel.push(doKeyword);
    super.visitDoWhileStatement(tree);
    nestingLevel.pop();
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    SyntaxToken switchKeyword = tree.switchKeyword();
    checkNesting(switchKeyword);
    nestingLevel.push(switchKeyword);
    super.visitSwitchStatement(tree);
    nestingLevel.pop();
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    SyntaxToken tryKeyword = tree.tryKeyword();
    checkNesting(tryKeyword);
    nestingLevel.push(tryKeyword);
    scan(tree.block());
    nestingLevel.pop();
    scan(tree.resourceList());
    scan(tree.catches());
    scan(tree.finallyBlock());
  }

  private void checkNesting(Tree tree) {
    int size = nestingLevel.size();
    if (size == max) {
      List<JavaFileScannerContext.Location> secondary = new ArrayList<>(size);
      for (Tree element : nestingLevel) {
        secondary.add(new JavaFileScannerContext.Location("Nesting + 1", element));
      }
      context.reportIssue(this, tree, "Refactor this code to not nest more than " + max + " if/for/while/switch/try statements.", secondary, null);
    }
  }

  private void visit(IfStatementTree tree) {
    scan(tree.condition());
    scan(tree.thenStatement());

    StatementTree elseStatementTree = tree.elseStatement();
    if (elseStatementTree != null && elseStatementTree.is(Tree.Kind.IF_STATEMENT)) {
      visit((IfStatementTree) elseStatementTree);
    } else {
      scan(elseStatementTree);
    }
  }

}
