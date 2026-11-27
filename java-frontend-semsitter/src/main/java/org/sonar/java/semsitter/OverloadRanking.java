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

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

/** Ranks applicable overloads to pick the most specific one. */
class OverloadRanking {

  /**
   * The overload that matches {@code arguments} when {@code candidate} is more specific than {@code current}.
   * @param candidate overload to compare, used once ranking lands
   * @param arguments types of the arguments, used once ranking lands
   * @param functions positions of the lambda and method reference arguments, used once ranking lands
   */
  Symbol.MethodSymbol preferOverload(Symbol.MethodSymbol current, @Nullable Symbol.MethodSymbol candidate, List<Type> arguments, Set<Integer> functions) {
    return current;
  }

  /**
   * The most specific variable-arity overload of {@code chosen} applicable to the arguments.
   * @param arguments types of the arguments, used once ranking lands
   */
  JMethodSymbol mostSpecificVarArgs(JMethodSymbol chosen, List<Type> arguments) {
    return chosen;
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
