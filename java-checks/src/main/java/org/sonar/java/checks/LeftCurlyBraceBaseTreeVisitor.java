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

import com.google.common.collect.Iterables;

import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.StaticInitializerTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import javax.annotation.CheckForNull;

import java.util.List;

public abstract class LeftCurlyBraceBaseTreeVisitor extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  protected void addIssue(SyntaxToken openBraceToken, JavaCheck check, String message) {
    this.context.reportIssue( check, openBraceToken, message);
  }

  protected abstract void checkTokens(SyntaxToken lastToken, SyntaxToken openBraceToken);

  @Override
  public void visitClass(ClassTree tree) {
    SyntaxToken lastToken = getLastTokenFromSignature(tree);
    if (lastToken != null) {
      checkTokens(lastToken, tree.openBraceToken());
    }
    super.visitClass(tree);
  }

  @CheckForNull
  private static SyntaxToken getLastTokenFromSignature(ClassTree classTree) {
    List<TypeTree> superInterfaces = classTree.superInterfaces();
    if (!superInterfaces.isEmpty()) {
      return getIdentifierToken(Iterables.getLast(superInterfaces));
    }
    TypeTree superClass = classTree.superClass();
    if (superClass != null) {
      return getIdentifierToken(superClass);
    }
    TypeParameters typeParameters = classTree.typeParameters();
    if (!typeParameters.isEmpty()) {
      return typeParameters.closeBracketToken();
    }
    IdentifierTree simpleName = classTree.simpleName();
    if (simpleName != null) {
      return simpleName.identifierToken();
    }
    // enum constants and new class trees are handled separately
    return null;
  }

  private static SyntaxToken getIdentifierToken(TypeTree typeTree) {
    if (typeTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) typeTree).identifierToken();
    } else if (typeTree.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) typeTree).identifier().identifierToken();
    } else {
      return ((ParameterizedTypeTree) typeTree).typeArguments().closeBracketToken();
    }
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    checkBlock(tree.closeParenToken(), tree.thenStatement());
    if (tree.elseKeyword() != null) {
      checkBlock(tree.elseKeyword(), tree.elseStatement());
    }
    super.visitIfStatement(tree);
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    checkTokens(tree.closeParenToken(), tree.openBraceToken());
    super.visitSwitchStatement(tree);
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    checkBlock(tree.closeParenToken(), tree.statement());
    super.visitWhileStatement(tree);
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    checkBlock(tree.doKeyword(), tree.statement());
    super.visitDoWhileStatement(tree);
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    checkBlock(tree.closeParenToken(), tree.statement());
    super.visitForStatement(tree);
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    checkBlock(tree.closeParenToken(), tree.statement());
    super.visitForEachStatement(tree);
  }

  @Override
  public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
    checkBlock(tree.closeParenToken(), tree.block());
    super.visitSynchronizedStatement(tree);
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    checkBlock(tree.colonToken(), tree.statement());
    super.visitLabeledStatement(tree);
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    SyntaxToken closeParenToken = tree.closeParenToken();
    if (closeParenToken != null) {
      checkBlock(closeParenToken, tree.block());
    } else {
      checkBlock(tree.tryKeyword(), tree.block());
    }
    SyntaxToken finallyKeyword = tree.finallyKeyword();
    if (finallyKeyword != null) {
      checkBlock(finallyKeyword, tree.finallyBlock());
    }
    super.visitTryStatement(tree);
  }

  @Override
  public void visitCatch(CatchTree tree) {
    checkBlock(tree.closeParenToken(), tree.block());
    super.visitCatch(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    ClassTree classBody = tree.classBody();
    if (classBody != null && tree.arguments().closeParenToken() != null) {
      checkTokens(tree.arguments().closeParenToken(), classBody.openBraceToken());
    }
    super.visitNewClass(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    BlockTree blockTree = tree.block();
    if (blockTree != null) {
      checkTokens(getLastTokenFromSignature(tree), blockTree.openBraceToken());
    }
    super.visitMethod(tree);
  }

  private static SyntaxToken getLastTokenFromSignature(MethodTree methodTree) {
    if (methodTree.throwsClauses().isEmpty()) {
      return methodTree.closeParenToken();
    } else {
      return getIdentifierToken(Iterables.getLast(methodTree.throwsClauses()));
    }
  }

  @Override
  public void visitBlock(BlockTree tree) {
    if (tree.is(Tree.Kind.STATIC_INITIALIZER)) {
      StaticInitializerTree staticInitializerTree = (StaticInitializerTree) tree;
      checkTokens(staticInitializerTree.staticKeyword(), staticInitializerTree.openBraceToken());
    }
    super.visitBlock(tree);
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    NewClassTree initializer = tree.initializer();
    ClassTree classBody = initializer.classBody();
    if (classBody != null) {
      SyntaxToken openBraceToken = classBody.openBraceToken();
      if (initializer.arguments().closeParenToken() != null) {
        checkTokens(initializer.arguments().closeParenToken(), openBraceToken);
      } else {
        checkTokens(tree.simpleName().identifierToken(), openBraceToken);
      }
    }
    super.visitEnumConstant(tree);
  }

  private void checkBlock(SyntaxToken previousToken, Tree tree) {
    if (tree.is(Tree.Kind.BLOCK)) {
      checkTokens(previousToken, ((BlockTree) tree).openBraceToken());
    }
  }
}
