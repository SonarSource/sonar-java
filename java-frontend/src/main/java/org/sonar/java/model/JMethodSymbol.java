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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

final class JMethodSymbol extends JSymbol implements Symbol.MethodSymbol {

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
    if (mentionsVariable(returnTypeSymbol == null ? null : returnTypeSymbol.type(), prefix)) {
      return true;
    }
    return parameterTypes.stream().anyMatch(type -> mentionsVariable(type, prefix));
  }

  private static boolean mentionsVariable(@Nullable Type type, String prefix) {
    if (type == null) {
      return false;
    }
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
      placeholders.add(sema.parameterPlaceholder(this, i, parameterTypes.get(i)));
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
      List<Type> served = sema.projectThrown(id);
      return served == null ? Collections.emptyList() : served;
    }
    return method.throwsClauses().stream().map(TypeTree::symbolType).toList();
  }

  @Override
  public List<MethodSymbol> overriddenSymbols() {
    if (overridden == null) {
      overridden = findOverridden();
    }
    return overridden;
  }

  private List<MethodSymbol> findOverridden() {
    if (!(owner() instanceof JTypeSymbol typeSymbol) || isUnknown() || isStatic() || isPrivate()) {
      return List.of();
    }
    List<MethodSymbol> found = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    if (typeSymbol.isInterface()) {
      match(found, sema.typeSymbol("java.lang.Object"), null);
      visited.add("java.lang.Object");
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
      match(found, parent, type);
      walk(found, visited, parent.superClass());
      for (Type iface : parent.interfaces()) {
        walk(found, visited, iface);
      }
    }
  }

  private void match(List<MethodSymbol> found, TypeSymbol parent, @Nullable Type declaring) {
    for (Symbol member : parent instanceof JTypeSymbol type ? type.membersNamed(name()) : parent.memberSymbols()) {
      if (member.isMethodSymbol() && !member.isStatic() && !member.isPrivate() && name().equals(member.name()) && sameSignature((MethodSymbol) member, declaring)) {
        found.add(withWrittenTypeArgument((MethodSymbol) member, declaring));
        return;
      }
    }
  }

  /**
   * A method returning a class type variable, seen through a supertype written with an annotated type argument
   * ({@code Converter<String, @Nullable Boolean>}), carries that annotation as the nullability of its result.
   */
  private MethodSymbol withWrittenTypeArgument(MethodSymbol overridden, @Nullable Type declaring) {
    if (declaring == null || !(overridden instanceof JMethodSymbol library)) {
      return overridden;
    }
    Integer position = returnedClassVariable(library, declaring);
    SymbolMetadata metadata = position == null ? null : writtenArgumentMetadata(declaring, position);
    if (metadata == null) {
      return overridden;
    }
    return (MethodSymbol) Proxy.newProxyInstance(MethodSymbol.class.getClassLoader(), new Class<?>[] {MethodSymbol.class}, (proxy, method, arguments) -> {
      if ("metadata".equals(method.getName()) && method.getParameterCount() == 0) {
        return metadata;
      }
      if ("equals".equals(method.getName()) && method.getParameterCount() == 1) {
        return proxy == arguments[0] || library.equals(arguments[0]);
      }
      try {
        return method.invoke(library, arguments);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      }
    });
  }

  /** The annotations of the type argument at {@code position} of {@code declaring}: as written in this class, or as a supertype states it. */
  @Nullable
  private SymbolMetadata writtenArgumentMetadata(Type declaring, int position) {
    if (owner() instanceof JTypeSymbol typeSymbol && typeSymbol.declaration() instanceof ClassTree classTree) {
      List<TypeTree> supertypes = new ArrayList<>(classTree.superInterfaces());
      if (classTree.superClass() != null) {
        supertypes.add(classTree.superClass());
      }
      for (TypeTree supertype : supertypes) {
        if (supertype instanceof ParameterizedTypeTree parameterized && parameterized.symbolType().erasure().fullyQualifiedName().equals(declaring.erasure().fullyQualifiedName())
          && position < parameterized.typeArguments().size()
          && parameterized.typeArguments().get(position) instanceof TypeTree argument && JSymbolMetadata.isAnnotatedType(argument)) {
          return JSymbolMetadata.ofTypeTree(sema, argument);
        }
      }
    }
    if (owner() instanceof JTypeSymbol typeSymbol) {
      List<JSema.DeclaredAnnotation> declared = sema.supertypeArgumentAnnotations(typeSymbol.type(), declaring, position);
      if (!declared.isEmpty()) {
        return JSymbolMetadata.ofDeclared(sema, declared);
      }
    }
    return null;
  }

  /** The position, among the type variables of its class, of the one the method returns. */
  @Nullable
  private Integer returnedClassVariable(JMethodSymbol method, Type declaring) {
    if (method.returnTypeParameter() != null) {
      return method.returnTypeParameter();
    }
    if (method.declaration() == null) {
      return sema.returnedClassVariable(method, declaring);
    }
    if (method.declaration() instanceof MethodTree tree && tree.returnType() instanceof IdentifierTree returned
      && method.owner() instanceof JTypeSymbol parent && parent.declaration() instanceof ClassTree parentClass) {
      for (int i = 0; i < parentClass.typeParameters().size(); i++) {
        if (parentClass.typeParameters().get(i).identifier().name().equals(returned.name())) {
          return i;
        }
      }
    }
    return null;
  }

  private boolean viaClassVariable(JMethodSymbol library, int index, Type declaring, String mine) {
    Type argument = sema.classVariableArgument(library, index, declaring);
    return argument != null && mine.equals(argument.erasure().fullyQualifiedName());
  }

  private boolean sameSignature(MethodSymbol other, @Nullable Type declaring) {
    List<Type> theirs = other.parameterTypes();
    if (parameterTypes.size() != theirs.size()) {
      return false;
    }
    for (int i = 0; i < parameterTypes.size(); i++) {
      String mine = parameterTypes.get(i).erasure().fullyQualifiedName();
      boolean same = mine.equals(theirs.get(i).erasure().fullyQualifiedName())
        || (declaring != null && mine.equals(sema.asMemberOf(theirs.get(i), declaring).erasure().fullyQualifiedName()))
        || (declaring != null && other instanceof JMethodSymbol library && viaClassVariable(library, i, declaring, mine));
      if (!same) {
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
    return !isUnknown() && (parametrized || (declaration() != null && !declaration().typeParameters().isEmpty()));
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

  @Override
  public boolean isNativeMethod() {
    return !isUnknown() && hasModifier("native");
  }
}
