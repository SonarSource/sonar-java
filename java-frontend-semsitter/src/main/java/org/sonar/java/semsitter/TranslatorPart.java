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
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import static org.sonar.java.semsitter.Bindings.ascribe;
import static org.sonar.java.semsitter.Bindings.bind;
import static org.sonar.java.semsitter.Bindings.declaredParameterTypes;
import static org.sonar.java.semsitter.Bindings.identifierType;
import static org.sonar.java.semsitter.Bindings.isField;
import static org.sonar.java.semsitter.Bindings.isKnownOrRecovered;
import static org.sonar.java.semsitter.Bindings.keepsArguments;
import static org.sonar.java.semsitter.Bindings.sameErasure;
import static org.sonar.java.semsitter.Bindings.usageId;
import static org.sonar.java.semsitter.CstNodes.field;
import static org.sonar.java.semsitter.CstNodes.firstType;
import static org.sonar.java.semsitter.CstNodes.isComment;
import static org.sonar.java.semsitter.CstNodes.isTypeKind;
import static org.sonar.java.semsitter.CstNodes.keyword;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.requiredField;

/** Base of the translator parts: shared state, token lookup, and declaration and use bindings. */
class TranslatorPart {

  final JSema sema;
  final TranslationState state;

  static final List<String> FINAL_MODIFIER = List.of("final");

  TranslatorPart(TranslationState state) {
    this.state = state;
    this.sema = state.sema;
  }

  DeclarationTranslator declarations() {
    return state.parts.declarations;
  }

  MemberTranslator members() {
    return state.parts.members;
  }

  StatementTranslator statements() {
    return state.parts.statements;
  }

  SwitchTranslator switches() {
    return state.parts.switches;
  }

  NameTranslator names() {
    return state.parts.names;
  }

  ExpressionTranslator expressions() {
    return state.parts.expressions;
  }

  InvocationTranslator invocations() {
    return state.parts.invocations;
  }

  LambdaTranslator lambdas() {
    return state.parts.lambdas;
  }

  TypeTranslator typeTrees() {
    return state.parts.typeTrees;
  }

  void bindType(IdentifierTreeImpl identifier, String fqn) {
    Symbol symbol = sema.resolve(fqn);
    bind(identifier, symbol);
    ascribe(identifier, symbol.type());
    sema.use(fqn, identifier);
  }

  void bindDeclaration(Tree tree, @Nullable CstNode name, IdentifierTreeImpl identifier) {
    if (name == null) {
      return;
    }
    String id = name.decl();
    if (id == null) {
      if (tree instanceof VariableTreeImpl variable && "_".equals(identifier.name())) {
        bindUnnamedVariable(variable, identifier);
      }
      return;
    }
    if (id.contains("#<init>(")) {
      id = name.decls().stream().filter(extra -> !extra.contains("(")).findFirst().orElse(id);
    }
    Symbol symbol = sema.resolve(id);
    sema.declare(id, tree);
    for (String extra : name.decls()) {
      if (!extra.equals(id)) {
        sema.declare(extra, tree);
      }
    }
    completeVariableSymbol(tree, symbol);
    completeMethodReturnType(tree, symbol);
    bind(identifier, symbol);
    ascribe(identifier, identifierType(symbol));
    if (!state.scopes.isEmpty() && symbol.isVariableSymbol() && symbol.owner() != null && symbol.owner().isMethodSymbol()) {
      state.scopes.peekFirst().put(symbol.name(), symbol);
    }
    setDeclaredSymbol(tree, symbol);
  }

  private static void completeVariableSymbol(Tree tree, Symbol symbol) {
    // The scan records no modifiers for locals and parameters.
    if (tree instanceof VariableTreeImpl declared && symbol instanceof JSymbol declaredSymbol && symbol.isVariableSymbol()
      && ModifiersUtils.hasModifier(declared.modifiers(), Modifier.FINAL)) {
      declaredSymbol.addModifiers(FINAL_MODIFIER);
    }
    if (tree instanceof VariableTreeImpl variable && symbol instanceof JSymbol variableSymbol && symbol.isVariableSymbol() && variable.type() != null) {
      Type declared = variable.type().symbolType();
      if (symbol.type().isUnknown() ? isKnownOrRecovered(declared) : (keepsArguments(declared) && sameErasure(declared, symbol.type()))) {
        variableSymbol.type(declared);
      }
    }
  }

  private static void completeMethodReturnType(Tree tree, Symbol symbol) {
    if (tree instanceof MethodTreeImpl method && symbol instanceof JMethodSymbol methodSymbol && method.returnType() != null
      && methodSymbol.returnType().type().isUnknown() && isKnownOrRecovered(method.returnType().symbolType())) {
      methodSymbol.returnTypeSymbol(method.returnType().symbolType().symbol());
    } else if (tree instanceof MethodTreeImpl method && symbol instanceof JMethodSymbol methodSymbol && method.returnType() != null
      && method.returnType().symbolType().isParameterized() && !methodSymbol.returnType().type().isParameterized()
      && sameErasure(method.returnType().symbolType(), methodSymbol.returnType().type())) {
      methodSymbol.returnTypeSymbol(method.returnType().symbolType().symbol());
    }
  }

  private static void setDeclaredSymbol(Tree tree, Symbol symbol) {
    if (tree instanceof ClassTreeImpl classTree && symbol instanceof Symbol.TypeSymbol typeSymbol) {
      classTree.setSymbol(typeSymbol);
      if (symbol instanceof JSymbol nested && ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.STATIC) && !nested.isStatic()) {
        nested.addModifiers(List.of("static"));
      }
    } else if (tree instanceof MethodTreeImpl method && symbol instanceof Symbol.MethodSymbol methodSymbol) {
      method.setSymbol(methodSymbol);
      if (methodSymbol instanceof JMethodSymbol declared) {
        declaredParameterTypes(declared, method.parameters());
      }
    } else if (tree instanceof VariableTreeImpl variable && symbol instanceof Symbol.VariableSymbol variableSymbol) {
      variable.setSymbol(variableSymbol);
    }
  }

  /** An unnamed variable declares nothing the scan records, so it gets a local symbol of its own. */
  private void bindUnnamedVariable(VariableTreeImpl variable, IdentifierTreeImpl identifier) {
    var start = identifier.identifierToken().range().start();
    JVariableSymbol symbol = new JVariableSymbol(sema, "_@" + start.line() + ":" + start.column(), "_", "local");
    Type declared = variable.type() == null ? Type.UNKNOWN : variable.type().symbolType();
    if (isKnownOrRecovered(declared)) {
      symbol.type(declared);
    }
    sema.declare(symbol.id, variable);
    variable.setSymbol(symbol);
  }

  /**
   * The scan records erased return types, so the type arguments of each declared return type are read from the
   * syntax before any body is translated: invocations may precede the declaration they call.
   */
  void declareGenericReturns(CstNode node) {
    if ("method_declaration".equals(node.k()) && field(node, "dimensions") == null
      && sema.resolve(requiredField(node, "name", null).decl()) instanceof JMethodSymbol method) {
      Type declared = syntacticType(requiredField(node, "type", null));
      if (declared.isParameterized() && sameErasure(declared, method.returnType().type())) {
        method.returnTypeSymbol(declared.symbol());
      }
    }
    for (CstNode child : node.children()) {
      declareGenericReturns(child);
    }
  }

  /** The type a type node denotes, from the references the scan bound, without translating it. */
  Type syntacticType(CstNode node) {
    switch (node.k()) {
      case "type_identifier", "scoped_type_identifier" -> {
        String target = node.typeTarget();
        return target != null && sema.resolve(target) instanceof Symbol.TypeSymbol type && !type.isUnknown() ? type.type() : Type.UNKNOWN;
      }
      case "generic_type" -> {
        return genericSyntacticType(node);
      }
      case "wildcard" -> {
        return wildcardSyntacticType(node);
      }
      default -> {
        return Type.UNKNOWN;
      }
    }
  }

  private Type genericSyntacticType(CstNode node) {
    Type raw = syntacticType(node.children().get(0));
    if (raw.isUnknown()) {
      return raw;
    }
    List<JType> types = new ArrayList<>();
    for (CstNode argument : required(node, "type_arguments").children()) {
      if (isTypeKind(argument.k())) {
        types.add(syntacticType(argument) instanceof JType type ? type : JType.unknown(sema));
      }
    }
    return JType.named(sema, raw.erasure().fullyQualifiedName(), types);
  }

  private Type wildcardSyntacticType(CstNode node) {
    CstNode bound = firstType(node);
    if (bound == null) {
      return JType.wildcard(sema, (JType) sema.getClassType("java.lang.Object"));
    }
    JType type = syntacticType(bound) instanceof JType known ? known : JType.unknown(sema);
    return keyword(node, "super") != null ? JType.superWildcard(sema, type) : JType.wildcard(sema, type);
  }

  void bindType(ClassTreeImpl tree, CstNode node) {
    String id = node.decl();
    if (id != null && sema.resolve(id) instanceof Symbol.TypeSymbol typeSymbol) {
      tree.setSymbol(typeSymbol);
      sema.declare(id, tree);
    }
  }

  void bindUse(IdentifierTreeImpl identifier, CstNode node) {
    bindUse(identifier, node, false);
  }

  void bindUse(IdentifierTreeImpl identifier, CstNode node, boolean qualified) {
    String target = node.valueTarget();
    if (target == null) {
      target = node.typeTarget();
    }
    if (target == null) {
      return;
    }
    Symbol symbol = sema.resolve(target);
    String typeTarget = node.typeTarget();
    if (symbol.isMethodSymbol() && "<init>".equals(symbol.name()) && typeTarget != null && !typeTarget.equals(target)) {
      target = typeTarget;
      symbol = sema.resolve(target);
    }
    if (symbol.isMethodSymbol() && symbol.isUnknown()) {
      symbol = Symbol.MethodSymbol.UNKNOWN_METHOD;
    }
    if (!qualified && (symbol.isUnknown() || (symbol.isVariableSymbol() && !isField(symbol)))) {
      Symbol local = scopedVariable(identifier.name());
      if (local != null) {
        symbol = local;
      }
    }
    bind(identifier, symbol);
    ascribe(identifier, identifierType(symbol));
    sema.use(usageId(symbol, target), identifier);
  }

  @Nullable
  private Symbol scopedVariable(String name) {
    for (Map<String, Symbol> scope : state.scopes) {
      Symbol found = scope.get(name);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  void recordUse(Symbol symbol, IdentifierTree identifier) {
    String id = usageId(symbol, "");
    if (!id.isEmpty()) {
      sema.use(id, identifier);
    }
  }

  Parens parens(CstNode node) {
    InternalSyntaxToken open = token(required(node, "("));
    InternalSyntaxToken close = token(required(node, ")"));
    return new Parens(open, expressions().expression(node.children().get(1)), close);
  }

  InternalSyntaxToken token(CstNode node) {
    InternalSyntaxToken found = state.tokens.get(node.proto);
    if (found != null) {
      return found;
    }
    if (!node.children().isEmpty()) {
      for (CstNode child : node.children()) {
        if (child.children().isEmpty() && !child.text().isEmpty() && !isComment(child)) {
          return token(child);
        }
      }
    }
    String text = node.text().isEmpty() ? node.k() : node.text();
    if (text.indexOf('_') >= 0) {
      text = "";
    }
    return new InternalSyntaxToken(node.line(), node.col(), text, List.of(), false);
  }

  @Nullable
  InternalSyntaxToken optionalToken(CstNode node, String text) {
    CstNode found = keyword(node, text);
    return found == null ? null : token(found);
  }

  record Parens(InternalSyntaxToken open, ExpressionTree expr, InternalSyntaxToken close) {
  }
}
