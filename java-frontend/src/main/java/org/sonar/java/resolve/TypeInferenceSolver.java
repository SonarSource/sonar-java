/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sonar.plugins.java.api.semantic.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeInferenceSolver {

  private final Symbols symbols;

  public TypeInferenceSolver(Symbols symbols) {
    this.symbols = symbols;
  }

  TypeSubstitution inferTypeSubstitution(JavaSymbol.MethodJavaSymbol method, List<JavaType> formals, List<JavaType> argTypes) {
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

      if (!method.isConstructor() && substitution.typeVariables().containsAll(method.typeVariableTypes)) {
        // we found all the substitution
        break;
      }
    }
    return substitution;
  }

  private TypeSubstitution inferTypeSubstitution(JavaSymbol.MethodJavaSymbol method, TypeSubstitution currentSubstitution, JavaType formalType, JavaType argumentType,
    boolean variableArity, List<JavaType> remainingArgTypes) {
    JavaType argType = argumentType;
    if (argType.isTagged(JavaType.DEFERRED) && ((DeferredType) argType).getUninferedType() != null) {
      argType = ((DeferredType) argType).getUninferedType();
    }
    if (formalType.isTagged(JavaType.TYPEVAR)) {
      completeSubstitution(currentSubstitution, formalType, argType);
    } else if (formalType.isArray()) {
      JavaType newArgType = null;
      if (argType.isArray()) {
        newArgType = ((ArrayJavaType) argType).elementType;
      } else if (variableArity) {
        newArgType = leastUpperBound(remainingArgTypes);
      }
      if (newArgType != null) {
        JavaType formalElementType = ((ArrayJavaType) formalType).elementType;
        TypeSubstitution newSubstitution = inferTypeSubstitution(method, currentSubstitution, formalElementType, newArgType, variableArity, remainingArgTypes);
        return mergeTypeSubstitutions(currentSubstitution, newSubstitution);
      }
    } else if (formalType.isParameterized()) {
      List<JavaType> formalTypeSubstitutedTypes = ((ParametrizedTypeJavaType) formalType).typeSubstitution.substitutedTypes();
      if (argType.isParameterized()) {
        List<JavaType> argTypeSubstitutedTypes = ((ParametrizedTypeJavaType) argType).typeSubstitution.substitutedTypes();
        TypeSubstitution newSubstitution = inferTypeSubstitution(method, formalTypeSubstitutedTypes, argTypeSubstitutedTypes);
        return mergeTypeSubstitutions(currentSubstitution, newSubstitution);
      } else if (isRawTypeOfType(argType, formalType) || isNullType(argType)) {
        List<JavaType> fakeTypes = new ArrayList<>(formalTypeSubstitutedTypes.size());
        for (int j = 0; j < formalTypeSubstitutedTypes.size(); j++) {
          fakeTypes.add(symbols.objectType);
        }
        TypeSubstitution newSubstitution = inferTypeSubstitution(method, formalTypeSubstitutedTypes, fakeTypes);
        return mergeTypeSubstitutions(currentSubstitution, newSubstitution);
      } else if (argType.isSubtypeOf(formalType.erasure()) && argType.isClass()) {
        for (JavaType superType : ((ClassJavaType) argType).symbol.superTypes()) {
          if (sameErasure(formalType, superType)) {
            return inferTypeSubstitution(method, currentSubstitution, formalType, superType, variableArity, remainingArgTypes);
          }
        }
      }
    } else if (formalType.isTagged(JavaType.WILDCARD)) {
      JavaType inferedFromArg = argType;
      if (argType.isTagged(JavaType.WILDCARD)) {
        inferedFromArg = ((WildCardType) argType).bound;
      }
      TypeSubstitution newSubstitution = inferTypeSubstitution(method, currentSubstitution, ((WildCardType) formalType).bound, inferedFromArg, variableArity, remainingArgTypes);
      return mergeTypeSubstitutions(currentSubstitution, newSubstitution);
    } else {
      // nothing to infer for simple class types or primitive types
    }
    return currentSubstitution;
  }

  private static boolean sameErasure(JavaType formalType, JavaType superType) {
    return formalType.erasure() == superType.erasure();
  }

  private boolean isNullType(JavaType argType) {
    return argType == symbols.nullType;
  }

  private static boolean isRawTypeOfType(JavaType argType, JavaType formalType) {
    return argType == formalType.erasure();
  }

  private static JavaType leastUpperBound(List<JavaType> remainingArgTypes) {
    return (JavaType) Types.leastUpperBound(mapToBoxedSet(remainingArgTypes));
  }

  private static Set<Type> mapToBoxedSet(List<JavaType> types) {
    return Sets.newHashSet(Iterables.transform(Sets.<Type>newHashSet(types), new Function<Type, Type>() {
      @Override
      public Type apply(Type type) {
        if (type.isPrimitive()) {
          return ((JavaType) type).primitiveWrapperType;
        }
        return type;
      }
    }));
  }

  private static TypeSubstitution mergeTypeSubstitutions(TypeSubstitution currentSubstitution, TypeSubstitution newSubstitution) {
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

  private void completeSubstitution(TypeSubstitution currentSubstitution, JavaType formalType, JavaType argType) {
    if (formalType.isTagged(JavaType.TYPEVAR) && currentSubstitution.substitutedType(formalType) == null) {
      JavaType expectedType = argType;
      if (expectedType.isPrimitive()) {
        expectedType = expectedType.primitiveWrapperType;
      } else if (isNullType(expectedType)) {
        expectedType = symbols.objectType;
      }
      TypeVariableJavaType typeVar = (TypeVariableJavaType) formalType;
      currentSubstitution.add(typeVar, expectedType);
    }
  }
}
