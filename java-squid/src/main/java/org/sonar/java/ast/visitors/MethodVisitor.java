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
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceMethod;

public class MethodVisitor extends JavaAstVisitor {

  @Override
  public void init() {
    subscribeTo(
        JavaGrammar.METHOD_DECLARATOR_REST,
        JavaGrammar.VOID_METHOD_DECLARATOR_REST,
        JavaGrammar.CONSTRUCTOR_DECLARATOR_REST,
        JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST,
        JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST,
        Kind.METHOD);
  }

  @Override
  public void visitNode(AstNode astNode) {
    String methodName = extractMethodName(new MethodHelper(astNode));
    SourceClass sourceClass = peekSourceClass();
    // TODO hack grammar to get proper start line
    int startLine = getDeclaration(astNode).getTokenLine();
    Preconditions.checkNotNull(sourceClass);
    Preconditions.checkNotNull(methodName);
    SourceMethod sourceMethod = new SourceMethod(sourceClass, methodName, startLine);
    sourceMethod.setSuppressWarnings(SuppressWarningsAnnotationUtils.isSuppressAllWarnings(astNode));
    getContext().addSourceCode(sourceMethod);
  }

  @Override
  public void leaveNode(AstNode astNode) {
    getContext().popSourceCode();
  }


  private String extractMethodName(MethodHelper methodHelper) {
    if (methodHelper.isConstructor()) {
      return "<init>";
    }
    return methodHelper.getName().getTokenValue();
  }

  private static AstNode getDeclaration(AstNode astNode) {
    AstNode declaration;
    if (astNode.getParent().is(JavaGrammar.MEMBER_DECL)) {
      declaration = astNode.getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.CLASS_BODY_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.GENERIC_METHOD_OR_CONSTRUCTOR_REST)) {
      declaration = astNode.getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.CLASS_BODY_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.INTERFACE_MEMBER_DECL)) {
      declaration = astNode.getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.INTERFACE_BODY_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.INTERFACE_METHOD_OR_FIELD_REST)) {
      declaration = astNode.getParent().getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.INTERFACE_BODY_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.INTERFACE_GENERIC_METHOD_DECL)) {
      declaration = astNode.getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.INTERFACE_BODY_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.TYPE_DECLARATION)) {
      declaration = astNode.getParent();
    } else if (astNode.getParent().is(JavaGrammar.BLOCK_STATEMENT)) {
      declaration = astNode.getParent();
    } else if (astNode.hasAncestor(Kind.METHOD, Kind.ANNOTATION_TYPE)) {
      declaration = astNode.getFirstAncestor(Kind.METHOD, Kind.ANNOTATION_TYPE);
    } else if (astNode.is(Kind.METHOD, Kind.ANNOTATION_TYPE)) {
      declaration = astNode;
    } else {
      throw new IllegalStateException(astNode.getType().toString());
    }
    return declaration;
  }
}
