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

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Rule;

public abstract class JavaGrammar extends Grammar {

  /**
   * >=
   */
  public Rule ge;

  /**
   * >>
   */
  public Rule sr;

  /**
   * >>=
   */
  public Rule srequ;

  /**
   * >>>
   */
  public Rule bsr;

  /**
   * >>>=
   */
  public Rule bsrequ;

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

  @Override
  public Rule getRootRule() {
    return compilationUnit;
  }

}
