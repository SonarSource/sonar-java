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

import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
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
import org.sonar.plugins.java.api.tree.StaticInitializerTree;
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class FirstSyntaxTokenFinder extends BaseTreeVisitor {

  private FirstSyntaxTokenFinder() {
  }

  private SyntaxToken firstSyntaxToken;

  /**
   * @param tree the tree to visit to get the first syntax token
   * @return the first syntax token of the tree, or null if the provided tree is:
   * <ul>
   *   <li>Empty list of modifiers ({@link org.sonar.plugins.java.api.tree.ModifiersTree})</li>
   *   <li>Any tree of Kind "OTHER" ({@link org.sonar.plugins.java.api.tree.Tree.Kind.OTHER})</li>
   * </ul>
   */
  @Nullable
  public static SyntaxToken firstSyntaxToken(Tree tree) {
    if (tree.is(Tree.Kind.TOKEN)) {
      return (SyntaxToken) tree;
    }
    FirstSyntaxTokenFinder visitor = new FirstSyntaxTokenFinder();
    tree.accept(visitor);
    return visitor.firstSyntaxToken;
  }

  @Override
  public void visitBlock(BlockTree tree) {
    if (tree.is(Tree.Kind.STATIC_INITIALIZER)) {
      firstSyntaxToken = ((StaticInitializerTree) tree).staticKeyword();
    } else {
      firstSyntaxToken = tree.openBraceToken();
    }
  }

  @Override
  public void visitEmptyStatement(EmptyStatementTree tree) {
    firstSyntaxToken = tree.semicolonToken();
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    scan(tree.variable());
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    scan(tree.methodSelect());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    firstSyntaxToken = tree.ifKeyword();
  }

  @Override
  public void visitAssertStatement(AssertStatementTree tree) {
    firstSyntaxToken = tree.assertKeyword();
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    firstSyntaxToken = tree.switchKeyword();
  }

  @Override
  public void visitCaseLabel(CaseLabelTree tree) {
    firstSyntaxToken = tree.caseOrDefaultKeyword();
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    firstSyntaxToken = tree.whileKeyword();
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    firstSyntaxToken = tree.doKeyword();
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    firstSyntaxToken = tree.forKeyword();
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    firstSyntaxToken = tree.forKeyword();
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    firstSyntaxToken = tree.breakKeyword();
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    firstSyntaxToken = tree.continueKeyword();
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    firstSyntaxToken = tree.returnKeyword();
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    firstSyntaxToken = tree.throwKeyword();
  }

  @Override
  public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
    firstSyntaxToken = tree.synchronizedKeyword();
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    firstSyntaxToken = tree.tryKeyword();
  }

  @Override
  public void visitCatch(CatchTree tree) {
    firstSyntaxToken = tree.catchKeyword();
  }

  @Override
  public void visitMethod(MethodTree tree) {
    SyntaxToken firstModifierToken = getFirstModifierToken(tree.modifiers());
    if (firstModifierToken != null) {
      firstSyntaxToken = firstModifierToken;
    } else {
      TypeParameters typeParameters = tree.typeParameters();
      if (!typeParameters.isEmpty()) {
        firstSyntaxToken = typeParameters.openBracketToken();
      } else if (tree.returnType() != null) {
        scan(tree.returnType());
      } else {
        firstSyntaxToken = tree.simpleName().identifierToken();
      }
    }
  }

  @CheckForNull
  private static SyntaxToken getFirstModifierToken(ModifiersTree modifiers) {
    if (!modifiers.isEmpty()) {
      ModifierTree firstModifier = modifiers.get(0);
      if (firstModifier.is(Tree.Kind.ANNOTATION)) {
        return ((AnnotationTree) firstModifier).atToken();
      } else {
        return ((ModifierKeywordTree) firstModifier).keyword();
      }
    }
    return null;
  }

  @Override
  public void visitClass(ClassTree tree) {
    SyntaxToken firstModifierToken = getFirstModifierToken(tree.modifiers());
    if (firstModifierToken != null) {
      firstSyntaxToken = firstModifierToken;
    } else if (tree.declarationKeyword() != null) {
      firstSyntaxToken = tree.declarationKeyword();
    } else {
      // case of anonymous classes
      firstSyntaxToken = tree.openBraceToken();
    }
  }

  @Override
  public void visitVariable(VariableTree tree) {
    SyntaxToken firstModifierToken = getFirstModifierToken(tree.modifiers());
    if (firstModifierToken != null) {
      firstSyntaxToken = firstModifierToken;
    } else if (!tree.type().is(Tree.Kind.INFERED_TYPE)) {
      scan(tree.type());
    } else {
      scan(tree.simpleName());
    }
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    if (!tree.annotations().isEmpty()) {
      scan(tree.annotations());
    } else {
      scan(tree.type());
    }
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if (tree.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT)) {
      scan(tree.expression());
    } else {
      firstSyntaxToken = tree.operatorToken();
    }
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    firstSyntaxToken = tree.label().identifierToken();
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    firstSyntaxToken = tree.simpleName().identifierToken();
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    if (!tree.annotations().isEmpty()) {
      scan(tree.annotations());
    } else {
      scan(tree.expression());
    }
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    if (!tree.annotations().isEmpty()) {
      scan(tree.annotations());
    } else {
      firstSyntaxToken = tree.identifierToken();
    }
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    firstSyntaxToken = tree.token();
  }

  @Override
  public void visitPrimitiveType(PrimitiveTypeTree tree) {
    if (!tree.annotations().isEmpty()) {
      scan(tree.annotations());
    } else {
      firstSyntaxToken = tree.keyword();
    }
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.enclosingExpression() != null) {
      scan(tree.enclosingExpression());
    } else {
      firstSyntaxToken = tree.newKeyword();
    }
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    firstSyntaxToken = tree.openParenToken();
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    scan(tree.leftOperand());
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    if (tree.packageDeclaration() != null) {
      scan(tree.packageDeclaration());
    } else if (!tree.imports().isEmpty()) {
      scan(tree.imports().get(0));
    } else if (!tree.types().isEmpty()) {
      scan(tree.types().get(0));
    } else {
      firstSyntaxToken = tree.eofToken();
    }
  }

  @Override
  public void visitImport(ImportTree tree) {
    firstSyntaxToken = tree.importKeyword();
  }

  @Override
  public void visitCaseGroup(CaseGroupTree tree) {
    scan(tree.labels().get(0));
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    scan(tree.condition());
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    firstSyntaxToken = tree.newKeyword();
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    firstSyntaxToken = tree.openParenToken();
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitArrayType(ArrayTypeTree tree) {
    scan(tree.type());
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    if (!tree.annotations().isEmpty()) {
      scan(tree.annotations());
    } else {
      firstSyntaxToken = tree.queryToken();
    }
  }

  @Override
  public void visitUnionType(UnionTypeTree tree) {
    scan(tree.typeAlternatives().get(0));
  }

  @Override
  public void visitModifier(ModifiersTree modifiersTree) {
    // if no modifier, firstSyntaxToken will be null
    firstSyntaxToken = getFirstModifierToken(modifiersTree);
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    firstSyntaxToken = annotationTree.atToken();
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    if (lambdaExpressionTree.openParenToken() != null) {
      firstSyntaxToken = lambdaExpressionTree.openParenToken();
    } else {
      scan(lambdaExpressionTree.parameters().get(0));
    }
  }

  @Override
  public void visitTypeParameter(TypeParameterTree typeParameter) {
    scan(typeParameter.identifier());
  }

  @Override
  public void visitTypeArguments(TypeArgumentListTreeImpl trees) {
    firstSyntaxToken = trees.openBracketToken();
  }

  @Override
  public void visitTypeParameters(TypeParameters trees) {
    firstSyntaxToken = trees.openBracketToken();
  }

  @Override
  public void visitMethodReference(MethodReferenceTree methodReferenceTree) {
    scan(methodReferenceTree.expression());
  }

  @Override
  public void visitOther(Tree tree) {
    // firstSyntaxToken will be null
  }

  @Override
  public void visitPackage(PackageDeclarationTree tree) {
    if (!tree.annotations().isEmpty()) {
      scan(tree.annotations().get(0));
    } else {
      firstSyntaxToken = tree.packageKeyword();
    }
  }
}
