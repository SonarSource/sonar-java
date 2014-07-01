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
package org.sonar.plugins.java.api.tree;

import com.google.common.annotations.Beta;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Default implementation of {@link TreeVisitor}.
 */
@Beta
public class BaseTreeVisitor implements TreeVisitor {

  protected void scan(List<? extends Tree> trees) {
    for (Tree tree : trees) {
      scan(tree);
    }
  }

  protected void scan(@Nullable Tree tree) {
    if (tree != null) {
      tree.accept(this);
    }
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    scan(tree.packageAnnotations());
    scan(tree.packageName());
    scan(tree.imports());
    scan(tree.types());
  }

  @Override
  public void visitImport(ImportTree tree) {
    scan(tree.qualifiedIdentifier());
  }

  @Override
  public void visitClass(ClassTree tree) {
    scan(tree.modifiers());
    scan(tree.typeParameters());
    scan(tree.superClass());
    scan(tree.superInterfaces());
    scan(tree.members());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    scan(tree.modifiers());
    scan(tree.typeParameters());
    scan(tree.returnType());
    scan(tree.parameters());
    scan(tree.defaultValue());
    scan(tree.block());
  }

  @Override
  public void visitBlock(BlockTree tree) {
    scan(tree.body());
  }

  @Override
  public void visitEmptyStatement(EmptyStatementTree tree) {
    // no subtrees
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    scan(tree.label());
    scan(tree.statement());
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    scan(tree.condition());
    scan(tree.thenStatement());
    scan(tree.elseStatement());
  }

  @Override
  public void visitAssertStatement(AssertStatementTree tree) {
    scan(tree.condition());
    scan(tree.detail());
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    scan(tree.expression());
    scan(tree.cases());
  }

  @Override
  public void visitCaseGroup(CaseGroupTree tree) {
    scan(tree.labels());
    scan(tree.body());
  }

  @Override
  public void visitCaseLabel(CaseLabelTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    scan(tree.condition());
    scan(tree.statement());
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    scan(tree.statement());
    scan(tree.condition());
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    scan(tree.initializer());
    scan(tree.condition());
    scan(tree.update());
    scan(tree.statement());
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    scan(tree.variable());
    scan(tree.expression());
    scan(tree.statement());
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    scan(tree.label());
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    scan(tree.label());
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
    scan(tree.expression());
    scan(tree.block());
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    scan(tree.resources());
    scan(tree.block());
    scan(tree.catches());
    scan(tree.finallyBlock());
  }

  @Override
  public void visitCatch(CatchTree tree) {
    scan(tree.parameter());
    scan(tree.block());
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    scan(tree.leftOperand());
    scan(tree.rightOperand());
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    scan(tree.condition());
    scan(tree.trueExpression());
    scan(tree.falseExpression());
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    scan(tree.expression());
    scan(tree.index());
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    scan(tree.expression());
    scan(tree.identifier());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    scan(tree.enclosingExpression());
    scan(tree.identifier());
    scan(tree.typeArguments());
    scan(tree.arguments());
    scan(tree.classBody());
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    scan(tree.type());
    scan(tree.dimensions());
    scan(tree.initializers());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    scan(tree.methodSelect());
    scan(tree.typeArguments());
    scan(tree.arguments());
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    scan(tree.type());
    scan(tree.expression());
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    scan(tree.expression());
    scan(tree.type());
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    scan(tree.variable());
    scan(tree.expression());
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    // no subtrees
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    // no subtrees
  }

  @Override
  public void visitVariable(VariableTree tree) {
    scan(tree.modifiers());
    scan(tree.type());
    scan(tree.initializer());
  }

  public void visitPrimitiveType(PrimitiveTypeTree tree) {
    // no subtrees
  }

  public void visitArrayType(ArrayTypeTree tree) {
    scan(tree.type());
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    scan(tree.modifiers());
    scan(tree.initializer());
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    scan(tree.type());
    scan(tree.typeArguments());
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    scan(tree.bound());
  }

  @Override
  public void visitUnionType(UnionTypeTree tree) {
    scan(tree.typeAlternatives());
  }

  @Override
  public void visitModifier(ModifiersTree modifiersTree) {
    scan(modifiersTree.annotations());
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    scan(annotationTree.annotationType());
    scan(annotationTree.arguments());
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    scan(lambdaExpressionTree.parameters());
    scan(lambdaExpressionTree.body());
  }

  @Override
  public void visitOther(Tree tree) {
   //nop
  }

}
