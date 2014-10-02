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
import org.sonar.java.ast.parser.TreeFactory.Tuple;
import org.sonar.java.model.JavaTree.PrimitiveTypeTreeImpl;
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
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
import org.sonar.java.parser.sslr.GrammarBuilder;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.ast.api.JavaPunctuator.COLON;
import static org.sonar.java.ast.api.JavaTokenType.IDENTIFIER;

public class JavaGrammar {

  private final GrammarBuilder b;
  private final TreeFactory f;

  public JavaGrammar(GrammarBuilder b, TreeFactory f) {
    this.b = b;
    this.f = f;
  }

  public ModifiersTreeImpl modifiers() {
    return b.<ModifiersTreeImpl>nonterminal(JavaLexer.MODIFIERS)
      .is(
        f.modifiers(
          b.zeroOrMore(
            b.<ModifierTree>firstOf(
              annotation(),
              modifierKeyword()))));
  }

  public ModifierKeywordTreeImpl modifierKeyword() {
    return b.<ModifierKeywordTreeImpl>nonterminal().is(
      f.modifierKeyword(
        b.firstOf(
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
          b.invokeRule(JavaKeyword.STRICTFP))));
  }

  // Literals

  public ExpressionTree literal() {
    return b.<ExpressionTree>nonterminal(JavaLexer.LITERAL)
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

  public ExpressionTree type() {
    return b.<ExpressionTree>nonterminal(JavaLexer.TYPE)
      .is(
        f.newType(
          b.firstOf(
            basicType(),
            qualifiedIdentifier()),
          b.zeroOrMore(f.newWrapperAstNode5(b.zeroOrMore((AstNode) annotation()), dimension()))));
  }

  public TypeArgumentListTreeImpl typeArguments() {
    return b.<TypeArgumentListTreeImpl>nonterminal(JavaLexer.TYPE_ARGUMENTS)
      .is(
        b.firstOf(
          f.newTypeArgumentList(
            b.invokeRule(JavaPunctuator.LPOINT),
            typeArgument(),
            b.zeroOrMore(f.newWrapperAstNode4(b.invokeRule(JavaPunctuator.COMMA), (AstNode) typeArgument())),
            b.invokeRule(JavaPunctuator.RPOINT)),
          f.newDiamondTypeArgument(b.invokeRule(JavaPunctuator.LPOINT), b.invokeRule(JavaPunctuator.RPOINT))));
  }

  public Tree typeArgument() {
    return b.<Tree>nonterminal(JavaLexer.TYPE_ARGUMENT)
      .is(
        f.completeTypeArgument(
          b.zeroOrMore(annotation()),
          b.firstOf(
            f.newBasicTypeArgument(type()),
            f.completeWildcardTypeArgument(
              b.invokeRule(JavaPunctuator.QUERY),
              b.optional(
                f.newWildcardTypeArguments(
                  b.firstOf(
                    b.invokeRule(JavaKeyword.EXTENDS),
                    b.invokeRule(JavaKeyword.SUPER)),
                  b.zeroOrMore(annotation()),
                  type()))))));
  }

  public TypeParameterListTreeImpl typeParameters() {
    return b.<TypeParameterListTreeImpl>nonterminal(JavaLexer.TYPE_PARAMETERS)
      .is(
        f.newTypeParameterList(
          b.invokeRule(JavaPunctuator.LPOINT),
          typeParameter(),
          b.zeroOrMore(f.newWrapperAstNode7(b.invokeRule(JavaPunctuator.COMMA), typeParameter())),
          b.invokeRule(JavaPunctuator.RPOINT)));
  }

  public TypeParameterTreeImpl typeParameter() {
    return b.<TypeParameterTreeImpl>nonterminal(JavaLexer.TYPE_PARAMETER)
      .is(
        f.completeTypeParameter(
          b.zeroOrMore(annotation()),
          b.invokeRule(JavaTokenType.IDENTIFIER),
          b.optional(
            f.newTypeParameter(b.invokeRule(JavaKeyword.EXTENDS), bound()))));
  }

  public BoundListTreeImpl bound() {
    return b.<BoundListTreeImpl>nonterminal(JavaLexer.BOUND)
      .is(
        f.newBounds(
          qualifiedIdentifier(),
          b.zeroOrMore(f.newWrapperAstNode6(b.invokeRule(JavaPunctuator.AND), (AstNode) qualifiedIdentifier()))));
  }

  // End of types

  // Classes

  public ClassTreeImpl classDeclaration() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.CLASS_DECLARATION)
      .is(
        f.completeClassDeclaration(
          b.invokeRule(JavaKeyword.CLASS),
          b.invokeRule(JavaTokenType.IDENTIFIER), b.optional(typeParameters()),
          b.optional(f.newTuple7(b.invokeRule(JavaKeyword.EXTENDS), qualifiedIdentifier())),
          b.optional(f.newTuple14(b.invokeRule(JavaKeyword.IMPLEMENTS), qualifiedIdentifierList())),
          classBody()));
  }

  public ClassTreeImpl classBody() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.CLASS_BODY)
      .is(f.newClassBody(b.invokeRule(JavaPunctuator.LWING), b.zeroOrMore(classMember()), b.invokeRule(JavaPunctuator.RWING)));
  }

  public AstNode classMember() {
    return b.<AstNode>nonterminal(JavaLexer.MEMBER_DECL)
      .is(
        b.firstOf(
          f.completeMember(
            modifiers(),
            b.firstOf(
              methodOrConstructorDeclaration(),
              fieldDeclaration(),
              classDeclaration(),
              annotationTypeDeclaration(),
              interfaceDeclaration(),
              enumDeclaration())),
          f.newInitializerMember(b.optional(b.invokeRule(JavaKeyword.STATIC)), block()),
          f.newEmptyMember(b.invokeRule(JavaPunctuator.SEMI))));
  }

  public MethodTreeImpl methodOrConstructorDeclaration() {
    return b.<MethodTreeImpl>nonterminal()
      .is(
        b.firstOf(
          f.completeGenericMethodOrConstructorDeclaration(typeParameters(), methodOrConstructorDeclaration()),
          f.newMethod(
            type(), b.invokeRule(JavaTokenType.IDENTIFIER), formalParameters(),
            // TOOD Dedicated rule for annotated dimensions
            b.zeroOrMore(f.newTuple9(b.zeroOrMore(annotation()), dimension())),
            b.optional(f.newTuple10(b.invokeRule(JavaKeyword.THROWS), qualifiedIdentifierList())),
            b.firstOf(
              block(),
              b.invokeRule(JavaPunctuator.SEMI))),
          // TODO Largely duplicated with method, but there is a prefix capture on the TYPE, it can be improved
          f.newConstructor(
            b.invokeRule(JavaTokenType.IDENTIFIER), formalParameters(),
            // TOOD Dedicated rule for annotated dimensions
            b.zeroOrMore(f.newTuple15(b.zeroOrMore(annotation()), dimension())),
            b.optional(f.newTuple16(b.invokeRule(JavaKeyword.THROWS), qualifiedIdentifierList())),
            b.firstOf(
              block(),
              b.invokeRule(JavaPunctuator.SEMI)))));
  }

  public VariableDeclaratorListTreeImpl fieldDeclaration() {
    return b.<VariableDeclaratorListTreeImpl>nonterminal(JavaLexer.FIELD_DECLARATION)
      .is(f.completeFieldDeclaration(type(), variableDeclarators(), b.invokeRule(JavaPunctuator.SEMI)));
  }

  // End of classes

  // Enums

  public ClassTreeImpl enumDeclaration() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.ENUM_DECLARATION)
      .is(
        f.newEnumDeclaration(
          b.invokeRule(JavaKeyword.ENUM),
          b.invokeRule(JavaTokenType.IDENTIFIER),
          b.optional(f.newTuple12(b.invokeRule(JavaKeyword.IMPLEMENTS), qualifiedIdentifierList())),
          b.invokeRule(JavaPunctuator.LWING),
          b.zeroOrMore(enumConstant()),
          // TODO Grammar has been relaxed
          b.optional(b.invokeRule(JavaPunctuator.SEMI)),
          b.zeroOrMore(classMember()),
          b.invokeRule(JavaPunctuator.RWING)));
  }

  public EnumConstantTreeImpl enumConstant() {
    return b.<EnumConstantTreeImpl>nonterminal(JavaLexer.ENUM_CONSTANT)
      .is(
        f.newEnumConstant(
          // TODO Annotated identifier?
          b.zeroOrMore(annotation()), b.invokeRule(JavaTokenType.IDENTIFIER),
          b.optional(arguments()),
          b.optional(classBody()),
          b.optional(b.invokeRule(JavaPunctuator.COMMA))));
  }

  // End of enums

  // Interfaces

  public ClassTreeImpl interfaceDeclaration() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.INTERFACE_DECLARATION)
      .is(
        f.completeInterfaceDeclaration(
          b.invokeRule(JavaKeyword.INTERFACE),
          b.invokeRule(JavaTokenType.IDENTIFIER),
          b.optional(typeParameters()),
          b.optional(f.newTuple11(b.invokeRule(JavaKeyword.EXTENDS), qualifiedIdentifierList())),
          interfaceBody()));
  }

  public ClassTreeImpl interfaceBody() {
    return b.<ClassTreeImpl>nonterminal()
      .is(f.newInterfaceBody(b.invokeRule(JavaPunctuator.LWING), b.zeroOrMore(classMember()), b.invokeRule(JavaPunctuator.RWING)));
  }

  // End of interfaces

  // Annotations

  // TODO modifiers
  public ClassTreeImpl annotationTypeDeclaration() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.ANNOTATION_TYPE_DECLARATION)
      .is(
        f.completeAnnotationType(
          b.invokeRule(JavaPunctuator.AT),
          b.invokeRule(JavaKeyword.INTERFACE),
          b.invokeRule(JavaTokenType.IDENTIFIER),
          annotationTypeBody()));
  }

  public ClassTreeImpl annotationTypeBody() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.ANNOTATION_TYPE_BODY)
      .is(
        f.newAnnotationType(
          b.invokeRule(JavaPunctuator.LWING), b.zeroOrMore(annotationTypeElementDeclaration()), b.invokeRule(JavaPunctuator.RWING)));
  }

  public AstNode annotationTypeElementDeclaration() {
    return b.<AstNode>nonterminal(JavaLexer.ANNOTATION_TYPE_ELEMENT_DECLARATION)
      .is(
        b.firstOf(
          f.completeAnnotationTypeMember(modifiers(), annotationTypeElementRest()),
          b.invokeRule(JavaPunctuator.SEMI)));
  }

  public AstNode annotationTypeElementRest() {
    return b.<AstNode>nonterminal(JavaLexer.ANNOTATION_TYPE_ELEMENT_REST)
      .is(
        b.firstOf(
          f.newAnnotationTypeMember(
            type(), b.invokeRule(JavaTokenType.IDENTIFIER), annotationMethodOrConstantRest(), b.invokeRule(JavaPunctuator.SEMI)),
          classDeclaration(),
          enumDeclaration(),
          interfaceDeclaration(),
          annotationTypeDeclaration()));
  }

  public AstNode annotationMethodOrConstantRest() {
    return b.<AstNode>nonterminal(JavaLexer.ANNOTATION_METHOD_OR_CONSTANT_REST)
      .is(
        b.firstOf(
          annotationMethodRest(),
          b.invokeRule(JavaLexer.CONSTANT_DECLARATORS_REST)));
  }

  public MethodTreeImpl annotationMethodRest() {
    return b.<MethodTreeImpl>nonterminal(JavaLexer.ANNOTATION_METHOD_REST)
      .is(
        f.newAnnotationTypeMethod(
          b.invokeRule(JavaPunctuator.LPAR),
          b.invokeRule(JavaPunctuator.RPAR),
          b.optional(defaultValue())));
  }

  public ExpressionTree defaultValue() {
    return b.<ExpressionTree>nonterminal(JavaLexer.DEFAULT_VALUE)
      .is(
        f.newDefaultValue(
          b.invokeRule(JavaKeyword.DEFAULT),
          elementValue()));
  }

  public AnnotationTreeImpl annotation() {
    return b.<AnnotationTreeImpl>nonterminal(JavaLexer.ANNOTATION)
      .is(
        f.newAnnotation(
          b.invokeRule(JavaPunctuator.AT),
          qualifiedIdentifier(),
          b.optional(annotationRest())));
  }

  public ArgumentListTreeImpl annotationRest() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.ANNOTATION_REST)
      .is(
        b.firstOf(
          normalAnnotationRest(),
          singleElementAnnotationRest()));
  }

  public ArgumentListTreeImpl normalAnnotationRest() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.NORMAL_ANNOTATION_REST)
      .is(
        f.completeNormalAnnotation(
          b.invokeRule(JavaPunctuator.LPAR),
          b.optional(elementValuePairs()),
          b.invokeRule(JavaPunctuator.RPAR)));
  }

  public ArgumentListTreeImpl elementValuePairs() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.ELEMENT_VALUE_PAIRS)
      .is(
        f.newNormalAnnotation(
          elementValuePair(), b.zeroOrMore(f.newWrapperAstNode9(b.invokeRule(JavaPunctuator.COMMA), elementValuePair()))));
  }

  public AssignmentExpressionTreeImpl elementValuePair() {
    return b.<AssignmentExpressionTreeImpl>nonterminal(JavaLexer.ELEMENT_VALUE_PAIR)
      .is(
        f.newElementValuePair(
          b.invokeRule(JavaTokenType.IDENTIFIER),
          b.invokeRule(JavaPunctuator.EQU),
          elementValue()));
  }

  public ExpressionTree elementValue() {
    return b.<ExpressionTree>nonterminal(JavaLexer.ELEMENT_VALUE)
      .is(
        b.firstOf(
          conditionalExpression(),
          annotation(),
          elementValueArrayInitializer()));
  }

  public NewArrayTreeImpl elementValueArrayInitializer() {
    return b.<NewArrayTreeImpl>nonterminal(JavaLexer.ELEMENT_VALUE_ARRAY_INITIALIZER)
      .is(
        f.completeElementValueArrayInitializer(
          b.invokeRule(JavaPunctuator.LWING),
          b.optional(elementValues()),
          b.optional(b.invokeRule(JavaPunctuator.COMMA)),
          b.invokeRule(JavaPunctuator.RWING)));
  }

  public NewArrayTreeImpl elementValues() {
    return b.<NewArrayTreeImpl>nonterminal(JavaLexer.ELEMENT_VALUES)
      .is(
        f.newElementValueArrayInitializer(
          elementValue(), b.zeroOrMore(f.newWrapperAstNode8(b.invokeRule(JavaPunctuator.COMMA), (AstNode) elementValue()))));
  }

  public ArgumentListTreeImpl singleElementAnnotationRest() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.SINGLE_ELEMENT_ANNOTATION_REST)
      .is(f.newSingleElementAnnotation(b.invokeRule(JavaPunctuator.LPAR), elementValue(), b.invokeRule(JavaPunctuator.RPAR)));
  }

  // End of annotations

  // Formal parameters

  public FormalParametersListTreeImpl formalParameters() {
    return b.<FormalParametersListTreeImpl>nonterminal(JavaLexer.FORMAL_PARAMETERS)
      .is(
        f.completeParenFormalParameters(
          b.invokeRule(JavaPunctuator.LPAR),
          b.optional(formalParametersDecls()),
          b.invokeRule(JavaPunctuator.RPAR)));
  }

  public FormalParametersListTreeImpl formalParametersDecls() {
    return b.<FormalParametersListTreeImpl>nonterminal(JavaLexer.FORMAL_PARAMETER_DECLS)
      .is(
        f.completeTypeFormalParameters(
          modifiers(),
          type(),
          formalParametersDeclsRest()));
  }

  public FormalParametersListTreeImpl formalParametersDeclsRest() {
    return b.<FormalParametersListTreeImpl>nonterminal(JavaLexer.FORMAL_PARAMETERS_DECLS_REST)
      .is(
        b.firstOf(
          f.prependNewFormalParameter(variableDeclaratorId(), b.optional(f.newWrapperAstNode10(b.invokeRule(JavaPunctuator.COMMA), formalParametersDecls()))),
          f.newVariableArgumentFormalParameter(b.zeroOrMore(annotation()), b.invokeRule(JavaPunctuator.ELLIPSIS), variableDeclaratorId())));
  }

  public VariableTreeImpl variableDeclaratorId() {
    return b.<VariableTreeImpl>nonterminal(JavaLexer.VARIABLE_DECLARATOR_ID)
      .is(
        f.newVariableDeclaratorId(
          b.invokeRule(JavaTokenType.IDENTIFIER),
          b.zeroOrMore(f.newWrapperAstNode11(b.zeroOrMore((AstNode) annotation()), dimension()))));
  }

  public VariableTreeImpl formalParameter() {
    // TODO Dim
    return b.<VariableTreeImpl>nonterminal(JavaLexer.FORMAL_PARAMETER)
      .is(
        f.newFormalParameter(
          modifiers(),
          type(),
          variableDeclaratorId()));
  }

  // End of formal parameters

  // Statements

  public VariableDeclaratorListTreeImpl localVariableDeclarationStatement() {
    return b.<VariableDeclaratorListTreeImpl>nonterminal(JavaLexer.LOCAL_VARIABLE_DECLARATION_STATEMENT)
      .is(f.completeLocalVariableDeclaration(modifiers(), type(), variableDeclarators(), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public VariableDeclaratorListTreeImpl variableDeclarators() {
    return b.<VariableDeclaratorListTreeImpl>nonterminal(JavaLexer.VARIABLE_DECLARATORS)
      .is(f.newVariableDeclarators(variableDeclarator(), b.zeroOrMore(f.newTuple3(b.invokeRule(JavaPunctuator.COMMA), variableDeclarator()))));
  }

  public VariableTreeImpl variableDeclarator() {
    return b.<VariableTreeImpl>nonterminal(JavaLexer.VARIABLE_DECLARATOR)
      .is(
        f.completeVariableDeclarator(
          b.invokeRule(JavaTokenType.IDENTIFIER), b.zeroOrMore(dimension()),
          b.optional(
            f.newVariableDeclarator(b.invokeRule(JavaPunctuator.EQU), variableInitializer()))));
  }

  public StatementTree statement() {
    return b.<StatementTree>nonterminal(JavaLexer.STATEMENT)
      .is(
        b.firstOf(
          block(),
          assertStatement(),
          ifStatement(),
          forStatement(),
          whileStatement(),
          doWhileStatement(),
          tryStatement(),
          switchStatement(),
          synchronizedStatement(),
          returnStatement(),
          throwStatement(),
          breakStatement(),
          continueStatement(),
          labeledStatement(),
          expressionStatement(),
          emptyStatement()));
  }

  public BlockTreeImpl block() {
    return b.<BlockTreeImpl>nonterminal(JavaLexer.BLOCK)
      .is(f.block(b.invokeRule(JavaPunctuator.LWING), b.invokeRule(JavaLexer.BLOCK_STATEMENTS), b.invokeRule(JavaPunctuator.RWING)));
  }

  public AssertStatementTreeImpl assertStatement() {
    return b.<AssertStatementTreeImpl>nonterminal(JavaLexer.ASSERT_STATEMENT)
      .is(f.completeAssertStatement(
        b.invokeRule(JavaKeyword.ASSERT), expression(),
        b.optional(
          f.newAssertStatement(b.invokeRule(JavaPunctuator.COLON), expression())),
        b.invokeRule(JavaPunctuator.SEMI)));
  }

  public IfStatementTreeImpl ifStatement() {
    return b.<IfStatementTreeImpl>nonterminal(JavaLexer.IF_STATEMENT)
      .is(
        f.completeIf(
          b.invokeRule(JavaKeyword.IF), b.invokeRule(JavaPunctuator.LPAR), expression(), b.invokeRule(JavaPunctuator.RPAR),
          statement(),
          b.optional(
            f.newIfWithElse(b.invokeRule(JavaKeyword.ELSE), statement()))));
  }

  public StatementTree forStatement() {
    return b.<StatementTree>nonterminal(JavaLexer.FOR_STATEMENT)
      .is(
        b.<StatementTree>firstOf(
          standardForStatement(),
          foreachStatement()));
  }

  public ForStatementTreeImpl standardForStatement() {
    return b.<ForStatementTreeImpl>nonterminal()
      .is(
        f.newStandardForStatement(
          b.invokeRule(JavaKeyword.FOR),
          b.invokeRule(JavaPunctuator.LPAR),
          b.optional(forInit()), b.invokeRule(JavaPunctuator.SEMI),
          b.optional(expression()), b.invokeRule(JavaPunctuator.SEMI),
          b.optional(forUpdate()),
          b.invokeRule(JavaPunctuator.RPAR),
          statement()));
  }

  public StatementExpressionListTreeImpl forInit() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(
        b.firstOf(
          forInitDeclaration(),
          forInitExpressions()));
  }

  public StatementExpressionListTreeImpl forInitDeclaration() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(f.newForInitDeclaration(modifiers(), type(), variableDeclarators()));
  }

  public StatementExpressionListTreeImpl forInitExpressions() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(statementExpressions());
  }

  public StatementExpressionListTreeImpl forUpdate() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(statementExpressions());
  }

  public StatementExpressionListTreeImpl statementExpressions() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(
        f.newStatementExpressions(
          expression(), b.zeroOrMore(f.newWrapperAstNode12(b.invokeRule(JavaPunctuator.COMMA), (AstNode) expression()))));
  }

  public ForEachStatementImpl foreachStatement() {
    return b.<ForEachStatementImpl>nonterminal()
      .is(
        f.newForeachStatement(
          b.invokeRule(JavaKeyword.FOR),
          b.invokeRule(JavaPunctuator.LPAR), formalParameter(), b.invokeRule(JavaPunctuator.COLON), expression(), b.invokeRule(JavaPunctuator.RPAR),
          statement()));
  }

  public WhileStatementTreeImpl whileStatement() {
    return b.<WhileStatementTreeImpl>nonterminal(JavaLexer.WHILE_STATEMENT)
      .is(f.whileStatement(b.invokeRule(JavaKeyword.WHILE), b.invokeRule(JavaPunctuator.LPAR), expression(), b.invokeRule(JavaPunctuator.RPAR),
        statement()));
  }

  public DoWhileStatementTreeImpl doWhileStatement() {
    return b.<DoWhileStatementTreeImpl>nonterminal(JavaLexer.DO_STATEMENT)
      .is(
        f.doWhileStatement(b.invokeRule(JavaKeyword.DO), statement(),
          b.invokeRule(JavaKeyword.WHILE), b.invokeRule(JavaPunctuator.LPAR), expression(), b.invokeRule(JavaPunctuator.RPAR),
          b.invokeRule(JavaPunctuator.SEMI)));
  }

  public TryStatementTreeImpl tryStatement() {
    return b.<TryStatementTreeImpl>nonterminal(JavaLexer.TRY_STATEMENT)
      .is(
        b.firstOf(
          standardTryStatement(),
          tryWithResourcesStatement()));
  }

  public TryStatementTreeImpl standardTryStatement() {
    return b.<TryStatementTreeImpl>nonterminal()
      .is(
        f.completeStandardTryStatement(
          b.invokeRule(JavaKeyword.TRY),
          block(),
          b.firstOf(
            f.newTryCatch(b.zeroOrMore(catchClause()), b.optional(finallyBlock())),
            f.newTryFinally(finallyBlock()))));
  }

  public CatchTreeImpl catchClause() {
    return b.<CatchTreeImpl>nonterminal(JavaLexer.CATCH_CLAUSE)
      .is(
        f.newCatchClause(
          b.invokeRule(JavaKeyword.CATCH), b.invokeRule(JavaPunctuator.LPAR), catchFormalParameter(), b.invokeRule(JavaPunctuator.RPAR), block()));
  }

  public VariableTreeImpl catchFormalParameter() {
    return b.<VariableTreeImpl>nonterminal()
      .is(
        f.newCatchFormalParameter(b.optional(modifiers()), catchType(), variableDeclaratorId()));
  }

  public Tree catchType() {
    return b.<Tree>nonterminal()
      .is(
        f.newCatchType(qualifiedIdentifier(), b.zeroOrMore(f.newWrapperAstNode13(b.invokeRule(JavaPunctuator.OR), (AstNode) qualifiedIdentifier()))));
  }

  public BlockTreeImpl finallyBlock() {
    return b.<BlockTreeImpl>nonterminal(JavaLexer.FINALLY_)
      .is(
        f.newFinallyBlock(b.invokeRule(JavaKeyword.FINALLY), block()));
  }

  public TryStatementTreeImpl tryWithResourcesStatement() {
    return b.<TryStatementTreeImpl>nonterminal()
      .is(
        f.newTryWithResourcesStatement(
          b.invokeRule(JavaKeyword.TRY),
          b.invokeRule(JavaPunctuator.LPAR),
          resources(),
          b.invokeRule(JavaPunctuator.RPAR),
          block(),
          b.zeroOrMore(catchClause()),
          b.optional(finallyBlock())));
  }

  public ResourceListTreeImpl resources() {
    return b.<ResourceListTreeImpl>nonterminal()
      .is(
        f.newResources(b.oneOrMore(f.newWrapperAstNode14(resource(), b.optional(b.invokeRule(JavaPunctuator.SEMI))))));
  }

  public VariableTreeImpl resource() {
    return b.<VariableTreeImpl>nonterminal(JavaLexer.RESOURCE)
      .is(
        f.newResource(modifiers(), qualifiedIdentifier(), variableDeclaratorId(), b.invokeRule(JavaPunctuator.EQU), expression()));
  }

  public SwitchStatementTreeImpl switchStatement() {
    return b.<SwitchStatementTreeImpl>nonterminal(JavaLexer.SWITCH_STATEMENT)
      .is(
        f.switchStatement(
          b.invokeRule(JavaKeyword.SWITCH), b.invokeRule(JavaPunctuator.LPAR), expression(), b.invokeRule(JavaPunctuator.RPAR),
          b.invokeRule(JavaPunctuator.LWING),
          b.zeroOrMore(switchGroup()),
          b.invokeRule(JavaPunctuator.RWING)));
  }

  public CaseGroupTreeImpl switchGroup() {
    return b.<CaseGroupTreeImpl>nonterminal(JavaLexer.SWITCH_BLOCK_STATEMENT_GROUP)
      .is(f.switchGroup(b.oneOrMore(switchLabel()), b.invokeRule(JavaLexer.BLOCK_STATEMENTS)));
  }

  public CaseLabelTreeImpl switchLabel() {
    return b.<CaseLabelTreeImpl>nonterminal(JavaLexer.SWITCH_LABEL)
      .is(
        b.firstOf(
          f.newCaseSwitchLabel(b.invokeRule(JavaKeyword.CASE), expression(), b.invokeRule(JavaPunctuator.COLON)),
          f.newDefaultSwitchLabel(b.invokeRule(JavaKeyword.DEFAULT), b.invokeRule(JavaPunctuator.COLON))));
  }

  public SynchronizedStatementTreeImpl synchronizedStatement() {
    return b.<SynchronizedStatementTreeImpl>nonterminal(JavaLexer.SYNCHRONIZED_STATEMENT)
      .is(
        f.synchronizedStatement(b.invokeRule(JavaKeyword.SYNCHRONIZED), b.invokeRule(JavaPunctuator.LPAR), expression(), b.invokeRule(JavaPunctuator.RPAR),
          block()));
  }

  public BreakStatementTreeImpl breakStatement() {
    return b.<BreakStatementTreeImpl>nonterminal(JavaLexer.BREAK_STATEMENT)
      .is(f.breakStatement(b.invokeRule(JavaKeyword.BREAK), b.optional(b.invokeRule(JavaTokenType.IDENTIFIER)), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public ContinueStatementTreeImpl continueStatement() {
    return b.<ContinueStatementTreeImpl>nonterminal(JavaLexer.CONTINUE_STATEMENT)
      .is(f.continueStatement(b.invokeRule(JavaKeyword.CONTINUE), b.optional(b.invokeRule(JavaTokenType.IDENTIFIER)), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public ReturnStatementTreeImpl returnStatement() {
    return b.<ReturnStatementTreeImpl>nonterminal(JavaLexer.RETURN_STATEMENT)
      .is(f.returnStatement(b.invokeRule(JavaKeyword.RETURN), b.optional(expression()), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public ThrowStatementTreeImpl throwStatement() {
    return b.<ThrowStatementTreeImpl>nonterminal(JavaLexer.THROW_STATEMENT)
      .is(f.throwStatement(b.invokeRule(JavaKeyword.THROW), expression(), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public LabeledStatementTreeImpl labeledStatement() {
    return b.<LabeledStatementTreeImpl>nonterminal(JavaLexer.LABELED_STATEMENT)
      .is(f.labeledStatement(b.invokeRule(IDENTIFIER), b.invokeRule(COLON), statement()));
  }

  public ExpressionStatementTreeImpl expressionStatement() {
    return b.<ExpressionStatementTreeImpl>nonterminal(JavaLexer.EXPRESSION_STATEMENT)
      .is(f.expressionStatement(expression(), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public EmptyStatementTreeImpl emptyStatement() {
    return b.<EmptyStatementTreeImpl>nonterminal(JavaLexer.EMPTY_STATEMENT)
      .is(f.emptyStatement(b.invokeRule(JavaPunctuator.SEMI)));
  }

  // End of statements

  // Expressions

  public ExpressionTree expression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.EXPRESSION)
      .is(assignmentExpression());
  }

  public ExpressionTree assignmentExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.ASSIGNMENT_EXPRESSION)
      .is(
        f.assignmentExpression(
          conditionalExpression(),
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
              conditionalExpression()))));
  }

  public ExpressionTree conditionalExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.CONDITIONAL_EXPRESSION)
      .is(
        f.completeTernaryExpression(
          conditionalOrExpression(),
          b.optional(
            f.newTernaryExpression(
              b.invokeRule(JavaPunctuator.QUERY),
              expression(),
              b.invokeRule(JavaPunctuator.COLON),
              expression()))));
  }

  public ExpressionTree conditionalOrExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.CONDITIONAL_OR_EXPRESSION)
      .is(
        f.binaryExpression10(
          conditionalAndExpression(),
          b.zeroOrMore(
            f.newOperatorAndOperand10(
              b.invokeRule(JavaPunctuator.OROR),
              conditionalAndExpression()))));
  }

  public ExpressionTree conditionalAndExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.CONDITIONAL_AND_EXPRESSION)
      .is(
        f.binaryExpression9(
          inclusiveOrExpression(),
          b.zeroOrMore(
            f.newOperatorAndOperand9(
              b.invokeRule(JavaPunctuator.ANDAND),
              inclusiveOrExpression()))));
  }

  public ExpressionTree inclusiveOrExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.INCLUSIVE_OR_EXPRESSION)
      .is(
        f.binaryExpression8(
          exclusiveOrExpression(),
          b.zeroOrMore(
            f.newOperatorAndOperand8(
              b.invokeRule(JavaPunctuator.OR),
              exclusiveOrExpression()))));
  }

  public ExpressionTree exclusiveOrExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.EXCLUSIVE_OR_EXPRESSION)
      .is(
        f.binaryExpression7(
          andExpression(),
          b.zeroOrMore(
            f.newOperatorAndOperand7(
              b.invokeRule(JavaPunctuator.HAT),
              andExpression()))));
  }

  public ExpressionTree andExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.AND_EXPRESSION)
      .is(
        f.binaryExpression6(
          equalityExpression(),
          b.zeroOrMore(
            f.newOperatorAndOperand6(
              b.invokeRule(JavaPunctuator.AND),
              equalityExpression()))));
  }

  public ExpressionTree equalityExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.EQUALITY_EXPRESSION)
      .is(
        f.binaryExpression5(
          instanceofExpression(),
          b.zeroOrMore(
            f.newOperatorAndOperand5(
              b.firstOf(
                b.invokeRule(JavaPunctuator.EQUAL),
                b.invokeRule(JavaPunctuator.NOTEQUAL)),
              instanceofExpression()))));
  }

  public ExpressionTree instanceofExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.RELATIONAL_EXPRESSION)
      .is(
        f.completeInstanceofExpression(
          relationalExpression(),
          b.optional(f.newInstanceofExpression(b.invokeRule(JavaKeyword.INSTANCEOF), type()))));
  }

  public ExpressionTree relationalExpression() {
    return b.<ExpressionTree>nonterminal()
      .is(
        f.binaryExpression4(
          shiftExpression(),
          b.zeroOrMore(
            f.newOperatorAndOperand4(
              b.firstOf(
                b.invokeRule(JavaPunctuator.GE),
                b.invokeRule(JavaPunctuator.GT),
                b.invokeRule(JavaPunctuator.LE),
                b.invokeRule(JavaPunctuator.LT)),
              shiftExpression()))));
  }

  public ExpressionTree shiftExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.SHIFT_EXPRESSION)
      .is(
        f.binaryExpression3(
          additiveExpression(),
          b.zeroOrMore(
            f.newOperatorAndOperand3(
              b.firstOf(
                b.invokeRule(JavaPunctuator.SL),
                b.invokeRule(JavaPunctuator.BSR),
                b.invokeRule(JavaPunctuator.SR)),
              additiveExpression()))));
  }

  public ExpressionTree additiveExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.ADDITIVE_EXPRESSION)
      .is(
        f.binaryExpression2(
          multiplicativeExpression(),
          b.zeroOrMore(
            f.newOperatorAndOperand2(
              b.firstOf(
                b.invokeRule(JavaPunctuator.PLUS),
                b.invokeRule(JavaPunctuator.MINUS)),
              multiplicativeExpression()))));
  }

  public ExpressionTree multiplicativeExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.MULTIPLICATIVE_EXPRESSION)
      .is(
        f.binaryExpression1(
          unaryExpression(),
          b.zeroOrMore(
            f.newOperatorAndOperand1(
              b.firstOf(
                b.invokeRule(JavaPunctuator.STAR),
                b.invokeRule(JavaPunctuator.DIV),
                b.invokeRule(JavaPunctuator.MOD)),
              unaryExpression()))));
  }

  public ExpressionTree unaryExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.UNARY_EXPRESSION)
      .is(
        b.firstOf(
          f.newPrefixedExpression(
            b.firstOf(
              b.invokeRule(JavaPunctuator.INC),
              b.invokeRule(JavaPunctuator.DEC),
              b.invokeRule(JavaPunctuator.PLUS),
              b.invokeRule(JavaPunctuator.MINUS)),
            unaryExpression()),
          unaryExpressionNotPlusMinus()));
  }

  public ExpressionTree unaryExpressionNotPlusMinus() {
    return b.<ExpressionTree>nonterminal(JavaLexer.UNARY_EXPRESSION_NOT_PLUS_MINUS)
      .is(
        b.firstOf(
          castExpression(),
          methodReference(),
          // TODO Extract postfix expressions somewhere else
          f.newPostfixExpression(
            f.applySelectors1(primary(), b.zeroOrMore(selector())),
            b.optional(
              b.firstOf(
                b.invokeRule(JavaPunctuator.INC),
                b.invokeRule(JavaPunctuator.DEC)))),
          f.newTildaExpression(b.invokeRule(JavaPunctuator.TILDA), unaryExpression()),
          f.newBangExpression(b.invokeRule(JavaPunctuator.BANG), unaryExpression())));
  }

  public ExpressionTree castExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.CAST_EXPRESSION)
      .is(
        f.completeCastExpression(
          b.invokeRule(JavaPunctuator.LPAR),
          b.firstOf(
            f.newBasicTypeCastExpression(basicType(), b.invokeRule(JavaPunctuator.RPAR), unaryExpression()),
            f.newClassCastExpression(
              type(),
              b.zeroOrMore(f.newWrapperAstNode(b.invokeRule(JavaPunctuator.AND), (AstNode) qualifiedIdentifier())),
              b.invokeRule(JavaPunctuator.RPAR),
              unaryExpressionNotPlusMinus()))));
  }

  public ExpressionTree methodReference() {
    return b.<ExpressionTree>nonterminal(JavaLexer.METHOD_REFERENCE)
      .is(
        f.completeMethodReference(
          b.firstOf(
            f.newSuperMethodReference(b.invokeRule(JavaKeyword.SUPER), b.invokeRule(JavaPunctuator.DBLECOLON)),
            f.newTypeMethodReference(type(), b.invokeRule(JavaPunctuator.DBLECOLON)),
            // TODO This is a postfix expression followed by a double colon
            f.newPrimaryMethodReference(
              f.applySelectors2(primary(), b.zeroOrMore(selector())),
              b.invokeRule(JavaPunctuator.DBLECOLON))),
          b.optional(b.invokeRule(JavaLexer.TYPE_ARGUMENTS)),
          b.firstOf(
            b.invokeRule(JavaKeyword.NEW),
            b.invokeRule(JavaTokenType.IDENTIFIER))));
  }

  public ExpressionTree primary() {
    return b.<ExpressionTree>nonterminal(JavaLexer.PRIMARY)
      .is(
        b.firstOf(
          lambdaExpression(),
          identifierOrMethodInvocation(),
          parenthesizedExpression(),
          literal(),
          newExpression(),
          basicClassExpression(),
          voidClassExpression()));
  }

  public ExpressionTree lambdaExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.LAMBDA_EXPRESSION)
      .is(f.lambdaExpression(lambdaParameters(), b.invokeRule(JavaLexer.ARROW), lambdaBody()));
  }

  public LambdaParameterListTreeImpl lambdaParameters() {
    return b.<LambdaParameterListTreeImpl>nonterminal(JavaLexer.LAMBDA_PARAMETERS)
      .is(
        b.firstOf(
          multipleInferedParameters(),
          f.formalLambdaParameters(formalParameters()),
          f.singleInferedParameter(inferedParameter())));
  }

  public LambdaParameterListTreeImpl multipleInferedParameters() {
    return b.<LambdaParameterListTreeImpl>nonterminal(JavaLexer.INFERED_PARAMS)
      .is(
        f.newInferedParameters(
          b.invokeRule(JavaPunctuator.LPAR),
          b.optional(
            f.newTuple2(
              inferedParameter(),
              b.zeroOrMore(f.newTuple1(b.invokeRule(JavaPunctuator.COMMA), inferedParameter())))),
          b.invokeRule(JavaPunctuator.RPAR)));
  }

  public VariableTreeImpl inferedParameter() {
    return b.<VariableTreeImpl>nonterminal()
      .is(
        f.newSimpleParameter(b.invokeRule(JavaTokenType.IDENTIFIER)));
  }

  public Tree lambdaBody() {
    return b.<Tree>nonterminal(JavaLexer.LAMBDA_BODY)
      .is(
        b.firstOf(
          block(),
          expression()));
  }

  public ParenthesizedTreeImpl parenthesizedExpression() {
    return b.<ParenthesizedTreeImpl>nonterminal(JavaLexer.PAR_EXPRESSION)
      .is(f.parenthesizedExpression(b.invokeRule(JavaPunctuator.LPAR), expression(), b.invokeRule(JavaPunctuator.RPAR)));
  }

  public ExpressionTree newExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.NEW_EXPRESSION)
      .is(f.newExpression(b.invokeRule(JavaKeyword.NEW), b.zeroOrMore(annotation()), creator()));
  }

  public ExpressionTree creator() {
    return b.<ExpressionTree>nonterminal(JavaLexer.CREATOR)
      .is(
        f.completeCreator(
          b.optional(typeArguments()),
          b.firstOf(
            f.newClassCreator(qualifiedIdentifier(), classCreatorRest()),
            f.newArrayCreator(
              b.firstOf(
                qualifiedIdentifier(),
                basicType()),
              arrayCreatorRest()))));
  }

  public NewArrayTreeImpl arrayCreatorRest() {
    return b.<NewArrayTreeImpl>nonterminal(JavaLexer.ARRAY_CREATOR_REST)
      .is(
        f.completeArrayCreator(
          b.zeroOrMore(annotation()),
          b.firstOf(
            f.newArrayCreatorWithInitializer(
              b.invokeRule(JavaPunctuator.LBRK), b.invokeRule(JavaPunctuator.RBRK), b.zeroOrMore(dimension()), arrayInitializer()),
            f.newArrayCreatorWithDimension(
              b.invokeRule(JavaPunctuator.LBRK), expression(), b.invokeRule(JavaPunctuator.RBRK),
              b.zeroOrMore(arrayAccessExpression()),
              b.zeroOrMore(f.newWrapperAstNode(b.zeroOrMore((AstNode) annotation()), dimension()))))));
  }

  // TODO This method should go away
  public ExpressionTree basicClassExpression() {
    return b
      .<ExpressionTree>nonterminal(JavaLexer.BASIC_CLASS_EXPRESSION)
      .is(
        f.basicClassExpression(basicType(), b.zeroOrMore(dimension()), b.invokeRule(JavaPunctuator.DOT), b.invokeRule(JavaKeyword.CLASS)));
  }

  // TODO This method should go away
  public ExpressionTree voidClassExpression() {
    return b.<ExpressionTree>nonterminal(JavaLexer.VOID_CLASS_EXPRESSION)
      .is(f.voidClassExpression(b.invokeRule(JavaKeyword.VOID), b.invokeRule(JavaPunctuator.DOT), b.invokeRule(JavaKeyword.CLASS)));
  }

  public PrimitiveTypeTreeImpl basicType() {
    return b.<PrimitiveTypeTreeImpl>nonterminal(JavaLexer.BASIC_TYPE)
      .is(
        f.newBasicType(
          b.zeroOrMore(annotation()),
          b.firstOf(
            b.invokeRule(JavaKeyword.BYTE),
            b.invokeRule(JavaKeyword.SHORT),
            b.invokeRule(JavaKeyword.CHAR),
            b.invokeRule(JavaKeyword.INT),
            b.invokeRule(JavaKeyword.LONG),
            b.invokeRule(JavaKeyword.FLOAT),
            b.invokeRule(JavaKeyword.DOUBLE),
            b.invokeRule(JavaKeyword.BOOLEAN),
            b.invokeRule(JavaKeyword.VOID))));
  }

  public ArgumentListTreeImpl arguments() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.ARGUMENTS)
      .is(
        f.completeArguments(
          b.invokeRule(JavaPunctuator.LPAR),
          b.optional(
            f.newArguments(
              expression(),
              b.zeroOrMore(f.newWrapperAstNode2(b.invokeRule(JavaPunctuator.COMMA), (AstNode) expression())))),
          b.invokeRule(JavaPunctuator.RPAR)));
  }

  public ExpressionTree qualifiedIdentifier() {
    return b.<ExpressionTree>nonterminal(JavaLexer.QUALIFIED_IDENTIFIER)
      .is(
        f.newQualifiedIdentifier(
          annotatedParameterizedIdentifier(), b.zeroOrMore(f.newTuple5(b.invokeRule(JavaPunctuator.DOT), annotatedParameterizedIdentifier()))));
  }

  public ExpressionTree annotatedParameterizedIdentifier() {
    return b.<ExpressionTree>nonterminal(JavaLexer.ANNOTATED_PARAMETERIZED_IDENTIFIER)
      .is(f.newAnnotatedParameterizedIdentifier(b.zeroOrMore(annotation()), b.invokeRule(JavaTokenType.IDENTIFIER), b.optional(typeArguments())));
  }

  public ExpressionTree variableInitializer() {
    return b.<ExpressionTree>nonterminal(JavaLexer.VARIABLE_INITIALIZER)
      .is(
        b.firstOf(
          expression(),
          arrayInitializer()));
  }

  public NewArrayTreeImpl arrayInitializer() {
    return b.<NewArrayTreeImpl>nonterminal(JavaLexer.ARRAY_INITIALIZER)
      .is(
        f.newArrayInitializer(
          b.invokeRule(JavaPunctuator.LWING),
          b.zeroOrMore(f.newWrapperAstNode15((AstNode) variableInitializer(), b.optional(b.invokeRule(JavaPunctuator.COMMA)))),
          b.invokeRule(JavaPunctuator.RWING)));
  }

  public QualifiedIdentifierListTreeImpl qualifiedIdentifierList() {
    return b.<QualifiedIdentifierListTreeImpl>nonterminal(JavaLexer.QUALIFIED_IDENTIFIER_LIST)
      .is(f.newQualifiedIdentifierList(qualifiedIdentifier(), b.zeroOrMore(f.newTuple4(b.invokeRule(JavaPunctuator.COMMA), qualifiedIdentifier()))));
  }

  public ArrayAccessExpressionTreeImpl arrayAccessExpression() {
    return b.<ArrayAccessExpressionTreeImpl>nonterminal(JavaLexer.DIM_EXPR)
      .is(f.newArrayAccessExpression(b.zeroOrMore(annotation()), b.invokeRule(JavaPunctuator.LBRK), expression(), b.invokeRule(JavaPunctuator.RBRK)));
  }

  public NewClassTreeImpl classCreatorRest() {
    return b.<NewClassTreeImpl>nonterminal(JavaLexer.CLASS_CREATOR_REST)
      .is(f.newClassCreatorRest(arguments(), b.optional(classBody())));
  }

  public Tuple<AstNode, AstNode> dimension() {
    return b.<Tuple<AstNode, AstNode>>nonterminal(JavaLexer.DIM)
      .is(f.newTuple6(b.invokeRule(JavaPunctuator.LBRK), b.invokeRule(JavaPunctuator.RBRK)));
  }

  public ExpressionTree selector() {
    return b.<ExpressionTree>nonterminal(JavaLexer.SELECTOR)
      .is(
        b.firstOf(
          f.completeMemberSelectOrMethodSelector(b.invokeRule(JavaPunctuator.DOT), identifierOrMethodInvocation()),
          // TODO Perhaps NEW_EXPRESSION() is not as good as before, as it allows NewArrayTree to be constructed
          f.completeCreatorSelector(b.invokeRule(JavaPunctuator.DOT), newExpression()),
          arrayAccessExpression(),
          f.newDotClassSelector(b.zeroOrMore(dimension()), b.invokeRule(JavaPunctuator.DOT), b.invokeRule(JavaKeyword.CLASS))));
  }

  public ExpressionTree identifierOrMethodInvocation() {
    return b.<ExpressionTree>nonterminal(JavaLexer.IDENTIFIER_OR_METHOD_INVOCATION)
      .is(
        f.newIdentifierOrMethodInvocation(
          b.optional(typeArguments()),
          b.firstOf(
            b.invokeRule(JavaTokenType.IDENTIFIER),
            b.invokeRule(JavaKeyword.THIS),
            b.invokeRule(JavaKeyword.SUPER)),
          b.optional(arguments())));
  }

  // End of expressions

}
