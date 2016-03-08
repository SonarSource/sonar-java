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

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TypeInferenceSolver {

  static class TypeInference {
    final List<JavaType> inferedTypes;
    final TypeSubstitution substitution;

    private TypeInference(List<JavaType> inferedTypes, TypeSubstitution substitution) {
      this.inferedTypes = inferedTypes;
      this.substitution = substitution;
    }
  }

  private final ParametrizedTypeCache parametrizedTypeCache;
  private final Symbols symbols;

  public TypeInferenceSolver(ParametrizedTypeCache parametrizedTypeCache, Symbols symbols) {
    this.parametrizedTypeCache = parametrizedTypeCache;
    this.symbols = symbols;
  }

  @CheckForNull
  TypeInference inferTypes(JavaSymbol.MethodJavaSymbol method, JavaType site, List<JavaType> typeParams) {
    List<JavaType> inferedTypes = ((JavaType.MethodJavaType) method.type).argTypes;
    TypeSubstitution substitution = new TypeSubstitution();
    if (!inferedTypes.isEmpty()) {
      if (isParametrizedType(site)) {
        inferedTypes = applySubstitution(inferedTypes, ((JavaType.ParametrizedTypeJavaType) site).typeSubstitution);
      }
      if (method.isParametrized() && !typeParams.isEmpty()) {
        substitution = getSubstitutionFromTypeParams(method.typeVariableTypes, typeParams);
        if (substitution.size() == 0) {
          // no substitution possible
          return null;
        }
        inferedTypes = applySubstitution(inferedTypes, substitution);
      }
    }
    return new TypeInference(inferedTypes, substitution);
  }

  JavaType inferReturnType(JavaSymbol.MethodJavaSymbol method, JavaType site, List<JavaType> typeParams) {
    JavaType resultType = applySubstitution(((JavaType.MethodJavaType) method.type).resultType, site);
    TypeSubstitution substitution = getSubstitutionFromTypeParams(method.typeVariableTypes, typeParams);
    return applySubstitution(resultType, substitution);
  }

  JavaType applySubstitution(JavaType type, JavaType site) {
    if (isParametrizedType(site)) {
      return applySubstitution(type, ((JavaType.ParametrizedTypeJavaType) site).typeSubstitution);
    }
    return type;
  }

  List<JavaType> applySubstitution(List<JavaType> types, TypeSubstitution substitution) {
    if (substitution.size() == 0) {
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
      return applySubstitution((JavaType.ParametrizedTypeJavaType) type, substitution);
    }
    if (isWildcardType(type)) {
      return applySubstitution((JavaType.WildCardType) type, substitution);
    }
    if (isArrayType(type)) {
      return applySubstitution((JavaType.ArrayJavaType) type, substitution);
    }
    return type;
  }

  private static boolean isParametrizedType(JavaType type) {
    return type instanceof JavaType.ParametrizedTypeJavaType;
  }

  private static boolean isWildcardType(JavaType type) {
    return type instanceof JavaType.WildCardType;
  }

  private static boolean isArrayType(JavaType type) {
    return type instanceof JavaType.ArrayJavaType;
  }

  private JavaType applySubstitution(JavaType.ParametrizedTypeJavaType type, TypeSubstitution substitution) {
    TypeSubstitution newSubstitution = new TypeSubstitution();
    for (Map.Entry<JavaType.TypeVariableJavaType, JavaType> entry : type.typeSubstitution.substitutionEntries()) {
      newSubstitution.add(entry.getKey(), applySubstitution(entry.getValue(), substitution));
    }
    return parametrizedTypeCache.getParametrizedTypeType(type.rawType.getSymbol(), newSubstitution);
  }

  private JavaType applySubstitution(JavaType.WildCardType wildcard, TypeSubstitution substitution) {
    JavaType substitutedType = substitution.substitutedType(wildcard.bound);
    if (substitutedType != null) {
      return parametrizedTypeCache.getWildcardType(substitutedType, wildcard.boundType);
    }
    return wildcard;
  }

  private JavaType applySubstitution(JavaType.ArrayJavaType arrayType, TypeSubstitution substitution) {
    JavaType rootElementType = arrayType.elementType;
    int nbDimensions = 1;
    while (rootElementType.isTagged(JavaType.ARRAY)) {
      rootElementType = ((JavaType.ArrayJavaType) rootElementType).elementType;
      nbDimensions++;
    }
    JavaType substitutedType = substitution.substitutedType(rootElementType);
    if (substitutedType != null) {
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
      if (!isValidSubtitution(substitution)) {
        // substitution discarded
        return new TypeSubstitution();
      }
    }
    return substitution;
  }

  private static boolean isValidSubtitution(TypeSubstitution substitutions) {
    for (Map.Entry<JavaType.TypeVariableJavaType, JavaType> substitution : substitutions.substitutionEntries()) {
      if (!isValidSubstitution(substitutions, substitution.getKey(), substitution.getValue())) {
        return false;
      }
    }
    return true;
  }

  private static boolean isValidSubstitution(TypeSubstitution candidate, JavaType.TypeVariableJavaType typeVar, JavaType typeParam) {
    for (JavaType bound : typeVar.bounds) {
      while (bound.isTagged(JavaType.TYPEVAR)) {
        bound = candidate.substitutedType(bound);
        if (bound == null) {
          return false;
        }
      }
      if (!typeParam.isSubtypeOf(bound)) {
        return false;
      }
    }
    return true;
  }

}
