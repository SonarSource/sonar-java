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
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.ArrayDimensionTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.java.model.expression.VarTypeTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import static org.sonar.java.semsitter.Bindings.ascribe;
import static org.sonar.java.semsitter.Bindings.bind;
import static org.sonar.java.semsitter.CstNodes.childKind;
import static org.sonar.java.semsitter.CstNodes.closingAngle;
import static org.sonar.java.semsitter.CstNodes.field;
import static org.sonar.java.semsitter.CstNodes.firstOf;
import static org.sonar.java.semsitter.CstNodes.firstType;
import static org.sonar.java.semsitter.CstNodes.isAnnotation;
import static org.sonar.java.semsitter.CstNodes.isTypeKind;
import static org.sonar.java.semsitter.CstNodes.keyword;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.requiredField;

/** Translates type syntax: primitive, array, generic, wildcard and union types, and type parameters. */
final class TypeTranslator extends TranslatorPart {

  private static final String EXTENDS = "extends";

  TypeTranslator(TranslationState state) {
    super(state);
  }

  TypeTree type(CstNode node) {
    return switch (node.k()) {
      case "integral_type", "floating_point_type", "boolean_type", "void_type" -> primitive(node);
      case "generic_type" -> generic(node);
      case "array_type" -> arrayType(type(requiredField(node, "element", firstType(node))), field(node, "dimensions"));
      case "scoped_identifier", "scoped_type_identifier", "field_access" -> names().memberSelect(node);
      case "wildcard" -> wildcard(node);
      case "annotated_type" -> annotatedType(node);
      case "type_identifier" -> "var".equals(node.text()) ? new VarTypeTreeImpl(token(node)) : (TypeTree) names().primary(node);
      default -> (TypeTree) ("identifier".equals(node.k()) ? names().primary(node) : expressions().expression(node));
    };
  }

  private TypeTree annotatedType(CstNode node) {
    return annotate(type(firstType(node)), node);
  }

  TypeTree annotate(TypeTree type, CstNode node) {
    annotateLeftmost(type, node.children().stream().filter(CstNodes::isAnnotation).<AnnotationTree>map(members()::annotation).toList());
    return type;
  }

  /** Like ECJ, the annotations written before a qualified or parameterized type go on its leftmost name. */
  static void annotateLeftmost(TypeTree type, List<AnnotationTree> annotations) {
    TypeTree target = type;
    while (true) {
      TypeTree next = switch (target) {
        case ParameterizedTypeTree parameterized -> parameterized.type();
        case MemberSelectExpressionTree select when select.expression() instanceof TypeTree outer -> outer;
        default -> null;
      };
      if (next == null) {
        break;
      }
      target = next;
    }
    if (!annotations.isEmpty() && target instanceof JavaTree.AnnotatedTypeTree annotated) {
      annotated.complete(annotations);
    }
  }

  private JavaTree.PrimitiveTypeTreeImpl primitive(CstNode node) {
    CstNode keyword = node.children().isEmpty() ? node : node.children().get(0);
    JavaTree.PrimitiveTypeTreeImpl tree = new JavaTree.PrimitiveTypeTreeImpl(token(keyword));
    tree.complete(List.of());
    ascribe(tree, sema.getClassType(keyword.text()));
    return tree;
  }

  private JavaTree.ParameterizedTypeTreeImpl generic(CstNode node) {
    TypeTree raw = type(firstType(node));
    JavaTree.ParameterizedTypeTreeImpl tree = new JavaTree.ParameterizedTypeTreeImpl(raw, typeArguments(required(node, "type_arguments")));
    List<Type> args = new ArrayList<>();
    for (TypeTree argument : tree.typeArguments()) {
      if (argument instanceof WildcardTree wildcard) {
        Type wildcardType = wildcardType(wildcard);
        ascribe(argument, wildcardType);
        args.add(wildcardType);
      } else {
        args.add(argument.symbolType());
      }
    }
    if (raw.symbolType() instanceof JType) {
      List<JType> typeArguments = new ArrayList<>();
      for (Type argument : args) {
        typeArguments.add(argument instanceof JType jType ? jType : JType.unknown(sema));
      }
      JType parameterized = JType.named(sema, raw.symbolType().erasure().fullyQualifiedName(), typeArguments);
      ascribe(tree, parameterized);
      if (raw instanceof AbstractTypedTree typed) {
        ascribe(typed, parameterized);
      }
      if (raw instanceof IdentifierTreeImpl identifier) {
        bind(identifier, parameterized.symbol());
      }
    }
    return tree;
  }

  TypeTree arrayType(TypeTree element, @Nullable CstNode dimensions) {
    if (dimensions == null) {
      return element;
    }
    TypeTree current = element;
    List<AnnotationTreeImpl> annotations = new ArrayList<>();
    List<CstNode> children = dimensions.children();
    for (int i = 0; i < children.size(); i++) {
      if ("[".equals(children.get(i).text())) {
        InternalSyntaxToken close = token(children.get(i + 1));
        JType component = current.symbolType() instanceof JType jType ? jType : JType.unknown(sema);
        current = new JavaTree.ArrayTypeTreeImpl(current, annotations, token(children.get(i)), close);
        annotations = new ArrayList<>();
        ascribe(current, JType.array(sema, component));
      } else if (isAnnotation(children.get(i))) {
        annotations.add(members().annotation(children.get(i)));
      }
    }
    return current;
  }

  private Type wildcardType(WildcardTree wildcard) {
    if (wildcard.is(Tree.Kind.SUPER_WILDCARD) && wildcard.bound() != null && wildcard.bound().symbolType() instanceof JType lower) {
      return JType.superWildcard(sema, lower);
    }
    Type upper = wildcard.is(Tree.Kind.EXTENDS_WILDCARD) && wildcard.bound() != null ? wildcard.bound().symbolType() : sema.getClassType("java.lang.Object");
    return upper instanceof JType bound ? JType.wildcard(sema, bound) : Type.UNKNOWN;
  }

  private JavaTree.WildcardTreeImpl wildcard(CstNode node) {
    CstNode bound = firstType(node);
    List<AnnotationTree> annotations = node.children().stream().filter(CstNodes::isAnnotation).<AnnotationTree>map(members()::annotation).toList();
    if (bound == null) {
      JavaTree.WildcardTreeImpl tree = new JavaTree.WildcardTreeImpl(token(required(node, "?")));
      tree.complete(annotations);
      return tree;
    }
    boolean upper = keyword(node, EXTENDS) != null;
    JavaTree.WildcardTreeImpl tree = new JavaTree.WildcardTreeImpl(
      upper ? Tree.Kind.EXTENDS_WILDCARD : Tree.Kind.SUPER_WILDCARD,
      token(required(node, upper ? EXTENDS : "super")),
      type(bound)).complete(token(required(node, "?")));
    tree.complete(annotations);
    return tree;
  }

  TypeArgumentListTreeImpl typeArguments(CstNode node) {
    TypeArgumentListTreeImpl list = new TypeArgumentListTreeImpl(token(required(node, "<")), closingAngle(node));
    for (CstNode child : node.children()) {
      if ("<".equals(child.text()) || ">".equals(child.text())) {
        continue;
      }
      if (",".equals(child.text())) {
        list.separators().add(token(child));
      } else {
        list.add(type(child));
      }
    }
    return list;
  }

  TypeParameterListTreeImpl typeParameters(CstNode node) {
    TypeParameterListTreeImpl list = new TypeParameterListTreeImpl(token(required(node, "<")), closingAngle(node));
    for (CstNode child : node.children()) {
      if ("<".equals(child.text()) || ">".equals(child.text())) {
        continue;
      }
      if (",".equals(child.text())) {
        list.separators().add(token(child));
      } else {
        list.add(typeParameter(child));
      }
    }
    return list;
  }

  private TypeParameterTree typeParameter(CstNode node) {
    CstNode nameNode = firstOf(node, "type_identifier", "identifier");
    IdentifierTreeImpl name = names().identifier(nameNode);
    bindUse(name, nameNode);
    CstNode bound = childKind(node, "type_bound");
    TypeParameterTreeImpl tree;
    if (bound == null) {
      tree = new TypeParameterTreeImpl(name);
    } else {
      tree = new TypeParameterTreeImpl(name, token(required(bound, EXTENDS)), separatedTypes(bound));
    }
    if (name.symbol().isUnknown()) {
      bindDeclaration(tree, nameNode, name);
    }
    if (name.symbol() instanceof Symbol.TypeSymbol typeSymbol) {
      tree.setSymbol(typeSymbol);
    }
    if (bound != null && name.symbol() instanceof JTypeSymbol parameter && parameter.isTypeParameter()) {
      parameter.boundTrees(tree.bounds());
    }
    return tree;
  }

  TypeTree catchType(CstNode node) {
    int alternatives = 0;
    for (CstNode child : node.children()) {
      if (isTypeKind(child.k())) {
        alternatives++;
      }
    }
    if (alternatives <= 1) {
      return type(firstType(node));
    }
    QualifiedIdentifierListTreeImpl list = QualifiedIdentifierListTreeImpl.emptyList();
    for (CstNode child : node.children()) {
      if (isTypeKind(child.k())) {
        list.add(type(child));
      } else {
        list.separators().add(token(child));
      }
    }
    JavaTree.UnionTypeTreeImpl union = new JavaTree.UnionTypeTreeImpl(list);
    ascribe(union, leastUpperBound(list.stream().map(TypeTree::symbolType).toList()));
    return union;
  }

  /** The closest class every alternative of a multi-catch descends from. */
  private Type leastUpperBound(List<Type> alternatives) {
    if (alternatives.stream().anyMatch(Type::isUnknown)) {
      return Type.UNKNOWN;
    }
    Type candidate = alternatives.get(0);
    while (candidate != null && !candidate.isUnknown()) {
      Type current = candidate;
      if (alternatives.stream().allMatch(alternative -> alternative.isSubtypeOf(current))) {
        return current;
      }
      candidate = current.symbol().superClass();
    }
    return sema.getClassType("java.lang.Object");
  }

  QualifiedIdentifierListTreeImpl separatedTypes(CstNode node) {
    QualifiedIdentifierListTreeImpl list = QualifiedIdentifierListTreeImpl.emptyList();
    addSeparatedTypes(list, node);
    return list;
  }

  private void addSeparatedTypes(QualifiedIdentifierListTreeImpl list, CstNode node) {
    for (CstNode child : node.children()) {
      if ("type_list".equals(child.k())) {
        addSeparatedTypes(list, child);
      } else if (isTypeKind(child.k())) {
        list.add(type(child));
      } else if (",".equals(child.text()) || "&".equals(child.text())) {
        list.separators().add(token(child));
      }
    }
  }

  List<ArrayDimensionTree> dimensions(CstNode node) {
    List<ArrayDimensionTree> dims = new ArrayList<>();
    List<AnnotationTree> annotations = new ArrayList<>();
    ExpressionTree index = null;
    InternalSyntaxToken open = null;
    for (CstNode child : node.children()) {
      if ("[".equals(child.text())) {
        open = token(child);
        index = null;
      } else if ("]".equals(child.text())) {
        dims.add(new ArrayDimensionTreeImpl(Objects.requireNonNull(open), index, token(child)).completeAnnotations(annotations));
        annotations = new ArrayList<>();
        open = null;
      } else if (open != null) {
        index = expressions().expression(child);
      } else if (isAnnotation(child)) {
        annotations.add(members().annotation(child));
      }
    }
    return dims;
  }

  Type currentType() {
    Symbol.TypeSymbol enclosing = state.types.peek();
    return enclosing == null ? Type.UNKNOWN : enclosing.type();
  }

  Type arrayTypeOf(TypeTree element, int dimensions) {
    Type type = element.symbolType();
    if (!(type instanceof JType jType)) {
      return Type.UNKNOWN;
    }
    for (int i = 0; i < Math.max(dimensions, 1); i++) {
      jType = JType.array(sema, jType);
    }
    return jType;
  }
}
