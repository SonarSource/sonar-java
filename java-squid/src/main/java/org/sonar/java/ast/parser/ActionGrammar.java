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
package org.sonar.java.ast.parser;

import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.model.JavaTree.PrimitiveTypeTreeImpl;
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.statement.AssertStatementTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.BreakStatementTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.CatchTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.DoWhileStatementTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ForEachStatementImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.sslr.grammar.GrammarRuleKey;

import java.util.List;

import static org.sonar.java.ast.api.JavaPunctuator.COLON;
import static org.sonar.java.ast.api.JavaTokenType.IDENTIFIER;

public class ActionGrammar {

  private final GrammarBuilder b;
  private final TreeFactory f;

  public ActionGrammar(GrammarBuilder b, TreeFactory f) {
    this.b = b;
    this.f = f;
  }

  public ModifiersTreeImpl MODIFIERS() {
    return b.<ModifiersTreeImpl>nonterminal(JavaGrammar.MODIFIERS)
      .is(
        f.modifiers(
          b.zeroOrMore(
            b.firstOf(
              ANNOTATION(),
              b.invokeRule(JavaKeyword.PUBLIC),
              b.invokeRule(JavaKeyword.PROTECTED),
              b.invokeRule(JavaKeyword.PRIVATE),
              b.invokeRule(JavaKeyword.ABSTRACT),
              b.invokeRule(JavaKeyword.STATIC),
              b.invokeRule(JavaKeyword.FINAL),
              b.invokeRule(JavaKeyword.TRANSIENT),
              b.invokeRule(JavaKeyword.VOLATILE),
              b.invokeRule(JavaKeyword.SYNCHRONIZED),
              b.invokeRule(JavaKeyword.NATIVE),
              b.invokeRule(JavaKeyword.DEFAULT),
              b.invokeRule(JavaKeyword.STRICTFP)))));
  }

  // Literals

  public ExpressionTree LITERAL() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.LITERAL)
      .is(
        f.literal(
          b.firstOf(
            b.invokeRule(JavaKeyword.TRUE),
            b.invokeRule(JavaKeyword.FALSE),
            b.invokeRule(JavaKeyword.NULL),
            b.invokeRule(JavaTokenType.CHARACTER_LITERAL),
            b.invokeRule(JavaTokenType.LITERAL),
            b.invokeRule(JavaTokenType.FLOAT_LITERAL),
            b.invokeRule(JavaTokenType.DOUBLE_LITERAL),
            b.invokeRule(JavaTokenType.LONG_LITERAL),
            b.invokeRule(JavaTokenType.INTEGER_LITERAL))));
  }

  // End of literals

  // Types

  public ExpressionTree TYPE() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.TYPE)
      .is(
        f.newType(
          b.firstOf(
            BASIC_TYPE(),
            CLASS_TYPE()),
          b.zeroOrMore(f.newWrapperAstNode5(b.zeroOrMore((AstNode) ANNOTATION()), b.invokeRule(JavaGrammar.DIM)))));
  }

  public ExpressionTree CLASS_TYPE() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.CLASS_TYPE)
      .is(
        f.newClassType(
          b.zeroOrMore(ANNOTATION()),
          b.invokeRule(JavaTokenType.IDENTIFIER),
          b.optional(TYPE_ARGUMENTS()),
          b.zeroOrMore(
            f.newClassTypeComplement(
              b.invokeRule(JavaPunctuator.DOT),
              b.zeroOrMore(ANNOTATION()),
              b.invokeRule(JavaTokenType.IDENTIFIER),
              b.optional(TYPE_ARGUMENTS())))));
  }

  public ClassTypeListTreeImpl CLASS_TYPE_LIST() {
    return b.<ClassTypeListTreeImpl>nonterminal(JavaGrammar.CLASS_TYPE_LIST)
      .is(
        f.newClassTypeList(
          CLASS_TYPE(),
          b.zeroOrMore(f.newWrapperAstNode3(b.invokeRule(JavaPunctuator.COMMA), (AstNode) CLASS_TYPE()))));
  }

  public TypeArgumentListTreeImpl TYPE_ARGUMENTS() {
    return b.<TypeArgumentListTreeImpl>nonterminal(JavaGrammar.TYPE_ARGUMENTS)
      .is(
        f.newTypeArgumentList(
          b.invokeRule(JavaPunctuator.LPOINT),
          TYPE_ARGUMENT(),
          b.zeroOrMore(f.newWrapperAstNode4(b.invokeRule(JavaPunctuator.COMMA), (AstNode) TYPE_ARGUMENT())),
          b.invokeRule(JavaPunctuator.RPOINT)));
  }

  public Tree TYPE_ARGUMENT() {
    return b.<Tree>nonterminal(JavaGrammar.TYPE_ARGUMENT)
      .is(
        f.completeTypeArgument(
          b.zeroOrMore(ANNOTATION()),
          b.firstOf(
            f.newBasicTypeArgument(TYPE()),
            f.completeWildcardTypeArgument(
              b.invokeRule(JavaPunctuator.QUERY),
              b.optional(
                f.newWildcardTypeArguments(
                  b.firstOf(
                    b.invokeRule(JavaKeyword.EXTENDS),
                    b.invokeRule(JavaKeyword.SUPER)),
                  b.zeroOrMore(ANNOTATION()),
                  TYPE()))))));
  }

  public TypeParameterListTreeImpl TYPE_PARAMETERS() {
    return b.<TypeParameterListTreeImpl>nonterminal(JavaGrammar.TYPE_PARAMETERS)
      .is(
        f.newTypeParameterList(
          b.invokeRule(JavaPunctuator.LPOINT),
          TYPE_PARAMETER(),
          b.zeroOrMore(f.newWrapperAstNode7(b.invokeRule(JavaPunctuator.COMMA), TYPE_PARAMETER())),
          b.invokeRule(JavaPunctuator.RPOINT)));
  }

  public TypeParameterTreeImpl TYPE_PARAMETER() {
    return b.<TypeParameterTreeImpl>nonterminal(JavaGrammar.TYPE_PARAMETER)
      .is(
        f.completeTypeParameter(
          b.zeroOrMore(ANNOTATION()),
          b.invokeRule(JavaTokenType.IDENTIFIER),
          b.optional(
            f.newTypeParameter(b.invokeRule(JavaKeyword.EXTENDS), BOUND()))));
  }

  public BoundListTreeImpl BOUND() {
    return b.<BoundListTreeImpl>nonterminal(JavaGrammar.BOUND)
      .is(
        f.newBounds(
          CLASS_TYPE(),
          b.zeroOrMore(f.newWrapperAstNode6(b.invokeRule(JavaPunctuator.AND), (AstNode) CLASS_TYPE()))));
  }

  // End of types

  // Annotations

  // TODO modifiers
  public ClassTreeImpl ANNOTATION_TYPE_DECLARATION() {
    return b.<ClassTreeImpl>nonterminal(JavaGrammar.ANNOTATION_TYPE_DECLARATION)
      .is(
        f.completeAnnotationType(
          b.invokeRule(JavaPunctuator.AT),
          b.invokeRule(JavaKeyword.INTERFACE),
          b.invokeRule(JavaTokenType.IDENTIFIER),
          ANNOTATION_TYPE_BODY()));
  }

  public ClassTreeImpl ANNOTATION_TYPE_BODY() {
    return b.<ClassTreeImpl>nonterminal(JavaGrammar.ANNOTATION_TYPE_BODY)
      .is(
        f.newAnnotationType(
          b.invokeRule(JavaPunctuator.LWING), b.zeroOrMore(ANNOTATION_TYPE_ELEMENT_DECLARATION()), b.invokeRule(JavaPunctuator.RWING)));
  }

  public AstNode ANNOTATION_TYPE_ELEMENT_DECLARATION() {
    return b.<AstNode>nonterminal(JavaGrammar.ANNOTATION_TYPE_ELEMENT_DECLARATION)
      .is(
        b.firstOf(
          f.completeAnnotationTypeMember(MODIFIERS(), ANNOTATION_TYPE_ELEMENT_REST()),
          b.invokeRule(JavaPunctuator.SEMI)));
  }

  public AstNode ANNOTATION_TYPE_ELEMENT_REST() {
    return b.<AstNode>nonterminal(JavaGrammar.ANNOTATION_TYPE_ELEMENT_REST)
      .is(
        b.firstOf(
          f.newAnnotationTypeMember(
            TYPE(), b.invokeRule(JavaTokenType.IDENTIFIER), ANNOTATION_METHOD_OR_CONSTANT_REST(), b.invokeRule(JavaPunctuator.SEMI)),
          b.firstOf(
            b.invokeRule(JavaGrammar.CLASS_DECLARATION),
            b.invokeRule(JavaGrammar.ENUM_DECLARATION),
            b.invokeRule(JavaGrammar.INTERFACE_DECLARATION)),
          ANNOTATION_TYPE_DECLARATION()));
  }

  public AstNode ANNOTATION_METHOD_OR_CONSTANT_REST() {
    return b.<AstNode>nonterminal(JavaGrammar.ANNOTATION_METHOD_OR_CONSTANT_REST)
      .is(
        b.firstOf(
          ANNOTATION_METHOD_REST(),
          b.invokeRule(JavaGrammar.CONSTANT_DECLARATORS_REST)));
  }

  public MethodTreeImpl ANNOTATION_METHOD_REST() {
    return b.<MethodTreeImpl>nonterminal(JavaGrammar.ANNOTATION_METHOD_REST)
      .is(
        f.newAnnotationTypeMethod(
          b.invokeRule(JavaPunctuator.LPAR),
          b.invokeRule(JavaPunctuator.RPAR),
          b.optional(DEFAULT_VALUE())));
  }

  public ExpressionTree DEFAULT_VALUE() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.DEFAULT_VALUE)
      .is(
        f.newDefaultValue(
          b.invokeRule(JavaKeyword.DEFAULT),
          ELEMENT_VALUE()));
  }

  public AnnotationTreeImpl ANNOTATION() {
    return b.<AnnotationTreeImpl>nonterminal(JavaGrammar.ANNOTATION)
      .is(
        f.newAnnotation(
          b.invokeRule(JavaPunctuator.AT),
          QUALIFIED_IDENTIFIER(),
          b.optional(ANNOTATION_REST())));
  }

  public ArgumentListTreeImpl ANNOTATION_REST() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaGrammar.ANNOTATION_REST)
      .is(
        b.firstOf(
          NORMAL_ANNOTATION_REST(),
          SINGLE_ELEMENT_ANNOTATION_REST()));
  }

  public ArgumentListTreeImpl NORMAL_ANNOTATION_REST() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaGrammar.NORMAL_ANNOTATION_REST)
      .is(
        f.completeNormalAnnotation(
          b.invokeRule(JavaPunctuator.LPAR),
          b.optional(ELEMENT_VALUE_PAIRS()),
          b.invokeRule(JavaPunctuator.RPAR)));
  }

  public ArgumentListTreeImpl ELEMENT_VALUE_PAIRS() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaGrammar.ELEMENT_VALUE_PAIRS)
      .is(
        f.newNormalAnnotation(
          ELEMENT_VALUE_PAIR(), b.zeroOrMore(f.newWrapperAstNode9(b.invokeRule(JavaPunctuator.COMMA), ELEMENT_VALUE_PAIR()))));
  }

  public AssignmentExpressionTreeImpl ELEMENT_VALUE_PAIR() {
    return b.<AssignmentExpressionTreeImpl>nonterminal(JavaGrammar.ELEMENT_VALUE_PAIR)
      .is(
        f.newElementValuePair(
          b.invokeRule(JavaTokenType.IDENTIFIER),
          b.invokeRule(JavaPunctuator.EQU),
          ELEMENT_VALUE()));
  }

  public ExpressionTree ELEMENT_VALUE() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.ELEMENT_VALUE)
      .is(
        b.firstOf(
          CONDITIONAL_EXPRESSION(),
          ANNOTATION(),
          ELEMENT_VALUE_ARRAY_INITIALIZER()));
  }

  public NewArrayTreeImpl ELEMENT_VALUE_ARRAY_INITIALIZER() {
    return b.<NewArrayTreeImpl>nonterminal(JavaGrammar.ELEMENT_VALUE_ARRAY_INITIALIZER)
      .is(
        f.completeElementValueArrayInitializer(
          b.invokeRule(JavaPunctuator.LWING),
          b.optional(ELEMENT_VALUES()),
          b.optional(b.invokeRule(JavaPunctuator.COMMA)),
          b.invokeRule(JavaPunctuator.RWING)));
  }

  public NewArrayTreeImpl ELEMENT_VALUES() {
    return b.<NewArrayTreeImpl>nonterminal(JavaGrammar.ELEMENT_VALUES)
      .is(
        f.newElementValueArrayInitializer(
          ELEMENT_VALUE(), b.zeroOrMore(f.newWrapperAstNode8(b.invokeRule(JavaPunctuator.COMMA), (AstNode) ELEMENT_VALUE()))));
  }

  public ArgumentListTreeImpl SINGLE_ELEMENT_ANNOTATION_REST() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaGrammar.SINGLE_ELEMENT_ANNOTATION_REST)
      .is(f.newSingleElementAnnotation(b.invokeRule(JavaPunctuator.LPAR), ELEMENT_VALUE(), b.invokeRule(JavaPunctuator.RPAR)));
  }

  // End of annotations

  // Formal parameters

  public FormalParametersListTreeImpl FORMAL_PARAMETERS() {
    return b.<FormalParametersListTreeImpl>nonterminal(JavaGrammar.FORMAL_PARAMETERS)
      .is(
        f.completeParenFormalParameters(
          b.invokeRule(JavaPunctuator.LPAR),
          b.optional(FORMAL_PARAMETERS_DECLS()),
          b.invokeRule(JavaPunctuator.RPAR)));
  }

  public FormalParametersListTreeImpl FORMAL_PARAMETERS_DECLS() {
    return b.<FormalParametersListTreeImpl>nonterminal(JavaGrammar.FORMAL_PARAMETER_DECLS)
      .is(
        f.completeTypeFormalParameters(
          MODIFIERS(),
          TYPE(),
          FORMAL_PARAMETERS_DECLS_REST()));
  }

  public FormalParametersListTreeImpl FORMAL_PARAMETERS_DECLS_REST() {
    return b.<FormalParametersListTreeImpl>nonterminal(JavaGrammar.FORMAL_PARAMETERS_DECLS_REST)
      .is(
        b.firstOf(
          f.prependNewFormalParameter(VARIABLE_DECLARATOR_ID(), b.optional(f.newWrapperAstNode10(b.invokeRule(JavaPunctuator.COMMA), FORMAL_PARAMETERS_DECLS()))),
          f.newVariableArgumentFormalParameter(b.zeroOrMore(ANNOTATION()), b.invokeRule(JavaPunctuator.ELLIPSIS), VARIABLE_DECLARATOR_ID())));
  }

  public VariableTreeImpl VARIABLE_DECLARATOR_ID() {
    return b.<VariableTreeImpl>nonterminal(JavaGrammar.VARIABLE_DECLARATOR_ID)
      .is(
        f.newVariableDeclaratorId(
          b.invokeRule(JavaTokenType.IDENTIFIER),
          b.zeroOrMore(f.newWrapperAstNode11(b.zeroOrMore((AstNode) ANNOTATION()), b.invokeRule(JavaGrammar.DIM)))));
  }

  public VariableTreeImpl FORMAL_PARAMETER() {
    // TODO Dim
    return b.<VariableTreeImpl>nonterminal(JavaGrammar.FORMAL_PARAMETER)
      .is(
        f.newFormalParameter(
          MODIFIERS(),
          TYPE(),
          VARIABLE_DECLARATOR_ID()));
  }

  // End of formal parameters

  // Statements

  public VariableDeclaratorListTreeImpl LOCAL_VARIABLE_DECLARATION_STATEMENT() {
    return b.<VariableDeclaratorListTreeImpl>nonterminal(JavaGrammar.LOCAL_VARIABLE_DECLARATION_STATEMENT)
      .is(f.completeLocalVariableDeclaration(MODIFIERS(), TYPE(), VARIABLE_DECLARATORS(), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public VariableDeclaratorListTreeImpl VARIABLE_DECLARATORS() {
    return b.<VariableDeclaratorListTreeImpl>nonterminal(JavaGrammar.VARIABLE_DECLARATORS)
      .is(f.newVariableDeclarators(VARIABLE_DECLARATOR(), b.zeroOrMore(f.newTuple3(b.invokeRule(JavaPunctuator.COMMA), VARIABLE_DECLARATOR()))));
  }

  public VariableTreeImpl VARIABLE_DECLARATOR() {
    return b.<VariableTreeImpl>nonterminal(JavaGrammar.VARIABLE_DECLARATOR)
      .is(
        f.completeVariableDeclarator(
          b.invokeRule(JavaTokenType.IDENTIFIER), b.zeroOrMore(b.invokeRule(JavaGrammar.DIM)),
          b.optional(
            f.newVariableDeclarator(b.invokeRule(JavaPunctuator.EQU), VARIABLE_INITIALIZER()))));
  }

  public StatementTree STATEMENT() {
    return b.<StatementTree>nonterminal(JavaGrammar.STATEMENT)
      .is(
        b.firstOf(
          BLOCK(),
          ASSERT_STATEMENT(),
          IF_STATEMENT(),
          FOR_STATEMENT(),
          WHILE_STATEMENT(),
          DO_WHILE_STATEMENT(),
          TRY_STATEMENT(),
          SWITCH_STATEMENT(),
          SYNCHRONIZED_STATEMENT(),
          RETURN_STATEMENT(),
          THROW_STATEMENT(),
          BREAK_STATEMENT(),
          CONTINUE_STATEMENT(),
          LABELED_STATEMENT(),
          EXPRESSION_STATEMENT(),
          EMPTY_STATEMENT()));
  }

  public BlockTreeImpl BLOCK() {
    return b.<BlockTreeImpl>nonterminal(JavaGrammar.BLOCK)
      .is(f.block(b.invokeRule(JavaPunctuator.LWING), b.invokeRule(JavaGrammar.BLOCK_STATEMENTS), b.invokeRule(JavaPunctuator.RWING)));
  }

  public AssertStatementTreeImpl ASSERT_STATEMENT() {
    return b.<AssertStatementTreeImpl>nonterminal(JavaGrammar.ASSERT_STATEMENT)
      .is(f.completeAssertStatement(
        b.invokeRule(JavaKeyword.ASSERT), EXPRESSION(),
        b.optional(
          f.newAssertStatement(b.invokeRule(JavaPunctuator.COLON), EXPRESSION())),
        b.invokeRule(JavaPunctuator.SEMI)));
  }

  public IfStatementTreeImpl IF_STATEMENT() {
    return b.<IfStatementTreeImpl>nonterminal(JavaGrammar.IF_STATEMENT)
      .is(
        f.completeIf(
          b.invokeRule(JavaKeyword.IF), b.invokeRule(JavaPunctuator.LPAR), EXPRESSION(), b.invokeRule(JavaPunctuator.RPAR),
          STATEMENT(),
          b.optional(
            f.newIfWithElse(b.invokeRule(JavaKeyword.ELSE), STATEMENT()))));
  }

  public StatementTree FOR_STATEMENT() {
    return b.<StatementTree>nonterminal(JavaGrammar.FOR_STATEMENT)
      .is(
        b.<StatementTree>firstOf(
          STANDARD_FOR_STATEMENT(),
          FOREACH_STATEMENT()));
  }

  public ForStatementTreeImpl STANDARD_FOR_STATEMENT() {
    return b.<ForStatementTreeImpl>nonterminal()
      .is(
        f.newStandardForStatement(
          b.invokeRule(JavaKeyword.FOR),
          b.invokeRule(JavaPunctuator.LPAR),
          b.optional(FOR_INIT()), b.invokeRule(JavaPunctuator.SEMI),
          b.optional(EXPRESSION()), b.invokeRule(JavaPunctuator.SEMI),
          b.optional(FOR_UPDATE()),
          b.invokeRule(JavaPunctuator.RPAR),
          STATEMENT()));
  }

  public StatementExpressionListTreeImpl FOR_INIT() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(
        b.firstOf(
          FOR_INIT_DECLARATION(),
          FOR_INIT_EXPRESSIONS()));
  }

  public StatementExpressionListTreeImpl FOR_INIT_DECLARATION() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(f.newForInitDeclaration(MODIFIERS(), TYPE(), VARIABLE_DECLARATORS()));
  }

  public StatementExpressionListTreeImpl FOR_INIT_EXPRESSIONS() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(STATEMENT_EXPRESSIONS());
  }

  public StatementExpressionListTreeImpl FOR_UPDATE() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(STATEMENT_EXPRESSIONS());
  }

  public StatementExpressionListTreeImpl STATEMENT_EXPRESSIONS() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(
        f.newStatementExpressions(
          EXPRESSION(), b.zeroOrMore(f.newWrapperAstNode12(b.invokeRule(JavaPunctuator.COMMA), (AstNode) EXPRESSION()))));
  }

  public ForEachStatementImpl FOREACH_STATEMENT() {
    return b.<ForEachStatementImpl>nonterminal()
      .is(
        f.newForeachStatement(
          b.invokeRule(JavaKeyword.FOR),
          b.invokeRule(JavaPunctuator.LPAR), FORMAL_PARAMETER(), b.invokeRule(JavaPunctuator.COLON), EXPRESSION(), b.invokeRule(JavaPunctuator.RPAR),
          STATEMENT()));
  }

  public WhileStatementTreeImpl WHILE_STATEMENT() {
    return b.<WhileStatementTreeImpl>nonterminal(JavaGrammar.WHILE_STATEMENT)
      .is(f.whileStatement(b.invokeRule(JavaKeyword.WHILE), b.invokeRule(JavaPunctuator.LPAR), EXPRESSION(), b.invokeRule(JavaPunctuator.RPAR),
        STATEMENT()));
  }

  public DoWhileStatementTreeImpl DO_WHILE_STATEMENT() {
    return b.<DoWhileStatementTreeImpl>nonterminal(JavaGrammar.DO_STATEMENT)
      .is(
        f.doWhileStatement(b.invokeRule(JavaKeyword.DO), STATEMENT(),
          b.invokeRule(JavaKeyword.WHILE), b.invokeRule(JavaPunctuator.LPAR), EXPRESSION(), b.invokeRule(JavaPunctuator.RPAR),
          b.invokeRule(JavaPunctuator.SEMI)));
  }

  public TryStatementTreeImpl TRY_STATEMENT() {
    return b.<TryStatementTreeImpl>nonterminal(JavaGrammar.TRY_STATEMENT)
      .is(
        b.firstOf(
          STANDARD_TRY_STATEMENT(),
          TRY_WITH_RESOURCES_STATEMENT()));
  }

  public TryStatementTreeImpl STANDARD_TRY_STATEMENT() {
    return b.<TryStatementTreeImpl>nonterminal()
      .is(
        f.completeStandardTryStatement(
          b.invokeRule(JavaKeyword.TRY),
          BLOCK(),
          b.firstOf(
            f.newTryCatch(b.zeroOrMore(CATCH_CLAUSE()), b.optional(FINALLY())),
            f.newTryFinally(FINALLY()))));
  }

  public CatchTreeImpl CATCH_CLAUSE() {
    return b.<CatchTreeImpl>nonterminal(JavaGrammar.CATCH_CLAUSE)
      .is(
        f.newCatchClause(
          b.invokeRule(JavaKeyword.CATCH), b.invokeRule(JavaPunctuator.LPAR), CATCH_FORMAL_PARAMETER(), b.invokeRule(JavaPunctuator.RPAR), BLOCK()));
  }

  public VariableTreeImpl CATCH_FORMAL_PARAMETER() {
    return b.<VariableTreeImpl>nonterminal()
      .is(
        f.newCatchFormalParameter(b.optional(MODIFIERS()), CATCH_TYPE(), VARIABLE_DECLARATOR_ID()));
  }

  public Tree CATCH_TYPE() {
    return b.<Tree>nonterminal()
      .is(
        f.newCatchType(QUALIFIED_IDENTIFIER(), b.zeroOrMore(f.newWrapperAstNode13(b.invokeRule(JavaPunctuator.OR), (AstNode) QUALIFIED_IDENTIFIER()))));
  }

  public BlockTreeImpl FINALLY() {
    return b.<BlockTreeImpl>nonterminal(JavaGrammar.FINALLY_)
      .is(
        f.newFinallyBlock(b.invokeRule(JavaKeyword.FINALLY), BLOCK()));
  }

  public TryStatementTreeImpl TRY_WITH_RESOURCES_STATEMENT() {
    return b.<TryStatementTreeImpl>nonterminal()
      .is(
        f.newTryWithResourcesStatement(
          b.invokeRule(JavaKeyword.TRY),
          b.invokeRule(JavaPunctuator.LPAR),
          RESOURCES(),
          b.invokeRule(JavaPunctuator.RPAR),
          BLOCK(),
          b.zeroOrMore(CATCH_CLAUSE()),
          b.optional(FINALLY())));
  }

  public ResourceListTreeImpl RESOURCES() {
    return b.<ResourceListTreeImpl>nonterminal()
      .is(
        f.newResources(b.oneOrMore(f.newWrapperAstNode14(RESOURCE(), b.optional(b.invokeRule(JavaPunctuator.SEMI))))));
  }

  public VariableTreeImpl RESOURCE() {
    return b.<VariableTreeImpl>nonterminal(JavaGrammar.RESOURCE)
      .is(
        f.newResource(MODIFIERS(), CLASS_TYPE(), VARIABLE_DECLARATOR_ID(), b.invokeRule(JavaPunctuator.EQU), EXPRESSION()));
  }

  public SwitchStatementTreeImpl SWITCH_STATEMENT() {
    return b.<SwitchStatementTreeImpl>nonterminal(JavaGrammar.SWITCH_STATEMENT)
      .is(
        f.switchStatement(
          b.invokeRule(JavaKeyword.SWITCH), b.invokeRule(JavaPunctuator.LPAR), EXPRESSION(), b.invokeRule(JavaPunctuator.RPAR),
          b.invokeRule(JavaPunctuator.LWING),
          b.zeroOrMore(SWITCH_GROUP()),
          b.invokeRule(JavaPunctuator.RWING)));
  }

  public CaseGroupTreeImpl SWITCH_GROUP() {
    return b.<CaseGroupTreeImpl>nonterminal(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUP)
      .is(f.switchGroup(b.oneOrMore(SWITCH_LABEL()), b.invokeRule(JavaGrammar.BLOCK_STATEMENTS)));
  }

  public CaseLabelTreeImpl SWITCH_LABEL() {
    return b.<CaseLabelTreeImpl>nonterminal(JavaGrammar.SWITCH_LABEL)
      .is(
        b.firstOf(
          f.newCaseSwitchLabel(b.invokeRule(JavaKeyword.CASE), EXPRESSION(), b.invokeRule(JavaPunctuator.COLON)),
          f.newDefaultSwitchLabel(b.invokeRule(JavaKeyword.DEFAULT), b.invokeRule(JavaPunctuator.COLON))));
  }

  public SynchronizedStatementTreeImpl SYNCHRONIZED_STATEMENT() {
    return b.<SynchronizedStatementTreeImpl>nonterminal(JavaGrammar.SYNCHRONIZED_STATEMENT)
      .is(
        f.synchronizedStatement(b.invokeRule(JavaKeyword.SYNCHRONIZED), b.invokeRule(JavaPunctuator.LPAR), EXPRESSION(), b.invokeRule(JavaPunctuator.RPAR),
          BLOCK()));
  }

  public BreakStatementTreeImpl BREAK_STATEMENT() {
    return b.<BreakStatementTreeImpl>nonterminal(JavaGrammar.BREAK_STATEMENT)
      .is(f.breakStatement(b.invokeRule(JavaKeyword.BREAK), b.optional(b.invokeRule(JavaTokenType.IDENTIFIER)), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public ContinueStatementTreeImpl CONTINUE_STATEMENT() {
    return b.<ContinueStatementTreeImpl>nonterminal(JavaGrammar.CONTINUE_STATEMENT)
      .is(f.continueStatement(b.invokeRule(JavaKeyword.CONTINUE), b.optional(b.invokeRule(JavaTokenType.IDENTIFIER)), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public ReturnStatementTreeImpl RETURN_STATEMENT() {
    return b.<ReturnStatementTreeImpl>nonterminal(JavaGrammar.RETURN_STATEMENT)
      .is(f.returnStatement(b.invokeRule(JavaKeyword.RETURN), b.optional(EXPRESSION()), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public ThrowStatementTreeImpl THROW_STATEMENT() {
    return b.<ThrowStatementTreeImpl>nonterminal(JavaGrammar.THROW_STATEMENT)
      .is(f.throwStatement(b.invokeRule(JavaKeyword.THROW), EXPRESSION(), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public LabeledStatementTreeImpl LABELED_STATEMENT() {
    return b.<LabeledStatementTreeImpl>nonterminal(JavaGrammar.LABELED_STATEMENT)
      .is(f.labeledStatement(b.invokeRule(IDENTIFIER), b.invokeRule(COLON), STATEMENT()));
  }

  public ExpressionStatementTreeImpl EXPRESSION_STATEMENT() {
    return b.<ExpressionStatementTreeImpl>nonterminal(JavaGrammar.EXPRESSION_STATEMENT)
      .is(f.expressionStatement(EXPRESSION(), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public EmptyStatementTreeImpl EMPTY_STATEMENT() {
    return b.<EmptyStatementTreeImpl>nonterminal(JavaGrammar.EMPTY_STATEMENT)
      .is(f.emptyStatement(b.invokeRule(JavaPunctuator.SEMI)));
  }

  // End of statements

  // Expressions

  public ExpressionTree EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.EXPRESSION)
      .is(ASSIGNMENT_EXPRESSION());
  }

  public ExpressionTree ASSIGNMENT_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.ASSIGNMENT_EXPRESSION)
      .is(
        f.assignmentExpression(
          CONDITIONAL_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand11(
              b.firstOf(
                b.invokeRule(JavaPunctuator.EQU),
                b.invokeRule(JavaPunctuator.PLUSEQU),
                b.invokeRule(JavaPunctuator.MINUSEQU),
                b.invokeRule(JavaPunctuator.STAREQU),
                b.invokeRule(JavaPunctuator.DIVEQU),
                b.invokeRule(JavaPunctuator.ANDEQU),
                b.invokeRule(JavaPunctuator.OREQU),
                b.invokeRule(JavaPunctuator.HATEQU),
                b.invokeRule(JavaPunctuator.MODEQU),
                b.invokeRule(JavaPunctuator.SLEQU),
                b.invokeRule(JavaPunctuator.SREQU),
                b.invokeRule(JavaPunctuator.BSREQU)),
              CONDITIONAL_EXPRESSION()))));
  }

  public ExpressionTree CONDITIONAL_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.CONDITIONAL_EXPRESSION)
      .is(
        f.completeTernaryExpression(
          CONDITIONAL_OR_EXPRESSION(),
          b.optional(
            f.newTernaryExpression(
              b.invokeRule(JavaPunctuator.QUERY),
              EXPRESSION(),
              b.invokeRule(JavaPunctuator.COLON),
              EXPRESSION()))));
  }

  public ExpressionTree CONDITIONAL_OR_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.CONDITIONAL_OR_EXPRESSION)
      .is(
        f.binaryExpression10(
          CONDITIONAL_AND_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand10(
              b.invokeRule(JavaPunctuator.OROR),
              CONDITIONAL_AND_EXPRESSION()))));
  }

  public ExpressionTree CONDITIONAL_AND_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.CONDITIONAL_AND_EXPRESSION)
      .is(
        f.binaryExpression9(
          INCLUSIVE_OR_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand9(
              b.invokeRule(JavaPunctuator.ANDAND),
              INCLUSIVE_OR_EXPRESSION()))));
  }

  public ExpressionTree INCLUSIVE_OR_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.INCLUSIVE_OR_EXPRESSION)
      .is(
        f.binaryExpression8(
          EXCLUSIVE_OR_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand8(
              b.invokeRule(JavaPunctuator.OR),
              EXCLUSIVE_OR_EXPRESSION()))));
  }

  public ExpressionTree EXCLUSIVE_OR_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.EXCLUSIVE_OR_EXPRESSION)
      .is(
        f.binaryExpression7(
          AND_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand7(
              b.invokeRule(JavaPunctuator.HAT),
              AND_EXPRESSION()))));
  }

  public ExpressionTree AND_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.AND_EXPRESSION)
      .is(
        f.binaryExpression6(
          EQUALITY_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand6(
              b.invokeRule(JavaPunctuator.AND),
              EQUALITY_EXPRESSION()))));
  }

  public ExpressionTree EQUALITY_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.EQUALITY_EXPRESSION)
      .is(
        f.binaryExpression5(
          INSTANCEOF_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand5(
              b.firstOf(
                b.invokeRule(JavaPunctuator.EQUAL),
                b.invokeRule(JavaPunctuator.NOTEQUAL)),
              INSTANCEOF_EXPRESSION()))));
  }

  public ExpressionTree INSTANCEOF_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.RELATIONAL_EXPRESSION)
      .is(
        f.completeInstanceofExpression(
          RELATIONAL_EXPRESSION(),
          b.optional(f.newInstanceofExpression(b.invokeRule(JavaKeyword.INSTANCEOF), TYPE()))));
  }

  public ExpressionTree RELATIONAL_EXPRESSION() {
    return b.<ExpressionTree>nonterminal()
      .is(
        f.binaryExpression4(
          SHIFT_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand4(
              b.firstOf(
                b.invokeRule(JavaPunctuator.GE),
                b.invokeRule(JavaPunctuator.GT),
                b.invokeRule(JavaPunctuator.LE),
                b.invokeRule(JavaPunctuator.LT)),
              SHIFT_EXPRESSION()))));
  }

  public ExpressionTree SHIFT_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.SHIFT_EXPRESSION)
      .is(
        f.binaryExpression3(
          ADDITIVE_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand3(
              b.firstOf(
                b.invokeRule(JavaPunctuator.SL),
                b.invokeRule(JavaPunctuator.BSR),
                b.invokeRule(JavaPunctuator.SR)),
              ADDITIVE_EXPRESSION()))));
  }

  public ExpressionTree ADDITIVE_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.ADDITIVE_EXPRESSION)
      .is(
        f.binaryExpression2(
          MULTIPLICATIVE_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand2(
              b.firstOf(
                b.invokeRule(JavaPunctuator.PLUS),
                b.invokeRule(JavaPunctuator.MINUS)),
              MULTIPLICATIVE_EXPRESSION()))));
  }

  public ExpressionTree MULTIPLICATIVE_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.MULTIPLICATIVE_EXPRESSION)
      .is(
        f.binaryExpression1(
          UNARY_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand1(
              b.firstOf(
                b.invokeRule(JavaPunctuator.STAR),
                b.invokeRule(JavaPunctuator.DIV),
                b.invokeRule(JavaPunctuator.MOD)),
              UNARY_EXPRESSION()))));
  }

  public ExpressionTree UNARY_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.UNARY_EXPRESSION)
      .is(
        b.firstOf(
          f.newPrefixedExpression(
            b.firstOf(
              b.invokeRule(JavaPunctuator.INC),
              b.invokeRule(JavaPunctuator.DEC),
              b.invokeRule(JavaPunctuator.PLUS),
              b.invokeRule(JavaPunctuator.MINUS)),
            UNARY_EXPRESSION()),
          UNARY_EXPRESSION_NOT_PLUS_MINUS()));
  }

  public ExpressionTree UNARY_EXPRESSION_NOT_PLUS_MINUS() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.UNARY_EXPRESSION_NOT_PLUS_MINUS)
      .is(
        b.firstOf(
          CAST_EXPRESSION(),
          METHOD_REFERENCE(),
          // TODO Extract postfix expressions somewhere else
          f.newPostfixExpression(
            PRIMARY(),
            b.zeroOrMore(b.invokeRule(JavaGrammar.SELECTOR)),
            b.zeroOrMore(
              b.firstOf(
                b.invokeRule(JavaPunctuator.INC),
                b.invokeRule(JavaPunctuator.DEC)))),
          f.newTildaExpression(b.invokeRule(JavaPunctuator.TILDA), UNARY_EXPRESSION()),
          f.newBangExpression(b.invokeRule(JavaPunctuator.BANG), UNARY_EXPRESSION())));
  }

  public ExpressionTree CAST_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.CAST_EXPRESSION)
      .is(
        f.completeCastExpression(
          b.invokeRule(JavaPunctuator.LPAR),
          b.firstOf(
            f.newBasicTypeCastExpression(BASIC_TYPE(), b.invokeRule(JavaPunctuator.RPAR), UNARY_EXPRESSION()),
            f.newClassCastExpression(
              TYPE(),
              b.zeroOrMore(f.newWrapperAstNode(b.invokeRule(JavaPunctuator.AND), (AstNode) CLASS_TYPE())),
              b.invokeRule(JavaPunctuator.RPAR),
              UNARY_EXPRESSION_NOT_PLUS_MINUS()))));
  }

  public ExpressionTree METHOD_REFERENCE() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.METHOD_REFERENCE)
      .is(
        f.completeMethodReference(
          b.firstOf(
            f.newSuperMethodReference(b.invokeRule(JavaKeyword.SUPER), b.invokeRule(JavaPunctuator.DBLECOLON)),
            f.newTypeMethodReference(TYPE(), b.invokeRule(JavaPunctuator.DBLECOLON)),
            f.newPrimaryMethodReference(PRIMARY(), b.zeroOrMore(b.invokeRule(JavaGrammar.SELECTOR)), b.invokeRule(JavaPunctuator.DBLECOLON))),
          b.optional(b.invokeRule(JavaGrammar.TYPE_ARGUMENTS)),
          b.firstOf(
            b.invokeRule(JavaKeyword.NEW),
            b.invokeRule(JavaTokenType.IDENTIFIER))));
  }

  public ExpressionTree PRIMARY() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.PRIMARY)
      .is(
        b.firstOf(
          LAMBDA_EXPRESSION(),
          PARENTHESIZED_EXPRESSION(),
          EXPLICIT_GENERIC_INVOCATION_EXPRESSION(),
          THIS_EXPRESSION(),
          SUPER_EXPRESSION(),
          LITERAL(),
          NEW_EXPRESSION(),
          QUALIFIED_IDENTIFIER_EXPRESSION(),
          BASIC_CLASS_EXPRESSION(),
          VOID_CLASS_EXPRESSION()));
  }

  public ExpressionTree LAMBDA_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.LAMBDA_EXPRESSION)
      .is(f.lambdaExpression(LAMBDA_PARAMETERS(), b.invokeRule(JavaGrammar.ARROW), LAMBDA_BODY()));
  }

  public LambdaParameterListTreeImpl LAMBDA_PARAMETERS() {
    return b.<LambdaParameterListTreeImpl>nonterminal(JavaGrammar.LAMBDA_PARAMETERS)
      .is(
        b.firstOf(
          MULTIPLE_INFERED_PARAMETERS(),
          f.formalLambdaParameters(FORMAL_PARAMETERS()),
          f.singleInferedParameter(INFERED_PARAMETER())));
  }

  public LambdaParameterListTreeImpl MULTIPLE_INFERED_PARAMETERS() {
    return b.<LambdaParameterListTreeImpl>nonterminal(JavaGrammar.INFERED_PARAMS)
      .is(
        f.newInferedParameters(
          b.invokeRule(JavaPunctuator.LPAR),
          b.optional(
            f.newTuple2(
              INFERED_PARAMETER(),
              b.zeroOrMore(f.newTuple1(b.invokeRule(JavaPunctuator.COMMA), INFERED_PARAMETER())))),
          b.invokeRule(JavaPunctuator.RPAR)));
  }

  public VariableTreeImpl INFERED_PARAMETER() {
    return b.<VariableTreeImpl>nonterminal()
      .is(
        f.newSimpleParameter(b.invokeRule(JavaTokenType.IDENTIFIER)));
  }

  public Tree LAMBDA_BODY() {
    return b.<Tree>nonterminal(JavaGrammar.LAMBDA_BODY)
      .is(
        b.firstOf(
          BLOCK(),
          EXPRESSION()));
  }

  public ParenthesizedTreeImpl PARENTHESIZED_EXPRESSION() {
    return b.<ParenthesizedTreeImpl>nonterminal(JavaGrammar.PAR_EXPRESSION)
      .is(f.parenthesizedExpression(b.invokeRule(JavaPunctuator.LPAR), EXPRESSION(), b.invokeRule(JavaPunctuator.RPAR)));
  }

  public ExpressionTree EXPLICIT_GENERIC_INVOCATION_EXPRESSION() {
    // TODO Own tree node?
    return b.<ExpressionTree>nonterminal(JavaGrammar.EXPLICIT_GENERIC_INVOCATION_EXPRESSION)
      .is(
        f.completeExplicityGenericInvocation(
          b.invokeRule(JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS),
          b.firstOf(
            f.newExplicitGenericInvokation(b.invokeRule(JavaGrammar.EXPLICIT_GENERIC_INVOCATION_SUFFIX)),
            f.newExplicitGenericInvokation(b.invokeRule(JavaKeyword.THIS), ARGUMENTS()))));
  }

  public ExpressionTree THIS_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.THIS_EXPRESSION)
      .is(f.thisExpression(b.invokeRule(JavaKeyword.THIS), b.optional(ARGUMENTS())));
  }

  public ExpressionTree SUPER_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.SUPER_EXPRESSION)
      .is(f.superExpression(b.invokeRule(JavaKeyword.SUPER), b.invokeRule(JavaGrammar.SUPER_SUFFIX)));
  }

  public ExpressionTree NEW_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.NEW_EXPRESSION)
      .is(f.newExpression(b.invokeRule(JavaKeyword.NEW), b.zeroOrMore(ANNOTATION()), CREATOR()));
  }

  public ExpressionTree CREATOR() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.CREATOR)
      .is(
        f.completeCreator(
          b.optional(b.invokeRule(JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS)),
          b.firstOf(
            f.newClassCreator(b.invokeRule(JavaGrammar.CREATED_NAME), b.invokeRule(JavaGrammar.CLASS_CREATOR_REST)),
            f.newArrayCreator(
              b.firstOf(
                CLASS_TYPE(),
                BASIC_TYPE()),
              ARRAY_CREATOR_REST()))));
  }

  public NewArrayTreeImpl ARRAY_CREATOR_REST() {
    return b.<NewArrayTreeImpl>nonterminal(JavaGrammar.ARRAY_CREATOR_REST)
      .is(
        f.completeArrayCreator(
          b.zeroOrMore(ANNOTATION()),
          b.firstOf(
            f.newArrayCreatorWithInitializer(
              b.invokeRule(JavaPunctuator.LBRK), b.invokeRule(JavaPunctuator.RBRK), b.zeroOrMore(b.invokeRule(JavaGrammar.DIM)), ARRAY_INITIALIZER()),
            f.newArrayCreatorWithDimension(
              b.invokeRule(JavaPunctuator.LBRK), EXPRESSION(), b.invokeRule(JavaPunctuator.RBRK),
              b.zeroOrMore(ARRAY_ACCESS_EXPRESSION()),
              b.zeroOrMore(f.newWrapperAstNode(b.zeroOrMore((AstNode) ANNOTATION()), b.invokeRule(JavaGrammar.DIM)))))));
  }

  public ExpressionTree QUALIFIED_IDENTIFIER_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.QUALIFIED_IDENTIFIER_EXPRESSION)
      .is(f.newQualifiedIdentifierExpression(QUALIFIED_IDENTIFIER(), b.optional(b.invokeRule(JavaGrammar.IDENTIFIER_SUFFIX))));
  }

  public ExpressionTree BASIC_CLASS_EXPRESSION() {
    return b
      .<ExpressionTree>nonterminal(JavaGrammar.BASIC_CLASS_EXPRESSION)
      .is(
        f.basicClassExpression(BASIC_TYPE(), b.zeroOrMore(b.invokeRule(JavaGrammar.DIM)), b.invokeRule(JavaPunctuator.DOT), b.invokeRule(JavaKeyword.CLASS)));
  }

  public ExpressionTree VOID_CLASS_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.VOID_CLASS_EXPRESSION)
      .is(f.voidClassExpression(b.invokeRule(JavaKeyword.VOID), b.invokeRule(JavaPunctuator.DOT), b.invokeRule(JavaKeyword.CLASS)));
  }

  public PrimitiveTypeTreeImpl BASIC_TYPE() {
    return b.<PrimitiveTypeTreeImpl>nonterminal(JavaGrammar.BASIC_TYPE)
      .is(
        f.newBasicType(
          b.zeroOrMore(ANNOTATION()),
          b.firstOf(
            b.invokeRule(JavaKeyword.BYTE),
            b.invokeRule(JavaKeyword.SHORT),
            b.invokeRule(JavaKeyword.CHAR),
            b.invokeRule(JavaKeyword.INT),
            b.invokeRule(JavaKeyword.LONG),
            b.invokeRule(JavaKeyword.FLOAT),
            b.invokeRule(JavaKeyword.DOUBLE),
            b.invokeRule(JavaKeyword.BOOLEAN))));
  }

  public ArgumentListTreeImpl ARGUMENTS() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaGrammar.ARGUMENTS)
      .is(
        f.completeArguments(
          b.invokeRule(JavaPunctuator.LPAR),
          b.optional(
            f.newArguments(
              EXPRESSION(),
              b.zeroOrMore(f.newWrapperAstNode2(b.invokeRule(JavaPunctuator.COMMA), (AstNode) EXPRESSION())))),
          b.invokeRule(JavaPunctuator.RPAR)));
  }

  public ExpressionTree QUALIFIED_IDENTIFIER() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.QUALIFIED_IDENTIFIER)
      .is(
        f.qualifiedIdentifier(
          b.zeroOrMore(ANNOTATION()),
          b.invokeRule(JavaTokenType.IDENTIFIER),
          b.zeroOrMore(f.newWrapperAstNode(b.invokeRule(JavaPunctuator.DOT), b.zeroOrMore((AstNode) ANNOTATION()), b.invokeRule(JavaTokenType.IDENTIFIER)))));
  }

  public ExpressionTree VARIABLE_INITIALIZER() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.VARIABLE_INITIALIZER)
      .is(
        b.firstOf(
          EXPRESSION(),
          ARRAY_INITIALIZER()));
  }

  public NewArrayTreeImpl ARRAY_INITIALIZER() {
    return b.<NewArrayTreeImpl>nonterminal(JavaGrammar.ARRAY_INITIALIZER)
      .is(
        f.newArrayInitializer(
          b.invokeRule(JavaPunctuator.LWING),
          b.zeroOrMore(f.newWrapperAstNode15((AstNode) VARIABLE_INITIALIZER(), b.optional(b.invokeRule(JavaPunctuator.COMMA)))),
          b.invokeRule(JavaPunctuator.RWING)));
  }

  public QualifiedIdentifierListTreeImpl QUALIFIED_IDENTIFIER_LIST() {
    return b.<QualifiedIdentifierListTreeImpl>nonterminal(JavaGrammar.QUALIFIED_IDENTIFIER_LIST)
      .is(f.newQualifiedIdentifierList(QUALIFIED_IDENTIFIER(), b.zeroOrMore(f.newTuple4(b.invokeRule(JavaPunctuator.COMMA), QUALIFIED_IDENTIFIER()))));
  }

  public ArrayAccessExpressionTreeImpl ARRAY_ACCESS_EXPRESSION() {
    return b.<ArrayAccessExpressionTreeImpl>nonterminal(JavaGrammar.DIM_EXPR)
      .is(f.newArrayAccessExpression(b.zeroOrMore(ANNOTATION()), b.invokeRule(JavaPunctuator.LBRK), EXPRESSION(), b.invokeRule(JavaPunctuator.RBRK)));
  }

  // End of expressions

  public interface GrammarBuilder {

    <T> NonterminalBuilder<T> nonterminal();

    <T> NonterminalBuilder<T> nonterminal(GrammarRuleKey ruleKey);

    <T> T firstOf(T... methods);

    <T> Optional<T> optional(T method);

    <T> List<T> oneOrMore(T method);

    <T> Optional<List<T>> zeroOrMore(T method);

    AstNode invokeRule(GrammarRuleKey ruleKey);

    AstNode token(String value);

  }

  public interface NonterminalBuilder<T> {

    T is(T method);

  }

}
