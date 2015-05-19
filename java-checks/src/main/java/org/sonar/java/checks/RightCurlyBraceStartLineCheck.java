/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.EmptyStatementTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "RightCurlyBraceStartLineCheck",
  name = "A close curly brace should be located at the beginning of a line",
  tags = {"convention"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("1min")
public class RightCurlyBraceStartLineCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(
      Tree.Kind.BLOCK, Tree.Kind.STATIC_INITIALIZER, Tree.Kind.INITIALIZER,
      Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.BLOCK, Tree.Kind.STATIC_INITIALIZER, Tree.Kind.INITIALIZER)) {
      BlockTree blockTree = (BlockTree) tree;
      checkBlockBody(blockTree.openBraceToken(), blockTree.closeBraceToken(), blockTree.body());
    } else {
      ClassTree classTree = (ClassTree) tree;
      checkBlockBody(classTree.openBraceToken(), classTree.closeBraceToken(), classTree.members());
    }
  }

  private void checkBlockBody(SyntaxToken openBraceToken, SyntaxToken closeBraceToken, List<? extends Tree> trees) {
    if (openBraceToken.line() == closeBraceToken.line()) {
      insertIssue(closeBraceToken);
    } else if (!trees.isEmpty()) {
      Tree lastTree = trees.get(trees.size() - 1);
      SyntaxToken lastToken = SyntaxTokenFinder.visit(lastTree).getLastToken();
      if (lastToken.line() == closeBraceToken.line()) {
        insertIssue(closeBraceToken);
      }
    }
  }

  private void insertIssue(SyntaxToken token) {
    addIssue(token, "Move this closing curly brace to the next line.");
  }

  private static class SyntaxTokenFinder extends BaseTreeVisitor {

    private SyntaxToken lastToken;

    public static SyntaxTokenFinder visit(Tree tree) {
      SyntaxTokenFinder visitor = new SyntaxTokenFinder();
      tree.accept(visitor);
      return visitor;
    }

    public SyntaxToken getLastToken() {
      return lastToken;
    }

    @Override
    public void visitBlock(BlockTree tree) {
      lastToken = tree.closeBraceToken();
    }

    @Override
    public void visitEmptyStatement(EmptyStatementTree tree) {
      lastToken = tree.semicolonToken();
    }

    @Override
    public void visitExpressionStatement(ExpressionStatementTree tree) {
      lastToken = tree.semicolonToken();
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      StatementTree elseStatement = tree.elseStatement();
      if (elseStatement != null) {
        scan(elseStatement);
      } else {
        scan(tree.thenStatement());
      }
    }

    @Override
    public void visitAssertStatement(AssertStatementTree tree) {
      lastToken = tree.semicolonToken();
    }

    @Override
    public void visitSwitchStatement(SwitchStatementTree tree) {
      lastToken = tree.closeBraceToken();
    }

    @Override
    public void visitWhileStatement(WhileStatementTree tree) {
      scan(tree.statement());
    }

    @Override
    public void visitDoWhileStatement(DoWhileStatementTree tree) {
      lastToken = tree.semicolonToken();
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      scan(tree.statement());
    }

    @Override
    public void visitForEachStatement(ForEachStatement tree) {
      scan(tree.statement());
    }

    @Override
    public void visitBreakStatement(BreakStatementTree tree) {
      lastToken = tree.semicolonToken();
    }

    @Override
    public void visitContinueStatement(ContinueStatementTree tree) {
      lastToken = tree.semicolonToken();
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      lastToken = tree.semicolonToken();
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      lastToken = tree.semicolonToken();
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
      scan(tree.block());
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      BlockTree finallyBlock = tree.finallyBlock();
      if (finallyBlock != null) {
        scan(finallyBlock);
      } else {
        List<CatchTree> catches = tree.catches();
        if (catches.isEmpty()) {
          // with try-with-resources, catches and block are optional (JLS8 14.20.3.1)
          scan(tree.block());
        } else {
          // scan only the last catch
          scan(catches.get(catches.size() - 1));
        }
      }
    }

    @Override
    public void visitCatch(CatchTree tree) {
      scan(tree.block());
    }

    @Override
    public void visitMethod(MethodTree tree) {
      SyntaxToken semicolonToken = tree.semicolonToken();
      if (semicolonToken != null) {
        lastToken = semicolonToken;
      } else {
        scan(tree.block());
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      lastToken = tree.closeBraceToken();
    }

    @Override
    public void visitVariable(VariableTree tree) {
      lastToken = tree.endToken();
    }

    @Override
    public void visitEnumConstant(EnumConstantTree tree) {
      ClassTree enumConstantBody = ((NewClassTree) tree.initializer()).classBody();
      if (enumConstantBody != null) {
        scan(enumConstantBody);
      } else {
        lastToken = tree.simpleName().identifierToken();
      }
    }
  }
}
