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
package org.sonar.java.model;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

final class JMethodSymbol extends JSymbol implements Symbol.MethodSymbol {

  private List<Type> parameterTypes = List.of();
  private TypeSymbol returnTypeSymbol = TypeSymbol.UNKNOWN_TYPE;
  private boolean varArgs;
  private boolean lambda;
  private boolean parametrized;
  private final String signature;
  @Nullable
  private Integer returnTypeParameter;
  private boolean returnsSelf;
  @Nullable
  private List<String> parameterVariables;
  @Nullable
  private List<JsonObject> declaredParameters;
  private boolean declaredReturnLoaded;
  @Nullable
  private JsonObject declaredReturn;
  private List<MethodSymbol> overridden;
  @Nullable
  private List<Type> thrownTypes;
  @Nullable
  private String descriptorSignature;

  JMethodSymbol(JSema sema, @Nullable String id, String name) {
    super(sema, Kind.METHOD, id, name);
    this.signature = id == null ? name : JSema.erase(id);
  }

  void parameterTypes(List<Type> parameterTypes) {
    this.parameterTypes = List.copyOf(parameterTypes);
  }

  void returnTypeSymbol(TypeSymbol returnTypeSymbol) {
    this.returnTypeSymbol = returnTypeSymbol == null ? TypeSymbol.UNKNOWN_TYPE : returnTypeSymbol;
  }

  void varArgs(boolean varArgs) {
    this.varArgs = varArgs;
  }

  void lambda(boolean lambda) {
    this.lambda = lambda;
  }

  boolean isLambda() {
    return lambda;
  }

  void parametrized(boolean parametrized) {
    this.parametrized = parametrized;
  }

  void returnTypeParameter(@Nullable Integer returnTypeParameter) {
    this.returnTypeParameter = returnTypeParameter;
  }

  @Nullable
  Integer returnTypeParameter() {
    return returnTypeParameter;
  }

  void returnsSelf(boolean returnsSelf) {
    this.returnsSelf = returnsSelf;
  }

  boolean returnsSelf() {
    return returnsSelf;
  }

  void parameterVariables(List<String> parameterVariables) {
    this.parameterVariables = parameterVariables;
  }

  @Nullable
  List<String> parameterVariables() {
    return parameterVariables;
  }

  void declaredParameters(List<JsonObject> declaredParameters) {
    this.declaredParameters = declaredParameters;
  }

  @Nullable
  List<JsonObject> declaredParameters() {
    return declaredParameters;
  }

  void declaredReturn(@Nullable JsonObject declaredReturn) {
    this.declaredReturnLoaded = true;
    this.declaredReturn = declaredReturn;
  }

  boolean declaredReturnLoaded() {
    return declaredReturnLoaded;
  }

  @Nullable
  JsonObject declaredReturn() {
    return declaredReturn;
  }

  /**
   * A per-invocation view with substituted parameter and return types. Equality stays on the SemSitter id,
   * so the view still denotes the same method as the declaration.
   */
  JMethodSymbol withSignature(List<Type> parameterTypes, Type returnType) {
    JMethodSymbol view = new JMethodSymbol(sema, id, name());
    view.owner(owner());
    view.addModifiers(modifiersCopy());
    view.parameterTypes = List.copyOf(parameterTypes);
    view.parameterVariables = parameterVariables;
    view.declaredParameters = declaredParameters;
    view.declaredReturnLoaded = declaredReturnLoaded;
    view.declaredReturn = declaredReturn;
    view.returnTypeSymbol = returnType.symbol();
    view.varArgs = varArgs;
    view.lambda = lambda;
    view.parametrized = parametrized || !returnType.equals(returnTypeSymbol.type()) || !view.parameterTypes.equals(this.parameterTypes);
    view.returnTypeParameter = returnTypeParameter;
    view.returnsSelf = returnsSelf;
    view.thrownTypes = thrownTypes;
    view.unknown(isUnknown());
    view.deprecated(isDeprecated());
    view.descriptorSignature = signature();
    return view;
  }

  private List<String> modifiersCopy() {
    List<String> copy = new ArrayList<>();
    if (isPublic()) {
      copy.add("public");
    }
    if (isPrivate()) {
      copy.add("private");
    }
    if (isProtected()) {
      copy.add("protected");
    }
    if (isStatic()) {
      copy.add("static");
    }
    if (isFinal()) {
      copy.add("final");
    }
    if (hasModifier("abstract")) {
      copy.add("abstract");
    }
    if (hasModifier("default")) {
      copy.add("default");
    }
    if (hasModifier("native")) {
      copy.add("native");
    }
    if (hasModifier("synchronized")) {
      copy.add("synchronized");
    }
    return copy;
  }

  @Override
  public List<Type> parameterTypes() {
    return parameterTypes;
  }

  @Override
  public List<Symbol> declarationParameters() {
    if (declaration() instanceof MethodTree method && method.closeParenToken() != null) {
      return method.parameters().stream().map(parameter -> (Symbol) parameter.symbol()).toList();
    }
    List<Symbol> placeholders = new ArrayList<>();
    for (int i = 0; i < parameterTypes.size(); i++) {
      placeholders.add(new JVariableSymbol.ParameterPlaceholderSymbol(i, this, parameterTypes.get(i)));
    }
    return placeholders;
  }

  @Override
  public TypeSymbol returnType() {
    return returnTypeSymbol;
  }

  void thrownTypes(List<Type> thrownTypes) {
    this.thrownTypes = List.copyOf(thrownTypes);
  }

  @Override
  public List<Type> thrownTypes() {
    if (thrownTypes != null) {
      return thrownTypes;
    }
    MethodTree method = declaration();
    if (method == null) {
      return Collections.emptyList();
    }
    return method.throwsClauses().stream().map(TypeTree::symbolType).toList();
  }

  @Override
  public List<MethodSymbol> overriddenSymbols() {
    if (overridden == null) {
      overridden = findOverridden();
      if (overridden.stream().anyMatch(Symbol::isDeprecated)) {
        deprecated(true);
      }
    }
    return overridden;
  }

  private List<MethodSymbol> findOverridden() {
    if (!(owner() instanceof JTypeSymbol typeSymbol) || isUnknown() || isStatic() || isPrivate() || "<init>".equals(name())) {
      return List.of();
    }
    List<MethodSymbol> found = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    if (typeSymbol.isInterface()) {
      match(found, sema.typeSymbol("java.lang.Object"));
    }
    walk(found, visited, typeSymbol.superClass());
    for (Type iface : typeSymbol.interfaces()) {
      walk(found, visited, iface);
    }
    return found;
  }

  private void walk(List<MethodSymbol> found, Set<String> visited, @Nullable Type type) {
    if (type == null || type.isUnknown() || !visited.add(type.fullyQualifiedName())) {
      return;
    }
    if (type.symbol() instanceof JTypeSymbol parent) {
      match(found, parent);
      walk(found, visited, parent.superClass());
      for (Type iface : parent.interfaces()) {
        walk(found, visited, iface);
      }
    }
  }

  private void match(List<MethodSymbol> found, TypeSymbol parent) {
    for (Symbol member : parent.memberSymbols()) {
      if (member.isMethodSymbol() && name().equals(member.name()) && sameSignature((MethodSymbol) member)) {
        found.add((MethodSymbol) member);
        return;
      }
    }
  }

  private boolean sameSignature(MethodSymbol other) {
    List<Type> theirs = other.parameterTypes();
    if (parameterTypes.size() != theirs.size()) {
      return false;
    }
    for (int i = 0; i < parameterTypes.size(); i++) {
      if (!parameterTypes.get(i).erasure().is(theirs.get(i).erasure().fullyQualifiedName())) {
        return false;
      }
    }
    return true;
  }

  /**
   * The ECJ form, {@code binaryOwner#name(descriptors)returnDescriptor}, from the erased parameter and return
   * types. Falls back to the SemSitter id when the owner is not a type.
   */
  @Override
  public String signature() {
    if (descriptorSignature == null) {
      descriptorSignature = descriptorSignature();
    }
    return descriptorSignature;
  }

  private String descriptorSignature() {
    Symbol owner = owner();
    if (!(owner instanceof TypeSymbol ownerType) || isUnknown()) {
      return signature;
    }
    StringBuilder out = new StringBuilder(ownerType.type().fullyQualifiedName()).append('#').append(name()).append('(');
    parameterTypes.forEach(parameter -> out.append(descriptor(parameter)));
    out.append(')');
    if ("<init>".equals(name())) {
      out.append('V');
    } else {
      out.append(descriptor(returnTypeSymbol.type()));
    }
    return out.toString();
  }

  private static String descriptor(@Nullable Type type) {
    if (type == null || type.isUnknown()) {
      return "Ljava/lang/Object;";
    }
    Type erased = type.erasure();
    if (erased.isArray()) {
      return "[" + descriptor(((Type.ArrayType) erased).elementType());
    }
    return switch (erased.fullyQualifiedName()) {
      case "byte" -> "B";
      case "char" -> "C";
      case "double" -> "D";
      case "float" -> "F";
      case "int" -> "I";
      case "long" -> "J";
      case "short" -> "S";
      case "boolean" -> "Z";
      case "void" -> "V";
      default -> "L" + erased.fullyQualifiedName().replace('.', '/') + ";";
    };
  }

  @Override
  public boolean isOverridable() {
    Symbol owner = owner();
    return !isUnknown() && !(isPrivate() || isStatic() || isFinal() || (owner != null && owner.isFinal()));
  }

  @Override
  public boolean isParametrizedMethod() {
    return !isUnknown() && parametrized;
  }

  @Override
  public boolean isDefaultMethod() {
    return !isUnknown() && hasModifier("default");
  }

  @Override
  public boolean isSynchronizedMethod() {
    return !isUnknown() && hasModifier("synchronized");
  }

  @Override
  public boolean isVarArgsMethod() {
    if (isUnknown()) {
      return false;
    }
    if (varArgs) {
      return true;
    }
    if (declaration() instanceof MethodTree method) {
      List<VariableTree> parameters = method.parameters();
      if (parameters.isEmpty()) {
        return false;
      }
      return parameters.get(parameters.size() - 1).type() instanceof ArrayTypeTree arrayType
        && arrayType.ellipsisToken() != null;
    }
    return !parameterTypes.isEmpty() && parameterTypes.get(parameterTypes.size() - 1).isArray();
  }

  @Nullable
  @Override
  public MethodTree declaration() {
    return (MethodTree) super.declaration();
  }

  @Override
  public boolean isNativeMethod() {
    return !isUnknown() && hasModifier("native");
  }
}
