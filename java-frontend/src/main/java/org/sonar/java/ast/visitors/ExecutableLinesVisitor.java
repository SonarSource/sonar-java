/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.ast.visitors;

import com.google.common.base.Throwables;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
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

/**
 * Saves information about lines directly into Sonar by using {@link FileLinesContext}.
 */
public class ExecutableLinesVisitor extends SubscriptionVisitor {

  private final SonarComponents sonarComponents;
  private final Charset charset;
  private final Set<Integer> executableLines = Sets.newHashSet();

  public ExecutableLinesVisitor(SonarComponents sonarComponents, Charset charset) {
    this.sonarComponents = sonarComponents;
    this.charset = charset;
  }

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
        if (count > 0) {
          executableLines.add(entry.getElement());
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
      SyntaxToken firstToken = tree.firstToken();
      if (firstToken != null) {
        addLines(firstToken, tree.semicolonToken());
      } else {
        addLine(tree.semicolonToken());
      }
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

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);

    FileLinesContext fileLinesContext = sonarComponents.fileLinesContextFor(context.getFile());
    // TODO minimize access to files, another one in LinesVisitor
    int fileLength;
    try {
      fileLength = Files.readLines(context.getFile(), charset).size();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
    for (int line = 1; line <= fileLength; line++) {
      fileLinesContext.setIntValue("executable_lines_data", line, executableLines.contains(line) ? 1 : 0);
    }
    fileLinesContext.save();

    executableLines.clear();
  }

}
