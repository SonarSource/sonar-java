/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java.api.tree;

import org.sonar.java.annotations.Beta;

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

  void visitYieldStatement(YieldStatementTree tree);

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

  void visitPatternInstanceOf(PatternInstanceOfTree tree);

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

  void visitTypePattern(TypePatternTree tree);

  void visitNullPattern(NullPatternTree tree);

  void visitDefaultPattern(DefaultPatternTree tree);

  void visitGuardedPattern(GuardedPatternTree tree);

  void visitRecordPattern(RecordPatternTree tree);
}
