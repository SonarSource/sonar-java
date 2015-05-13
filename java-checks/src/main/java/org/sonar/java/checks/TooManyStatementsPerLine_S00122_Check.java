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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

@Rule(
  key = "S00122",
  name = "Statements should be on separate lines",
  tags = {"convention"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("1min")
public class TooManyStatementsPerLine_S00122_Check extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.STATIC_INITIALIZER);
  }

  @Override
  public void visitNode(Tree tree) {
    BlockTree block;
    if (tree.is(Tree.Kind.STATIC_INITIALIZER)) {
      block = (BlockTree) tree;
    } else {
      block = ((MethodTree) tree).block();
    }
    if (block != null) {
      StatementVisitor visitor = new StatementVisitor();
      block.accept(visitor);
      for (Entry<Integer> entry : visitor.statementsPerLine.entrySet()) {
        int count = entry.getCount();
        if (count > 1) {
          addIssue(entry.getElement(), "At most one statement is allowed per line, but " + count + " statements were found on this line.");
        }
      }
    }
  }

  private static class StatementVisitor extends BaseTreeVisitor {
    private final Multiset<Integer> statementsPerLine = HashMultiset.create();

    @Override
    public void visitClass(ClassTree tree) {
      // do nothing, as inner class will be visited later
    }

    @Override
    public void visitAssertStatement(AssertStatementTree tree) {
      addLines(tree.assertKeyword(), tree.semicolonToken());
    }

    @Override
    public void visitBreakStatement(BreakStatementTree tree) {
      addLines(tree.breakKeyword(), tree.semicolonToken());
    }

    @Override
    public void visitContinueStatement(ContinueStatementTree tree) {
      addLines(tree.continueKeyword(), tree.semicolonToken());
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      addLines(tree.returnKeyword(), tree.semicolonToken());
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      addLines(tree.throwKeyword(), tree.semicolonToken());
    }

    @Override
    public void visitExpressionStatement(ExpressionStatementTree tree) {
      SyntaxToken firstToken = getFirstToken(tree.expression());
      if (firstToken != null) {
        addLines(firstToken, tree.semicolonToken());
      } else {
        addLine(tree.semicolonToken());
      }
    }

    @CheckForNull
    private SyntaxToken getFirstToken(Tree expression) {
      ExpressionVisitor visitor = new ExpressionVisitor();
      expression.accept(visitor);
      return visitor.firstToken;
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      addLine(tree.ifKeyword());
      StatementTree thenStatement = tree.thenStatement();
      StatementTree elseStatement = tree.elseStatement();
      scan(thenStatement);
      scan(elseStatement);
      if (elseStatement == null) {
        addLineOfCloseBrace(tree.ifKeyword(), thenStatement);
      } else {
        addLineOfCloseBrace(tree.ifKeyword(), elseStatement);
      }
    }

    private void addLineOfCloseBrace(SyntaxToken startToken, StatementTree tree) {
      if (tree.is(Tree.Kind.BLOCK)) {
        SyntaxToken closeBraceToken = ((BlockTree) tree).closeBraceToken();
        if (startToken.line() != closeBraceToken.line() && !statementsPerLine.contains(closeBraceToken.line())) {
          addLine(closeBraceToken);
        }
      }
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
      addLine(tree.synchronizedKeyword());
      addLineOfCloseBrace(tree.synchronizedKeyword(), tree.block());
      scan(tree.block());
    }

    @Override
    public void visitSwitchStatement(SwitchStatementTree tree) {
      addLines(tree.switchKeyword(), tree.openBraceToken());
      scan(tree.cases());
      addLine(tree.closeBraceToken());
    }

    @Override
    public void visitVariable(VariableTree tree) {
      SyntaxToken endToken = tree.endToken();
      if (endToken != null && ";".equals(endToken.text())) {
        addLine(endToken);
      }
    }

    @Override
    public void visitWhileStatement(WhileStatementTree tree) {
      // do not scan the condition
      addLines(tree.whileKeyword(), tree.closeParenToken());
      addLineOfCloseBrace(tree.whileKeyword(), tree.statement());
      scan(tree.statement());
    }

    @Override
    public void visitDoWhileStatement(DoWhileStatementTree tree) {
      // do not scan the condition
      addLine(tree.doKeyword());
      if (tree.doKeyword().line() != tree.whileKeyword().line()) {
        addLines(tree.whileKeyword(), tree.semicolonToken());
      }
      scan(tree.statement());
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      // do not scan the initializer, updater and condition
      addLines(tree.forKeyword(), tree.closeParenToken());
      addLineOfCloseBrace(tree.forKeyword(), tree.statement());
      scan(tree.statement());
    }

    @Override
    public void visitForEachStatement(ForEachStatement tree) {
      // do not scan the variable and expression
      addLines(tree.forKeyword(), tree.closeParenToken());
      addLineOfCloseBrace(tree.forKeyword(), tree.statement());
      scan(tree.statement());
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      // do not scan resources
      if (tree.resources().isEmpty()) {
        addLine(tree.tryKeyword());
      } else {
        addLines(tree.tryKeyword(), tree.closeParenToken());
      }
      scan(tree.block());
      scan(tree.catches());
      if (tree.finallyKeyword() != null) {
        addLine(tree.finallyKeyword());
      }
      scan(tree.finallyBlock());
    }

    private void addLine(SyntaxToken token) {
      statementsPerLine.add(token.line());
    }

    private void addLines(SyntaxToken startToken, SyntaxToken endToken) {
      addLine(startToken);
      if (startToken.line() != endToken.line()) {
        addLine(endToken);
      }
    }
  }

  private static class ExpressionVisitor extends BaseTreeVisitor {

    private SyntaxToken firstToken;

    @Override
    public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
      scan(tree.expression());
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      scan(tree.expression());
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      if (tree.enclosingExpression() != null) {
        scan(tree.enclosingExpression());
      } else if (!tree.typeArguments().isEmpty()) {
        scan(tree.typeArguments());
      } else {
        scan(tree.identifier());
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (tree.typeArguments() != null) {
        scan(tree.typeArguments());
      } else {
        scan(tree.methodSelect());
      }
    }

    @Override
    public void visitParenthesized(ParenthesizedTree tree) {
      firstToken = tree.openParenToken();
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      scan(tree.variable());
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      if (tree.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT)) {
        scan(tree.expression());
      } else {
        firstToken = tree.operatorToken();
      }
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      firstToken = tree.identifierToken();
    }

    @Override
    public void visitTypeArguments(TypeArgumentListTreeImpl trees) {
      firstToken = trees.openBracketToken();
    }
  }
}
