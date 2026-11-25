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
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.KeywordSuper;
import org.sonar.java.model.KeywordThis;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import static org.sonar.java.semsitter.Bindings.ascribe;
import static org.sonar.java.semsitter.Bindings.bind;
import static org.sonar.java.semsitter.Bindings.literal;
import static org.sonar.java.semsitter.CstNodes.compoundName;
import static org.sonar.java.semsitter.CstNodes.dottedName;
import static org.sonar.java.semsitter.CstNodes.field;
import static org.sonar.java.semsitter.CstNodes.firstNamed;
import static org.sonar.java.semsitter.CstNodes.keyword;
import static org.sonar.java.semsitter.CstNodes.lastKeyword;
import static org.sonar.java.semsitter.CstNodes.lastNamed;
import static org.sonar.java.semsitter.CstNodes.receiverKeyword;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.same;
/** Translates and resolves identifiers, qualified names, member selects and literals. */
final class NameTranslator extends TranslatorPart {

  private static final String SUPER = "super";
  private static final String JAVA_LANG_OBJECT = "java.lang.Object";

  NameTranslator(TranslationState state) {
    super(state);
  }

  ExpressionTree primary(CstNode node) {
    if ("this".equals(node.k())) {
      return thisKeyword(node);
    }
    if (SUPER.equals(node.k())) {
      return superKeyword(node);
    }
    if ("true".equals(node.k()) || "false".equals(node.k())) {
      return literal(Tree.Kind.BOOLEAN_LITERAL, token(node), sema.getClassType("boolean"));
    }
    if ("null_literal".equals(node.k())) {
      return literal(Tree.Kind.NULL_LITERAL, token(node), JType.nullType(sema));
    }
    IdentifierTreeImpl identifier = identifier(node);
    bindUse(identifier, node);
    if (!state.naming && identifier.symbol().isUnknown() && node.unresolved()) {
      String type = importedType(identifier.name());
      if (type != null) {
        bindType(identifier, type);
      }
    }
    if (!state.naming && "type_identifier".equals(node.k())) {
      preferSingleImport(identifier);
      recoverType(identifier);
    }
    return identifier;
  }

  private KeywordThis thisKeyword(CstNode node) {
    KeywordThis keyword = new KeywordThis(token(node), Symbol.UNKNOWN_SYMBOL);
    Symbol.TypeSymbol enclosing = state.types.peek();
    if (enclosing != null) {
      bind(keyword, sema.lookup().receiverSymbol(enclosing, "this", enclosing.type()));
      ascribe(keyword, enclosing.type());
    }
    return keyword;
  }

  private KeywordSuper superKeyword(CstNode node) {
    KeywordSuper keyword = new KeywordSuper(token(node), Symbol.UNKNOWN_SYMBOL);
    Symbol.TypeSymbol enclosing = state.types.peek();
    if (enclosing != null) {
      Type superType = enclosing.superClass() == null ? Type.UNKNOWN : enclosing.superClass();
      bind(keyword, sema.lookup().receiverSymbol(enclosing, SUPER, superType));
      ascribe(keyword, superType);
    }
    return keyword;
  }

  /** An unresolved qualified type name keeps its written name where the compiler's recovery does. */
  TypeTree recoverQualifiedType(TypeTree type) {
    if (type instanceof MemberSelectExpressionTreeImpl select && select.symbolType().isUnknown() && select.identifier().symbol().isUnknown()) {
      IdentifierTreeImpl identifier = (IdentifierTreeImpl) select.identifier();
      JTypeSymbol recovered = sema.memberFactory().recoveredType(dottedName(select));
      bind(identifier, recovered);
      ascribe(identifier, recovered.type());
      ascribe(select, recovered.type());
    }
    return type;
  }

  /** The type of an annotation: an unresolved one keeps its written, possibly qualified, name. */
  Type annotationType(TypeTree type) {
    if (type.symbolType().isUnknown()) {
      return sema.memberFactory().recoveredType(dottedName((ExpressionTree) type)).type();
    }
    return type.symbolType();
  }

  /** An unresolved type name keeps its written name, as a recovered binding does, so checks can match on the simple name. */
  void recoverType(IdentifierTreeImpl identifier) {
    if (identifier.symbol().isUnknown()) {
      JTypeSymbol recovered = sema.memberFactory().recoveredType(identifier.name());
      bind(identifier, recovered);
      ascribe(identifier, recovered.type());
    }
  }

  /** A duplicate single-type import is an error; recovery keeps the first, which is the type this simple name denotes. */
  private void preferSingleImport(IdentifierTreeImpl identifier) {
    String imported = state.imports.singleImports.get(identifier.name());
    if (imported == null || !sema.declaresType(imported)) {
      return;
    }
    Symbol symbol = identifier.symbol();
    boolean constructor = symbol.isMethodSymbol() && "<init>".equals(symbol.name());
    if (!symbol.isTypeSymbol() && !symbol.isUnknown() && !constructor) {
      return;
    }
    String resolved = identifier.symbolType().erasure().fullyQualifiedName();
    if (!imported.equals(resolved)) {
      bindType(identifier, imported);
    }
  }

  /** The type a simple name stands for when SemSitter left it unresolved: imported, in the package, or in java.lang. */
  @Nullable
  private String importedType(String name) {
    List<String> candidates = new ArrayList<>();
    if (state.imports.singleImports.containsKey(name)) {
      candidates.add(state.imports.singleImports.get(name));
    }
    candidates.add(state.packageName.isEmpty() ? name : (state.packageName + "." + name));
    state.imports.onDemandImports.forEach(prefix -> candidates.add(prefix + "." + name));
    candidates.add("java.lang." + name);
    return candidates.stream().filter(sema::declaresType).findFirst().orElse(null);
  }

  private void resolveQualifiedType(ExpressionTree qualifier, IdentifierTreeImpl identifier) {
    Symbol owner = qualifierSymbol(qualifier);
    String candidate;
    if (owner.isTypeSymbol() && !owner.isUnknown()) {
      candidate = owner.type().fullyQualifiedName() + "$" + identifier.name();
    } else if (owner.isUnknown() || owner.isPackageSymbol()) {
      String prefix = dottedName(qualifier);
      candidate = prefix.isEmpty() ? null : (prefix + "." + identifier.name());
    } else {
      candidate = null;
    }
    if (candidate != null && sema.declaresType(candidate)) {
      bindType(identifier, candidate);
    }
  }

  private static Symbol qualifierSymbol(ExpressionTree qualifier) {
    if (qualifier instanceof MemberSelectExpressionTree select) {
      return select.identifier().symbol();
    }
    if (qualifier instanceof IdentifierTree name) {
      return name.symbol();
    }
    return Symbol.UNKNOWN_SYMBOL;
  }

  @Nullable
  private String fieldOwner(ExpressionTree qualifier) {
    Type receiver = qualifier.symbolType().erasure();
    if (receiver.isClass()) {
      return receiver.fullyQualifiedName();
    }
    return qualifier instanceof IdentifierTree name ? importedType(name.name()) : null;
  }

  /** The method SemSitter left unresolved, looked up in the receiver type, or in the enclosing types and static imports of an unqualified call. */
  @Nullable
  Symbol.MethodSymbol libraryMethod(@Nullable ExpressionTree qualifier, String name, List<Type> arguments, Set<Integer> functions) {
    List<String> owners = new ArrayList<>();
    if (qualifier != null) {
      Type receiver = qualifier.symbolType().erasure();
      if (receiver.isClass()) {
        owners.add(receiver.fullyQualifiedName());
      } else if (receiver.isArray()) {
        owners.add(JAVA_LANG_OBJECT);
      } else if (qualifier instanceof IdentifierTree typeName) {
        String imported = importedType(typeName.name());
        if (imported != null) {
          owners.add(imported);
        }
      }
    } else {
      state.types.stream().filter(type -> !type.isUnknown()).forEach(type -> owners.add(type.type().fullyQualifiedName()));
      if (state.imports.staticImports.containsKey(name)) {
        owners.add(state.imports.staticImports.get(name));
      }
      owners.addAll(state.imports.staticOnDemandImports);
    }
    Symbol.MethodSymbol found = owners.stream().map(owner -> sema.lookup().declaredMethod(owner, name, arguments, functions)).filter(Objects::nonNull).findFirst().orElse(null);
    if (found == null && qualifier != null && !qualifier.symbolType().isUnknown() && !owners.contains(JAVA_LANG_OBJECT)) {
      found = sema.lookup().declaredMethod(JAVA_LANG_OBJECT, name, arguments, functions);
    }
    return found;
  }

  private void bindQualifierPackages(ExpressionTree qualifier, IdentifierTree identifier) {
    if (identifier.symbol() instanceof Symbol.TypeSymbol type && !type.isUnknown()) {
      Symbol owner = type.owner();
      while (owner != null && !owner.isPackageSymbol()) {
        owner = owner.owner();
      }
      if (owner != null && dottedName(qualifier).equals(owner.name())) {
        bindPackage(qualifier, owner.name());
      }
    }
  }

  void bindPackage(ExpressionTree tree, String name) {
    ExpressionTree last = tree;
    if (tree instanceof MemberSelectExpressionTree select) {
      int dot = name.lastIndexOf('.');
      if (dot < 0) {
        return;
      }
      bindPackage(select.expression(), name.substring(0, dot));
      last = select.identifier();
    }
    if (last instanceof IdentifierTreeImpl identifier && identifier.symbol().isUnknown()) {
      bind(identifier, sema.packageSymbol(name));
    }
  }

  IdentifierTreeImpl identifier(CstNode node) {
    String text = node.text().isEmpty() ? node.k() : node.text();
    InternalSyntaxToken token = state.tokens.getOrDefault(node.proto, new InternalSyntaxToken(node.line(), node.col(), text, List.of(), false));
    return new IdentifierTreeImpl(token, state.unnamedVariables && "_".equals(text));
  }

  LiteralTreeImpl integerLiteral(CstNode node) {
    String text = node.text();
    boolean isLong = text.endsWith("l") || text.endsWith("L");
    return literal(isLong ? Tree.Kind.LONG_LITERAL : Tree.Kind.INT_LITERAL, token(node), sema.getClassType(isLong ? "long" : "int"));
  }

  LiteralTreeImpl floatingLiteral(CstNode node) {
    String text = node.text();
    boolean isFloat = text.endsWith("f") || text.endsWith("F");
    return literal(isFloat ? Tree.Kind.FLOAT_LITERAL : Tree.Kind.DOUBLE_LITERAL, token(node), sema.getClassType(isFloat ? "float" : "double"));
  }

  MemberSelectExpressionTreeImpl memberSelect(CstNode node) {
    CstNode object = field(node, "object");
    if (object == null) {
      object = field(node, "scope");
    }
    CstNode name = field(node, "name");
    if (name == null) {
      name = field(node, "field");
    }
    CstNode scope = object == null ? firstNamed(node) : object;
    ExpressionTree qualifier = memberSelectQualifier(node, object, scope);
    CstNode nameNode = name == null ? lastNamed(node) : name;
    IdentifierTreeImpl identifier = receiverKeyword(identifier(nameNode));
    bindUse(identifier, name != null || nameNode.proto.getRefsCount() > 0 ? nameNode : node, true);
    if (!state.naming) {
      resolveMemberSelect(qualifier, identifier);
    }
    List<AnnotationTree> annotations = node.children().stream().filter(CstNodes::isAnnotation).<AnnotationTree>map(members()::annotation).toList();
    if (!annotations.isEmpty()) {
      identifier.complete(annotations);
    }
    MemberSelectExpressionTreeImpl tree = new MemberSelectExpressionTreeImpl(qualifier, token(lastKeyword(node, ".")), identifier);
    ascribe(tree, identifier.symbolType());
    return tree;
  }

  private ExpressionTree memberSelectQualifier(CstNode node, @Nullable CstNode object, CstNode scope) {
    if (object != null) {
      return superQualifier(node, object);
    }
    return compoundName(scope) ? invocations().typeExpression(scope) : primary(scope);
  }

  /** Resolves an identifier left unknown by SemSitter as a member of the qualifier: a nested type, a field, or a package. */
  private void resolveMemberSelect(ExpressionTree qualifier, IdentifierTreeImpl identifier) {
    bindQualifiedReceiver(qualifier, identifier);
    if (identifier.symbol().isUnknown()) {
      resolveQualifiedType(qualifier, identifier);
    }
    if (identifier.symbol().isUnknown()) {
      String owner = fieldOwner(qualifier);
      Symbol.VariableSymbol field = owner == null ? null : sema.lookup().declaredField(owner, identifier.name());
      if (field != null) {
        bind(identifier, field);
        ascribe(identifier, field.type());
      }
    }
    bindQualifierPackages(qualifier, identifier);
    if (identifier.symbol() instanceof JVariableSymbol field && !field.isUnknown() && field.type() != null) {
      Type read = sema.substitution().fieldTypeOn(field, qualifier.symbolType());
      if (read != field.type()) {
        ascribe(identifier, read);
      }
    }
  }

  ExpressionTree superQualifier(CstNode node, CstNode object) {
    ExpressionTree qualifier = expressions().expression(object);
    CstNode superNode = keyword(node, SUPER);
    if (superNode == null || same(superNode, object)) {
      return qualifier;
    }
    IdentifierTreeImpl identifier = new KeywordSuper(token(superNode), Symbol.UNKNOWN_SYMBOL);
    bindQualifiedReceiver(qualifier, identifier);
    MemberSelectExpressionTreeImpl select = new MemberSelectExpressionTreeImpl(qualifier, token(required(node, ".")), identifier);
    ascribe(select, identifier.symbolType());
    return select;
  }

  private void bindQualifiedReceiver(ExpressionTree qualifier, IdentifierTreeImpl identifier) {
    boolean isThis = "this".equals(identifier.name());
    if (!identifier.symbol().isUnknown() || (!isThis && !SUPER.equals(identifier.name()))
      || !(qualifier.symbolType().symbol() instanceof Symbol.TypeSymbol owner) || owner.isUnknown()) {
      return;
    }
    Type type = isThis || owner.isInterface() || owner.superClass() == null ? owner.type() : owner.superClass();
    bind(identifier, sema.lookup().receiverSymbol(owner, identifier.name(), type));
    ascribe(identifier, type);
  }
}
