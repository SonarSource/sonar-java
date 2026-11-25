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

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

final class JMethodSymbol extends JSymbol implements Symbol.MethodSymbol {

  private static final String DEFAULT = "default";
  private static final String NATIVE = "native";
  private static final String SYNCHRONIZED = "synchronized";
  private static final String CONSTRUCTOR_NAME = "<init>";

  private List<Type> parameterTypes = List.of();
  private TypeSymbol returnTypeSymbol = TypeSymbol.UNKNOWN_TYPE;
  private boolean varArgs;
  private boolean varArgsKnown;
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
    owner(Symbol.TypeSymbol.UNKNOWN_TYPE);
    this.signature = id == null ? name : SemaFacts.erase(id);
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

  /** The declaring answer says whether the method is variable arity, so a trailing array parameter proves nothing. */
  void varArgsKnown(boolean known) {
    this.varArgsKnown = known;
  }

  void lambda(boolean lambda) {
    this.lambda = lambda;
  }

  boolean isLambda() {
    return lambda;
  }

  boolean substitutes(List<Type> parameters, Type returned) {
    return !returned.equals(returnTypeSymbol.type()) || !parameters.equals(parameterTypes);
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

  /** Whether the declared signature is written with a type variable of this method itself, which makes it a generic method. */
  private boolean declaresTypeVariable() {
    if (id == null) {
      return false;
    }
    String prefix = id + "<";
    if (mentionsVariable(returnTypeSymbol.type(), prefix)) {
      return true;
    }
    return parameterTypes.stream().anyMatch(type -> mentionsVariable(type, prefix));
  }

  private static boolean mentionsVariable(Type type, String prefix) {
    if (type.isArray()) {
      return mentionsVariable(((Type.ArrayType) type).elementType(), prefix);
    }
    if (type instanceof JType jType && jType.typeVarId != null && jType.typeVarId.startsWith(prefix)) {
      return true;
    }
    if (type instanceof JType jType && jType.element != null && mentionsVariable(jType.element, prefix)) {
      return true;
    }
    return type.typeArguments().stream().anyMatch(argument -> mentionsVariable(argument, prefix));
  }

  /**
   * A per-invocation view with substituted parameter and return types. Equality stays on the SemSitter id,
   * so the view still denotes the same method as the declaration.
   */
  JMethodSymbol withSignature(List<Type> parameterTypes, Type returnType) {
    return withSignature(id, parameterTypes, returnType);
  }

  /** The view of the functional method a lambda implements, with an identity of its own: two lambdas are never the same symbol. */
  JMethodSymbol lambdaView(String lambdaId) {
    JMethodSymbol view = withSignature(lambdaId, parameterTypes, returnTypeSymbol.type());
    view.lambda = true;
    return view;
  }

  private JMethodSymbol withSignature(@Nullable String viewId, List<Type> parameterTypes, Type returnType) {
    JMethodSymbol view = new JMethodSymbol(sema, viewId, name());
    view.owner(owner());
    view.addModifiers(modifiersCopy());
    view.parameterTypes = List.copyOf(parameterTypes);
    view.parameterVariables = parameterVariables;
    view.declaredParameters = declaredParameters;
    view.declaredReturnLoaded = declaredReturnLoaded;
    view.declaredReturn = declaredReturn;
    view.returnTypeSymbol = returnType.symbol();
    view.varArgs = varArgs;
    view.varArgsKnown = varArgsKnown;
    view.lambda = lambda;
    view.parametrized = parametrized || declaresTypeVariable();
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
    if (hasModifier(DEFAULT)) {
      copy.add(DEFAULT);
    }
    if (hasModifier(NATIVE)) {
      copy.add(NATIVE);
    }
    if (hasModifier(SYNCHRONIZED)) {
      copy.add(SYNCHRONIZED);
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
      return method.parameters().stream().<Symbol>map(VariableTree::symbol).toList();
    }
    List<Symbol> placeholders = new ArrayList<>();
    for (int i = 0; i < parameterTypes.size(); i++) {
      placeholders.add(sema.factTypes().parameterPlaceholder(this, i, parameterTypes.get(i)));
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
      List<Type> served = sema.libraryMembers().projectThrown(id);
      return served == null ? Collections.emptyList() : served;
    }
    return method.throwsClauses().stream().map(TypeTree::symbolType).toList();
  }

  @Override
  public List<MethodSymbol> overriddenSymbols() {
    if (overridden == null) {
      overridden = new OverriddenMethods(this).find();
    }
    return overridden;
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
    if (CONSTRUCTOR_NAME.equals(name()) && ownerType.isEnum()) {
      out.append("Ljava/lang/String;I");
    } else if (CONSTRUCTOR_NAME.equals(name()) && !ownerType.isStatic() && ownerType.owner() instanceof TypeSymbol enclosing) {
      out.append(descriptor(enclosing.type()));
    }
    parameterTypes.forEach(parameter -> out.append(descriptor(parameter)));
    out.append(')');
    if (CONSTRUCTOR_NAME.equals(name())) {
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
    return !isUnknown() && !(isPrivate() || isStatic() || isFinal() || owner().isFinal());
  }

  @Override
  public boolean isParametrizedMethod() {
    return !isUnknown() && (parametrized || (declaration() instanceof MethodTree method && !method.typeParameters().isEmpty()));
  }

  @Override
  public boolean isDefaultMethod() {
    return !isUnknown() && hasModifier(DEFAULT);
  }

  @Override
  public boolean isSynchronizedMethod() {
    return !isUnknown() && hasModifier(SYNCHRONIZED);
  }

  @Override
  public boolean isVarArgsMethod() {
    if (isUnknown()) {
      return false;
    }
    if (varArgs) {
      return true;
    }
    if (varArgsKnown) {
      return false;
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

  @Nullable
  @Override
  public Object annotationElementDefaultValue() {
    MethodTree method = declaration();
    if (method == null) {
      return sema.annotationFacts().annotationDefault(this);
    }
    if (method.defaultValue() == null) {
      return null;
    }
    Object value = TreeAnnotation.literal(sema, method.defaultValue());
    return value instanceof Object[] || !returnType().type().isArray() ? value : new Object[] {value};
  }

  @Override
  public boolean isNativeMethod() {
    return !isUnknown() && hasModifier(NATIVE);
  }
}
