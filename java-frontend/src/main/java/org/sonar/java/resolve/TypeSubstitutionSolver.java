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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeSubstitutionSolver {

  private final ParametrizedTypeCache parametrizedTypeCache;
  private final Symbols symbols;

  public TypeSubstitutionSolver(ParametrizedTypeCache parametrizedTypeCache, Symbols symbols) {
    this.parametrizedTypeCache = parametrizedTypeCache;
    this.symbols = symbols;
  }

  @CheckForNull
  TypeSubstitution getTypeSubstitution(JavaSymbol.MethodJavaSymbol method, JavaType site, List<JavaType> typeParams, List<JavaType> argTypes) {
    List<JavaType> formals = ((JavaType.MethodJavaType) method.type).argTypes;
    TypeSubstitution substitution = new TypeSubstitution();
    if (method.isParametrized()) {
      if (!typeParams.isEmpty()) {
        substitution = getSubstitutionFromTypeParams(method.typeVariableTypes, typeParams);
      } else if (formals.isEmpty()) {
        // substitution can not be inferred, as it is not based on arguments, method call is still valid
        return substitution;
      } else {
        formals = applySiteSubstitutionToFormalParameters(formals, site);
        substitution = inferTypeSubstitution(method, formals, argTypes);
      }
      if (substitution.size() == 0 || !isValidSubtitution(substitution, site)) {
        // substitution discarded
        return null;
      }
    }
    return substitution;
  }

  JavaType getReturnType(@Nullable JavaType returnType, JavaType defSite, JavaType callSite, boolean parametrizedMethodCall, TypeSubstitution substitution) {
    if (returnType == null) {
      // case of constructors
      return returnType;
    }
    JavaType resultType = applySiteSubstitution(returnType, defSite);
    if (callSite != defSite) {
      resultType = applySiteSubstitution(resultType, callSite);
    }
    if (isRawTypeOfParametrizedType(callSite) && !parametrizedMethodCall) {
      // JLS8 5.1.9 + JLS8 15.12.2.6 : unchecked conversion
      return resultType.erasure();
    }
    return applySubstitution(resultType, substitution);
  }

  private static boolean isRawTypeOfParametrizedType(JavaType site) {
    return !isParametrizedType(site) && !site.symbol.typeVariableTypes.isEmpty();
  }

  List<JavaType> applySiteSubstitutionToFormalParameters(List<JavaType> formals, JavaType site) {
    if (isParametrizedType(site)) {
      return applySubstitutionToFormalParameters(formals, ((JavaType.ParametrizedTypeJavaType) site).typeSubstitution);
    }
    return formals;
  }

  JavaType applySiteSubstitution(JavaType type, JavaType site) {
    if (isParametrizedType(site)) {
      return applySubstitution(type, ((JavaType.ParametrizedTypeJavaType) site).typeSubstitution);
    }
    return type;
  }

  JavaType applySiteSubstitution(@Nullable JavaType resolvedType, JavaType callSite, JavaType resolvedTypeDefinition) {
    if (resolvedType == null) {
      // case of constructors
      return null;
    }
    return applySiteSubstitution(applySiteSubstitution(resolvedType, resolvedTypeDefinition), callSite);
  }

  List<JavaType> applySubstitutionToFormalParameters(List<JavaType> types, TypeSubstitution substitution) {
    if (substitution.size() == 0 || types.isEmpty()) {
      return types;
    }
    List<JavaType> results = new ArrayList<>(types.size());
    for (JavaType type : types) {
      results.add(applySubstitution(type, substitution));
    }
    return results;
  }

  private JavaType applySubstitution(JavaType type, TypeSubstitution substitution) {
    JavaType substitutedType = substitution.substitutedType(type);
    if (substitutedType != null) {
      return substitutedType;
    }
    if (isParametrizedType(type)) {
      return substituteInParametrizedType((JavaType.ParametrizedTypeJavaType) type, substitution);
    }
    if (type.isTagged(JavaType.WILDCARD)) {
      return substituteInWildCardType((JavaType.WildCardType) type, substitution);
    }
    if (type.isArray()) {
      return substituteInArrayType((JavaType.ArrayJavaType) type, substitution);
    }
    return type;
  }

  private static boolean isParametrizedType(JavaType type) {
    return type instanceof JavaType.ParametrizedTypeJavaType;
  }

  private JavaType substituteInParametrizedType(JavaType.ParametrizedTypeJavaType type, TypeSubstitution substitution) {
    TypeSubstitution newSubstitution = new TypeSubstitution();
    for (Map.Entry<JavaType.TypeVariableJavaType, JavaType> entry : type.typeSubstitution.substitutionEntries()) {
      newSubstitution.add(entry.getKey(), applySubstitution(entry.getValue(), substitution));
    }
    return parametrizedTypeCache.getParametrizedTypeType(type.rawType.getSymbol(), newSubstitution);
  }

  private JavaType substituteInWildCardType(JavaType.WildCardType wildcard, TypeSubstitution substitution) {
    JavaType substitutedType = applySubstitution(wildcard.bound, substitution);
    if (substitutedType != wildcard.bound) {
      return parametrizedTypeCache.getWildcardType(substitutedType, wildcard.boundType);
    }
    return wildcard;
  }

  private JavaType substituteInArrayType(JavaType.ArrayJavaType arrayType, TypeSubstitution substitution) {
    JavaType rootElementType = arrayType.elementType;
    int nbDimensions = 1;
    while (rootElementType.isArray()) {
      rootElementType = ((JavaType.ArrayJavaType) rootElementType).elementType;
      nbDimensions++;
    }
    JavaType substitutedType = applySubstitution(rootElementType, substitution);
    if (substitutedType != rootElementType) {
      // FIXME SONARJAVA-1574 a new array type should not be created but reused if already existing for the current element type
      for (int i = 0; i < nbDimensions; i++) {
        substitutedType = new JavaType.ArrayJavaType(substitutedType, symbols.arrayClass);
      }
      return substitutedType;
    }
    return arrayType;
  }

  TypeSubstitution getSubstitutionFromTypeParams(List<JavaType.TypeVariableJavaType> typeVariableTypes, List<JavaType> typeParams) {
    TypeSubstitution substitution = new TypeSubstitution();
    if (typeVariableTypes.size() == typeParams.size()) {
      // create naive substitution
      for (int i = 0; i < typeVariableTypes.size(); i++) {
        JavaType.TypeVariableJavaType typeVariableType = typeVariableTypes.get(i);
        JavaType typeParam = typeParams.get(i);
        substitution.add(typeVariableType, typeParam);
      }
    }
    return substitution;
  }

  private TypeSubstitution inferTypeSubstitution(JavaSymbol.MethodJavaSymbol method, List<JavaType> formals, List<JavaType> argTypes) {
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

      if (substitution.typeVariables().containsAll(method.typeVariableTypes)) {
        // we found all the substitution
        break;
      }
    }
    return substitution;
  }

  private TypeSubstitution inferTypeSubstitution(JavaSymbol.MethodJavaSymbol method, TypeSubstitution currentSubstitution, JavaType formalType, JavaType argType,
    boolean variableArity, List<JavaType> remainingArgTypes) {
    if (formalType.isTagged(JavaType.TYPEVAR)) {
      completeSubstitution(currentSubstitution, formalType, argType);
    } else if (formalType.isArray()) {
      JavaType newArgType = null;
      if (argType.isArray()) {
        newArgType = ((JavaType.ArrayJavaType) argType).elementType;
      } else if (variableArity) {
        newArgType = leastUpperBound(remainingArgTypes);
      }
      if (newArgType != null) {
        JavaType formalElementType = ((JavaType.ArrayJavaType) formalType).elementType;
        TypeSubstitution newSubstitution = inferTypeSubstitution(method, currentSubstitution, formalElementType, newArgType, variableArity, remainingArgTypes);
        return mergeTypeSubstitutions(currentSubstitution, newSubstitution);
      }
    } else if (isParametrizedType(formalType)) {
      List<JavaType> formalTypeSubstitutedTypes = ((JavaType.ParametrizedTypeJavaType) formalType).typeSubstitution.substitutedTypes();
      if (isParametrizedType(argType)) {
        List<JavaType> argTypeSubstitutedTypes = ((JavaType.ParametrizedTypeJavaType) argType).typeSubstitution.substitutedTypes();
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
        for (JavaType superType : ((JavaType.ClassJavaType) argType).symbol.superTypes()) {
          if (sameErasure(formalType, superType)) {
            return inferTypeSubstitution(method, currentSubstitution, formalType, superType, variableArity, remainingArgTypes);
          }
        }
      }
    } else if (formalType.isTagged(JavaType.WILDCARD)) {
      TypeSubstitution newSubstitution = inferTypeSubstitution(method, currentSubstitution, ((JavaType.WildCardType) formalType).bound, argType, variableArity,
        remainingArgTypes);
      return mergeTypeSubstitutions(currentSubstitution, newSubstitution);
    } else {
      // nothing to infer for simple class types or primitive types
    }
    return currentSubstitution;
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

  private static boolean sameErasure(JavaType formalType, JavaType superType) {
    return formalType.erasure() == superType.erasure();
  }

  private boolean isNullType(JavaType argType) {
    return argType == symbols.nullType;
  }

  private static boolean isRawTypeOfType(JavaType argType, JavaType formalType) {
    return argType == formalType.erasure();
  }

  private static TypeSubstitution mergeTypeSubstitutions(TypeSubstitution currentSubstitution, TypeSubstitution newSubstitution) {
    TypeSubstitution result = new TypeSubstitution();
    for (Map.Entry<JavaType.TypeVariableJavaType, JavaType> substitution : currentSubstitution.substitutionEntries()) {
      result.add(substitution.getKey(), substitution.getValue());
    }
    for (Map.Entry<JavaType.TypeVariableJavaType, JavaType> substitution : newSubstitution.substitutionEntries()) {
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
      JavaType.TypeVariableJavaType typeVar = (JavaType.TypeVariableJavaType) formalType;
      currentSubstitution.add(typeVar, expectedType);
    }
  }

  private boolean isValidSubtitution(TypeSubstitution substitutions, JavaType site) {
    for (Map.Entry<JavaType.TypeVariableJavaType, JavaType> substitution : substitutions.substitutionEntries()) {
      if (!isValidSubstitution(substitutions, substitution.getKey(), substitution.getValue(), site)) {
        return false;
      }
    }
    return true;
  }

  private boolean isValidSubstitution(TypeSubstitution candidate, JavaType.TypeVariableJavaType typeVar, JavaType typeParam, JavaType site) {
    for (JavaType bound : typeVar.bounds) {
      JavaType currentBound = applySubstitution(bound, candidate);
      while (currentBound.isTagged(JavaType.TYPEVAR)) {
        JavaType newBound = candidate.substitutedType(currentBound);
        if (currentBound.equals(newBound)) {
          break;
        }
        if (newBound == null && isParametrizedType(site)) {
          newBound = ((JavaType.ParametrizedTypeJavaType) site).typeSubstitution.substitutedType(currentBound);
        }
        if (newBound == null) {
          return ((JavaSymbol.TypeJavaSymbol) site.symbol()).typeVariableTypes.contains(currentBound);
        }
        currentBound = newBound;
      }
      if (!typeParam.isSubtypeOf(currentBound)) {
        return false;
      }
    }
    return true;
  }

}
