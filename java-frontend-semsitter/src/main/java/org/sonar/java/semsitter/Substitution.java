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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import static org.sonar.java.semsitter.SemaFacts.strings;
import static org.sonar.java.semsitter.SemaFacts.text;

/** Type variable substitution: members seen from a parameterized type, supertypes and explicit type arguments. */
final class Substitution {

  private static final String TYPE_VARS = "typeVars";
  private static final String PARAM_VARS = "paramVars";

  private final JSema sema;

  Substitution(JSema sema) {
    this.sema = sema;
  }

  static boolean mentionsTypeVariable(Type type) {
    return mentionsTypeVariable(type, name -> true);
  }

  static boolean mentionsTypeVariable(Type type, Predicate<String> variable) {
    if (type.isTypeVar()) {
      return variable.test(type.name());
    }
    if (type.isArray()) {
      return mentionsTypeVariable(((Type.ArrayType) type).elementType(), variable);
    }
    return type.isParameterized() && type.typeArguments().stream().anyMatch(argument -> mentionsTypeVariable(argument, variable));
  }

  /** {@code declared}, written in terms of the type variables of {@code declaring}'s class, as it reads for that parameterization. */
  Type asMemberOf(Type declared, Type declaring) {
    if (!(declared instanceof JType type) || !(declaring instanceof JType owner) || !owner.isParameterized()) {
      return declared;
    }
    return type.substitute(typeArgumentsOf(owner));
  }

  /** The type of {@code field} read through {@code receiver}: the owner's type variables take the receiver's type arguments. */
  Type fieldTypeOn(Symbol.VariableSymbol field, @Nullable Type receiver) {
    Type declared = field.type();
    if (!(declared instanceof JType type) || receiver == null || field.isStatic() || !(field.owner() instanceof Symbol.TypeSymbol owner) || owner.type() == null) {
      return declared;
    }
    String ownerFqn = owner.type().erasure().fullyQualifiedName();
    JsonObject info = sema.typeInfos.get(sema.sourceName(ownerFqn));
    List<String> names = info == null ? List.of() : strings(info, TYPE_VARS);
    Type declaring = names.isEmpty() ? null : asSuperType(receiver, ownerFqn, new HashSet<>());
    if (declaring == null || declaring.typeArguments().isEmpty()) {
      return declared;
    }
    Map<String, Type> substitution = new HashMap<>();
    for (int i = 0; i < names.size() && i < declaring.typeArguments().size(); i++) {
      substitution.put(names.get(i), declaring.typeArguments().get(i));
    }
    return type.substitute(substitution);
  }

  /** The supertype of {@code receiver} that declares {@code method}, with the receiver's type arguments applied. */
  @Nullable
  Type declaringType(JMethodSymbol method, Type receiver) {
    Type owner = method.owner().type();
    if (owner == null || owner.isUnknown()) {
      return receiver;
    }
    Type found = asSuperType(receiver, owner.erasure().fullyQualifiedName(), new HashSet<>());
    return found == null ? receiver : found;
  }

  /** The method as seen from {@code receiver}, so its owner keeps that parameterization. */
  JMethodSymbol invocationView(JMethodSymbol method, @Nullable Type receiver, List<Type> parameters, Type returned) {
    return invocationView(method, receiver, parameters, returned, null);
  }

  /** As above, with the thrown types the call infers when the method throws a type variable. */
  JMethodSymbol invocationView(JMethodSymbol method, @Nullable Type receiver, List<Type> parameters, Type returned, @Nullable List<Type> thrown) {
    boolean substituted = method.substitutes(parameters, returned) || thrown != null;
    JMethodSymbol view = method.withSignature(parameters, returned);
    if (thrown != null) {
      view.thrownTypes(thrown);
    }
    if (!view.isParametrizedMethod() && sema.inference().declaresTypeVariables(method, substituted)) {
      view.parametrized(true);
    }
    if (receiver != null && !method.isStatic() && method.owner().type() != null) {
      Type declaring = asSuperType(sema.inference().captureWildcards(receiver), method.owner().type().erasure().fullyQualifiedName(), new HashSet<>());
      if (declaring != null && declaring.isParameterized()) {
        view.owner(declaring.symbol());
      }
    }
    boolean same = !substituted && view.owner() == method.owner() && view.isParametrizedMethod() == method.isParametrizedMethod();
    return same ? method : view;
  }

  /**
   * The parameter types of {@code method} at one invocation: type variables of the owner are replaced by
   * the receiver's type arguments, and type variables of the method by the boxed type of the first argument
   * they are inferred from.
   */
  List<Type> invocationParameters(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    List<Type> parameters = method.parameterTypes();
    List<String> variables = parameterVariables(method);
    if (variables.size() != parameters.size()) {
      return parameters;
    }
    List<Type> result = new ArrayList<>(parameters);
    for (int i = 0; i < variables.size(); i++) {
      String variable = variables.get(i);
      if (variable.isEmpty()) {
        continue;
      }
      String base = variable.replace("[]", "");
      Type bound = base.charAt(0) == 'c'
        ? classArgument(method, receiver, Integer.parseInt(base.substring(1)))
        : methodArgument(variables, base, arguments);
      if (bound != null) {
        JType type = sema.asJType(bound);
        for (int dims = (variable.length() - base.length()) / 2; dims > 0; dims--) {
          type = JType.array(sema, type);
        }
        result.set(i, type);
      }
    }
    substituteDeclaredParameters(method, receiver, variables, result);
    return result;
  }

  /**
   * Fills in the parameters that are not a type variable on their own but are written in terms of one,
   * such as {@code Consumer<T>}. Their declared form is substituted for the arguments the receiver gives
   * the declaring type, which may be a supertype of the receiver's own type. Without such arguments, a declared
   * parameterized type that names no type variable is taken as written.
   */
  private void substituteDeclaredParameters(JMethodSymbol method, @Nullable Type receiver, List<String> variables, List<Type> result) {
    List<JsonObject> declared = declaredParameters(method);
    if (declared.size() != result.size()) {
      return;
    }
    Map<String, Type> substitution = receiver == null ? Map.of() : classSubstitution(method, receiver);
    for (int i = 0; i < declared.size(); i++) {
      if (variables.isEmpty() || variables.get(i).isEmpty()) {
        JType type = sema.factTypes().typeFromFact(declared.get(i), substitution);
        if (!substitution.isEmpty() || (type.isParameterized() && !mentionsTypeVariable(type))) {
          result.set(i, type);
        }
      }
    }
  }

  /** Maps the type variables of the type declaring {@code method} to the arguments {@code receiver} gives them. */
  Map<String, Type> classSubstitution(JMethodSymbol method, Type receiver) {
    String ownerFqn = method.owner().type().erasure().fullyQualifiedName();
    JsonObject info = sema.typeInfos.get(sema.sourceName(ownerFqn));
    List<String> names = info == null ? List.of() : strings(info, TYPE_VARS);
    Map<String, Type> substitution = new HashMap<>();
    if (receiver instanceof JType inner && inner.outer() != null && inner.erasure().fullyQualifiedName().equals(ownerFqn)) {
      substitution.putAll(enclosingSubstitution(inner.outer()));
    }
    Type declaring = names.isEmpty() ? null : asSuperType(receiver, ownerFqn, new HashSet<>());
    if (declaring != null && declaring.isParameterized()) {
      List<Type> arguments = declaring.typeArguments();
      for (int i = 0; i < names.size() && i < arguments.size(); i++) {
        substitution.put(names.get(i), arguments.get(i));
      }
    }
    return substitution;
  }

  private Map<String, Type> enclosingSubstitution(JType enclosing) {
    JsonObject info = sema.typeInfos.get(sema.sourceName(enclosing.erasure().fullyQualifiedName()));
    List<String> names = info == null ? List.of() : strings(info, TYPE_VARS);
    Map<String, Type> substitution = new HashMap<>();
    for (int i = 0; i < names.size() && i < enclosing.args.size(); i++) {
      substitution.put(names.get(i), enclosing.args.get(i));
    }
    return substitution;
  }

  /** Whether {@code from}, read as {@code to}'s class, gives it the type arguments {@code to} names; a wildcard argument takes any. */
  boolean preservesArguments(Type from, Type to) {
    if (!to.isParameterized()) {
      return true;
    }
    Type aligned = asSuperType(from, to.erasure().fullyQualifiedName(), new HashSet<>());
    if (aligned == null || !aligned.isParameterized() || aligned.typeArguments().size() != to.typeArguments().size()) {
      return false;
    }
    for (int i = 0; i < to.typeArguments().size(); i++) {
      Type written = to.typeArguments().get(i);
      if (!(written instanceof JType type && type.isWildcard()) && !written.equals(aligned.typeArguments().get(i))) {
        return false;
      }
    }
    return true;
  }

  /** The instantiation of {@code targetFqn} reached from {@code from}, which is itself or one of its supertypes. */
  @Nullable
  Type asSuperType(Type from, String targetFqn, Set<String> visited) {
    if (from.erasure().fullyQualifiedName().equals(targetFqn)) {
      return from;
    }
    Symbol.TypeSymbol symbol = from.symbol();
    if (symbol.isUnknown() || !visited.add(from.erasure().fullyQualifiedName())) {
      return null;
    }
    List<Type> supertypes = new ArrayList<>(symbol.interfaces());
    Type superClass = symbol.superClass();
    if (superClass != null) {
      supertypes.add(0, superClass);
    }
    for (Type supertype : supertypes) {
      Type found = asSuperType(instantiateSupertype(from, supertype), targetFqn, visited);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  /** Applies {@code from}'s type arguments to a supertype written with those type variables. */
  Type instantiateSupertype(Type from, Type supertype) {
    if (!from.isParameterized() || !(from instanceof JType fromType) || !(supertype instanceof JType superType)) {
      return supertype;
    }
    return superType.substitute(typeArgumentsOf(fromType));
  }

  private Map<String, Type> typeArgumentsOf(JType type) {
    JsonObject info = sema.typeInfos.get(sema.sourceName(type.erasure().fullyQualifiedName()));
    List<String> names = info == null ? List.of() : strings(info, TYPE_VARS);
    List<Type> arguments = type.typeArguments();
    Map<String, Type> substitution = new HashMap<>();
    for (int i = 0; i < names.size() && i < arguments.size(); i++) {
      substitution.put(names.get(i), arguments.get(i));
    }
    return substitution;
  }

  @Nullable
  JsonObject declaredReturn(JMethodSymbol method) {
    if (method.declaredReturnLoaded()) {
      return method.declaredReturn();
    }
    JsonObject member = lookupGenericMember(method);
    JsonObject retType = member != null && member.get("retType") instanceof JsonObject fact ? fact : null;
    method.declaredReturn(retType);
    return retType;
  }

  List<JsonObject> declaredParameters(JMethodSymbol method) {
    List<JsonObject> cached = method.declaredParameters();
    if (cached == null) {
      JsonObject member = lookupGenericMember(method);
      cached = member == null ? List.of() : FactTypes.objects(member, "paramTypes");
      method.declaredParameters(cached);
    }
    return cached;
  }

  @Nullable
  private static Type classArgument(JMethodSymbol method, @Nullable Type receiver, int index) {
    if (receiver == null || !receiver.isParameterized() || receiver.typeArguments().size() <= index
      || !receiver.erasure().equals(method.owner().type().erasure())) {
      return null;
    }
    return receiver.typeArguments().get(index);
  }

  /** Runs {@code action} with the type arguments an invocation spells out, which fix the type variables of its method. */
  <T> T withExplicitTypeArguments(List<Type> explicit, java.util.function.Supplier<T> action) {
    List<Type> previous = sema.explicitTypeArguments;
    sema.explicitTypeArguments = explicit;
    try {
      return action.get();
    } finally {
      sema.explicitTypeArguments = previous;
    }
  }

  @Nullable
  Type methodArgument(List<String> variables, String base, List<Type> arguments) {
    Type explicit = explicitMethodArgument(base);
    if (explicit != null) {
      return explicit;
    }
    for (int i = 0; i < variables.size() && i < arguments.size(); i++) {
      Type argument = candidateArgument(variables, base, arguments, i);
      if (argument != null && !argument.isUnknown() && !argument.isNullType()) {
        return argument.isPrimitive() ? argument.primitiveWrapperType() : argument;
      }
    }
    return null;
  }

  @Nullable
  private Type explicitMethodArgument(String base) {
    if (base.charAt(0) != 'm' || sema.explicitTypeArguments.isEmpty()) {
      return null;
    }
    try {
      int index = Integer.parseInt(base.substring(1));
      if (index < sema.explicitTypeArguments.size() && !sema.explicitTypeArguments.get(index).isUnknown()) {
        return sema.explicitTypeArguments.get(index);
      }
    } catch (NumberFormatException e) {
      // not a method type variable
    }
    return null;
  }

  /** The argument at {@code index} when it can bind the variable {@code base} there, or null; an array of a primitive type is one element of the variable arity. */
  @Nullable
  private static Type candidateArgument(List<String> variables, String base, List<Type> arguments, int index) {
    String variable = variables.get(index);
    if (!variable.replace("[]", "").equals(base)) {
      return null;
    }
    Type argument = arguments.get(index);
    if (variable.equals(base)) {
      return argument;
    }
    if (arguments.size() == variables.size() && argument.isArray() && !((Type.ArrayType) argument).elementType().isPrimitive()) {
      return ((Type.ArrayType) argument).elementType();
    }
    return index == variables.size() - 1 ? argument : null;
  }

  /** The type {@code declaring} gives the class type variable that the parameter {@code index} of the library {@code method} is declared as. */
  @Nullable
  Type classVariableArgument(JMethodSymbol method, int index, Type declaring) {
    List<JsonObject> parameters = declaredParameters(method);
    if (index >= parameters.size() || !"typevar".equals(text(parameters.get(index), "kind")) || !declaring.isParameterized()) {
      return null;
    }
    JsonObject info = sema.typeInfos.get(sema.sourceName(declaring.erasure().fullyQualifiedName()));
    int position = info == null ? -1 : strings(info, TYPE_VARS).indexOf(text(parameters.get(index), "id"));
    return position < 0 || position >= declaring.typeArguments().size() ? null : declaring.typeArguments().get(position);
  }

  /** The position, among the type variables of {@code declaring}, of the one the library {@code method} returns. */
  @Nullable
  Integer returnedClassVariable(JMethodSymbol method, Type declaring) {
    JsonObject returned = declaredReturn(method);
    if (returned == null || !"typevar".equals(text(returned, "kind"))) {
      return null;
    }
    JsonObject info = sema.typeInfos.get(sema.sourceName(declaring.erasure().fullyQualifiedName()));
    int position = info == null ? -1 : strings(info, TYPE_VARS).indexOf(text(returned, "id"));
    return position < 0 ? null : position;
  }

  static boolean isArgumentOf(JsonObject entry, int position) {
    return entry.get("path") instanceof JsonArray path && path.size() == 1 && path.get(0).isJsonPrimitive() && ("<" + position + ">").equals(path.get(0).getAsString());
  }

  List<String> parameterVariables(JMethodSymbol method) {
    List<String> cached = method.parameterVariables();
    if (cached == null) {
      cached = lookupParameterVariables(method);
      method.parameterVariables(cached);
    }
    return cached;
  }

  private List<String> lookupParameterVariables(JMethodSymbol method) {
    JsonObject member = lookupGenericMember(method);
    if (member == null) {
      return List.of();
    }
    List<String> variables = new ArrayList<>();
    member.getAsJsonArray(PARAM_VARS).forEach(v -> variables.add(v.getAsString()));
    return variables;
  }

  /** The loaded declaration of {@code method}, when its declaring type is known and carries a generic signature. */
  @Nullable
  JsonObject lookupGenericMember(JMethodSymbol method) {
    String ownerFqn = method.owner().type().erasure().fullyQualifiedName();
    if (!sema.declaresType(ownerFqn)) {
      return null;
    }
    JsonArray members = sema.typeInfos.get(sema.sourceName(ownerFqn)).getAsJsonArray(JSema.MEMBERS);
    if (members == null) {
      return null;
    }
    List<Type> parameters = method.parameterTypes();
    for (JsonElement element : members) {
      JsonObject member = element.getAsJsonObject();
      String descriptor = text(member, "desc");
      if (method.name().equals(text(member, "name")) && descriptor != null && member.has(PARAM_VARS) && member.get(PARAM_VARS).isJsonArray()
        && sameParameters(parameters, sema.factTypes().parameterTypesOf(descriptor))) {
        return member;
      }
    }
    return null;
  }

  /** Whether the parameters match the descriptor's erasures; a type variable, which the symbol cannot name, stands for what the descriptor erases it to. */
  private static boolean sameParameters(List<Type> parameters, List<Type> descriptor) {
    if (parameters.size() != descriptor.size()) {
      return false;
    }
    for (int i = 0; i < parameters.size(); i++) {
      Type parameter = parameters.get(i);
      String name = parameter.erasure().fullyQualifiedName();
      boolean variable = parameter.isUnknown() || parameter.isTypeVar() || name.contains("::");
      if (!variable && !name.equals(descriptor.get(i).erasure().fullyQualifiedName())) {
        return false;
      }
    }
    return true;
  }
}
