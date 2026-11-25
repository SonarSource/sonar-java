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
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

/** Ranks applicable overloads to pick the most specific one. */
final class OverloadRanking {

  private final JSema sema;

  OverloadRanking(JSema sema) {
    this.sema = sema;
  }

  /** The overload that matches {@code arguments} when {@code candidate} is more specific than {@code current}. */
  Symbol.MethodSymbol preferOverload(Symbol.MethodSymbol current, @Nullable Symbol.MethodSymbol candidate, List<Type> arguments, Set<Integer> functions) {
    if (candidate == null || candidate == current || candidate.isUnknown() || !MemberLookup.arityMatches(candidate, arguments.size())) {
      return current;
    }
    if (!functions.isEmpty() && sema.lookup().takesNoFunction(candidate, functions)) {
      return current;
    }
    if (!functions.isEmpty() && sema.lookup().takesNoFunction(current, functions)) {
      return candidate;
    }
    return byPhase(current, candidate, arguments);
  }

  private static Symbol.MethodSymbol byPhase(Symbol.MethodSymbol current, Symbol.MethodSymbol candidate, List<Type> arguments) {
    for (int phase = 0; phase < 3; phase++) {
      boolean currentOk = applicable(current, arguments, phase);
      boolean candidateOk = applicable(candidate, arguments, phase);
      if (currentOk && !candidateOk) {
        return current;
      }
      if (candidateOk && !currentOk) {
        return candidate;
      }
      if (currentOk) {
        return moreSpecific(candidate, current) && !moreSpecific(current, candidate) ? candidate : current;
      }
    }
    return current;
  }

  /** The most specific variable-arity overload of {@code chosen} applicable to the arguments, when no earlier phase applies to {@code chosen}. */
  static JMethodSymbol mostSpecificVarArgs(JMethodSymbol chosen, List<Type> arguments) {
    if (chosen.isUnknown() || !chosen.isVarArgsMethod() || !(chosen.owner() instanceof JTypeSymbol owner) || owner.isDeclaredInDocument()
      || applicable(chosen, arguments, 0) || applicable(chosen, arguments, 1) || arguments.stream().anyMatch(Type::isUnknown)) {
      return chosen;
    }
    List<JMethodSymbol> candidates = new ArrayList<>();
    for (Symbol member : owner.lookupSymbols(chosen.name())) {
      if (member instanceof JMethodSymbol method && !method.isUnknown() && method.isVarArgsMethod() && method.isStatic() == chosen.isStatic() && applicable(method, arguments, 2)) {
        candidates.add(method);
      }
    }
    for (JMethodSymbol candidate : candidates) {
      if (candidates.stream().allMatch(other -> other == candidate || moreSpecificVarArgs(candidate, other, arguments.size()))) {
        return candidate;
      }
    }
    return chosen;
  }

  private static boolean moreSpecificVarArgs(Symbol.MethodSymbol method, Symbol.MethodSymbol other, int count) {
    for (int i = 0; i < Math.max(count, Math.max(method.parameterTypes().size(), other.parameterTypes().size())); i++) {
      if (!expanded(method, i).erasure().isSubtypeOf(expanded(other, i).erasure())) {
        return false;
      }
    }
    return true;
  }

  private static Type expanded(Symbol.MethodSymbol method, int index) {
    List<Type> parameters = method.parameterTypes();
    return index < parameters.size() - 1 ? parameters.get(index) : ((Type.ArrayType) parameters.get(parameters.size() - 1)).elementType();
  }

  static boolean applicable(Symbol.MethodSymbol method, List<Type> arguments, int phase) {
    List<Type> parameters = method.parameterTypes();
    int count = parameters.size();
    boolean varargs = phase == 2 && method.isVarArgsMethod();
    if (varargs ? (arguments.size() < count - 1) : (arguments.size() != count)) {
      return false;
    }
    for (int i = 0; i < arguments.size(); i++) {
      Type parameter = varargs && i >= count - 1 ? ((Type.ArrayType) parameters.get(count - 1)).elementType() : parameters.get(i);
      if (!convertible(arguments.get(i), parameter, phase > 0)) {
        return false;
      }
    }
    return true;
  }

  private static boolean convertible(Type argument, Type parameter, boolean boxing) {
    Type target = parameter.erasure();
    if (argument.isUnknown() || target.isUnknown() || argument.erasure().isSubtypeOf(target)) {
      return true;
    }
    if (!boxing || argument.isPrimitive() == target.isPrimitive()) {
      return false;
    }
    Type converted = argument.isPrimitive() ? argument.primitiveWrapperType() : argument.primitiveType();
    return converted != null && converted.isSubtypeOf(target);
  }

  static boolean moreSpecific(Symbol.MethodSymbol method, Symbol.MethodSymbol other) {
    List<Type> mine = method.parameterTypes();
    List<Type> theirs = other.parameterTypes();
    if (mine.size() != theirs.size()) {
      return true;
    }
    for (int i = 0; i < mine.size(); i++) {
      if (!mine.get(i).erasure().isSubtypeOf(theirs.get(i).erasure())) {
        return false;
      }
    }
    return true;
  }
}
