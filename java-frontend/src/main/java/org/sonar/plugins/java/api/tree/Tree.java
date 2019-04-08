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
import javax.annotation.Nullable;
import org.sonar.sslr.grammar.GrammarRuleKey;

/**
 * Common interface for all nodes in a syntax tree.
 *
 * <p><b>WARNING:</b> This interface and its sub-interfaces are subject to change as the Java&trade; language evolves.</p>
 */
@Beta
public interface Tree {

  boolean is(Kind... kinds);

  void accept(TreeVisitor visitor);

  @Nullable
  Tree parent();

  @Nullable
  SyntaxToken firstToken();

  @Nullable
  SyntaxToken lastToken();

  enum Kind implements GrammarRuleKey {
    /**
     * {@link CompilationUnitTree}
     */
    COMPILATION_UNIT(CompilationUnitTree.class),

    /**
     * {@link ClassTree}
     */
    CLASS(ClassTree.class),

    /**
     * {@link ClassTree}
     *
     * @since Java 1.5
     */
    ENUM(ClassTree.class),

    /**
     * {@link ClassTree}
     */
    INTERFACE(ClassTree.class),

    /**
     * {@link ClassTree}
     *
     * @since Java 1.5
     */
    ANNOTATION_TYPE(ClassTree.class),

    /**
     * {@link EnumConstantTree}
     *
     * @since Java 1.5
     */
    ENUM_CONSTANT(EnumConstantTree.class),

    /**
     * {@link BlockTree}
     */
    INITIALIZER(BlockTree.class),

    /**
     * {@link StaticInitializerTree}
     */
    STATIC_INITIALIZER(StaticInitializerTree.class),

    /**
     * {@link MethodTree}
     */
    CONSTRUCTOR(MethodTree.class),

    /**
     * {@link MethodTree}
     */
    METHOD(MethodTree.class),

    /**
     * {@link BlockTree}
     */
    BLOCK(BlockTree.class),

    /**
     * {@link EmptyStatementTree}
     */
    EMPTY_STATEMENT(EmptyStatementTree.class),

    /**
     * {@link LabeledStatementTree}
     */
    LABELED_STATEMENT(LabeledStatementTree.class),

    /**
     * {@link ExpressionStatementTree}
     */
    EXPRESSION_STATEMENT(ExpressionStatementTree.class),

    /**
     * {@link IfStatementTree}
     */
    IF_STATEMENT(IfStatementTree.class),

    /**
     * {@link AssertStatementTree}
     *
     * @since Java 1.4
     */
    ASSERT_STATEMENT(AssertStatementTree.class),

    /**
     * {@link SwitchStatementTree}
     */
    SWITCH_STATEMENT(SwitchStatementTree.class),

    /**
     * {@link SwitchExpressionTree}
     * @since SonarJava 5.12: Support of Java 12
     */
    SWITCH_EXPRESSION(SwitchExpressionTree.class),

    /**
     * {@link CaseGroupTree}
     */
    CASE_GROUP(CaseGroupTree.class),

    /**
     * {@link CaseLabelTree}
     */
    CASE_LABEL(CaseLabelTree.class),

    /**
     * {@link WhileStatementTree}
     */
    WHILE_STATEMENT(WhileStatementTree.class),

    /**
     * {@link DoWhileStatementTree}
     */
    DO_STATEMENT(DoWhileStatementTree.class),

    /**
     * {@link ForStatementTree}
     */
    FOR_STATEMENT(ForStatementTree.class),

    /**
     * {@link ForEachStatement}
     *
     * @since Java 1.5
     */
    FOR_EACH_STATEMENT(ForEachStatement.class),

    /**
     * {@link BreakStatementTree}
     */
    BREAK_STATEMENT(BreakStatementTree.class),

    /**
     * {@link ContinueStatementTree}
     */
    CONTINUE_STATEMENT(ContinueStatementTree.class),

    /**
     * {@link ReturnStatementTree}
     */
    RETURN_STATEMENT(ReturnStatementTree.class),

    /**
     * {@link ThrowStatementTree}
     */
    THROW_STATEMENT(ThrowStatementTree.class),

    /**
     * {@link SynchronizedStatementTree}
     */
    SYNCHRONIZED_STATEMENT(SynchronizedStatementTree.class),

    /**
     * {@link TryStatementTree}
     */
    TRY_STATEMENT(TryStatementTree.class),

    /**
     * {@link CatchTree}
     */
    CATCH(CatchTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code ++}
     */
    POSTFIX_INCREMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code --}
     */
    POSTFIX_DECREMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code ++}
     */
    PREFIX_INCREMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code --}
     */
    PREFIX_DECREMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code +}
     */
    UNARY_PLUS(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code -}
     */
    UNARY_MINUS(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code ~}
     */
    BITWISE_COMPLEMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code !}
     */
    LOGICAL_COMPLEMENT(UnaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code *}
     */
    MULTIPLY(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code /}
     */
    DIVIDE(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code %}
     */
    REMAINDER(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code +}
     */
    PLUS(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code -}
     */
    MINUS(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code <<}
     */
    LEFT_SHIFT(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code >>}
     */
    RIGHT_SHIFT(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code >>>}
     */
    UNSIGNED_RIGHT_SHIFT(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code <}
     */
    LESS_THAN(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code >}
     */
    GREATER_THAN(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code <=}
     */
    LESS_THAN_OR_EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code >=}
     */
    GREATER_THAN_OR_EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code ==}
     */
    EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code !=}
     */
    NOT_EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code &}
     */
    AND(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code ^}
     */
    XOR(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code |}
     */
    OR(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code &&}
     */
    CONDITIONAL_AND(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code ||}
     */
    CONDITIONAL_OR(BinaryExpressionTree.class),

    /**
     * {@link ConditionalExpressionTree}
     */
    CONDITIONAL_EXPRESSION(ConditionalExpressionTree.class),

    /**
     * {@link ArrayAccessExpressionTree}
     */
    ARRAY_ACCESS_EXPRESSION(ArrayAccessExpressionTree.class),

    /**
     * {@link MemberSelectExpressionTree}
     */
    MEMBER_SELECT(MemberSelectExpressionTree.class),

    /**
     * {@link NewClassTree}
     */
    NEW_CLASS(NewClassTree.class),

    /**
     * {@link NewArrayTree}
     */
    NEW_ARRAY(NewArrayTree.class),

    /**
     * {@link MethodInvocationTree}
     */
    METHOD_INVOCATION(MethodInvocationTree.class),

    /**
     * {@link TypeCastTree}
     */
    TYPE_CAST(TypeCastTree.class),

    /**
     * {@link InstanceOfTree}
     */
    INSTANCE_OF(InstanceOfTree.class),

    /**
     * {@link ParenthesizedTree}
     */
    PARENTHESIZED_EXPRESSION(ParenthesizedTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code =}
     */
    ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code *=}
     */
    MULTIPLY_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code /=}
     */
    DIVIDE_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code %=}
     */
    REMAINDER_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code +=}
     */
    PLUS_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code -=}
     */
    MINUS_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code <<=}
     */
    LEFT_SHIFT_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code >>=}
     */
    RIGHT_SHIFT_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code >>>=}
     */
    UNSIGNED_RIGHT_SHIFT_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code &=}
     */
    AND_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code ^=}
     */
    XOR_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code |=}
     */
    OR_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link LiteralTree}
     * {@code int}
     */
    INT_LITERAL(LiteralTree.class),

    /**
     * {@link LiteralTree}
     * {@code long}
     */
    LONG_LITERAL(LiteralTree.class),

    /**
     * {@link LiteralTree}
     * {@code float}
     */
    FLOAT_LITERAL(LiteralTree.class),

    /**
     * {@link LiteralTree}
     * {@code double}
     */
    DOUBLE_LITERAL(LiteralTree.class),

    /**
     * {@link LiteralTree}
     * {@code boolean}
     */
    BOOLEAN_LITERAL(LiteralTree.class),

    /**
     * {@link LiteralTree}
     * {@code char}
     */
    CHAR_LITERAL(LiteralTree.class),

    /**
     * {@link LiteralTree}
     */
    STRING_LITERAL(LiteralTree.class),

    /**
     * {@link LiteralTree}
     * {@code null}
     */
    NULL_LITERAL(LiteralTree.class),

    /**
     * {@link IdentifierTree}
     */
    IDENTIFIER(IdentifierTree.class),

    /**
     * {@link VarTypeTree}
     */
    VAR_TYPE(VarTypeTree.class),

    /**
     * {@link VariableTree}
     */
    VARIABLE(VariableTree.class),

    /**
     * {@link ArrayTypeTree}
     */
    ARRAY_TYPE(ArrayTypeTree.class),

    /**
     * {@link ParameterizedTypeTree}
     *
     * @since Java 1.5
     */
    PARAMETERIZED_TYPE(ParameterizedTypeTree.class),

    /**
     * @since Java 1.7
     */
    UNION_TYPE(UnionTypeTree.class),

    /**
     * {@link WildcardTree}
     *
     * @since Java 1.5
     */
    UNBOUNDED_WILDCARD(WildcardTree.class),

    /**
     * {@link WildcardTree}
     *
     * @since Java 1.5
     */
    EXTENDS_WILDCARD(WildcardTree.class),

    /**
     * {@link WildcardTree}
     *
     * @since Java 1.5
     */
    SUPER_WILDCARD(WildcardTree.class),

    /**
     * {@link AnnotationTree}
     *
     * @since Java 1.5
     */
    ANNOTATION(AnnotationTree.class),

    /**
     * {@link ModifiersTree}
     *
     */
    MODIFIERS(ModifiersTree.class),

    /**
     * {@link LambdaExpressionTree}
     *
     * @since Java 1.8
     */
    LAMBDA_EXPRESSION(LambdaExpressionTree.class),

    /**
     * {@link PrimitiveTypeTree}
     */
    PRIMITIVE_TYPE(PrimitiveTypeTree.class),

    /**
     * {@link TypeParameterTree}
     */
    TYPE_PARAMETER(TypeParameterTree.class),

    /**
     * {@link ImportTree}
     */
    IMPORT(ImportTree.class),

    /**
     * {@link PackageDeclarationTree}
     */
    PACKAGE(PackageDeclarationTree.class),

    /**
     * {@link ModuleDeclarationTree}
     *
     * @since Java 9
     */
    MODULE(ModuleDeclarationTree.class),

    /**
     * {@link RequiresDirectiveTree}
     *
     * @since Java 9
     */
    REQUIRES_DIRECTIVE(RequiresDirectiveTree.class),

    /**
     * {@link ExportsDirectiveTree}
     *
     * @since Java 9
     */
    EXPORTS_DIRECTIVE(ExportsDirectiveTree.class),

    /**
     * {@link OpensDirectiveTree}
     *
     * @since Java 9
     */
    OPENS_DIRECTIVE(OpensDirectiveTree.class),

    /**
     * {@link UsesDirectiveTree}
     *
     * @since Java 9
     */
    USES_DIRECTIVE(UsesDirectiveTree.class),

    /**
     * {@link ProvidesDirectiveTree}
     *
     * @since Java 9
     */
    PROVIDES_DIRECTIVE(ProvidesDirectiveTree.class),

    /**
     * {@link ArrayDimensionTree}
     */
    ARRAY_DIMENSION(ArrayDimensionTree.class),

    /**
     *An implementation-reserved node.
     *
     */
    OTHER(Tree.class),

    TOKEN(SyntaxToken.class),

    TRIVIA(SyntaxTrivia.class),

    INFERED_TYPE(InferedTypeTree.class),

    /**
     * {@link TypeArguments}
     */
    TYPE_ARGUMENTS(TypeArguments.class),
    /**
     * {@link MethodReferenceTree}
     */
    METHOD_REFERENCE(MethodReferenceTree.class),
    /**
     * {@link TypeParameters}
     */
    TYPE_PARAMETERS(TypeParameters.class),
    /**
     * {@link Arguments}
     */
    ARGUMENTS(Arguments.class),
    /**
     * {@link ListTree}
     */
    LIST(ListTree.class);

    final Class<? extends Tree> associatedInterface;

    Kind(Class<? extends Tree> associatedInterface) {
      this.associatedInterface = associatedInterface;
    }

    public Class<? extends Tree> getAssociatedInterface() {
      return associatedInterface;
    }
  }

  Kind kind();
}
