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
package org.sonar.plugins.java.api.tree;

import com.google.common.annotations.Beta;

/**
 * @see BaseTreeVisitor
 */
@Beta
public interface TreeVisitor {

  void visitCompilationUnit(CompilationUnitTree tree);

  void visitImport(ImportTree tree);

  void visitClass(ClassTree tree);

  void visitMethod(MethodTree tree);

  void visitBlock(BlockTree tree);

  void visitEmptyStatement(EmptyStatementTree tree);

  void visitLabeledStatement(LabeledStatementTree tree);

  void visitExpressionStatement(ExpressionStatementTree tree);

  void visitIfStatement(IfStatementTree tree);

  void visitAssertStatement(AssertStatementTree tree);

  void visitSwitchStatement(SwitchStatementTree tree);

  void visitSwitchExpression(SwitchExpressionTree tree);

  void visitCaseGroup(CaseGroupTree tree);

  void visitCaseLabel(CaseLabelTree tree);

  void visitWhileStatement(WhileStatementTree tree);

  void visitDoWhileStatement(DoWhileStatementTree tree);

  void visitForStatement(ForStatementTree tree);

  void visitForEachStatement(ForEachStatement tree);

  void visitBreakStatement(BreakStatementTree tree);

  void visitContinueStatement(ContinueStatementTree tree);

  void visitReturnStatement(ReturnStatementTree tree);

  void visitThrowStatement(ThrowStatementTree tree);

  void visitSynchronizedStatement(SynchronizedStatementTree tree);

  void visitTryStatement(TryStatementTree tree);

  void visitCatch(CatchTree tree);

  void visitUnaryExpression(UnaryExpressionTree tree);

  void visitBinaryExpression(BinaryExpressionTree tree);

  void visitConditionalExpression(ConditionalExpressionTree tree);

  void visitArrayAccessExpression(ArrayAccessExpressionTree tree);

  void visitMemberSelectExpression(MemberSelectExpressionTree tree);

  void visitNewClass(NewClassTree tree);

  void visitNewArray(NewArrayTree tree);

  void visitMethodInvocation(MethodInvocationTree tree);

  void visitTypeCast(TypeCastTree tree);

  void visitInstanceOf(InstanceOfTree tree);

  void visitParenthesized(ParenthesizedTree tree);

  void visitAssignmentExpression(AssignmentExpressionTree tree);

  void visitLiteral(LiteralTree tree);

  void visitIdentifier(IdentifierTree tree);

  void visitVarType(VarTypeTree tree);

  void visitVariable(VariableTree tree);

  void visitEnumConstant(EnumConstantTree tree);

  void visitPrimitiveType(PrimitiveTypeTree tree);

  void visitArrayType(ArrayTypeTree tree);

  void visitParameterizedType(ParameterizedTypeTree tree);

  void visitWildcard(WildcardTree tree);

  void visitUnionType(UnionTypeTree tree);

  void visitModifier(ModifiersTree modifiersTree);

  void visitAnnotation(AnnotationTree annotationTree);

  void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree);

  void visitTypeParameter(TypeParameterTree typeParameter);

  void visitTypeArguments(TypeArguments trees);

  void visitTypeParameters(TypeParameters trees);

  void visitOther(Tree tree);

  void visitMethodReference(MethodReferenceTree methodReferenceTree);

  void visitPackage(PackageDeclarationTree tree);

  void visitModule(ModuleDeclarationTree module);

  void visitRequiresDirectiveTree(RequiresDirectiveTree tree);

  void visitExportsDirectiveTree(ExportsDirectiveTree tree);

  void visitOpensDirective(OpensDirectiveTree tree);

  void visitUsesDirective(UsesDirectiveTree tree);

  void visitProvidesDirective(ProvidesDirectiveTree tree);

  void visitArrayDimension(ArrayDimensionTree tree);
}
