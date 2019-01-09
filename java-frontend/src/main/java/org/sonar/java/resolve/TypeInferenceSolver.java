/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.resolve;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol;
import org.sonar.plugins.java.api.semantic.Type;

public class TypeInferenceSolver {

  private final Symbols symbols;
  private final LeastUpperBound leastUpperBound;
  private final TypeSubstitutionSolver typeSubstitutionSolver;
  private final Multimap<Type, Type> handledFormals = HashMultimap.create();

  public TypeInferenceSolver(LeastUpperBound leastUpperBound, Symbols symbols, TypeSubstitutionSolver typeSubstitutionSolver) {
    this.symbols = symbols;
    this.leastUpperBound = leastUpperBound;
    this.typeSubstitutionSolver = typeSubstitutionSolver;
  }

  TypeSubstitution inferTypeSubstitution(MethodJavaSymbol method, List<JavaType> formals, List<JavaType> argTypes) {
    handledFormals.clear();
    return inferTypeSubstitutionRec(method, formals, argTypes);
  }
  private TypeSubstitution inferTypeSubstitutionRec(MethodJavaSymbol method, List<JavaType> formals, List<JavaType> argTypes) {
    boolean isVarArgs = method.isVarArgs();
    int numberFormals = formals.size();
    int numberArgs = argTypes.size();
    int numberParamToCheck = Math.min(numberFormals, numberArgs);
    List<JavaType> newArgTypes = new ArrayList<>(argTypes);
    TypeSubstitution substitution = new TypeSubstitution();

    // method is varargs but parameter is not provided
    if (isVarArgs && numberFormals == numberArgs + 1) {
      numberParamToCheck += 1;
      newArgTypes.add(symbols.objectType);
    }
    for (int i = 0; i < numberParamToCheck; i++) {
      JavaType formalType = formals.get(i);
      JavaType argType = newArgTypes.get(i);
      boolean variableArity = isVarArgs && i == (numberFormals - 1);
      List<JavaType> remainingArgTypes = new ArrayList<>(newArgTypes.subList(i, newArgTypes.size()));

      substitution = inferTypeSubstitution(method, substitution, formalType, argType, variableArity, remainingArgTypes);

      if (substitution.isUnchecked() || (!method.isConstructor() && substitution.typeVariables().containsAll(method.typeVariableTypes))) {
        // we found all the substitution
        break;
      }
    }
    return substitution;
  }

  private TypeSubstitution inferTypeSubstitution(MethodJavaSymbol method, TypeSubstitution substitution, JavaType formalType, JavaType argumentType,
    boolean variableArity, List<JavaType> remainingArgTypes) {
    if(handledFormals.get(formalType).contains(argumentType)) {
      return substitution;
    }
    JavaType argType = argumentType;
    if (argType.isTagged(JavaType.DEFERRED)) {
      argType = ((DeferredType) argType).getUninferedType();
    }
    handledFormals.put(formalType, argType);
    TypeSubstitution result = substitution;

    if (formalType.isTagged(JavaType.TYPEVAR)) {
      result = inferTypeSubstitutionInTypeVariable(method, substitution, (TypeVariableJavaType) formalType, argType, variableArity, remainingArgTypes);
    } else if (formalType.isArray()) {
      result = inferTypeSubstitutionInArrayType(method, substitution, (ArrayJavaType) formalType, argType, variableArity, remainingArgTypes);
    } else if (formalType.isParameterized()) {
      result = inferTypeSubstitutionInParameterizedType(method, substitution, (ParametrizedTypeJavaType) formalType, argType, variableArity, remainingArgTypes);
    } else if (formalType.isTagged(JavaType.WILDCARD)) {
      result = inferTypeSubstitutionInWildcardType(method, substitution, (WildCardType) formalType, argType, variableArity, remainingArgTypes);
    } else {
      // nothing to infer for simple class types or primitive types
    }
    return result;
  }

  private TypeSubstitution inferTypeSubstitutionInArrayType(MethodJavaSymbol method, TypeSubstitution substitution, ArrayJavaType formalType, JavaType argType,
    boolean variableArity, List<JavaType> remainingArgTypes) {
    JavaType newArgType = null;
    if (argType.isArray()) {
      newArgType = ((ArrayJavaType) argType).elementType;
    } else if (variableArity) {
      newArgType = (JavaType) leastUpperBound.leastUpperBound(mapToBoxedSet(remainingArgTypes));
    }
    if (newArgType != null) {
      TypeSubstitution newSubstitution = inferTypeSubstitution(method, substitution, formalType.elementType, newArgType, variableArity, remainingArgTypes);
      return mergeTypeSubstitutions(substitution, newSubstitution);
    }
    return substitution;
  }

  private static Set<Type> mapToBoxedSet(List<JavaType> types) {
    return types.stream().map(type -> type.isPrimitive() ? type.primitiveWrapperType : type).collect(Collectors.toSet());
  }

  private TypeSubstitution inferTypeSubstitutionInParameterizedType(MethodJavaSymbol method, TypeSubstitution substitution, ParametrizedTypeJavaType formalType, JavaType argType,
    boolean variableArity, List<JavaType> remainingArgTypes) {
    List<JavaType> formalTypeSubstitutedTypes = formalType.typeSubstitution.substitutedTypes();
    TypeSubstitution result = substitution;
    if (argType.isParameterized()) {
      ParametrizedTypeJavaType parametrizedArgType = (ParametrizedTypeJavaType) argType;
      if (parametrizedArgType.rawType == formalType.rawType) {
        List<JavaType> argTypeSubstitutedTypes = parametrizedArgType.typeSubstitution.substitutedTypes();
        TypeSubstitution newSubstitution = inferTypeSubstitutionRec(method, formalTypeSubstitutedTypes, argTypeSubstitutedTypes);
        return mergeTypeSubstitutions(substitution, newSubstitution);
      }
      JavaType superclass = argType.symbol.getSuperclass();
      if (superclass != null) {
        superclass = typeSubstitutionSolver.applySubstitution(superclass, parametrizedArgType.typeSubstitution);
        TypeSubstitution newSubstitution = inferTypeSubstitutionInParameterizedType(method, substitution, formalType, superclass, variableArity, remainingArgTypes);
        if (!newSubstitution.substitutedTypes().isEmpty()) {
          result = mergeTypeSubstitutions(substitution, newSubstitution);
        }
      }
      for (JavaType superInterface : argType.symbol.getInterfaces()) {
        superclass = typeSubstitutionSolver.applySubstitution(superInterface, parametrizedArgType.typeSubstitution);
        TypeSubstitution newSubstitution = inferTypeSubstitutionInParameterizedType(method, substitution, formalType, superclass, variableArity, remainingArgTypes);
        if (!newSubstitution.substitutedTypes().isEmpty()) {
          result = mergeTypeSubstitutions(result, newSubstitution);
        }
      }

    } else if (isRawTypeOfType(argType, formalType) || isNullType(argType)) {
      return TypeSubstitution.uncheckedTypeSubstitution();
    } else if (argType.isSubtypeOf(formalType.erasure()) && argType.isClass()) {
      for (JavaType superType : ((ClassJavaType) argType).superTypes()) {
        if (sameErasure(formalType, superType)) {
          TypeSubstitution newSubstitution = inferTypeSubstitution(method, substitution, formalType, superType, variableArity, remainingArgTypes);
          result = mergeTypeSubstitutions(substitution, newSubstitution);
          break;
        }
      }
    }
    return result;
  }

  private static boolean isRawTypeOfType(JavaType rawType, JavaType type) {
    return rawType == type.erasure();
  }

  private static boolean sameErasure(JavaType type1, JavaType type2) {
    return type1.erasure() == type2.erasure();
  }

  private boolean isNullType(JavaType type) {
    return type == symbols.nullType;
  }

  private TypeSubstitution inferTypeSubstitutionInWildcardType(MethodJavaSymbol method, TypeSubstitution substitution, WildCardType formalType, JavaType argType,
    boolean variableArity, List<JavaType> remainingArgTypes) {
    JavaType newArgType = argType;
    if (argType.isTagged(JavaType.WILDCARD)) {
      newArgType = ((WildCardType) argType).bound;
    }
    TypeSubstitution newSubstitution = inferTypeSubstitution(method, substitution, formalType.bound, newArgType, variableArity, remainingArgTypes);
    return mergeTypeSubstitutions(substitution, newSubstitution);
  }

  private static TypeSubstitution mergeTypeSubstitutions(TypeSubstitution currentSubstitution, TypeSubstitution newSubstitution) {
    if(newSubstitution.isUnchecked()) {
      return newSubstitution;
    }
    TypeSubstitution result = new TypeSubstitution();
    for (Map.Entry<TypeVariableJavaType, JavaType> substitution : currentSubstitution.substitutionEntries()) {
      result.add(substitution.getKey(), substitution.getValue());
    }
    for (Map.Entry<TypeVariableJavaType, JavaType> substitution : newSubstitution.substitutionEntries()) {
      if (!result.typeVariables().contains(substitution.getKey())) {
        result.add(substitution.getKey(), substitution.getValue());
      }
    }
    return result;
  }

  private TypeSubstitution inferTypeSubstitutionInTypeVariable(MethodJavaSymbol method, TypeSubstitution substitution, TypeVariableJavaType formalType, JavaType argType,
    boolean variableArity, List<JavaType> remainingArgTypes) {
    TypeSubstitution result = new TypeSubstitution(substitution);
    if (substitution.substitutedType(formalType) == null) {
      JavaType expectedType = argType;
      if (expectedType.isPrimitive()) {
        expectedType = expectedType.primitiveWrapperType;
      } else if (isNullType(expectedType)) {
        expectedType = symbols.objectType;
      }
      TypeVariableJavaType typeVar = formalType;
      result.add(typeVar, expectedType);
    }
    for (JavaType bound : formalType.bounds) {
      if (!bound.is("java.lang.Object")) {
        result = inferTypeSubstitution(method, result, bound, argType, variableArity, remainingArgTypes);
      }
    }
    return result;
  }
}
