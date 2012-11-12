/*
 * Sonar Java
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
package org.sonar.java.ast.api;

import com.sonar.sslr.api.Rule;
import org.sonar.sslr.parser.LexerlessGrammar;

public abstract class JavaGrammar extends LexerlessGrammar {

  public Rule compilationUnit;
  public Rule packageDeclaration;
  public Rule importDeclaration;
  public Rule typeDeclaration;

  public Rule annotation;
  public Rule qualifiedIdentifier;

  public Rule modifier;
  public Rule classDeclaration;
  public Rule enumDeclaration;
  public Rule interfaceDeclaration;
  public Rule annotationTypeDeclaration;

  public Rule typeParameters;
  public Rule classType;
  public Rule classTypeList;
  public Rule classBody;

  public Rule classBodyDeclaration;
  public Rule classInitDeclaration;

  public Rule block;
  public Rule memberDecl;

  public Rule fieldDeclaration;

  public Rule genericMethodOrConstructorRest;
  public Rule type;
  public Rule methodDeclaratorRest;
  public Rule variableDeclarators;
  public Rule voidMethodDeclaratorRest;
  public Rule constructorDeclaratorRest;

  public Rule formalParameters;
  public Rule dim;
  public Rule methodBody;

  public Rule interfaceBody;

  public Rule interfaceBodyDeclaration;

  public Rule interfaceMemberDecl;

  public Rule interfaceMethodOrFieldDecl;
  public Rule interfaceGenericMethodDecl;
  public Rule voidInterfaceMethodDeclaratorsRest;

  public Rule interfaceMethodOrFieldRest;

  public Rule constantDeclaratorsRest;
  public Rule interfaceMethodDeclaratorRest;

  public Rule constantDeclaratorRest;
  public Rule constantDeclarator;

  public Rule variableInitializer;

  public Rule enumBody;

  public Rule enumConstants;
  public Rule enumBodyDeclarations;

  public Rule enumConstant;

  public Rule arguments;

  public Rule localVariableDeclarationStatement;
  public Rule variableModifiers;
  public Rule variableDeclarator;

  public Rule formalParameter;
  public Rule formalParameterDecls;
  public Rule formalParametersDeclsRest;

  public Rule variableDeclaratorId;

  public Rule blockStatements;
  public Rule blockStatement;

  public Rule statement;
  public Rule labeledStatement;
  public Rule expressionStatement;
  public Rule ifStatement;
  public Rule whileStatement;
  public Rule forStatement;
  public Rule assertStatement;
  public Rule switchStatement;
  public Rule doStatement;
  public Rule breakStatement;
  public Rule continueStatement;
  public Rule returnStatement;
  public Rule synchronizedStatement;
  public Rule throwStatement;
  public Rule emptyStatement;

  public Rule expression;
  public Rule resource;
  public Rule parExpression;
  public Rule forInit;
  public Rule forUpdate;

  public Rule catchClause;
  public Rule catchFormalParameter;
  public Rule catchType;

  public Rule finally_;
  public Rule switchBlockStatementGroups;
  public Rule statementExpression;

  public Rule tryStatement;
  public Rule tryWithResourcesStatement;
  public Rule resourceSpecification;

  public Rule switchBlockStatementGroup;

  public Rule switchLabel;

  public Rule constantExpression;
  public Rule enumConstantName;

  public Rule basicType;
  public Rule referenceType;
  public Rule typeArguments;
  public Rule typeArgument;
  public Rule typeParameter;
  public Rule bound;

  public Rule conditionalExpression;
  public Rule defaultValue;

  public Rule annotationTypeBody;
  public Rule annotationTypeElementDeclaration;
  public Rule annotationTypeElementRest;
  public Rule annotationMethodOrConstantRest;
  public Rule annotationMethodRest;
  public Rule annotationConstantRest;
  public Rule annotationRest;
  public Rule normalAnnotationRest;
  public Rule elementValuePairs;
  public Rule elementValuePair;
  public Rule elementValue;
  public Rule elementValueArrayInitializer;
  public Rule elementValues;
  public Rule singleElementAnnotationRest;

  public Rule assignmentExpression;
  public Rule assignmentOperator;
  public Rule conditionalOrExpression;
  public Rule conditionalAndExpression;
  public Rule inclusiveOrExpression;
  public Rule exclusiveOrExpression;
  public Rule andExpression;
  public Rule equalityExpression;
  public Rule relationalExpression;
  public Rule shiftExpression;
  public Rule additiveExpression;
  public Rule multiplicativeExpression;
  public Rule unaryExpression;
  public Rule prefixOp;
  public Rule primary;
  public Rule selector;
  public Rule postFixOp;
  public Rule nonWildcardTypeArguments;
  public Rule explicitGenericInvocationSuffix;
  public Rule superSuffix;
  public Rule literal;
  public Rule creator;
  public Rule identifierSuffix;
  public Rule explicitGenericInvocation;
  public Rule innerCreator;
  public Rule dimExpr;
  public Rule createdName;
  public Rule classCreatorRest;
  public Rule diamond;
  public Rule arrayCreatorRest;
  public Rule arrayInitializer;

  public Rule at; // @
  public Rule and; // &
  public Rule andand; // &&
  public Rule andequ; // &=
  public Rule bang; // !
  public Rule bsr; // >>>
  public Rule bsrequ; // >>>=
  public Rule colon; // :
  public Rule comma; // ,
  public Rule dec; // --
  public Rule div; // /
  public Rule divequ; // /=
  public Rule dot; // .
  public Rule ellipsis; // ...
  public Rule equ; // =
  public Rule equal; // ==
  public Rule ge; // >=
  public Rule gt; // >
  public Rule hat; // ^
  public Rule hatequ; // ^=
  public Rule inc; // ++
  public Rule lbrk; // [
  public Rule lt; // <
  public Rule le; // <=
  public Rule lpar; // )
  public Rule lwing; // {
  public Rule minus; // -
  public Rule minsequ; // -=
  public Rule mod; // %
  public Rule modequ; // %=
  public Rule notequal; // !=
  public Rule or; // |
  public Rule orequ; // |=
  public Rule oror; // ||
  public Rule plus; // +
  public Rule plusequ; // +=
  public Rule query; // ?
  public Rule rbrk; // ]
  public Rule rpar; // )
  public Rule rwing; // }
  public Rule semi; // ;
  public Rule sl; // <<
  public Rule slequ; // <<=
  public Rule sr; // >>
  public Rule srequ; // >>=
  public Rule star; // *
  public Rule starequ; // *=
  public Rule tilda; // ~

  public Rule lpoint; // <
  public Rule rpoint; // >

  public Rule assertKeyword;
  public Rule breakKeyword;
  public Rule caseKeyword;
  public Rule catchKeyword;
  public Rule classKeyword;
  public Rule continueKeyword;
  public Rule defaultKeyword;
  public Rule doKeyword;
  public Rule elseKeyword;
  public Rule enumKeyword;
  public Rule extendsKeyword;
  public Rule finallyKeyword;
  public Rule finalKeyword;
  public Rule forKeyword;
  public Rule ifKeyword;
  public Rule implementsKeyword;
  public Rule importKeyword;
  public Rule interfaceKeyword;
  public Rule instanceofKeyword;
  public Rule newKeyword;
  public Rule packageKeyword;
  public Rule returnKeyword;
  public Rule staticKeyword;
  public Rule superKeyword;
  public Rule switchKeyword;
  public Rule synchronizedKeyword;
  public Rule thisKeyword;
  public Rule throwsKeyword;
  public Rule throwKeyword;
  public Rule tryKeyword;
  public Rule voidKeyword;
  public Rule whileKeyword;
  public Rule trueKeyword;
  public Rule falseKeyword;
  public Rule nullKeyword;
  public Rule publicKeyword;
  public Rule protectedKeyword;
  public Rule privateKeyword;
  public Rule abstractKeyword;
  public Rule nativeKeyword;
  public Rule transientKeyword;
  public Rule volatileKeyword;
  public Rule strictfpKeyword;
  public Rule byteKeyword;
  public Rule shortKeyword;
  public Rule charKeyword;
  public Rule intKeyword;
  public Rule longKeyword;
  public Rule floatKeyword;
  public Rule doubleKeyword;
  public Rule booleanKeyword;

  public Rule identifier;
  public Rule eof;
  public Rule floatingLiteral;
  public Rule integerLiteral;
  public Rule characterLiteral;
  public Rule stringLiteral;

  protected Rule letterOrDigit;
  protected Rule keyword;
  protected Rule spacing;

  @Override
  public Rule getRootRule() {
    return compilationUnit;
  }

}
