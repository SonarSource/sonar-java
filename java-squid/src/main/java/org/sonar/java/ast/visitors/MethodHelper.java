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
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Collections;
import java.util.List;

public class MethodHelper {

  private final AstNode astNode;

  public MethodHelper(AstNode astNode) {
    this.astNode = astNode;
  }

  public static void subscribe(SquidAstVisitor<LexerlessGrammar> visitor) {
    visitor.subscribeTo(
      JavaGrammar.METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_METHOD_DECLARATOR_REST,
      JavaGrammar.CONSTRUCTOR_DECLARATOR_REST,
      JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST,
      Kind.METHOD);
  }

  public boolean isPublic() {
    ModifiersTree modifiers;

    if (astNode.is(Kind.METHOD)) {
      modifiers = ((MethodTree) astNode).modifiers();
    } else {
      final AstNode node;

      if (astNode.is(JavaGrammar.METHOD_DECLARATOR_REST, JavaGrammar.VOID_METHOD_DECLARATOR_REST, JavaGrammar.CONSTRUCTOR_DECLARATOR_REST)) {
        node = astNode.getFirstAncestor(JavaGrammar.CLASS_BODY_DECLARATION);
      } else if (astNode.is(JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST, JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST)) {
        node = astNode.getFirstAncestor(JavaGrammar.INTERFACE_BODY_DECLARATION);
      } else {
        throw new IllegalStateException();
      }

      modifiers = (ModifiersTree) node.getFirstChild(JavaGrammar.MODIFIERS);
    }

    return modifiers.modifiers().contains(Modifier.PUBLIC);
  }

  public boolean isConstructor() {
    return astNode.is(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST);
  }

  public AstNode getReturnType() {
    final AstNode typeNode = getName().getPreviousAstNode();
    Preconditions.checkState(typeNode.is(JavaTreeMaker.TYPE_KINDS) || typeNode.is(JavaKeyword.VOID));
    return typeNode;
  }

  public AstNode getName() {
    final AstNode methodNameNode;

    if (astNode.is(Kind.METHOD)) {
      methodNameNode = (AstNode) ((MethodTree) astNode).simpleName();
    } else {
      if (astNode.is(JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST)) {
        methodNameNode = astNode.getPreviousAstNode();
      } else {
        methodNameNode = astNode.getPreviousSibling();
      }
      Preconditions.checkState(methodNameNode.is(JavaTokenType.IDENTIFIER));
    }

    return methodNameNode;
  }

  public List<VariableTree> getParameters() {
    if (astNode.is(Kind.METHOD)) {
      MethodTree tree = (MethodTree) astNode;
      return tree.parameters();
    }

    FormalParametersListTreeImpl node = (FormalParametersListTreeImpl) astNode.getFirstChild(JavaGrammar.FORMAL_PARAMETERS);
    if (node == null) {
      // in case of annotationMethodRest
      return Collections.emptyList();
    }
    // TODO try to avoid usage of "getDescendants" by refactoring grammar rule "formalParameterDecls"
    return (List) node;
  }

  public boolean hasParameters() {
    return !getParameters().isEmpty();
  }

  public List<AstNode> getStatements() {
    if (astNode.is(Kind.METHOD)) {
      MethodTree tree = (MethodTree) astNode;
      return tree.block() == null ?
        Collections.EMPTY_LIST :
        tree.block().body();
    }

    AstNode node = astNode.getFirstChild(JavaGrammar.METHOD_BODY);
    if (node == null) {
      return Collections.emptyList();
    }
    return node.getFirstChild(Kind.BLOCK).getFirstChild(JavaGrammar.BLOCK_STATEMENTS).getChildren();
  }

  public static List<MethodHelper> getMethods(AstNode classOrEnumNode) {
    Preconditions.checkArgument(classOrEnumNode.is(JavaGrammar.CLASS_BODY, JavaGrammar.ENUM_BODY_DECLARATIONS));

    ImmutableList.Builder<MethodHelper> builder = ImmutableList.builder();

    for (AstNode classBodyDeclaration : classOrEnumNode.getChildren(JavaGrammar.CLASS_BODY_DECLARATION)) {
      AstNode memberDecl = classBodyDeclaration.getFirstChild(JavaGrammar.MEMBER_DECL);
      if (memberDecl != null) {
        AstNode actualMember = getFirstDescendant(memberDecl,
          JavaGrammar.METHOD_DECLARATOR_REST,
          JavaGrammar.CONSTRUCTOR_DECLARATOR_REST,
          JavaGrammar.FIELD_DECLARATION,
          JavaGrammar.VOID_METHOD_DECLARATOR_REST,
          JavaGrammar.CONSTRUCTOR_DECLARATOR_REST,
          JavaGrammar.INTERFACE_DECLARATION,
          JavaGrammar.CLASS_DECLARATION,
          JavaGrammar.ENUM_DECLARATION,
          Kind.METHOD);
        if (actualMember.is(JavaGrammar.METHOD_DECLARATOR_REST, JavaGrammar.VOID_METHOD_DECLARATOR_REST, Kind.METHOD)) {
          builder.add(new MethodHelper(actualMember));
        }
      }
    }

    return builder.build();
  }

  /**
   * SSLR-345
   */
  private static AstNode getFirstDescendant(AstNode node, AstNodeType... nodeTypes) {
    for (AstNode child : node.getChildren()) {
      if (child.is(nodeTypes)) {
        return child;
      }
      AstNode result = getFirstDescendant(child, nodeTypes);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

}
