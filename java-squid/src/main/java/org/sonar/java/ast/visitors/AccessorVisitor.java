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
package org.sonar.java.ast.visitors;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.squidbridge.api.SourceMethod;
import org.sonar.squidbridge.measures.Metric;

import java.util.List;

public class AccessorVisitor extends JavaAstVisitor {

  @Override
  public void init() {
    MethodHelper.subscribe(this);
  }

  @Override
  public void visitNode(AstNode astNode) {
    SourceMethod sourceMethod = (SourceMethod) getContext().peekSourceCode();

    if (astNode.is(JavaGrammar.METHOD_DECLARATOR_REST, JavaGrammar.VOID_METHOD_DECLARATOR_REST)) {
      MethodHelper methodHelper = new MethodHelper(astNode);
      if (methodHelper.isPublic() && isAccessor(methodHelper)) {
        sourceMethod.setMeasure(Metric.ACCESSORS, 1);
      }
    }
  }

  @Override
  public void leaveNode(AstNode astNode) {
    SourceMethod sourceMethod = (SourceMethod) getContext().peekSourceCode();
    if (sourceMethod.isAccessor()) {
      sourceMethod.setMeasure(Metric.PUBLIC_API, 0);
      sourceMethod.setMeasure(Metric.PUBLIC_DOC_API, 0);
      sourceMethod.setMeasure(JavaMetric.METHODS, 0);
      sourceMethod.setMeasure(JavaMetric.COMPLEXITY, 0);
    }
  }

  private boolean isAccessor(MethodHelper method) {
    return isValidGetter(method) || isValidSetter(method) || isValidBooleanGetter(method);
  }

  private boolean isValidGetter(MethodHelper method) {
    String methodName = method.getName().getTokenValue();
    if (methodName.startsWith("get") && !method.hasParameters() && !method.getReturnType().is(JavaKeyword.VOID)) {
      List<AstNode> statements = method.getStatements();
      if (statements.size() == 1) {
        AstNode blockStatement = statements.get(0);
        Preconditions.checkState(blockStatement.is(JavaGrammar.BLOCK_STATEMENT));
        return inspectGetterMethodBody(blockStatement.getFirstChild().getFirstChild());
      }
    }
    return false;
  }

  private boolean isValidSetter(MethodHelper method) {
    String methodName = method.getName().getTokenValue();
    if (methodName.startsWith("set") && (method.getParameters().size() == 1) && method.getReturnType().is(JavaKeyword.VOID)) {
      List<AstNode> statements = method.getStatements();
      if (statements.size() == 1) {
        AstNode blockStatement = statements.get(0);
        Preconditions.checkState(blockStatement.is(JavaGrammar.BLOCK_STATEMENT));
        return inspectSetterMethodBody(blockStatement.getFirstChild().getFirstChild());
      }
    }
    return false;
  }

  private boolean inspectSetterMethodBody(AstNode astNode) {
    if (astNode.is(JavaGrammar.EXPRESSION_STATEMENT)) {
      AstNode expression = astNode.getFirstChild(JavaGrammar.STATEMENT_EXPRESSION).getFirstChild(JavaGrammar.EXPRESSION);
      AstNode assignmentExpression = expression.getFirstChild();
      if (assignmentExpression.is(JavaGrammar.ASSIGNMENT_EXPRESSION)) {
        // TODO in previous version we had a more complex check
        // TODO try to avoid usage of "getFirstDescendant" by refactoring grammar
        AstNode varToAssign = assignmentExpression.getFirstDescendant(JavaTokenType.IDENTIFIER);
        return findPrivateClassVariable(varToAssign);
      }
    }
    return false;
  }

  private boolean isValidBooleanGetter(MethodHelper method) {
    String methodName = method.getName().getTokenValue();
    if (methodName.startsWith("is") && !method.hasParameters() && hasBooleanReturnType(method)) {
      List<AstNode> statements = method.getStatements();
      if (statements.size() == 1) {
        AstNode blockStatement = statements.get(0);
        Preconditions.checkState(blockStatement.is(JavaGrammar.BLOCK_STATEMENT));
        return inspectGetterMethodBody(blockStatement.getFirstChild().getFirstChild());
      }
    }
    return false;
  }

  private boolean inspectGetterMethodBody(AstNode astNode) {
    if (astNode.is(JavaGrammar.RETURN_STATEMENT) && astNode.hasDirectChildren(JavaGrammar.EXPRESSION)) {
      AstNode expression = astNode.getFirstChild(JavaGrammar.EXPRESSION);
      if (expression.getNumberOfChildren() == 1 && expression.getFirstChild().is(JavaGrammar.PRIMARY)) {
        AstNode primary = expression.getFirstChild();
        if (primary.getNumberOfChildren() == 1 && primary.getFirstChild().is(JavaGrammar.QUALIFIED_IDENTIFIER)) {
          AstNode qualifiedIdentifier = primary.getFirstChild();
          if (qualifiedIdentifier.getNumberOfChildren() == 1) {
            AstNode varReturned = qualifiedIdentifier.getFirstChild();
            if (findPrivateClassVariable(varReturned)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private boolean findPrivateClassVariable(AstNode varReturned) {
    AstNode classBody = varReturned.getFirstAncestor(JavaGrammar.CLASS_BODY_DECLARATION).getParent();
    for (AstNode classBodyDeclaration : classBody.getChildren(JavaGrammar.CLASS_BODY_DECLARATION)) {
      if (!hasPrivateModifier(classBodyDeclaration)) {
        continue;
      }

      for (AstNode memberDecl : classBodyDeclaration.getChildren(JavaGrammar.MEMBER_DECL)) {
        AstNode fieldDeclaration = memberDecl.getFirstChild(JavaGrammar.FIELD_DECLARATION);
        if (fieldDeclaration != null) {
          for (AstNode variableDeclarator : fieldDeclaration.getFirstChild(JavaGrammar.VARIABLE_DECLARATORS).getChildren(JavaGrammar.VARIABLE_DECLARATOR)) {
            if (varReturned.getTokenValue().equals(variableDeclarator.getFirstChild().getTokenValue())) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private boolean hasPrivateModifier(AstNode classBodyDeclaration) {
    for (AstNode modifierNode : classBodyDeclaration.getChildren(JavaGrammar.MODIFIER)) {
      if (modifierNode.getChild(0).is(JavaKeyword.PRIVATE)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasBooleanReturnType(MethodHelper method) {
    AstNode typeNode = method.getReturnType();
    return typeNode.isNot(JavaKeyword.VOID) && typeNode.getChildren().size() == 1 && typeNode.getChild(0).getChild(0).is(JavaKeyword.BOOLEAN);
  }

}
