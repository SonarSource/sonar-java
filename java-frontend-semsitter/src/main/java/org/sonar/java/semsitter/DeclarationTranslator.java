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
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.semsitter.Bindings.argumentTypes;
import static org.sonar.java.semsitter.Bindings.ascribe;
import static org.sonar.java.semsitter.Bindings.bind;
import static org.sonar.java.semsitter.CstNodes.childKind;
import static org.sonar.java.semsitter.CstNodes.enumSeparator;
import static org.sonar.java.semsitter.CstNodes.field;
import static org.sonar.java.semsitter.CstNodes.firstKeyword;
import static org.sonar.java.semsitter.CstNodes.firstOf;
import static org.sonar.java.semsitter.CstNodes.firstType;
import static org.sonar.java.semsitter.CstNodes.nameNode;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.requiredField;

/** Translates class, interface, enum, record and annotation type declarations. */
final class DeclarationTranslator extends TranslatorPart {

  private static final String INTERFACE_DECLARATION = "interface_declaration";
  private static final String ENUM_DECLARATION = "enum_declaration";
  private static final String RECORD_DECLARATION = "record_declaration";
  private static final String ANNOTATION_TYPE_DECLARATION = "annotation_type_declaration";

  DeclarationTranslator(TranslationState state) {
    super(state);
  }

  void addMember(List<Tree> members, CstNode node) {
    switch (node.k()) {
      case "field_declaration", "constant_declaration", "local_variable_declaration" -> members.addAll(members().variables(node));
      case "method_declaration", "constructor_declaration", "compact_constructor_declaration" -> members.add(members().method(node));
      case "annotation_type_element_declaration" -> members.add(annotationElement(node));
      case "class_declaration", INTERFACE_DECLARATION, ENUM_DECLARATION, RECORD_DECLARATION, ANNOTATION_TYPE_DECLARATION -> members.add(typeDeclaration(node));
      case "static_initializer" -> members.add(members().staticInitializer(node));
      case "block" -> members.add(members().initializer(node));
      case ";" -> members.add(new EmptyStatementTreeImpl(token(node)));
      case "{", "}" -> {
        // punctuation of the body
      }
      default -> {
        // skipped construct
      }
    }
  }

  ClassTreeImpl typeDeclaration(CstNode node) {
    Tree.Kind kind = declarationKind(node);
    CstNode body = requiredField(node, "body", null);
    CstNode name = requiredField(node, "name", null);
    state.types.push(sema.resolveTypeSymbol(name.decl()));
    // translated before the members so that the bounds of the type parameters are known inside the bodies
    CstNode typeParameters = field(node, "type_parameters");
    TypeParameterListTreeImpl typeParameterList = typeParameters == null ? null : typeTrees().typeParameters(typeParameters);
    List<Tree> members;
    try {
      members = membersOf(body);
    } finally {
      state.types.pop();
    }
    ClassTreeImpl tree = new ClassTreeImpl(kind, token(required(body, "{")), members, token(required(body, "}")));
    IdentifierTreeImpl identifier = names().identifier(name);
    InternalSyntaxToken keywordToken = token(required(node, declarationKeyword(node)));
    if (kind == Tree.Kind.ANNOTATION_TYPE) {
      tree.completeAtToken(new InternalSyntaxToken(keywordToken.line(), keywordToken.column(), "@", keywordToken.trivias(), false));
      keywordToken = new InternalSyntaxToken(keywordToken.line(), keywordToken.column() + 1, "interface", List.of(), false);
    }
    tree.complete(members().modifiers(node), keywordToken, identifier);
    if (typeParameterList != null) {
      tree.completeTypeParameters(typeParameterList);
    }
    if (kind == Tree.Kind.RECORD) {
      CstNode parameters = requiredField(node, "parameters", null);
      // the components must not shadow same-named variables of the enclosing method after the record
      state.scopes.addFirst(new HashMap<>());
      try {
        tree.completeRecordComponents(token(required(parameters, "(")), members().parameters(parameters), token(required(parameters, ")")));
      } finally {
        state.scopes.removeFirst();
      }
    }
    completeHierarchy(tree, node);
    bindDeclaration(tree, name, identifier);
    bindSupertypes(tree, kind);
    return tree;
  }

  private Tree.Kind declarationKind(CstNode node) {
    return switch (node.k()) {
      case INTERFACE_DECLARATION -> Tree.Kind.INTERFACE;
      case ENUM_DECLARATION -> Tree.Kind.ENUM;
      case RECORD_DECLARATION -> state.records ? Tree.Kind.RECORD : Tree.Kind.CLASS;
      case ANNOTATION_TYPE_DECLARATION -> Tree.Kind.ANNOTATION_TYPE;
      default -> Tree.Kind.CLASS;
    };
  }

  private static String declarationKeyword(CstNode node) {
    return switch (node.k()) {
      case INTERFACE_DECLARATION -> "interface";
      case ANNOTATION_TYPE_DECLARATION -> "@interface";
      case ENUM_DECLARATION -> "enum";
      case RECORD_DECLARATION -> "record";
      default -> "class";
    };
  }

  private void completeHierarchy(ClassTreeImpl tree, CstNode node) {
    CstNode superclass = childKind(node, "superclass");
    if (superclass != null) {
      tree.completeSuperclass(token(required(superclass, "extends")), typeTrees().type(firstType(superclass)));
    }
    CstNode interfaces = childKind(node, "super_interfaces");
    if (interfaces == null) {
      interfaces = childKind(node, "extends_interfaces");
    }
    if (interfaces != null) {
      tree.completeInterfaces(firstKeyword(interfaces), typeTrees().separatedTypes(interfaces));
    }
    CstNode permits = childKind(node, "permits");
    if (permits != null) {
      tree.completePermittedTypes(token(required(permits, "permits")), typeTrees().separatedTypes(permits));
    }
  }

  private static void bindSupertypes(ClassTreeImpl tree, Tree.Kind kind) {
    if (kind == Tree.Kind.RECORD && tree.symbol() instanceof JTypeSymbol recordSymbol) {
      // the components of a record belong to the record, not to its canonical constructor
      tree.recordComponents().stream()
        .map(VariableTree::symbol)
        .filter(JVariableSymbol.class::isInstance)
        .map(JVariableSymbol.class::cast)
        .forEach(component -> component.owner(recordSymbol));
    }
    if (tree.symbol() instanceof JTypeSymbol declared) {
      if (tree.superClass() != null
        && tree.superClass().symbolType().symbol() instanceof JTypeSymbol parent && parent.id != null && !parent.isUnknown() && !parent.equals(declared)) {
        declared.superName(parent.id);
      } else if (kind == Tree.Kind.RECORD) {
        declared.superName("java.lang.Record");
      } else if (kind == Tree.Kind.ENUM) {
        declared.superName("java.lang.Enum");
      }
      declared.declaredSupertypes(tree.superClass() == null ? null : tree.superClass().symbolType(),
        tree.superInterfaces().stream().map(TypeTree::symbolType).toList());
    }
  }

  List<Tree> membersOf(CstNode body) {
    List<Tree> members = new ArrayList<>();
    List<CstNode> children = body.children();
    for (int i = 0; i < children.size(); i++) {
      CstNode child = children.get(i);
      if ("enum_constant".equals(child.k())) {
        members.add(enumConstant(child, enumSeparator(children, i)));
      } else if ("enum_body_declarations".equals(child.k())) {
        List<CstNode> declarations = child.children();
        int j = 0;
        while (childKind(body, "enum_constant") != null && j < declarations.size() && ";".equals(declarations.get(j).text())) {
          j++;
        }
        for (; j < declarations.size(); j++) {
          addMember(members, declarations.get(j));
        }
      } else {
        addMember(members, child);
      }
    }
    return members;
  }

  private EnumConstantTreeImpl enumConstant(CstNode node, @Nullable CstNode separatorNode) {
    IdentifierTreeImpl name = names().identifier(requiredField(node, "name", firstOf(node, "identifier")));
    CstNode arguments = field(node, "arguments");
    ArgumentListTreeImpl args = arguments == null ? ArgumentListTreeImpl.emptyList() : expressions().arguments(arguments);
    CstNode body = field(node, "body");
    ClassTreeImpl classBody = null;
    if (body != null) {
      classBody = new ClassTreeImpl(Tree.Kind.CLASS, token(required(body, "{")), membersOf(body), token(required(body, "}")));
      bindType(classBody, node);
    }
    InternalSyntaxToken separator = separatorNode == null ? null : token(separatorNode);
    EnumConstantTreeImpl tree = new EnumConstantTreeImpl(members().modifiers(node), name, new NewClassTreeImpl(name, args, classBody), separator);
    bindDeclaration(tree, nameNode(node), name);
    Symbol.TypeSymbol enumType = state.types.element();
    if (classBody != null && enumType instanceof JSymbol enumSymbol) {
      // An enum with a constant body is not final.
      enumSymbol.removeModifier("final");
    }
    boolean knownEnum = !enumType.isUnknown();
    bindEnumConstructor(name, args, knownEnum ? enumType : null);
    if (knownEnum) {
      ascribe(name, enumType.type());
      if (tree.symbol() instanceof JSymbol field && field.type().isUnknown()) {
        field.type(enumType.type());
      }
    }
    return tree;
  }

  /** The name of an enum constant is also its constructor call: it names the constructor of {@code enumType}, or nothing. */
  private void bindEnumConstructor(IdentifierTreeImpl name, ArgumentListTreeImpl args, @Nullable Symbol.TypeSymbol enumType) {
    Symbol constructor = null;
    if (enumType != null) {
      String owner = enumType.type().fullyQualifiedName();
      Symbol.MethodSymbol declared = sema.lookup().declaredMethod(owner, "<init>", argumentTypes(args));
      constructor = declared != null || !args.isEmpty() ? declared : sema.resolve(owner + "#<init>()");
    }
    bind(name, constructor instanceof Symbol.MethodSymbol ? constructor : null);
    if (name.symbol() instanceof Symbol.MethodSymbol method && !method.isUnknown()) {
      recordUse(method, name);
    }
  }

  private MethodTreeImpl annotationElement(CstNode node) {
    FormalParametersListTreeImpl formals = new FormalParametersListTreeImpl(token(required(node, "(")), token(required(node, ")")));
    CstNode value = field(node, "value");
    IdentifierTreeImpl name = names().identifier(requiredField(node, "name", firstOf(node, "identifier")));
    MethodTreeImpl tree = new MethodTreeImpl(formals, value == null ? null : token(required(node, "default")), value == null ? null : expressions().expression(value))
      .complete(typeTrees().arrayType(typeTrees().type(requiredField(node, "type", firstType(node))), field(node, "dimensions")), name, token(required(node, ";")))
      .completeWithModifiers(members().modifiers(node));
    bindDeclaration(tree, nameNode(node), name);
    return tree;
  }

  void bindImplicitClass(ClassTreeImpl tree, List<Tree> members) {
    JTypeSymbol implicitClass = firstKnownOwner(members);
    if (implicitClass == null && !state.implicitClassName.isEmpty() && sema.resolve(state.implicitClassName) instanceof JTypeSymbol named) {
      implicitClass = named;
    }
    if (implicitClass == null) {
      return;
    }
    tree.setSymbol(implicitClass);
    sema.declare(implicitClass.id, tree);
    adoptOrphanMethods(members, implicitClass);
  }

  private static JTypeSymbol firstKnownOwner(List<Tree> members) {
    for (Tree member : members) {
      if (member instanceof MethodTree method && method.symbol().owner() instanceof JTypeSymbol owner && !owner.isUnknown()) {
        return owner;
      }
    }
    return null;
  }

  private static void adoptOrphanMethods(List<Tree> members, JTypeSymbol implicitClass) {
    for (Tree member : members) {
      if (member instanceof MethodTree method && method.symbol() instanceof JMethodSymbol symbol && (symbol.owner() == null || symbol.owner().isUnknown())) {
        symbol.owner(implicitClass);
      }
    }
  }
}
