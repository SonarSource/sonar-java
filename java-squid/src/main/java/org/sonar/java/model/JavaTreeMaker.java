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
package org.sonar.java.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.impl.ast.AstXmlPrinter;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.util.List;

public class JavaTreeMaker {

  private final KindMaps kindMaps = new KindMaps();

  private static void checkType(AstNode astNode, AstNodeType... expected) {
    Preconditions.checkArgument(astNode.is(expected), "Unexpected AstNodeType: %s", astNode.getType().toString());
  }

  private IdentifierTree identifier(AstNode astNode) {
    checkType(astNode, JavaTokenType.IDENTIFIER, JavaKeyword.THIS, JavaKeyword.CLASS, JavaKeyword.SUPER);
    return new JavaTree.IdentifierTreeImpl(astNode, astNode.getTokenValue());
  }

  private ExpressionTree qualifiedIdentifier(AstNode astNode) {
    checkType(astNode, JavaGrammar.QUALIFIED_IDENTIFIER);
    List<AstNode> identifierNodes = astNode.getChildren(JavaTokenType.IDENTIFIER);
    ExpressionTree result = identifier(identifierNodes.get(0));
    for (int i = 1; i < identifierNodes.size(); i++) {
      result = new JavaTree.MemberSelectExpressionTreeImpl(
        identifierNodes.get(i),
        result,
        identifier(identifierNodes.get(i))
        );
    }
    return result;
  }

  private List<ExpressionTree> qualifiedIdentifierList(AstNode astNode) {
    checkType(astNode, JavaGrammar.QUALIFIED_IDENTIFIER_LIST);
    ImmutableList.Builder<ExpressionTree> result = ImmutableList.builder();
    for (AstNode qualifiedIdentifierNode : astNode.getChildren(JavaGrammar.QUALIFIED_IDENTIFIER)) {
      result.add(qualifiedIdentifier(qualifiedIdentifierNode));
    }
    return result.build();
  }

  @VisibleForTesting
  LiteralTree literal(AstNode astNode) {
    checkType(astNode, JavaGrammar.LITERAL);
    AstNode childNode = astNode.getFirstChild();
    return new JavaTree.LiteralTreeImpl(childNode, kindMaps.getLiteral(childNode.getType()));
  }

  /*
   * 4. Types, Values and Variables
   */

  @VisibleForTesting
  PrimitiveTypeTree basicType(AstNode astNode) {
    checkType(astNode, JavaGrammar.BASIC_TYPE, JavaKeyword.VOID);
    return new JavaTree.PrimitiveTypeTreeImpl(astNode);
  }

  private ExpressionTree classType(AstNode astNode) {
    checkType(astNode, JavaGrammar.CLASS_TYPE, JavaGrammar.CREATED_NAME);
    AstNode child = astNode.getFirstChild();
    ExpressionTree result = identifier(child);
    for (int i = 1; i < astNode.getNumberOfChildren(); i++) {
      child = astNode.getChild(i);
      if (child.is(JavaTokenType.IDENTIFIER)) {
        result = new JavaTree.MemberSelectExpressionTreeImpl(child, result, identifier(child));
      } else if (child.is(JavaGrammar.TYPE_ARGUMENTS)) {
        result = new JavaTree.ParameterizedTypeTreeImpl(child, result, typeArguments(child));
      } else if (child.is(JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS)) {
        result = new JavaTree.ParameterizedTypeTreeImpl(child, result, nonWildcardTypeArguments(child));
      } else if (!child.is(JavaPunctuator.DOT)) {
        throw new IllegalStateException("Unexpected AstNodeType: " + astNode.getType().toString());
      }
    }
    return result;
  }

  @VisibleForTesting
  List<Tree> typeArguments(AstNode astNode) {
    checkType(astNode, JavaGrammar.TYPE_ARGUMENTS);
    ImmutableList.Builder<Tree> result = ImmutableList.builder();
    for (AstNode child : astNode.getChildren(JavaGrammar.TYPE_ARGUMENT)) {
      AstNode referenceTypeNode = child.getFirstChild(JavaGrammar.REFERENCE_TYPE);
      Tree typeArgument = referenceTypeNode != null ? referenceType(referenceTypeNode) : null;
      if (child.getFirstChild().is(JavaPunctuator.QUERY)) {
        final Tree.Kind kind;
        if (child.hasDirectChildren(JavaKeyword.EXTENDS)) {
          kind = Tree.Kind.EXTENDS_WILDCARD;
        } else if (child.hasDirectChildren(JavaKeyword.SUPER)) {
          kind = Tree.Kind.SUPER_WILDCARD;
        } else {
          kind = Tree.Kind.UNBOUNDED_WILDCARD;
        }
        typeArgument = new JavaTree.WildcardTreeImpl(child, kind, typeArgument);
      }
      result.add(typeArgument);
    }
    return result.build();
  }

  private List<Tree> nonWildcardTypeArguments(AstNode astNode) {
    checkType(astNode, JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS);
    ImmutableList.Builder<Tree> result = ImmutableList.builder();
    for (AstNode child : astNode.getChildren(JavaGrammar.REFERENCE_TYPE)) {
      result.add(referenceType(child));
    }
    return result.build();
  }

  @VisibleForTesting
  ExpressionTree referenceType(AstNode astNode) {
    checkType(astNode, JavaGrammar.REFERENCE_TYPE, JavaGrammar.TYPE);
    ExpressionTree result = astNode.getFirstChild().is(JavaGrammar.BASIC_TYPE) ? basicType(astNode.getFirstChild()) : classType(astNode.getFirstChild());
    return applyDim(result, astNode.getChildren(JavaGrammar.DIM).size());
  }

  private ModifiersTree modifiers(List<AstNode> modifierNodes) {
    if (modifierNodes.isEmpty()) {
      return JavaTree.ModifiersTreeImpl.EMPTY;
    }

    ImmutableList.Builder<Modifier> modifiers = ImmutableList.builder();
    for (AstNode astNode : modifierNodes) {
      Preconditions.checkArgument(astNode.is(JavaGrammar.MODIFIER), "Unexpected AstNodeType: %s", astNode.getType().toString());
      astNode = astNode.getFirstChild();
      if (astNode.is(JavaGrammar.ANNOTATION)) {
        // TODO
      } else {
        JavaKeyword keyword = (JavaKeyword) astNode.getType();
        modifiers.add(kindMaps.getModifier(keyword));
      }
    }
    return new JavaTree.ModifiersTreeImpl(modifierNodes.get(0), modifiers.build());
  }

  private VariableTree variableDeclarator(ModifiersTree modifiers, ExpressionTree type, AstNode astNode) {
    checkType(astNode, JavaGrammar.VARIABLE_DECLARATOR);
    return new JavaTree.VariableTreeImpl(
      astNode,
      modifiers,
      applyDim(type, astNode.getChildren(JavaGrammar.DIM).size()),
      astNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue(),
      astNode.hasDirectChildren(JavaGrammar.VARIABLE_INITIALIZER) ? variableInitializer(astNode.getFirstChild(JavaGrammar.VARIABLE_INITIALIZER)) : null);
  }

  private List<StatementTree> variableDeclarators(ModifiersTree modifiers, ExpressionTree type, AstNode astNode) {
    checkType(astNode, JavaGrammar.VARIABLE_DECLARATORS);
    ImmutableList.Builder<StatementTree> result = ImmutableList.builder();
    for (AstNode variableDeclaratorNode : astNode.getChildren(JavaGrammar.VARIABLE_DECLARATOR)) {
      result.add(variableDeclarator(modifiers, type, variableDeclaratorNode));
    }
    return result.build();
  }

  /*
   * 7.3. Compilation Units
   */

  public CompilationUnitTree compilationUnit(AstNode astNode) {
    checkType(astNode, JavaGrammar.COMPILATION_UNIT);
    ImmutableList.Builder<ImportTree> imports = ImmutableList.builder();
    for (AstNode importNode : astNode.getChildren(JavaGrammar.IMPORT_DECLARATION)) {
      // TODO star import?
      imports.add(new JavaTree.ImportTreeImpl(
        importNode,
        importNode.hasDirectChildren(JavaKeyword.STATIC),
        qualifiedIdentifier(importNode.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER))
        ));
    }
    ImmutableList.Builder<Tree> types = ImmutableList.builder();
    for (AstNode typeNode : astNode.getChildren(JavaGrammar.TYPE_DECLARATION)) {
      AstNode declarationNode = typeNode.getFirstChild(
        JavaGrammar.CLASS_DECLARATION,
        JavaGrammar.ENUM_DECLARATION,
        JavaGrammar.INTERFACE_DECLARATION,
        JavaGrammar.ANNOTATION_TYPE_DECLARATION
        );
      if (declarationNode != null) {
        types.add(typeDeclaration(modifiers(typeNode.getChildren(JavaGrammar.MODIFIER)), declarationNode));
      }
    }
    // TODO package annotations
    ExpressionTree packageDeclaration = null;
    if (astNode.hasDirectChildren(JavaGrammar.PACKAGE_DECLARATION)) {
      packageDeclaration = qualifiedIdentifier(astNode.getFirstChild(JavaGrammar.PACKAGE_DECLARATION).getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER));
    }
    return new JavaTree.CompilationUnitTreeImpl(
      astNode,
      packageDeclaration,
      imports.build(),
      types.build());
  }

  private ClassTree typeDeclaration(ModifiersTree modifiers, AstNode astNode) {
    if (astNode.is(JavaGrammar.CLASS_DECLARATION)) {
      return classDeclaration(modifiers, astNode);
    } else if (astNode.is(JavaGrammar.ENUM_DECLARATION)) {
      return enumDeclaration(modifiers, astNode);
    } else if (astNode.is(JavaGrammar.INTERFACE_DECLARATION)) {
      return interfaceDeclaration(modifiers, astNode);
    } else if (astNode.is(JavaGrammar.ANNOTATION_TYPE_DECLARATION)) {
      return annotationTypeDeclaration(modifiers, astNode);
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + astNode.getType().toString());
    }
  }

  /*
   * 8. Classes
   */

  /**
   * 8.1. Class Declarations
   */
  private ClassTree classDeclaration(ModifiersTree modifiers, AstNode astNode) {
    checkType(astNode, JavaGrammar.CLASS_DECLARATION);
    String simpleName = astNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue();
    AstNode extendsNode = astNode.getFirstChild(JavaKeyword.EXTENDS);
    Tree superClass = extendsNode != null ? classType(extendsNode.getNextSibling()) : null;
    AstNode implementsNode = astNode.getFirstChild(JavaKeyword.IMPLEMENTS);
    List<Tree> superInterfaces = implementsNode != null ? classTypeList(implementsNode.getNextSibling()) : ImmutableList.<Tree>of();
    return new JavaTree.ClassTreeImpl(astNode, Tree.Kind.CLASS,
      modifiers,
      simpleName,
      superClass,
      superInterfaces,
      classBody(astNode.getFirstChild(JavaGrammar.CLASS_BODY)));
  }

  private List<Tree> classTypeList(AstNode astNode) {
    checkType(astNode, JavaGrammar.CLASS_TYPE_LIST);
    ImmutableList.Builder<Tree> result = ImmutableList.builder();
    for (AstNode classTypeNode : astNode.getChildren(JavaGrammar.CLASS_TYPE)) {
      result.add(classType(classTypeNode));
    }
    return result.build();
  }

  /**
   * 8.1.6. Class Body and Member Declarations
   */
  private List<Tree> classBody(AstNode astNode) {
    checkType(astNode, JavaGrammar.CLASS_BODY, JavaGrammar.ENUM_BODY_DECLARATIONS);
    ImmutableList.Builder<Tree> members = ImmutableList.builder();
    for (AstNode classBodyDeclaration : astNode.getChildren(JavaGrammar.CLASS_BODY_DECLARATION)) {
      ModifiersTree modifiers = modifiers(classBodyDeclaration.getChildren(JavaGrammar.MODIFIER));
      if (classBodyDeclaration.hasDirectChildren(JavaGrammar.MEMBER_DECL)) {
        AstNode memberDeclNode = classBodyDeclaration.getFirstChild(JavaGrammar.MEMBER_DECL);
        if (memberDeclNode.hasDirectChildren(JavaGrammar.FIELD_DECLARATION)) {
          members.addAll(fieldDeclaration(
            modifiers,
            memberDeclNode.getFirstChild(JavaGrammar.FIELD_DECLARATION)
            ));
        } else {
          members.add(memberDeclaration(modifiers, memberDeclNode));
        }
      } else if (classBodyDeclaration.getFirstChild().is(JavaGrammar.CLASS_INIT_DECLARATION)) {
        AstNode classInitDeclarationNode = classBodyDeclaration.getFirstChild();
        members.add(new JavaTree.BlockTreeImpl(
          classInitDeclarationNode,
          classInitDeclarationNode.hasDirectChildren(JavaKeyword.STATIC) ? Tree.Kind.STATIC_INITIALIZER : Tree.Kind.INITIALIZER,
          blockStatements(classInitDeclarationNode.getFirstChild(JavaGrammar.BLOCK).getFirstChild(JavaGrammar.BLOCK_STATEMENTS))
          ));
      }
    }
    return members.build();
  }

  /**
   * 8.2. Class Members
   */
  private Tree memberDeclaration(ModifiersTree modifiers, AstNode astNode) {
    checkType(astNode, JavaGrammar.MEMBER_DECL);
    AstNode declaration = astNode.getFirstChild(
      JavaGrammar.INTERFACE_DECLARATION,
      JavaGrammar.CLASS_DECLARATION,
      JavaGrammar.ENUM_DECLARATION,
      JavaGrammar.ANNOTATION_TYPE_DECLARATION
      );
    if (declaration != null) {
      return typeDeclaration(modifiers, declaration);
    }
    declaration = astNode.getFirstChild(JavaGrammar.GENERIC_METHOD_OR_CONSTRUCTOR_REST);
    if (declaration != null) {
      // TODO TYPE_PARAMETERS
      return methodDeclarator(
        modifiers,
        /* type */declaration.getFirstChild(JavaGrammar.TYPE, JavaKeyword.VOID),
        /* name */declaration.getFirstChild(JavaTokenType.IDENTIFIER),
        declaration.getFirstChild(JavaGrammar.METHOD_DECLARATOR_REST, JavaGrammar.CONSTRUCTOR_DECLARATOR_REST));
    }
    declaration = astNode.getFirstChild(
      JavaGrammar.METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_METHOD_DECLARATOR_REST,
      JavaGrammar.CONSTRUCTOR_DECLARATOR_REST
      );
    if (declaration != null) {
      return methodDeclarator(
        modifiers,
        /* type */astNode.getFirstChild(JavaGrammar.TYPE, JavaKeyword.VOID),
        /* name */astNode.getFirstChild(JavaTokenType.IDENTIFIER),
        declaration);
    }
    throw new IllegalStateException();
  }

  /**
   * 8.3. Field Declarations
   */
  private List<StatementTree> fieldDeclaration(ModifiersTree modifiers, AstNode astNode) {
    checkType(astNode, JavaGrammar.FIELD_DECLARATION);
    return variableDeclarators(modifiers, referenceType(astNode.getFirstChild(JavaGrammar.TYPE)), astNode.getFirstChild(JavaGrammar.VARIABLE_DECLARATORS));
  }

  /**
   * 8.4. Method Declarations
   */
  private MethodTree methodDeclarator(ModifiersTree modifiers, @Nullable AstNode returnTypeNode, AstNode name, AstNode astNode) {
    checkType(name, JavaTokenType.IDENTIFIER);
    checkType(astNode, JavaGrammar.METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_METHOD_DECLARATOR_REST,
      JavaGrammar.CONSTRUCTOR_DECLARATOR_REST,
      JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST,
      JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST);
    // TODO type parameters
    Tree returnType = null;
    if (returnTypeNode != null) {
      if (returnTypeNode.is(JavaKeyword.VOID)) {
        returnType = basicType(returnTypeNode);
      } else {
        returnType = referenceType(returnTypeNode);
      }
    }
    BlockTree body = null;
    if (astNode.hasDirectChildren(JavaGrammar.METHOD_BODY)) {
      body = block(astNode.getFirstChild(JavaGrammar.METHOD_BODY).getFirstChild(JavaGrammar.BLOCK));
    }
    AstNode throwsClauseNode = astNode.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER_LIST);
    return new JavaTree.MethodTreeImpl(
      astNode,
      modifiers,
      returnType,
      name.getTokenValue(),
      formalParameters(astNode.getFirstChild(JavaGrammar.FORMAL_PARAMETERS)),
      body,
      throwsClauseNode != null ? qualifiedIdentifierList(throwsClauseNode) : ImmutableList.<ExpressionTree>of(),
      null);
  }

  private List<VariableTree> formalParameters(AstNode astNode) {
    checkType(astNode, JavaGrammar.FORMAL_PARAMETERS);
    ImmutableList.Builder<VariableTree> result = ImmutableList.builder();
    for (AstNode variableDeclaratorIdNode : astNode.getDescendants(JavaGrammar.VARIABLE_DECLARATOR_ID)) {
      AstNode typeNode = variableDeclaratorIdNode.getPreviousAstNode();
      Tree type = typeNode.is(JavaPunctuator.ELLIPSIS) ? new JavaTree.ArrayTypeTreeImpl(typeNode, referenceType(typeNode.getPreviousAstNode())) : referenceType(typeNode);
      result.add(new JavaTree.VariableTreeImpl(
        variableDeclaratorIdNode,
        JavaTree.ModifiersTreeImpl.EMPTY,
        type,
        variableDeclaratorIdNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue(),
        null
        ));
    }
    return result.build();
  }

  /**
   * 8.9. Enums
   */
  private ClassTree enumDeclaration(ModifiersTree modifiers, AstNode astNode) {
    checkType(astNode, JavaGrammar.ENUM_DECLARATION);
    IdentifierTree enumType = identifier(astNode.getFirstChild(JavaTokenType.IDENTIFIER));
    ImmutableList.Builder<Tree> members = ImmutableList.builder();
    AstNode enumBodyNode = astNode.getFirstChild(JavaGrammar.ENUM_BODY);
    AstNode enumConstantsNode = enumBodyNode.getFirstChild(JavaGrammar.ENUM_CONSTANTS);
    if (enumConstantsNode != null) {
      for (AstNode enumConstantNode : enumConstantsNode.getChildren(JavaGrammar.ENUM_CONSTANT)) {
        AstNode argumentsNode = enumConstantNode.getFirstChild(JavaGrammar.ARGUMENTS);
        AstNode classBodyNode = enumConstantNode.getFirstChild(JavaGrammar.CLASS_BODY);
        IdentifierTree enumIdentifier = identifier(enumConstantNode.getFirstChild(JavaTokenType.IDENTIFIER));
        members.add(new JavaTree.EnumConstantTreeImpl(
          enumConstantNode,
          JavaTree.ModifiersTreeImpl.EMPTY,
          enumType,
          enumIdentifier.name(),
          new JavaTree.NewClassTreeImpl(
            enumConstantNode,
            /* enclosing expression: */null,
            enumIdentifier,
            argumentsNode != null ? arguments(argumentsNode) : ImmutableList.<ExpressionTree>of(),
            classBodyNode == null ? null : new JavaTree.ClassTreeImpl(
              classBodyNode,
              Tree.Kind.CLASS,
              JavaTree.ModifiersTreeImpl.EMPTY,
              classBody(classBodyNode)
              )
          )
          ));
      }
    }
    AstNode enumBodyDeclarationsNode = enumBodyNode.getFirstChild(JavaGrammar.ENUM_BODY_DECLARATIONS);
    if (enumBodyDeclarationsNode != null) {
      members.addAll(classBody(enumBodyDeclarationsNode));
    }
    AstNode implementsNode = astNode.getFirstChild(JavaKeyword.IMPLEMENTS);
    List<Tree> superInterfaces = implementsNode != null ? classTypeList(implementsNode.getNextSibling()) : ImmutableList.<Tree>of();
    return new JavaTree.ClassTreeImpl(astNode, Tree.Kind.ENUM, modifiers, enumType.name(), /* super class: */null, superInterfaces, members.build());
  }

  /*
   * 9. Interfaces
   */

  /**
   * 9.1. Interface Declarations
   */
  private ClassTree interfaceDeclaration(ModifiersTree modifiers, AstNode astNode) {
    checkType(astNode, JavaGrammar.INTERFACE_DECLARATION);
    String simpleName = astNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue();
    ImmutableList.Builder<Tree> members = ImmutableList.builder();
    for (AstNode interfaceBodyDeclarationNode : astNode.getFirstChild(JavaGrammar.INTERFACE_BODY).getChildren(JavaGrammar.INTERFACE_BODY_DECLARATION)) {
      ModifiersTree memberModifiers = modifiers(interfaceBodyDeclarationNode.getChildren(JavaGrammar.MODIFIER));
      AstNode interfaceMemberDeclNode = interfaceBodyDeclarationNode.getFirstChild(JavaGrammar.INTERFACE_MEMBER_DECL);
      if (interfaceMemberDeclNode != null) {
        appendInterfaceMember(memberModifiers, members, interfaceMemberDeclNode);
      }
    }
    AstNode extendsNode = astNode.getFirstChild(JavaKeyword.EXTENDS);
    List<Tree> superInterfaces = extendsNode != null ? classTypeList(extendsNode.getNextSibling()) : ImmutableList.<Tree>of();
    return new JavaTree.ClassTreeImpl(astNode, Tree.Kind.INTERFACE, modifiers, simpleName, null, superInterfaces, members.build());
  }

  /**
   * 9.1.4. Interface Body and Member Declarations
   */
  private void appendInterfaceMember(ModifiersTree modifiers, ImmutableList.Builder<Tree> members, AstNode astNode) {
    checkType(astNode, JavaGrammar.INTERFACE_MEMBER_DECL);
    AstNode declarationNode = astNode.getFirstChild(
      JavaGrammar.INTERFACE_DECLARATION,
      JavaGrammar.CLASS_DECLARATION,
      JavaGrammar.ENUM_DECLARATION,
      JavaGrammar.ANNOTATION_TYPE_DECLARATION
      );
    if (declarationNode != null) {
      members.add(typeDeclaration(modifiers, declarationNode));
      return;
    }
    declarationNode = astNode.getFirstChild(JavaGrammar.INTERFACE_METHOD_OR_FIELD_DECL);
    if (declarationNode != null) {
      AstNode interfaceMethodOrFieldRestNode = declarationNode.getFirstChild(JavaGrammar.INTERFACE_METHOD_OR_FIELD_REST);
      AstNode interfaceMethodDeclaratorRestNode = interfaceMethodOrFieldRestNode.getFirstChild(JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST);
      if (interfaceMethodDeclaratorRestNode != null) {
        members.add(methodDeclarator(
          modifiers,
          declarationNode.getFirstChild(JavaGrammar.TYPE, JavaKeyword.VOID),
          declarationNode.getFirstChild(JavaTokenType.IDENTIFIER),
          interfaceMethodDeclaratorRestNode
          ));
        return;
      } else {
        appendConstantDeclarations(modifiers, members, declarationNode);
        return;
      }
    }
    declarationNode = astNode.getFirstChild(JavaGrammar.INTERFACE_GENERIC_METHOD_DECL);
    if (declarationNode != null) {
      // TODO TYPE_PARAMETERS
      members.add(methodDeclarator(
        modifiers,
        /* type */declarationNode.getFirstChild(JavaGrammar.TYPE, JavaKeyword.VOID),
        /* name */declarationNode.getFirstChild(JavaTokenType.IDENTIFIER),
        declarationNode.getFirstChild(JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST)
        ));
      return;
    }
    declarationNode = astNode.getFirstChild(JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST);
    if (declarationNode != null) {
      members.add(methodDeclarator(
        modifiers,
        /* type */astNode.getFirstChild(JavaKeyword.VOID),
        /* name */astNode.getFirstChild(JavaTokenType.IDENTIFIER),
        declarationNode
        ));
      return;
    }
    throw new IllegalStateException();
  }

  private void appendConstantDeclarations(ModifiersTree modifiers, ImmutableList.Builder<Tree> members, AstNode astNode) {
    checkType(astNode, JavaGrammar.INTERFACE_METHOD_OR_FIELD_DECL, JavaGrammar.ANNOTATION_TYPE_ELEMENT_REST);
    ExpressionTree type = referenceType(astNode.getFirstChild(JavaGrammar.TYPE, JavaKeyword.VOID));
    for (AstNode constantDeclaratorRestNode : astNode.getDescendants(JavaGrammar.CONSTANT_DECLARATOR_REST)) {
      AstNode identifierNode = constantDeclaratorRestNode.getPreviousAstNode();
      Preconditions.checkState(identifierNode.is(JavaTokenType.IDENTIFIER));
      members.add(new JavaTree.VariableTreeImpl(
        constantDeclaratorRestNode,
        modifiers,
        applyDim(type, constantDeclaratorRestNode.getChildren(JavaGrammar.DIM).size()),
        identifierNode.getTokenValue(),
        variableInitializer(constantDeclaratorRestNode.getFirstChild(JavaGrammar.VARIABLE_INITIALIZER))
        ));
    }
  }

  /**
   * 9.6. Annotation Types
   */
  private ClassTree annotationTypeDeclaration(ModifiersTree modifiers, AstNode astNode) {
    checkType(astNode, JavaGrammar.ANNOTATION_TYPE_DECLARATION);
    String simpleName = astNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue();
    ImmutableList.Builder<Tree> members = ImmutableList.builder();
    for (AstNode annotationTypeElementDeclarationNode : astNode.getFirstChild(JavaGrammar.ANNOTATION_TYPE_BODY).getChildren(JavaGrammar.ANNOTATION_TYPE_ELEMENT_DECLARATION)) {
      AstNode annotationTypeElementRestNode = annotationTypeElementDeclarationNode.getFirstChild(JavaGrammar.ANNOTATION_TYPE_ELEMENT_REST);
      if (annotationTypeElementRestNode != null) {
        appendAnnotationTypeElementDeclaration(members, annotationTypeElementRestNode);
      }
    }
    return new JavaTree.ClassTreeImpl(astNode, Tree.Kind.ANNOTATION_TYPE,
      modifiers,
      simpleName,
      /* super class: */null,
      ImmutableList.<Tree>of(),
      members.build());
  }

  /**
   * 9.6.1. Annotation Type Elements
   */
  private void appendAnnotationTypeElementDeclaration(ImmutableList.Builder<Tree> members, AstNode astNode) {
    checkType(astNode, JavaGrammar.ANNOTATION_TYPE_ELEMENT_REST);
    AstNode declarationNode = astNode.getFirstChild(
      JavaGrammar.INTERFACE_DECLARATION,
      JavaGrammar.CLASS_DECLARATION,
      JavaGrammar.ENUM_DECLARATION,
      JavaGrammar.ANNOTATION_TYPE_DECLARATION
      );
    if (declarationNode != null) {
      members.add(typeDeclaration(JavaTree.ModifiersTreeImpl.EMPTY, declarationNode));
      return;
    }
    AstNode typeNode = astNode.getFirstChild(JavaGrammar.TYPE);
    AstNode identifierNode = astNode.getFirstChild(JavaTokenType.IDENTIFIER);
    AstNode annotationMethodRestNode = astNode.getFirstChild(JavaGrammar.ANNOTATION_METHOD_OR_CONSTANT_REST).getFirstChild(JavaGrammar.ANNOTATION_METHOD_REST);
    if (annotationMethodRestNode != null) {
      members.add(new JavaTree.MethodTreeImpl(
        annotationMethodRestNode,
        /* modifiers */JavaTree.ModifiersTreeImpl.EMPTY,
        /* return type */referenceType(typeNode),
        /* name */identifierNode.getTokenValue(),
        /* parameters */ImmutableList.<VariableTree>of(),
        /* block */null,
        /* throws */ImmutableList.<ExpressionTree>of(),
        // TODO DEFAULT_VALUE
        /* default value */null
        ));
    } else {
      appendConstantDeclarations(JavaTree.ModifiersTreeImpl.EMPTY, members, astNode);
    }
  }

  /*
   * 14. Blocks and Statements
   */

  @VisibleForTesting
  BlockTree block(AstNode astNode) {
    checkType(astNode, JavaGrammar.BLOCK);
    return new JavaTree.BlockTreeImpl(astNode, Tree.Kind.BLOCK, blockStatements(astNode.getFirstChild(JavaGrammar.BLOCK_STATEMENTS)));
  }

  private List<StatementTree> blockStatements(AstNode astNode) {
    checkType(astNode, JavaGrammar.BLOCK_STATEMENTS);
    ImmutableList.Builder<StatementTree> statements = ImmutableList.builder();
    for (AstNode blockStatementNode : astNode.getChildren(JavaGrammar.BLOCK_STATEMENT)) {
      AstNode statementNode = blockStatementNode.getFirstChild(
        JavaGrammar.STATEMENT,
        JavaGrammar.LOCAL_VARIABLE_DECLARATION_STATEMENT,
        JavaGrammar.CLASS_DECLARATION,
        JavaGrammar.ENUM_DECLARATION
        );
      if (statementNode.is(JavaGrammar.STATEMENT)) {
        statements.add(statement(statementNode));
      } else if (statementNode.is(JavaGrammar.LOCAL_VARIABLE_DECLARATION_STATEMENT)) {
        statements.addAll(variableDeclarators(
          variableModifiers(statementNode.getFirstChild(JavaGrammar.VARIABLE_MODIFIERS)),
          referenceType(statementNode.getFirstChild(JavaGrammar.TYPE)),
          statementNode.getFirstChild(JavaGrammar.VARIABLE_DECLARATORS)
          ));
      } else if (statementNode.is(JavaGrammar.CLASS_DECLARATION)) {
        statements.add(classDeclaration(modifiers(blockStatementNode.getChildren(JavaGrammar.MODIFIER)), statementNode));
      } else if (statementNode.is(JavaGrammar.ENUM_DECLARATION)) {
        statements.add(enumDeclaration(modifiers(blockStatementNode.getChildren(JavaGrammar.MODIFIER)), statementNode));
      } else {
        throw new IllegalStateException("Unexpected AstNodeType: " + statementNode.getType().toString());
      }
    }
    return statements.build();
  }

  private ModifiersTree variableModifiers(@Nullable AstNode astNode) {
    if (astNode == null) {
      return JavaTree.ModifiersTreeImpl.EMPTY;
    }

    Preconditions.checkArgument(astNode.is(JavaGrammar.VARIABLE_MODIFIERS), "Unexpected AstNodeType: %s", astNode.getType().toString());

    ImmutableList.Builder<Modifier> modifiers = ImmutableList.builder();
    for (AstNode modifierAstNode : astNode.getChildren()) {
      JavaKeyword keyword = (JavaKeyword) modifierAstNode.getType();
      modifiers.add(kindMaps.getModifier(keyword));
    }
    return new JavaTree.ModifiersTreeImpl(astNode, modifiers.build());
  }

  @VisibleForTesting
  StatementTree statement(AstNode astNode) {
    checkType(astNode, JavaGrammar.STATEMENT);
    final AstNode statementNode = astNode.getFirstChild();
    final StatementTree result;
    switch ((JavaGrammar) statementNode.getType()) {
      case BLOCK:
        result = block(statementNode);
        break;
      case EMPTY_STATEMENT:
        // 14.6. The Empty Statement
        result = new JavaTree.EmptyStatementTreeImpl(statementNode);
        break;
      case LABELED_STATEMENT:
        // 14.7. Labeled Statement
        result = new JavaTree.LabeledStatementTreeImpl(
          statementNode,
          statementNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue(),
          statement(statementNode.getFirstChild(JavaGrammar.STATEMENT))
          );
        break;
      case EXPRESSION_STATEMENT:
        // 14.8. Expression Statement
        result = new JavaTree.ExpressionStatementTreeImpl(
          statementNode,
          expression(statementNode.getFirstChild(JavaGrammar.STATEMENT_EXPRESSION))
          );
        break;
      case IF_STATEMENT:
        // 14.9. The if Statement
        List<AstNode> statements = statementNode.getChildren(JavaGrammar.STATEMENT);
        result = new JavaTree.IfStatementTreeImpl(
          statementNode,
          expression(statementNode.getFirstChild(JavaGrammar.PAR_EXPRESSION)),
          statement(statements.get(0)),
          statements.size() > 1 ? statement(statements.get(1)) : null
          );
        break;
      case ASSERT_STATEMENT:
        // 14.10. The assert Statement
        List<AstNode> expressions = statementNode.getChildren(JavaGrammar.EXPRESSION);
        result = new JavaTree.AssertStatementTreeImpl(
          statementNode,
          expression(expressions.get(0)),
          expressions.size() > 1 ? expression(expressions.get(1)) : null
          );
        break;
      case SWITCH_STATEMENT:
        result = switchStatement(statementNode);
        break;
      case WHILE_STATEMENT:
        // 14.12. The while Statement
        result = new JavaTree.WhileStatementTreeImpl(
          statementNode,
          expression(statementNode.getFirstChild(JavaGrammar.PAR_EXPRESSION)),
          statement(statementNode.getFirstChild(JavaGrammar.STATEMENT))
          );
        break;
      case DO_STATEMENT:
        // 14.13. The do Statement
        result = new JavaTree.DoWhileStatementTreeImpl(
          statementNode,
          statement(statementNode.getFirstChild(JavaGrammar.STATEMENT)),
          expression(statementNode.getFirstChild(JavaGrammar.PAR_EXPRESSION))
          );
        break;
      case FOR_STATEMENT:
        result = forStatement(statementNode);
        break;
      case BREAK_STATEMENT:
        // 14.15. The break Statement
        result = new JavaTree.BreakStatementTreeImpl(
          statementNode,
          statementNode.hasDirectChildren(JavaTokenType.IDENTIFIER) ? statementNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue() : null
          );
        break;
      case CONTINUE_STATEMENT:
        // 14.16. The continue Statement
        result = new JavaTree.ContinueStatementTreeImpl(
          statementNode,
          statementNode.hasDirectChildren(JavaTokenType.IDENTIFIER) ? statementNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue() : null
          );
        break;
      case RETURN_STATEMENT:
        // 14.17. The return Statement
        result = new JavaTree.ReturnStatementTreeImpl(
          statementNode,
          statementNode.hasDirectChildren(JavaGrammar.EXPRESSION) ? expression(statementNode.getFirstChild(JavaGrammar.EXPRESSION)) : null
          );
        break;
      case THROW_STATEMENT:
        // 14.18. The throw Statement
        result = new JavaTree.ThrowStatementTreeImpl(
          statementNode,
          expression(statementNode.getFirstChild(JavaGrammar.EXPRESSION))
          );
        break;
      case SYNCHRONIZED_STATEMENT:
        // 14.19. The synchronized Statement
        result = new JavaTree.SynchronizedStatementTreeImpl(
          statementNode,
          expression(statementNode.getFirstChild(JavaGrammar.PAR_EXPRESSION)),
          block(statementNode.getFirstChild(JavaGrammar.BLOCK))
          );
        break;
      case TRY_STATEMENT:
        result = tryStatement(statementNode);
        break;
      default:
        throw new IllegalStateException("Unexpected AstNodeType: " + astNode.getType().toString());
    }
    return result;
  }

  /**
   * 14.11. The switch Statement
   */
  private SwitchStatementTree switchStatement(AstNode astNode) {
    ImmutableList.Builder<CaseGroupTree> cases = ImmutableList.builder();
    List<JavaTree.CaseLabelTreeImpl> labels = Lists.newArrayList();
    for (AstNode caseNode : astNode.getFirstChild(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUPS).getChildren(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUP)) {
      AstNode expressionNode = caseNode.getFirstChild(JavaGrammar.SWITCH_LABEL).getFirstChild(JavaGrammar.CONSTANT_EXPRESSION);
      AstNode blockStatementsNode = caseNode.getFirstChild(JavaGrammar.BLOCK_STATEMENTS);
      labels.add(new JavaTree.CaseLabelTreeImpl(caseNode, expressionNode != null ? expression(expressionNode) : null));
      if (blockStatementsNode.hasChildren()) {
        cases.add(new JavaTree.CaseGroupTreeImpl(
          labels.get(0).getAstNode(),
          ImmutableList.<CaseLabelTree>copyOf(labels),
          blockStatements(caseNode.getFirstChild(JavaGrammar.BLOCK_STATEMENTS))
          ));
        labels.clear();
      }
    }
    if (!labels.isEmpty()) {
      cases.add(new JavaTree.CaseGroupTreeImpl(
        labels.get(0).getAstNode(),
        ImmutableList.<CaseLabelTree>copyOf(labels),
        ImmutableList.<StatementTree>of()
        ));
    }
    return new JavaTree.SwitchStatementTreeImpl(
      astNode,
      expression(astNode.getFirstChild(JavaGrammar.PAR_EXPRESSION)),
      cases.build());
  }

  /**
   * 14.14. The for Statement
   */
  private StatementTree forStatement(AstNode astNode) {
    AstNode formalParameterNode = astNode.getFirstChild(JavaGrammar.FORMAL_PARAMETER);
    if (formalParameterNode == null) {
      AstNode forInitNode = astNode.getFirstChild(JavaGrammar.FOR_INIT);
      final List<StatementTree> forInit;
      if (forInitNode == null) {
        forInit = ImmutableList.of();
      } else if (forInitNode.hasDirectChildren(JavaGrammar.VARIABLE_DECLARATORS)) {
        // TODO modifiers
        forInit = variableDeclarators(
          JavaTree.ModifiersTreeImpl.EMPTY,
          referenceType(forInitNode.getFirstChild(JavaGrammar.TYPE)),
          forInitNode.getFirstChild(JavaGrammar.VARIABLE_DECLARATORS)
          );
      } else {
        forInit = statementExpressions(astNode.getFirstChild(JavaGrammar.FOR_INIT));
      }
      return new JavaTree.ForStatementTreeImpl(
        astNode,
        forInit,
        astNode.hasDirectChildren(JavaGrammar.EXPRESSION) ? expression(astNode.getFirstChild(JavaGrammar.EXPRESSION)) : null,
        astNode.hasDirectChildren(JavaGrammar.FOR_UPDATE) ? statementExpressions(astNode.getFirstChild(JavaGrammar.FOR_UPDATE)) : ImmutableList.<StatementTree>of(),
        statement(astNode.getFirstChild(JavaGrammar.STATEMENT)));
    } else {
      return new JavaTree.ForEachStatementImpl(
        astNode,
        new JavaTree.VariableTreeImpl(
          formalParameterNode,
          JavaTree.ModifiersTreeImpl.EMPTY,
          // TODO dim
          referenceType(formalParameterNode.getFirstChild(JavaGrammar.TYPE)),
          formalParameterNode.getFirstChild(JavaGrammar.VARIABLE_DECLARATOR_ID).getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue(),
          /* initializer: */null
        ),
        expression(astNode.getFirstChild(JavaGrammar.EXPRESSION)),
        statement(astNode.getFirstChild(JavaGrammar.STATEMENT)));
    }
  }

  private List<StatementTree> statementExpressions(AstNode astNode) {
    checkType(astNode, JavaGrammar.FOR_INIT, JavaGrammar.FOR_UPDATE);
    ImmutableList.Builder<StatementTree> result = ImmutableList.builder();
    for (AstNode statementExpressionNode : astNode.getChildren(JavaGrammar.STATEMENT_EXPRESSION)) {
      result.add(new JavaTree.ExpressionStatementTreeImpl(statementExpressionNode, expression(statementExpressionNode)));
    }
    return result.build();
  }

  /**
   * 14.20. The try statement
   */
  private TryStatementTree tryStatement(AstNode astNode) {
    if (astNode.hasDirectChildren(JavaGrammar.TRY_WITH_RESOURCES_STATEMENT)) {
      astNode = astNode.getFirstChild(JavaGrammar.TRY_WITH_RESOURCES_STATEMENT);
    }
    ImmutableList.Builder<CatchTree> catches = ImmutableList.builder();
    for (AstNode catchNode : astNode.getChildren(JavaGrammar.CATCH_CLAUSE)) {
      AstNode catchFormalParameterNode = catchNode.getFirstChild(JavaGrammar.CATCH_FORMAL_PARAMETER);
      catches.add(new JavaTree.CatchTreeImpl(
        catchNode,
        new JavaTree.VariableTreeImpl(
          catchFormalParameterNode,
          // TODO modifiers:
          JavaTree.ModifiersTreeImpl.EMPTY,
          catchType(catchFormalParameterNode.getFirstChild(JavaGrammar.CATCH_TYPE)),
          // TODO WTF why VARIABLE_DECLARATOR_ID in grammar?
          catchFormalParameterNode.getFirstChild(JavaGrammar.VARIABLE_DECLARATOR_ID).getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue(),
          /* initializer: */null
        ),
        block(catchNode.getFirstChild(JavaGrammar.BLOCK))
        ));
    }
    BlockTree finallyBlock = null;
    if (astNode.hasDirectChildren(JavaGrammar.FINALLY_)) {
      finallyBlock = block(astNode.getFirstChild(JavaGrammar.FINALLY_).getFirstChild(JavaGrammar.BLOCK));
    }
    AstNode resourceSpecificationNode = astNode.getFirstChild(JavaGrammar.RESOURCE_SPECIFICATION);
    return new JavaTree.TryStatementTreeImpl(
      astNode,
      resourceSpecificationNode == null ? ImmutableList.<VariableTree>of() : resourceSpecification(resourceSpecificationNode),
      block(astNode.getFirstChild(JavaGrammar.BLOCK)),
      catches.build(),
      finallyBlock);
  }

  private Tree catchType(AstNode astNode) {
    checkType(astNode, JavaGrammar.CATCH_TYPE);
    List<AstNode> children = astNode.getChildren(JavaGrammar.QUALIFIED_IDENTIFIER);
    if (children.size() == 1) {
      return qualifiedIdentifier(children.get(0));
    } else {
      ImmutableList.Builder<Tree> typeAlternatives = ImmutableList.builder();
      for (AstNode child : children) {
        typeAlternatives.add(qualifiedIdentifier(child));
      }
      return new JavaTree.UnionTypeTreeImpl(astNode, typeAlternatives.build());
    }
  }

  private List<VariableTree> resourceSpecification(AstNode astNode) {
    checkType(astNode, JavaGrammar.RESOURCE_SPECIFICATION);
    ImmutableList.Builder<VariableTree> result = ImmutableList.builder();
    for (AstNode resourceNode : astNode.getChildren(JavaGrammar.RESOURCE)) {
      result.add(new JavaTree.VariableTreeImpl(
        resourceNode,
        // TODO modifiers:
        JavaTree.ModifiersTreeImpl.EMPTY,
        classType(resourceNode.getFirstChild(JavaGrammar.CLASS_TYPE)),
        resourceNode.getFirstChild(JavaGrammar.VARIABLE_DECLARATOR_ID).getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue(),
        expression(resourceNode.getFirstChild(JavaGrammar.EXPRESSION))
        ));
    }
    return result.build();
  }

  @VisibleForTesting
  ExpressionTree expression(AstNode astNode) {
    if (astNode.is(JavaGrammar.CONSTANT_EXPRESSION, JavaGrammar.STATEMENT_EXPRESSION)) {
      astNode = astNode.getFirstChild(JavaGrammar.EXPRESSION).getFirstChild();
    } else if (astNode.is(JavaGrammar.EXPRESSION)) {
      astNode = astNode.getFirstChild();
    }

    if (astNode.is(JavaGrammar.PAR_EXPRESSION)) {
      return new JavaTree.ParenthesizedTreeImpl(astNode, expression(astNode.getFirstChild(JavaGrammar.EXPRESSION)));
    } else if (astNode.is(JavaGrammar.PRIMARY)) {
      return primary(astNode);
    } else if (astNode.is(JavaGrammar.CONDITIONAL_OR_EXPRESSION,
      JavaGrammar.CONDITIONAL_AND_EXPRESSION,
      JavaGrammar.INCLUSIVE_OR_EXPRESSION,
      JavaGrammar.EXCLUSIVE_OR_EXPRESSION,
      JavaGrammar.AND_EXPRESSION,
      JavaGrammar.EQUALITY_EXPRESSION,
      JavaGrammar.RELATIONAL_EXPRESSION,
      JavaGrammar.SHIFT_EXPRESSION,
      JavaGrammar.ADDITIVE_EXPRESSION,
      JavaGrammar.MULTIPLICATIVE_EXPRESSION)) {
      return binaryExpression(astNode);
    } else if (astNode.is(JavaGrammar.CONDITIONAL_EXPRESSION)) {
      return conditionalExpression(astNode);
    } else if (astNode.is(JavaGrammar.ASSIGNMENT_EXPRESSION)) {
      return assignmentExpression(astNode);
    } else if (astNode.is(JavaGrammar.UNARY_EXPRESSION)) {
      return unaryExpression(astNode);
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + astNode.getType().toString());
    }
  }

  /**
   * 15.11. Field Access Expressions
   * 15.12. Method Invocation Expressions
   * 15.13. Array Access Expressions
   */
  @VisibleForTesting
  ExpressionTree primary(AstNode astNode) {
    AstNode firstChildNode = astNode.getFirstChild();
    if (firstChildNode.is(JavaGrammar.PAR_EXPRESSION)) {
      // (expression)
      return expression(firstChildNode);
    } else if (firstChildNode.is(JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS)) {
      if (astNode.hasDirectChildren(JavaKeyword.THIS)) {
        // <T>this(arguments)
        return new JavaTree.MethodInvocationTreeImpl(
          astNode,
          identifier(astNode.getFirstChild(JavaKeyword.THIS)),
          arguments(astNode.getFirstChild(JavaGrammar.ARGUMENTS)));
      } else {
        AstNode explicitGenericInvocationSuffixNode = astNode.getFirstChild(JavaGrammar.EXPLICIT_GENERIC_INVOCATION_SUFFIX);
        if (explicitGenericInvocationSuffixNode.hasDirectChildren(JavaKeyword.SUPER)) {
          // <T>super...
          return applySuperSuffix(
            identifier(explicitGenericInvocationSuffixNode.getFirstChild(JavaKeyword.SUPER)),
            explicitGenericInvocationSuffixNode.getFirstChild(JavaGrammar.SUPER_SUFFIX));
        } else {
          // <T>id(arguments)
          return new JavaTree.MethodInvocationTreeImpl(
            astNode,
            identifier(explicitGenericInvocationSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER)),
            arguments(explicitGenericInvocationSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS)));
        }
      }
    } else if (firstChildNode.is(JavaKeyword.THIS)) {
      IdentifierTree identifier = identifier(firstChildNode);
      if (astNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
        // this(arguments)
        return new JavaTree.MethodInvocationTreeImpl(
          astNode,
          identifier,
          arguments(astNode.getFirstChild(JavaGrammar.ARGUMENTS)));
      } else {
        // this
        return identifier;
      }
    } else if (firstChildNode.is(JavaKeyword.SUPER)) {
      // super...
      return applySuperSuffix(
        identifier(firstChildNode),
        astNode.getFirstChild(JavaGrammar.SUPER_SUFFIX));
    } else if (firstChildNode.is(JavaGrammar.LITERAL)) {
      // "literal"
      return literal(firstChildNode);
    } else if (firstChildNode.is(JavaKeyword.NEW)) {
      // new...
      return creator(astNode.getFirstChild(JavaGrammar.CREATOR));
    } else if (firstChildNode.is(JavaGrammar.QUALIFIED_IDENTIFIER)) {
      ExpressionTree identifier = qualifiedIdentifier(firstChildNode);
      AstNode identifierSuffixNode = astNode.getFirstChild(JavaGrammar.IDENTIFIER_SUFFIX);
      if (identifierSuffixNode == null) {
        // id
        return identifier;
      } else {
        if (identifierSuffixNode.getFirstChild().is(JavaPunctuator.LBRK)) {
          if (identifierSuffixNode.hasDirectChildren(JavaKeyword.CLASS)) {
            // 15.8.2. Class Literals
            // id[].class
            return new JavaTree.MemberSelectExpressionTreeImpl(
              astNode,
              applyDim(identifier, identifierSuffixNode.getChildren(JavaGrammar.DIM).size() + 1),
              identifier(identifierSuffixNode.getFirstChild(JavaKeyword.CLASS)));
          } else {
            // id[expression]
            return new JavaTree.ArrayAccessExpressionTreeImpl(
              astNode,
              identifier,
              expression(identifierSuffixNode.getFirstChild(JavaGrammar.EXPRESSION)));
          }
        } else if (identifierSuffixNode.getFirstChild().is(JavaGrammar.ARGUMENTS)) {
          // id(arguments)
          return new JavaTree.MethodInvocationTreeImpl(
            astNode,
            identifier,
            arguments(identifierSuffixNode.getFirstChild()));
        } else if (identifierSuffixNode.getFirstChild().is(JavaPunctuator.DOT)) {
          if (identifierSuffixNode.hasDirectChildren(JavaKeyword.CLASS)) {
            // 15.8.2. Class Literals
            // id.class
            return new JavaTree.MemberSelectExpressionTreeImpl(
              astNode,
              identifier,
              identifier(identifierSuffixNode.getFirstChild(JavaKeyword.CLASS)));
          } else if (identifierSuffixNode.hasDirectChildren(JavaGrammar.EXPLICIT_GENERIC_INVOCATION)) {
            // id.<...>...
            return applyExplicitGenericInvocation(identifier, identifierSuffixNode.getFirstChild(JavaGrammar.EXPLICIT_GENERIC_INVOCATION));
          } else if (identifierSuffixNode.hasDirectChildren(JavaKeyword.THIS)) {
            // id.this
            return new JavaTree.MemberSelectExpressionTreeImpl(
              astNode,
              identifier,
              identifier(identifierSuffixNode.getFirstChild(JavaKeyword.THIS)));
          } else if (identifierSuffixNode.hasDirectChildren(JavaKeyword.SUPER)) {
            // id.super(arguments)
            return new JavaTree.MethodInvocationTreeImpl(
              astNode,
              new JavaTree.MemberSelectExpressionTreeImpl(
                astNode,
                identifier,
                identifier(identifierSuffixNode.getFirstChild(JavaKeyword.SUPER))
              ),
              arguments(identifierSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS)));
          } else if (identifierSuffixNode.hasDirectChildren(JavaKeyword.NEW)) {
            // id.new...
            AstNode innerCreatorNode = identifierSuffixNode.getFirstChild(JavaGrammar.INNER_CREATOR);
            return applyClassCreatorRest(
              identifier,
              identifier(innerCreatorNode.getFirstChild(JavaTokenType.IDENTIFIER)),
              innerCreatorNode.getFirstChild(JavaGrammar.CLASS_CREATOR_REST));
          } else {
            throw new IllegalArgumentException("Unexpected AstNodeType: " + identifierSuffixNode.getChild(1));
          }
        } else {
          throw new IllegalArgumentException("Unexpected AstNodeType: " + identifierSuffixNode.getFirstChild());
        }
      }
    } else if (firstChildNode.is(JavaGrammar.BASIC_TYPE, JavaKeyword.VOID)) {
      // 15.8.2. Class Literals
      // int.class
      // int[].class
      // void.class
      return new JavaTree.MemberSelectExpressionTreeImpl(
        astNode,
        applyDim(basicType(firstChildNode), astNode.getChildren(JavaGrammar.DIM).size()),
        identifier(astNode.getFirstChild(JavaKeyword.CLASS)));
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + firstChildNode.getType());
    }
  }

  private ExpressionTree creator(AstNode astNode) {
    // TODO NON_WILDCARD_TYPE_ARGUMENTS
    if (astNode.hasDirectChildren(JavaGrammar.CLASS_CREATOR_REST)) {
      return applyClassCreatorRest(
        /* enclosing expression: */null,
        classType(astNode.getFirstChild(JavaGrammar.CREATED_NAME)),
        astNode.getFirstChild(JavaGrammar.CLASS_CREATOR_REST));
    } else if (astNode.hasDirectChildren(JavaGrammar.ARRAY_CREATOR_REST)) {
      AstNode arrayCreatorRestNode = astNode.getFirstChild(JavaGrammar.ARRAY_CREATOR_REST);
      AstNode typeNode = arrayCreatorRestNode.getPreviousSibling();
      Tree type = typeNode.is(JavaGrammar.BASIC_TYPE) ? basicType(typeNode) : classType(typeNode);
      if (arrayCreatorRestNode.hasDirectChildren(JavaGrammar.ARRAY_INITIALIZER)) {
        return arrayInitializer(type, arrayCreatorRestNode.getFirstChild(JavaGrammar.ARRAY_INITIALIZER));
      } else {
        ImmutableList.Builder<ExpressionTree> dimensions = ImmutableList.builder();
        dimensions.add(expression(arrayCreatorRestNode.getFirstChild(JavaGrammar.EXPRESSION)));
        for (AstNode dimExpr : arrayCreatorRestNode.getChildren(JavaGrammar.DIM_EXPR)) {
          dimensions.add(expression(dimExpr.getFirstChild(JavaGrammar.EXPRESSION)));
        }
        return new JavaTree.NewArrayTreeImpl(astNode, type, dimensions.build(), ImmutableList.<ExpressionTree>of());
      }
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + astNode);
    }
  }

  private ExpressionTree arrayInitializer(@Nullable Tree t, AstNode astNode) {
    ImmutableList.Builder<ExpressionTree> elems = ImmutableList.builder();
    for (AstNode elem : astNode.getChildren(JavaGrammar.VARIABLE_INITIALIZER)) {
      elems.add(variableInitializer(elem));
    }
    return new JavaTree.NewArrayTreeImpl(astNode, t, ImmutableList.<ExpressionTree>of(), elems.build());
  }

  private ExpressionTree variableInitializer(AstNode astNode) {
    if (astNode.getFirstChild().is(JavaGrammar.EXPRESSION)) {
      return expression(astNode.getFirstChild());
    } else {
      return arrayInitializer(null, astNode.getFirstChild());
    }
  }

  /**
   * 15.14. Postfix Expressions
   * 15.15. Unary Operators
   * 15.16. Cast Expressions
   */
  private ExpressionTree unaryExpression(AstNode astNode) {
    if (astNode.hasDirectChildren(JavaGrammar.TYPE)) {
      // 15.16. Cast Expressions
      return new JavaTree.TypeCastExpressionTreeImpl(
        astNode,
        referenceType(astNode.getFirstChild(JavaGrammar.TYPE)),
        expression(astNode.getChild(3)));
    } else if (astNode.hasDirectChildren(JavaGrammar.PREFIX_OP)) {
      // 15.15. Unary Operators
      JavaPunctuator punctuator = (JavaPunctuator) astNode.getFirstChild(JavaGrammar.PREFIX_OP).getFirstChild().getType();
      Tree.Kind kind = kindMaps.getPrefixOperator(punctuator);
      return new JavaTree.UnaryExpressionTreeImpl(
        astNode,
        kind,
        expression(astNode.getChild(1)));
    } else {
      // 15.14. Postfix Expressions
      ExpressionTree result = expression(astNode.getFirstChild());
      for (AstNode selectorNode : astNode.getChildren(JavaGrammar.SELECTOR)) {
        result = applySelector(result, selectorNode);
      }
      for (AstNode postfixOpNode : astNode.getChildren(JavaGrammar.POST_FIX_OP)) {
        JavaPunctuator punctuator = (JavaPunctuator) postfixOpNode.getFirstChild().getType();
        Tree.Kind kind = kindMaps.getPostfixOperator(punctuator);
        result = new JavaTree.UnaryExpressionTreeImpl(astNode, kind, result);
      }
      return result;
    }
  }

  /**
   * 15.17. Multiplicative Operators
   * 15.18. Additive Operators
   * 15.19. Shift Operators
   * 15.20. Relational Operators
   * 15.21. Equality Operators
   * 15.22. Bitwise and Logical Operators
   * 15.23. Conditional-And Operator &&
   * 15.24. Conditional-Or Operator ||
   */
  private ExpressionTree binaryExpression(AstNode astNode) {
    if (astNode.hasDirectChildren(JavaKeyword.INSTANCEOF)) {
      // 15.20.2. Type Comparison Operator instanceof
      // TODO fix grammar - instanceof can't be chained
      return new JavaTree.InstanceOfTreeImpl(
        astNode,
        expression(astNode.getFirstChild()),
        referenceType(astNode.getFirstChild(JavaGrammar.REFERENCE_TYPE)));
    }

    ExpressionTree expression = expression(astNode.getLastChild());
    for (int i = astNode.getNumberOfChildren() - 3; i >= 0; i -= 2) {
      JavaPunctuator punctuator = (JavaPunctuator) astNode.getChild(i + 1).getType();
      Tree.Kind kind = kindMaps.getBinaryOperator(punctuator);
      expression = new JavaTree.BinaryExpressionTreeImpl(
        astNode,
        expression(astNode.getChild(i)),
        kind,
        expression
        );
    }
    return expression;
  }

  /**
   * 15.25. Conditional Operator ? :
   */
  private ExpressionTree conditionalExpression(AstNode astNode) {
    ExpressionTree expression = expression(astNode.getLastChild());
    for (int i = astNode.getNumberOfChildren() - 5; i >= 0; i -= 4) {
      expression = new JavaTree.ConditionalExpressionTreeImpl(
        astNode,
        expression(astNode.getChild(i)),
        expression(astNode.getChild(i + 2)),
        expression
        );
    }
    return expression;
  }

  /**
   * 15.26. Assignment Operators
   */
  private ExpressionTree assignmentExpression(AstNode astNode) {
    ExpressionTree expression = expression(astNode.getLastChild());
    for (int i = astNode.getNumberOfChildren() - 3; i >= 0; i -= 2) {
      JavaPunctuator punctuator = (JavaPunctuator) astNode.getChild(i + 1).getFirstChild().getType();
      Tree.Kind kind = kindMaps.getAssignmentOperator(punctuator);
      expression = new JavaTree.AssignmentExpressionTreeImpl(
        astNode,
        expression(astNode.getChild(i)),
        kind,
        expression
        );
    }
    return expression;
  }

  private ExpressionTree applySelector(ExpressionTree expression, AstNode selectorNode) {
    checkType(selectorNode, JavaGrammar.SELECTOR);
    if (selectorNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
      return new JavaTree.MethodInvocationTreeImpl(
        selectorNode,
        new JavaTree.MemberSelectExpressionTreeImpl(
          selectorNode,
          expression,
          identifier(selectorNode.getFirstChild(JavaTokenType.IDENTIFIER))
        ),
        arguments(selectorNode.getFirstChild(JavaGrammar.ARGUMENTS)));
    } else if (selectorNode.hasDirectChildren(JavaTokenType.IDENTIFIER)) {
      return new JavaTree.MemberSelectExpressionTreeImpl(
        selectorNode,
        expression,
        identifier(selectorNode.getFirstChild(JavaTokenType.IDENTIFIER)));
    } else if (selectorNode.hasDirectChildren(JavaGrammar.EXPLICIT_GENERIC_INVOCATION)) {
      return applyExplicitGenericInvocation(expression, selectorNode.getFirstChild(JavaGrammar.EXPLICIT_GENERIC_INVOCATION));
    } else if (selectorNode.hasDirectChildren(JavaKeyword.THIS)) {
      return new JavaTree.MemberSelectExpressionTreeImpl(
        selectorNode,
        expression,
        identifier(selectorNode.getFirstChild(JavaKeyword.THIS)));
    } else if (selectorNode.hasDirectChildren(JavaGrammar.SUPER_SUFFIX)) {
      return applySuperSuffix(
        new JavaTree.MemberSelectExpressionTreeImpl(
          selectorNode,
          expression,
          identifier(selectorNode.getFirstChild(JavaKeyword.SUPER))
        ),
        selectorNode.getFirstChild(JavaGrammar.SUPER_SUFFIX));
    } else if (selectorNode.hasDirectChildren(JavaKeyword.NEW)) {
      AstNode innerCreatorNode = selectorNode.getFirstChild(JavaGrammar.INNER_CREATOR);
      return applyClassCreatorRest(
        expression,
        identifier(innerCreatorNode.getFirstChild(JavaTokenType.IDENTIFIER)),
        innerCreatorNode.getFirstChild(JavaGrammar.CLASS_CREATOR_REST));
    } else if (selectorNode.hasDirectChildren(JavaGrammar.DIM_EXPR)) {
      return new JavaTree.ArrayAccessExpressionTreeImpl(
        selectorNode,
        expression,
        expression(selectorNode.getFirstChild(JavaGrammar.DIM_EXPR).getFirstChild(JavaGrammar.EXPRESSION)));
    } else {
      throw new IllegalStateException(AstXmlPrinter.print(selectorNode));
    }
  }

  private ExpressionTree applySuperSuffix(ExpressionTree expression, AstNode superSuffixNode) {
    checkType(superSuffixNode, JavaGrammar.SUPER_SUFFIX);
    if (superSuffixNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
      // super(arguments)
      // super.method(arguments)
      // super.<T>method(arguments)
      // TODO typeArguments
      ExpressionTree methodSelect = expression;
      if (superSuffixNode.hasDirectChildren(JavaTokenType.IDENTIFIER)) {
        methodSelect = new JavaTree.MemberSelectExpressionTreeImpl(
          superSuffixNode,
          expression,
          identifier(superSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER))
          );
      }
      return new JavaTree.MethodInvocationTreeImpl(
        superSuffixNode,
        methodSelect,
        arguments(superSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS)));
    } else {
      // super.field
      return new JavaTree.MemberSelectExpressionTreeImpl(
        superSuffixNode,
        expression,
        identifier(superSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER)));
    }
  }

  private ExpressionTree applyClassCreatorRest(ExpressionTree enclosingExpression, ExpressionTree identifier, AstNode classCreatorRestNode) {
    checkType(classCreatorRestNode, JavaGrammar.CLASS_CREATOR_REST);
    ClassTree classBody = null;
    if (classCreatorRestNode.hasDirectChildren(JavaGrammar.CLASS_BODY)) {
      classBody = new JavaTree.ClassTreeImpl(
        classCreatorRestNode,
        Tree.Kind.CLASS,
        JavaTree.ModifiersTreeImpl.EMPTY,
        classBody(classCreatorRestNode.getFirstChild(JavaGrammar.CLASS_BODY))
        );
    }
    return new JavaTree.NewClassTreeImpl(
      classCreatorRestNode,
      enclosingExpression,
      identifier,
      arguments(classCreatorRestNode.getFirstChild(JavaGrammar.ARGUMENTS)),
      classBody);
  }

  private ExpressionTree applyExplicitGenericInvocation(ExpressionTree expression, AstNode astNode) {
    checkType(astNode, JavaGrammar.EXPLICIT_GENERIC_INVOCATION);
    // TODO NON_WILDCARD_TYPE_ARGUMENTS
    AstNode explicitGenericInvocationSuffixNode = astNode.getFirstChild(JavaGrammar.EXPLICIT_GENERIC_INVOCATION_SUFFIX);
    if (explicitGenericInvocationSuffixNode.hasDirectChildren(JavaGrammar.SUPER_SUFFIX)) {
      expression = new JavaTree.MemberSelectExpressionTreeImpl(
        astNode,
        expression,
        identifier(explicitGenericInvocationSuffixNode.getFirstChild(JavaKeyword.SUPER))
        );
      return applySuperSuffix(expression, explicitGenericInvocationSuffixNode.getFirstChild(JavaGrammar.SUPER_SUFFIX));
    } else {
      return new JavaTree.MethodInvocationTreeImpl(
        astNode,
        new JavaTree.MemberSelectExpressionTreeImpl(
          astNode,
          expression,
          identifier(explicitGenericInvocationSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER))
        ),
        arguments(explicitGenericInvocationSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS)));
    }
  }

  private List<ExpressionTree> arguments(AstNode astNode) {
    checkType(astNode, JavaGrammar.ARGUMENTS);
    ImmutableList.Builder<ExpressionTree> arguments = ImmutableList.builder();
    for (AstNode argument : astNode.getChildren(JavaGrammar.EXPRESSION)) {
      arguments.add(expression(argument));
    }
    return arguments.build();
  }

  private ExpressionTree applyDim(ExpressionTree expression, int count) {
    ExpressionTree result = expression;
    for (int i = 0; i < count; i++) {
      result = new JavaTree.ArrayTypeTreeImpl(/* FIXME should not be null */null, expression);
    }
    return result;
  }

}
