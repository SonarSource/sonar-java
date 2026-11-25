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
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Type;

import static org.sonar.java.semsitter.SemaFacts.strings;
import static org.sonar.java.semsitter.SemaFacts.text;

/** Return and thrown types of invocations and diamonds, inferred from the arguments and the target type. */
final class Inference {

  private static final String TYPE_VARS = "typeVars";

  private final JSema sema;

  Inference(JSema sema) {
    this.sema = sema;
  }

  /**
   * The type an invocation of {@code method} evaluates to. A self-typed method returns the receiver, or a
   * capture of it when the receiver leaves its own type argument as a wildcard.
   */
  Type invocationReturn(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    if (receiver != null && receiver.isArray() && "clone".equals(method.name())) {
      return receiver;
    }
    Type result = invocationReturnType(method, receiver, arguments);
    return withEnclosingArguments(method, receiver, result);
  }

  /** The inner class a member of a parameterized class returns keeps the arguments of its enclosing class; a static member type has none. */
  private Type withEnclosingArguments(JMethodSymbol method, @Nullable Type receiver, Type result) {
    if (receiver == null || method.isStatic() || !(result instanceof JType inner) || !inner.isClass() || inner.isParameterized() || method.owner().type() == null) {
      return result;
    }
    String ownerFqn = method.owner().type().erasure().fullyQualifiedName();
    if ((!inner.fullyQualifiedName().startsWith(ownerFqn + "$") && !inner.fullyQualifiedName().startsWith(ownerFqn + "."))
      || inner.symbol().isStatic() || inner.symbol().isInterface() || inner.symbol().isEnum()) {
      return result;
    }
    Type declaring = sema.substitution().asSuperType(receiver, ownerFqn, new HashSet<>());
    return declaring instanceof JType enclosing && enclosing.isParameterized() ? inner.withOuter(enclosing) : result;
  }

  private Type invocationReturnType(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    Type fromReceiver = receiverReturn(method, receiver, arguments);
    return fromReceiver != null ? fromReceiver : declaredReturnType(method, receiver, arguments);
  }

  @Nullable
  private Type receiverReturn(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    if (method.returnsSelf() && receiver instanceof JType returned && returned.isCapture()) {
      return returned;
    }
    if (receiver instanceof JType captured && captured.isCapture() && captured.element.isClass() && !method.isStatic()) {
      // a member of a capture is a member of its bound
      return invocationReturnType(method, captured.element, arguments);
    }
    Type argument = returnedTypeArgument(method, receiver);
    if (argument != null) {
      if (method.returnsSelf() && argument instanceof JType captured && captured.isCapture() && receiver instanceof JType self && self.isClass()) {
        return JType.capture(sema, self);
      }
      if (!argument.isTypeVar()) {
        return argument;
      }
    }
    if (method.returnsSelf() && receiver instanceof JType returned && returned.isClass()) {
      boolean wildcard = returned.typeArguments().stream().anyMatch(typeArgument -> typeArgument instanceof JType type && type.isCapture());
      return wildcard ? JType.capture(sema, returned) : returned;
    }
    return null;
  }

  @Nullable
  private Type returnedTypeArgument(JMethodSymbol method, @Nullable Type receiver) {
    Integer index = method.returnTypeParameter();
    Type declaring = index == null || receiver == null ? null : sema.substitution().declaringType(method, receiver);
    return declaring != null && declaring.isParameterized() && declaring.typeArguments().size() > index ? declaring.typeArguments().get(index) : null;
  }

  private Type declaredReturnType(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    JsonObject declaredReturn = sema.substitution().declaredReturn(method);
    if (declaredReturn != null) {
      Type substituted = substitutedReturn(method, receiver, arguments, declaredReturn);
      Type known = substituted != null ? substituted : libraryReturn(method, declaredReturn);
      if (known != null) {
        return known;
      }
    }
    Type declared = method.returnType().type();
    if ((declared == null || declared.isUnknown()) && declaredReturn != null) {
      // the declaration comes later in the file than this call
      JType plain = sema.factTypes().typeFromFact(declaredReturn, Map.of(), true);
      if (!plain.isUnknown() && !mentionsMethodTypeVariable(method, plain)) {
        return plain;
      }
    }
    return declared == null ? Type.UNKNOWN : declared;
  }

  /** The method's own type variables shadow the class ones of the same name, even when the call leaves them unbound. */
  private Map<String, Type> invocationSubstitution(JMethodSymbol method, Map<String, Type> classSubstitution, List<Type> arguments) {
    Map<String, Type> substitution = new HashMap<>(classSubstitution);
    JsonObject member = classSubstitution.isEmpty() ? null : sema.substitution().lookupGenericMember(method);
    if (member != null) {
      strings(member, TYPE_VARS).forEach(substitution::remove);
    }
    substitution.putAll(methodSubstitution(method, arguments));
    return substitution;
  }

  @Nullable
  private Type substitutedReturn(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments, JsonObject declaredReturn) {
    Map<String, Type> classSubstitution = receiver == null ? Map.of() : sema.substitution().classSubstitution(method, receiver);
    Map<String, Type> substitution = invocationSubstitution(method, classSubstitution, arguments);
    if (substitution.isEmpty() && !method.isStatic()) {
      return null;
    }
    JType substituted = sema.factTypes().typeFromFact(declaredReturn, substitution, true);
    boolean leaks = classSubstitution.isEmpty() ? Substitution.mentionsTypeVariable(substituted) : mentionsMethodTypeVariable(method, substituted);
    return substituted.isUnknown() || leaks ? null : substituted;
  }

  /** A library method with no type variables in play still returns the type arguments its signature spells out. */
  @Nullable
  private JType libraryReturn(JMethodSymbol method, JsonObject declaredReturn) {
    if (method.isStatic() || method.declaration() != null) {
      return null;
    }
    JType plain = sema.factTypes().typeFromFact(declaredReturn, Map.of(), true);
    boolean spelledOut = !plain.isUnknown() && plain.isParameterized() && !Substitution.mentionsTypeVariable(plain);
    return spelledOut && plain.args.stream().noneMatch(JType::isCapture) ? plain : null;
  }

  /** The erased return type of an instance method of a generic class called on a raw receiver, or null when it does not apply. */
  @Nullable
  Type rawReturn(JMethodSymbol method) {
    JsonObject declaredReturn = sema.substitution().declaredReturn(method);
    if (declaredReturn == null || method.isStatic() || !hasClassTypeVariables(method)) {
      return null;
    }
    JType plain = sema.factTypes().typeFromFact(declaredReturn, Map.of(), true);
    if (plain.isTypeVar() && !method.returnType().type().isUnknown()) {
      // the descriptor erases a type variable to its bound
      return method.returnType().type();
    }
    return plain.isUnknown() ? null : plain.erasure();
  }

  private boolean hasClassTypeVariables(JMethodSymbol method) {
    JsonObject info = sema.typeInfos.get(sema.sourceName(method.owner().type().erasure().fullyQualifiedName()));
    return info != null && !strings(info, TYPE_VARS).isEmpty();
  }

  /**
   * The thrown types of a call when the method throws a type variable, taken from the arguments the variable is
   * inferred from, or null when the erased thrown types stand.
   */
  @Nullable
  List<Type> invocationThrown(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    JsonObject member = method.isUnknown() ? null : sema.substitution().lookupGenericMember(method);
    if (member == null || !(member.get("throwsTypes") instanceof JsonArray facts) || facts.isEmpty()) {
      return null;
    }
    Map<String, Type> substitution = invocationSubstitution(method, receiver == null ? Map.of() : sema.substitution().classSubstitution(method, receiver), arguments);
    List<Type> erased = method.thrownTypes();
    List<Type> result = new ArrayList<>();
    boolean changed = false;
    for (int i = 0; i < facts.size(); i++) {
      JsonObject fact = facts.get(i) instanceof JsonObject object ? object : null;
      Type inferred = inferredThrown(fact, substitution);
      if (inferred == null) {
        result.add(i < erased.size() ? erased.get(i) : declaredThrown(fact));
      } else {
        result.add(inferred);
        changed = true;
      }
    }
    return changed ? result : null;
  }

  @Nullable
  private Type inferredThrown(@Nullable JsonObject fact, Map<String, Type> substitution) {
    if (fact == null || !isTypeVariable(fact)) {
      return null;
    }
    Type inferred = sema.factTypes().typeFromFact(fact, substitution, true);
    return inferred.isUnknown() || Substitution.mentionsTypeVariable(inferred) ? null : inferred;
  }

  private Type declaredThrown(@Nullable JsonObject fact) {
    return fact == null || isTypeVariable(fact) ? Type.UNKNOWN : sema.factTypes().typeFromFact(fact, Map.of(), true);
  }

  private static boolean isTypeVariable(JsonObject fact) {
    return "typevar".equals(text(fact, "kind"));
  }

  /** Maps the type variables of {@code method} itself to the arguments they are inferred from. */
  private Map<String, Type> methodSubstitution(JMethodSymbol method, List<Type> arguments) {
    JsonObject member = arguments.isEmpty() && sema.explicitTypeArguments.isEmpty() ? null : sema.substitution().lookupGenericMember(method);
    if (member == null || !(member.get(TYPE_VARS) instanceof JsonArray names)) {
      return Map.of();
    }
    List<String> variables = sema.substitution().parameterVariables(method);
    Map<String, Type> substitution = new HashMap<>();
    for (int i = 0; i < names.size(); i++) {
      Type inferred = sema.substitution().methodArgument(variables, "m" + i, arguments);
      if (inferred != null && !inferred.isUnknown()) {
        substitution.put(names.get(i).getAsString(), inferred);
      }
    }
    Set<String> own = new HashSet<>();
    names.forEach(name -> own.add(name.getAsString()));
    List<JsonObject> declared = sema.substitution().declaredParameters(method);
    for (int i = 0; i < declared.size() && i < arguments.size(); i++) {
      sema.functionalInference().bindFromType(declared.get(i), arguments.get(i), own, substitution);
    }
    return substitution;
  }

  /**
   * The type {@code method} returns when the type its call is passed as fixes the type variables of the
   * method, or null when they stay open.
   */
  @Nullable
  Type targetedReturn(JMethodSymbol method, Type target) {
    JsonObject member = sema.substitution().lookupGenericMember(method);
    JsonObject declared = sema.substitution().declaredReturn(method);
    if (member == null || declared == null || !(member.get(TYPE_VARS) instanceof JsonArray names) || names.isEmpty()) {
      return null;
    }
    Set<String> own = new HashSet<>();
    names.forEach(name -> own.add(name.getAsString()));
    Map<String, Type> bound = new HashMap<>();
    sema.functionalInference().bindReturnToTarget(declared, target, own, bound);
    if (bound.isEmpty()) {
      return null;
    }
    JType result = sema.factTypes().typeFromFact(declared, bound);
    return result.isUnknown() || Substitution.mentionsTypeVariable(result) ? null : result;
  }

  /**
   * The result of a call that returns a bare method type variable, such as {@code Stream<E>.collect(collector)}, when
   * the type it is passed as takes a supertype of the element where the result carries it: a collector factory leaves
   * its element free, so {@code Collection<CharSequence>} fixes the result of a stream of {@code String} to
   * {@code List<CharSequence>}. Null when the result stands.
   */
  @Nullable
  Type retargetedElement(JMethodSymbol method, Type receiver, Type returned, Type target) {
    JsonObject declared = sema.substitution().declaredReturn(method);
    JsonObject member = method.isUnknown() ? null : sema.substitution().lookupGenericMember(method);
    if (declared == null || member == null || !"typevar".equals(text(declared, "kind"))
      || !(receiver instanceof JType owner) || owner.args.isEmpty()
      || !(returned instanceof JType result) || !result.isParameterized() || !(target instanceof JType wantedType) || !wantedType.isParameterized()) {
      return null;
    }
    JType element = owner.args.get(0);
    if (element.isWildcard() || element.isUnknown() || element.isParameterized() || !element.isClass()) {
      return null;
    }
    int open = onlyPosition(result.args, element);
    Type aligned = open < 0 ? null : sema.substitution().asSuperType(result, wantedType.erasure().fullyQualifiedName(), new HashSet<>());
    if (aligned == null || !aligned.isParameterized() || aligned.typeArguments().size() != wantedType.args.size()) {
      return null;
    }
    for (int k = 0; k < wantedType.args.size(); k++) {
      JType wanted = wantedType.args.get(k);
      if (aligned.typeArguments().get(k).fullyQualifiedName().equals(element.fullyQualifiedName())
        && !wanted.isWildcard() && !wanted.isUnknown() && !wanted.isParameterized() && wanted.isClass()
        && !wanted.fullyQualifiedName().equals(element.fullyQualifiedName()) && element.isSubtypeOf(wanted)) {
        List<JType> arguments = new ArrayList<>(result.args);
        arguments.set(open, wanted);
        return JType.named(sema, result.erasure().fullyQualifiedName(), arguments);
      }
    }
    return null;
  }

  /** The single position of {@code element} among {@code arguments}, or -1 when it appears there zero or several times. */
  private static int onlyPosition(List<JType> arguments, JType element) {
    int position = -1;
    for (int i = 0; i < arguments.size(); i++) {
      if (arguments.get(i).fullyQualifiedName().equals(element.fullyQualifiedName())) {
        if (position >= 0) {
          return -1;
        }
        position = i;
      }
    }
    return position;
  }

  private boolean mentionsMethodTypeVariable(JMethodSymbol method, Type type) {
    JsonObject member = sema.substitution().lookupGenericMember(method);
    if (member == null || !(member.get(TYPE_VARS) instanceof JsonArray names) || names.isEmpty()) {
      return false;
    }
    Set<String> own = new HashSet<>();
    names.forEach(name -> own.add(name.getAsString()));
    return Substitution.mentionsTypeVariable(type, own::contains);
  }

  /**
   * The parameterization of {@code created} a diamond infers from {@code target}: each type parameter of the created
   * class takes the target argument it is passed to through the supertype matching the target.
   */
  Type inferDiamond(Type created, Type target) {
    if (!(created.erasure() instanceof JType raw) || raw.isUnknown() || !(target instanceof JType expected) || !expected.isParameterized()) {
      return created;
    }
    String targetFqn = expected.erasure().fullyQualifiedName();
    if (raw.fullyQualifiedName().equals(targetFqn)) {
      return JType.named(sema, raw.fullyQualifiedName(), expected.args);
    }
    Type generic = raw.declaringType();
    Type declared = generic.isParameterized() ? sema.substitution().asSuperType(generic, targetFqn, new HashSet<>()) : null;
    if (declared == null || declared.typeArguments().size() != expected.args.size()) {
      return created;
    }
    Map<String, JType> inferred = new HashMap<>();
    for (int i = 0; i < expected.args.size(); i++) {
      if (declared.typeArguments().get(i).isTypeVar()) {
        inferred.put(declared.typeArguments().get(i).fullyQualifiedName(), expected.args.get(i));
      }
    }
    List<JType> arguments = new ArrayList<>();
    for (Type variable : generic.typeArguments()) {
      JType argument = inferred.get(variable.fullyQualifiedName());
      if (argument == null) {
        return created;
      }
      arguments.add(argument);
    }
    return JType.named(sema, raw.fullyQualifiedName(), arguments);
  }

  /**
   * The parameterization of {@code created} a diamond infers from the arguments of its constructor: each type
   * parameter of the class takes the argument type it is passed as, or {@code created} when one stays open.
   */
  Type inferDiamondFromArguments(JMethodSymbol constructor, Type created, List<Type> arguments) {
    if (!(created.erasure() instanceof JType raw) || raw.isUnknown() || created.isParameterized()) {
      return created;
    }
    JsonObject info = sema.typeInfos.get(sema.sourceName(raw.fullyQualifiedName()));
    List<String> names = info == null ? List.of() : strings(info, TYPE_VARS);
    List<JsonObject> declared = names.isEmpty() ? List.of() : sema.substitution().declaredParameters(constructor);
    if (declared.isEmpty() || declared.size() != arguments.size()) {
      return created;
    }
    Set<String> own = new HashSet<>(names);
    Map<String, Type> bound = new HashMap<>();
    for (int i = 0; i < declared.size(); i++) {
      sema.functionalInference().bindFromType(declared.get(i), arguments.get(i), own, bound);
    }
    List<JType> inferred = new ArrayList<>();
    for (String name : names) {
      Type argument = bound.get(name);
      if (argument == null || argument.isUnknown()) {
        return created;
      }
      inferred.add(sema.asJType(argument));
    }
    return JType.named(sema, raw.fullyQualifiedName(), inferred);
  }

  /** The type a lambda or method reference implements for a wildcard-parameterized target (JLS 9.9). */
  Type groundType(Type target) {
    if (!(target instanceof JType type) || !type.isParameterized() || type.args.stream().noneMatch(JType::isWildcard)) {
      return target;
    }
    return JType.named(sema, type.fullyQualifiedName(), type.args.stream().map(JType::groundType).toList());
  }

  /** Capture conversion: a member is accessed through captures of the receiver's wildcard arguments. */
  Type captureWildcards(Type receiver) {
    if (!(receiver instanceof JType type) || !type.isParameterized() || type.args.stream().noneMatch(JType::isWildcard)) {
      return receiver;
    }
    List<JType> captured = type.args.stream().map(argument -> argument.isWildcard() ? JType.capture(sema, argument.element) : argument).toList();
    return JType.named(sema, type.fullyQualifiedName(), captured);
  }

  /**
   * Whether {@code method} declares type variables of its own. A constructor, whose diamond infers them, and a
   * method whose declaration is unknown count when their types are substituted.
   */
  boolean declaresTypeVariables(JMethodSymbol method, boolean substituted) {
    JsonObject member = "<init>".equals(method.name()) ? null : sema.substitution().lookupGenericMember(method);
    if (member == null) {
      return substituted;
    }
    return member.get(TYPE_VARS) instanceof JsonArray names && !names.isEmpty();
  }
}
