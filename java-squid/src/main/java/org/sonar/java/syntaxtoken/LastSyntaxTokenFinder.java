/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.syntaxtoken;

import com.google.common.collect.Iterables;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.EmptyStatementTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import javax.annotation.Nullable;

import java.util.List;

public class LastSyntaxTokenFinder extends BaseTreeVisitor {

  private LastSyntaxTokenFinder() {
  }

  private SyntaxToken lastSyntaxToken;

  /**
   * @param tree the tree to visit to get its last syntax token
   * @return the last syntax token of the tree, or null if the provided tree is:
   * <ul>
   *   <li>Empty list of modifiers ({@link org.sonar.plugins.java.api.tree.ModifiersTree})</li>
   *   <li>Any tree of Kind "OTHER" ({@link org.sonar.plugins.java.api.tree.Tree.Kind.OTHER})</li>
   * </ul>
   */
  @Nullable
  public static SyntaxToken lastSyntaxToken(Tree tree) {
    if (tree.is(Tree.Kind.TOKEN)) {
      return (SyntaxToken) tree;
    }
    LastSyntaxTokenFinder visitor = new LastSyntaxTokenFinder();
    tree.accept(visitor);
    return visitor.lastSyntaxToken;
  }

  @Override
  public void visitBlock(BlockTree tree) {
    lastSyntaxToken = tree.closeBraceToken();
  }

  @Override
  public void visitEmptyStatement(EmptyStatementTree tree) {
    lastSyntaxToken = tree.semicolonToken();
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    lastSyntaxToken = tree.semicolonToken();
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
    lastSyntaxToken = tree.semicolonToken();
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    lastSyntaxToken = tree.closeBraceToken();
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    scan(tree.statement());
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    lastSyntaxToken = tree.semicolonToken();
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
    lastSyntaxToken = tree.semicolonToken();
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    lastSyntaxToken = tree.semicolonToken();
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    lastSyntaxToken = tree.semicolonToken();
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    lastSyntaxToken = tree.semicolonToken();
  }

  @Override
  public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
    // avoid visiting the expression
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
      lastSyntaxToken = semicolonToken;
    } else {
      scan(tree.block());
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    lastSyntaxToken = tree.closeBraceToken();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    SyntaxToken endToken = tree.endToken();
    if(endToken == null) {
      ExpressionTree initializer = tree.initializer();
      if(initializer == null) {
        scan(tree.simpleName());
      } else {
        scan(initializer);
      }
    } else {
      lastSyntaxToken = endToken;
    }
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    if (tree.separatorToken() != null) {
      lastSyntaxToken = tree.separatorToken();
    } else {
      scan(tree.initializer());
    }
  }

  @Override
  public void visitCaseLabel(CaseLabelTree tree) {
    lastSyntaxToken = tree.colonToken();
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    scan(tree.rightOperand());
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    lastSyntaxToken = tree.token();
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    lastSyntaxToken = tree.identifierToken();
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    lastSyntaxToken = tree.eofToken();
  }

  @Override
  public void visitImport(ImportTree tree) {
    lastSyntaxToken = tree.semicolonToken();
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    scan(tree.statement());
  }

  @Override
  public void visitCaseGroup(CaseGroupTree tree) {
    if (!tree.body().isEmpty()) {
      scan(Iterables.getLast(tree.body()));
    } else {
      scan(Iterables.getLast(tree.labels()));
    }
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if (tree.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT)) {
      lastSyntaxToken = tree.operatorToken();
    } else {
      scan(tree.expression());
    }
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    scan(tree.falseExpression());
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    scan(tree.dimension());
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    scan(tree.identifier());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.classBody() != null) {
      scan(tree.classBody());
    } else if (tree.arguments().closeParenToken() != null) {
      lastSyntaxToken = tree.arguments().closeParenToken();
    } else {
      scan(tree.identifier());
    }
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    SyntaxToken closeBraceToken = tree.closeBraceToken();
    if (closeBraceToken != null) {
      lastSyntaxToken = closeBraceToken;
    } else {
      scan(Iterables.getLast(tree.dimensions()));
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    lastSyntaxToken = tree.arguments().closeParenToken();
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    scan(tree.type());
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    lastSyntaxToken = tree.closeParenToken();
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitPrimitiveType(PrimitiveTypeTree tree) {
    lastSyntaxToken = tree.keyword();
  }

  @Override
  public void visitArrayType(ArrayTypeTree tree) {
    if (tree.ellipsisToken() != null) {
      lastSyntaxToken = tree.ellipsisToken();
    } else {
      lastSyntaxToken = tree.closeBracketToken();
    }
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    lastSyntaxToken = tree.typeArguments().closeBracketToken();
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    if (tree.bound() != null) {
      scan(tree.bound());
    } else {
      lastSyntaxToken = tree.queryToken();
    }
  }

  @Override
  public void visitUnionType(UnionTypeTree tree) {
    scan(Iterables.getLast(tree.typeAlternatives()));
  }

  @Override
  public void visitModifier(ModifiersTree modifiersTree) {
    // if no modifier, firstSyntaxToken will be null
    if (!modifiersTree.isEmpty()) {
      ModifierTree lastModifier = Iterables.getLast(modifiersTree);
      if (lastModifier.is(Tree.Kind.ANNOTATION)) {
        scan(lastModifier);
      } else {
        lastSyntaxToken = ((ModifierKeywordTree) lastModifier).keyword();
      }
    }
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    if (annotationTree.arguments().closeParenToken() != null) {
      lastSyntaxToken = annotationTree.arguments().closeParenToken();
    } else {
      scan(annotationTree.annotationType());
    }
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    scan(lambdaExpressionTree.body());
  }

  @Override
  public void visitTypeParameter(TypeParameterTree typeParameter) {
    if (!typeParameter.bounds().isEmpty()) {
      scan(Iterables.getLast(typeParameter.bounds()));
    } else {
      scan(typeParameter.identifier());
    }
  }

  @Override
  public void visitTypeArguments(TypeArgumentListTreeImpl trees) {
    lastSyntaxToken = trees.closeBracketToken();
  }

  @Override
  public void visitTypeParameters(TypeParameters trees) {
    lastSyntaxToken = trees.closeBracketToken();
  }

  @Override
  public void visitMethodReference(MethodReferenceTree methodReferenceTree) {
    scan(methodReferenceTree.method());
  }

  @Override
  public void visitOther(Tree tree) {
    // lastSyntaxToken will be null
  }

  @Override
  public void visitPackage(PackageDeclarationTree tree) {
    lastSyntaxToken = tree.semicolonToken();
  }

  @Override
  public void visitArrayDimension(ArrayDimensionTree tree) {
    lastSyntaxToken = tree.closeBracketToken();
  }
}
