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
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import java.util.Deque;
import java.util.LinkedList;

@Rule(key = "S1143")
public class ReturnInFinallyCheck extends BaseTreeVisitor implements JavaFileScanner {

  private final Deque<Tree.Kind> treeKindStack = new LinkedList<>();
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    treeKindStack.clear();
    scan(context.getTree());
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    scan(tree.resourceList());
    scan(tree.block());
    scan(tree.catches());
    BlockTree finallyBlock = tree.finallyBlock();
    if (finallyBlock != null) {
      treeKindStack.push(finallyBlock.kind());
      scan(finallyBlock);
      treeKindStack.pop();
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    treeKindStack.push(tree.kind());
    super.visitMethod(tree);
    treeKindStack.pop();
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    treeKindStack.push(tree.kind());
    super.visitForStatement(tree);
    treeKindStack.pop();
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    treeKindStack.push(tree.kind());
    super.visitForEachStatement(tree);
    treeKindStack.pop();
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    treeKindStack.push(tree.kind());
    super.visitWhileStatement(tree);
    treeKindStack.pop();
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    treeKindStack.push(tree.kind());
    super.visitDoWhileStatement(tree);
    treeKindStack.pop();
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    treeKindStack.push(tree.kind());
    super.visitSwitchStatement(tree);
    treeKindStack.pop();
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    reportIssue(tree.returnKeyword(), tree.kind());
    super.visitReturnStatement(tree);
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    reportIssue(tree.throwKeyword(), tree.kind());
    super.visitThrowStatement(tree);
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    reportIssue(tree.continueKeyword(), tree.kind());
    super.visitContinueStatement(tree);
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    reportIssue(tree.breakKeyword(), tree.kind());
    super.visitBreakStatement(tree);
  }

  private void reportIssue(SyntaxToken syntaxToken, Tree.Kind jumpKind) {
    if (isAbruptFinallyBlock(jumpKind)) {
      context.reportIssue(this, syntaxToken, "Remove this " + syntaxToken.text() + " statement from this finally block.");
    }
  }

  private boolean isAbruptFinallyBlock(Tree.Kind jumpKind) {
    if (treeKindStack.isEmpty()) {
      return false;
    }
    Tree.Kind blockKind = treeKindStack.peek();
    switch (blockKind) {
      case BLOCK:
        return true;
      case FOR_STATEMENT:
      case FOR_EACH_STATEMENT:
      case WHILE_STATEMENT:
      case DO_STATEMENT:
      case SWITCH_STATEMENT:
        return handleControlFlowInFinally(jumpKind);
      case METHOD:
      default:
        return false;
    }
  }

  private boolean handleControlFlowInFinally(Tree.Kind jumpKind) {
    if (jumpKind == Tree.Kind.BREAK_STATEMENT || jumpKind == Tree.Kind.CONTINUE_STATEMENT) {
      return false;
    } else {
      Tree.Kind parentOfControlFlowStatement = treeKindStack.stream()
        .filter(t -> t == Tree.Kind.BLOCK || t == Tree.Kind.METHOD)
        .findFirst()
        .orElse(Tree.Kind.METHOD);
      return parentOfControlFlowStatement == Tree.Kind.BLOCK;
    }
  }

}
