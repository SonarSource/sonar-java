/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.VarTypeTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.StaticInitializerTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.semsitter.Bindings.ascribe;
import static org.sonar.java.semsitter.Bindings.bind;
import static org.sonar.java.semsitter.Bindings.keepsArguments;
import static org.sonar.java.semsitter.CstNodes.childKind;
import static org.sonar.java.semsitter.CstNodes.field;
import static org.sonar.java.semsitter.CstNodes.firstNamed;
import static org.sonar.java.semsitter.CstNodes.firstOf;
import static org.sonar.java.semsitter.CstNodes.firstType;
import static org.sonar.java.semsitter.CstNodes.isAnnotation;
import static org.sonar.java.semsitter.CstNodes.lastNamed;
import static org.sonar.java.semsitter.CstNodes.modifierKeyword;
import static org.sonar.java.semsitter.CstNodes.nameNode;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.requiredField;

/** Translates methods, fields, variables, initializers, modifiers and annotations. */
final class MemberTranslator extends TranslatorPart {

  MemberTranslator(TranslationState state) {
    super(state);
  }

  MethodTreeImpl method(CstNode node) {
    state.scopes.addFirst(new HashMap<>());
    CstNode typeNode = field(node, "type");
    state.returnTargets.push(typeNode == null || field(node, "dimensions") != null ? Type.UNKNOWN : syntacticType(typeNode));
    try {
      return methodInScope(node);
    } finally {
      state.scopes.removeFirst();
      state.returnTargets.pop();
    }
  }

  private MethodTreeImpl methodInScope(CstNode node) {
    // translated before the parameters and the body so that the bounds of the type parameters are known there
    CstNode typeParameters = field(node, "type_parameters");
    TypeParameterListTreeImpl typeParameterList = typeParameters == null ? null : typeTrees().typeParameters(typeParameters);
    CstNode parameters = field(node, "parameters");
    FormalParametersListTreeImpl formals = new FormalParametersListTreeImpl(
      parameters == null ? null : token(required(parameters, "(")),
      parameters == null ? null : token(required(parameters, ")")));
    if (parameters != null) {
      for (CstNode child : parameters.children()) {
        if ("formal_parameter".equals(child.k()) || "spread_parameter".equals(child.k()) || "receiver_parameter".equals(child.k())) {
          formals.add(variable(child));
        } else if (",".equals(child.text())) {
          ((VariableTreeImpl) formals.get(formals.size() - 1)).setEndToken(token(child));
        }
      }
    }
    CstNode body = field(node, "body");
    BlockTreeImpl block = body == null ? null : statements().block(body);
    CstNode typeNode = field(node, "type");
    TypeTree returnType = null;
    if (typeNode != null) {
      TypeTree written = names().recoverQualifiedType(typeTrees().type(typeNode));
      List<AnnotationTree> afterTypeParameters = node.children().stream().filter(CstNodes::isAnnotation).<AnnotationTree>map(this::annotation).toList();
      TypeTree element = written;
      while (element instanceof ArrayTypeTree array) {
        element = array.type();
      }
      TypeTranslator.annotateLeftmost(element, afterTypeParameters);
      returnType = typeTrees().arrayType(written, field(node, "dimensions"));
    }
    IdentifierTreeImpl name = names().identifier(requiredField(node, "name", firstOf(node, "identifier")));
    CstNode thrown = childKind(node, "throws");
    MethodTreeImpl tree = new MethodTreeImpl(
      returnType,
      name,
      formals,
      thrown == null ? null : token(required(thrown, "throws")),
      thrown == null ? QualifiedIdentifierListTreeImpl.emptyList() : typeTrees().separatedTypes(thrown),
      block,
      body == null ? token(required(node, ";")) : null)
      .completeWithModifiers(modifiers(node));
    if (typeParameterList != null) {
      tree.completeWithTypeParameters(typeParameterList);
    }
    bindDeclaration(tree, nameNode(node), name);
    return tree;
  }

  List<VariableTree> parameters(CstNode parameters) {
    List<VariableTreeImpl> trees = new ArrayList<>();
    for (CstNode child : parameters.children()) {
      if ("formal_parameter".equals(child.k()) || "spread_parameter".equals(child.k())) {
        trees.add(variable(child));
      } else if (",".equals(child.text())) {
        trees.get(trees.size() - 1).setEndToken(token(child));
      }
    }
    return new ArrayList<>(trees);
  }

  List<VariableTreeImpl> variables(CstNode declaration) {
    ModifiersTreeImpl modifiers = modifiers(declaration);
    CstNode typeNode = field(declaration, "type");
    TypeTree type;
    if (typeNode != null) {
      type = typeTrees().type(typeNode);
      if ("field_declaration".equals(declaration.k()) || "constant_declaration".equals(declaration.k())) {
        type = names().recoverQualifiedType(type);
      } else if (type instanceof MemberSelectExpressionTreeImpl select && select.symbolType().isUnknown()) {
        // an unresolved qualified name of a local or a parameter is not typed by what is written
        ascribe(select, JType.unknown(sema));
      }
    } else if ("spread_parameter".equals(declaration.k())) {
      type = spreadType(declaration);
    } else {
      type = typeTrees().catchType(required(declaration, "catch_type"));
    }
    List<VariableTreeImpl> trees = new ArrayList<>();
    for (CstNode child : declaration.children()) {
      if ("variable_declarator".equals(child.k())) {
        trees.add(declarator(child, modifiers, type));
      } else if (",".equals(child.text()) || ";".equals(child.text())) {
        trees.get(trees.size() - 1).setEndToken(token(child));
      }
    }
    if (trees.isEmpty()) {
      trees.add(declarator(declaration, modifiers, type));
    }
    return trees;
  }

  private TypeTree spreadType(CstNode node) {
    TypeTree element = typeTrees().type(firstType(node));
    List<AnnotationTreeImpl> annotations = node.children().stream().filter(CstNodes::isAnnotation).map(this::annotation).toList();
    JavaTree.ArrayTypeTreeImpl array = new JavaTree.ArrayTypeTreeImpl(element, annotations, token(required(node, "...")));
    ascribe(array, JType.array(sema, element.symbolType() instanceof JType elementType ? elementType : JType.unknown(sema)));
    return array;
  }

  VariableTreeImpl variable(CstNode parameter) {
    return variables(parameter).get(0);
  }

  private VariableTreeImpl declarator(CstNode node, ModifiersTreeImpl modifiers, TypeTree type) {
    IdentifierTreeImpl name = names().identifier(requiredField(node, "name", firstOf(node, "identifier")));
    VariableTreeImpl tree = new VariableTreeImpl(name).completeModifiersAndType(modifiers, applyDimensions(type, node));
    CstNode value = field(node, "value");
    if (value != null) {
      ExpressionTree initializer = expressions().expression(value);
      tree.completeTypeAndInitializer(tree.type(), token(required(node, "=")), initializer);
    }
    bindDeclaration(tree, nameNode(node), name);
    Type declared = tree.type().symbolType();
    if (tree.type() instanceof VarTypeTreeImpl && tree.initializer() != null && tree.initializer().symbolType().isIntersectionType() && tree.symbol() instanceof JSymbol symbol) {
      symbol.type(tree.initializer().symbolType());
      ascribe(tree.type(), tree.initializer().symbolType());
    } else if (keepsArguments(declared) && tree.symbol() instanceof JSymbol symbol) {
      symbol.type(declared);
    } else if (tree.symbol().type() != null && !tree.symbol().type().isUnknown()) {
      ascribe(tree.type(), tree.symbol().type());
    }
    return tree;
  }

  private TypeTree applyDimensions(TypeTree type, CstNode node) {
    CstNode dimensions = field(node, "dimensions");
    if (dimensions == null) {
      return type;
    }
    return typeTrees().arrayType(type, dimensions);
  }

  BlockTreeImpl staticInitializer(CstNode node) {
    BlockTreeImpl block = statements().block(node);
    ownInitializerVariables(block, true);
    return new StaticInitializerTreeImpl(token(required(node, "static")), (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken());
  }

  /** Variables declared in an initializer block belong to the block, not to the class. */
  private void ownInitializerVariables(BlockTreeImpl block, boolean isStatic) {
    Symbol.TypeSymbol enclosing = state.types.peek();
    if (!(enclosing instanceof JTypeSymbol enclosingType)) {
      return;
    }
    JInitializerBlockSymbol owner = isStatic ? sema.memberFactory().staticInitializerBlockSymbol(enclosingType) : sema.memberFactory().initializerBlockSymbol(enclosingType);
    block.accept(new BaseTreeVisitor() {
      @Override
      public void visitVariable(VariableTree tree) {
        if (tree.symbol() instanceof JVariableSymbol variable && enclosing.equals(variable.owner())) {
          variable.owner(owner);
        }
        super.visitVariable(tree);
      }

      @Override
      public void visitClass(ClassTree tree) {
        // members of local and anonymous classes keep their own owner
      }

      @Override
      public void visitLambdaExpression(LambdaExpressionTree tree) {
        // lambda variables belong to the lambda
      }
    });
  }

  BlockTreeImpl initializer(CstNode node) {
    BlockTreeImpl block = statements().block(node);
    ownInitializerVariables(block, false);
    return new BlockTreeImpl(Tree.Kind.INITIALIZER, (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken());
  }

  AssignmentExpressionTreeImpl elementValuePair(CstNode node) {
    CstNode key = requiredField(node, "key", firstNamed(node));
    IdentifierTreeImpl name = names().identifier(key);
    // An element name is never a local variable or a parameter of the same name.
    bindUse(name, key, true);
    ExpressionTree value = expressions().expression(requiredField(node, "value", lastNamed(node)));
    AssignmentExpressionTreeImpl tree = new AssignmentExpressionTreeImpl(Tree.Kind.ASSIGNMENT, name, token(required(node, "=")), value);
    ascribe(tree, value.symbolType());
    return tree;
  }

  ModifiersTreeImpl modifiers(CstNode node) {
    CstNode modifiers = childKind(node, "modifiers");
    List<ModifierTree> trees = new ArrayList<>();
    if (modifiers != null) {
      for (CstNode child : modifiers.children()) {
        trees.add(isAnnotation(child) ? annotation(child) : new ModifierKeywordTreeImpl(modifierKeyword(child.text()), token(child)));
      }
    }
    return new ModifiersTreeImpl(trees);
  }

  AnnotationTreeImpl annotation(CstNode node) {
    TypeTree type = (TypeTree) expressions().expression(requiredField(node, "name", null));
    if (type instanceof IdentifierTreeImpl identifier) {
      names().recoverType(identifier);
    }
    ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList();
    CstNode args = field(node, "arguments");
    if (args != null) {
      arguments = expressions().arguments(args);
    }
    AnnotationTreeImpl tree = new AnnotationTreeImpl(token(required(node, "@")), type, arguments);
    ascribe(tree, names().annotationType(type));
    if (type instanceof MemberSelectExpressionTreeImpl select && select.symbolType().isUnknown()) {
      // the written qualified name is the annotation's type; the name tree itself stays unresolved
      ascribe(select, JType.unknown(sema));
    }
    for (ExpressionTree argument : arguments) {
      if (argument instanceof AssignmentExpressionTree assignment && assignment.variable() instanceof IdentifierTreeImpl element && element.symbol().isUnknown()
        && type.symbolType().isClass()) {
        Symbol.MethodSymbol method = sema.lookup().declaredMethod(type.symbolType().fullyQualifiedName(), element.name(), List.of());
        if (method != null) {
          bind(element, method);
          ascribe(element, method.returnType().type());
        }
      }
    }
    return tree;
  }

  List<AnnotationTree> annotations(CstNode node) {
    return node.children().stream().filter(CstNodes::isAnnotation).<AnnotationTree>map(this::annotation).collect(Collectors.toCollection(ArrayList::new));
  }
}
