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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Type;

import static org.sonar.java.semsitter.SemaFacts.erase;
import static org.sonar.java.semsitter.SemaFacts.text;

/**
 * Binds type variables from the types they are given. Parameter and result types of lambdas and method references
 * stay unknown until inference from their functional target lands.
 */
class FunctionalInference {

  private final JSema sema;

  FunctionalInference(JSema sema) {
    this.sema = sema;
  }

  /**
   * The functional interface {@code parameter} with only the type argument its method returns known, as the type a
   * function that produces {@code produced} has there, or null when {@code parameter} is not such an interface.
   * @param parameter functional interface the function is passed as, used once lambda inference lands
   * @param produced type the function produces, used once lambda inference lands
   */
  @Nullable
  Type functionProducing(Type parameter, Type produced) {
    return null;
  }

  /**
   * Whether {@code method} returns a type variable, as the descriptor erases it or as its source declares it.
   * @param method method to inspect, used once lambda inference lands
   */
  boolean returnsClassVariable(JMethodSymbol method) {
    return false;
  }

  /**
   * What {@code Type::method} yields when the method returns a class type variable, read from the first
   * parameter of {@code function}, the receiver it is applied to; null when that is not known.
   * @param method referenced method, used once lambda inference lands
   * @param function functional interface the reference implements, used once lambda inference lands
   */
  @Nullable
  Type unboundReferenceResult(JMethodSymbol method, Type function) {
    return null;
  }

  /**
   * The parameter types of {@code method} when the type its call is passed as fixes the type variables of the
   * method, or null when they stay open.
   * @param method invoked method, used once lambda inference lands
   * @param target type the call is passed as, used once lambda inference lands
   */
  @Nullable
  List<Type> targetedParameters(JMethodSymbol method, Type target) {
    return null;
  }

  /**
   * Binds the type variables a declared return type names from the type the call is passed as. The target may be a
   * supertype of the declared type: its arguments are read through the supertype of the declared type it names.
   */
  void bindReturnToTarget(JsonObject declared, Type target, Set<String> variables, Map<String, Type> bound) {
    bindFromType(declared, target, variables, bound);
    if (!bound.isEmpty() || !(target instanceof JType expected) || !expected.isParameterized() || declared.get("args") == null) {
      return;
    }
    JType written = sema.factTypes().typeFromFact(declared);
    Type aligned = written.isParameterized() ? sema.substitution().asSuperType(written, expected.erasure().fullyQualifiedName(), new HashSet<>()) : null;
    if (aligned == null || !aligned.isParameterized() || aligned.typeArguments().size() != expected.args.size()) {
      return;
    }
    for (int i = 0; i < expected.args.size(); i++) {
      Type argument = aligned.typeArguments().get(i);
      JType given = expected.args.get(i);
      if (argument.isTypeVar() && variables.contains(argument.name()) && !given.isWildcard() && !given.isUnknown()) {
        bound.putIfAbsent(argument.name(), given);
      }
    }
  }

  /**
   * Binds the type variables of {@code variables} that the declared type {@code declared} names to the types
   * {@code actual} gives them at the same position. Open type variables and unbounded wildcards bind nothing.
   */
  void bindFromType(JsonObject declared, Type actual, Set<String> variables, Map<String, Type> bound) {
    Type known = groundOf(actual);
    if (known.isUnknown() || known.isNullType() || known.isTypeVar() || (actual != known && known.is("java.lang.Object"))) {
      return;
    }
    String kind = text(declared, "kind");
    if ("typevar".equals(kind)) {
      bindVariable(text(declared, "id"), known, variables, bound);
    } else if (JSema.WILDCARD.equals(kind)) {
      JsonObject limit = wildcardLimit(declared);
      if (limit != null) {
        bindFromType(limit, known, variables, bound);
      }
    } else if (JSema.ARRAY.equals(kind) && declared.get("elem") instanceof JsonObject element && known instanceof Type.ArrayType array) {
      bindFromType(element, array.elementType(), variables, bound);
    } else if (declared.get("args") instanceof JsonArray arguments && !arguments.isEmpty() && text(declared, "id") != null) {
      bindArguments(text(declared, "id"), arguments, known, variables, bound);
    }
  }

  private static void bindVariable(@Nullable String id, Type known, Set<String> variables, Map<String, Type> bound) {
    if (variables.contains(id)) {
      bound.putIfAbsent(id, known.isPrimitive() ? known.primitiveWrapperType() : known);
    }
  }

  private Type groundOf(Type actual) {
    Type known = actual;
    while (known instanceof JType wildcard && wildcard.isWildcard()) {
      JType ground = wildcard.groundType();
      known = ground == wildcard.element && !ground.is("java.lang.Object") ? JType.capture(sema, ground) : ground;
    }
    return known;
  }

  @Nullable
  private static JsonObject wildcardLimit(JsonObject declared) {
    if (declared.get("super") instanceof JsonObject lower) {
      return lower;
    }
    return declared.get("extends") instanceof JsonObject upper ? upper : null;
  }

  private void bindArguments(String id, JsonArray arguments, Type known, Set<String> variables, Map<String, Type> bound) {
    Type aligned = sema.substitution().asSuperType(known, erase(id), new HashSet<>());
    if (aligned == null || !aligned.isParameterized() || aligned.typeArguments().size() != arguments.size()) {
      return;
    }
    for (int i = 0; i < arguments.size(); i++) {
      if (arguments.get(i) instanceof JsonObject argument) {
        bindFromType(argument, aligned.typeArguments().get(i), variables, bound);
      }
    }
  }
}
