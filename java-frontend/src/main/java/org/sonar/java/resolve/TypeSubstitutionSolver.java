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

import com.google.common.collect.Lists;

import org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol;
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
  private final LeastUpperBound leastUpperBound;
  private final TypeInferenceSolver typeInferenceSolver;

  public TypeSubstitutionSolver(ParametrizedTypeCache parametrizedTypeCache, Symbols symbols) {
    this.parametrizedTypeCache = parametrizedTypeCache;
    this.symbols = symbols;
    this.leastUpperBound = new LeastUpperBound(this, parametrizedTypeCache, symbols);
    this.typeInferenceSolver = new TypeInferenceSolver(leastUpperBound, symbols);
  }

  Type leastUpperBound(Set<Type> refTypes) {
    return leastUpperBound.leastUpperBound(refTypes);
  }

  @CheckForNull
  TypeSubstitution getTypeSubstitution(JavaSymbol.MethodJavaSymbol method, JavaType site, List<JavaType> typeParams, List<JavaType> argTypes) {
    List<JavaType> formals = ((MethodJavaType) method.type).argTypes;
    TypeSubstitution substitution = new TypeSubstitution();
    if (method.isParametrized() || constructParametrizedTypeWithoutSubstitution(method, site)) {
      if (!typeParams.isEmpty()) {
        substitution = getSubstitutionFromTypeParams(method.typeVariableTypes, typeParams);
      } else if (formals.isEmpty()) {
        // substitution can not be inferred, as it is not based on arguments, method call is still valid
        return substitution;
      } else {
        formals = applySiteSubstitutionToFormalParameters(formals, site);
        substitution = typeInferenceSolver.inferTypeSubstitution(method, formals, argTypes);
      }
      if (!isValidSubtitution(substitution, site)) {
        // substitution discarded
        return null;
      }
    }
    return substitution;
  }

  private static boolean constructParametrizedTypeWithoutSubstitution(JavaSymbol.MethodJavaSymbol method, JavaType site) {
    return method.isConstructor() && site.isParameterized() && ((ParametrizedTypeJavaType) site).typeSubstitution.isIdentity();
  }

  JavaType getReturnType(@Nullable JavaType returnType, JavaType defSite, JavaType callSite, TypeSubstitution substitution, JavaSymbol.MethodJavaSymbol method) {
    JavaType resultType = returnType;
    if (method.isConstructor()) {
      if (constructParametrizedTypeWithoutSubstitution(method, defSite)) {
        resultType = applySubstitution(defSite, substitution);
      } else {
        return defSite;
      }
    }
    resultType = applySiteSubstitution(resultType, defSite);
    if (callSite != defSite) {
      resultType = applySiteSubstitution(resultType, callSite);
    }
    resultType = applySubstitution(resultType, substitution);
    if (!isReturnTypeCompletelySubstituted(resultType, method.typeVariableTypes)
      || (method.isConstructor() && !isReturnTypeCompletelySubstituted(resultType, defSite.symbol.typeVariableTypes))) {
      resultType = symbols.deferedType(resultType.erasure());
    }
    return resultType;
  }

  private static boolean isReturnTypeCompletelySubstituted(JavaType resultType, List<TypeVariableJavaType> typeVariables){
    if(typeVariables.contains(resultType)) {
      return false;
    }
    if(resultType.isArray()) {
      return isReturnTypeCompletelySubstituted(((ArrayJavaType) resultType).elementType, typeVariables);
    }
    if(resultType.isTagged(JavaType.WILDCARD)) {
      return isReturnTypeCompletelySubstituted(((WildCardType) resultType).bound, typeVariables);
    }
    if (resultType.isParameterized()) {
      for (JavaType substitutedType : ((ParametrizedTypeJavaType) resultType).typeSubstitution.substitutedTypes()) {
        if(!isReturnTypeCompletelySubstituted(substitutedType, typeVariables)) {
          return false;
        }
      }
      
    }
    return true;
  }

  List<JavaType> applySiteSubstitutionToFormalParameters(List<JavaType> formals, JavaType site) {
    if (site.isParameterized()) {
      return applySubstitutionToFormalParameters(formals, ((ParametrizedTypeJavaType) site).typeSubstitution);
    }
    return formals;
  }

  JavaType applySiteSubstitution(JavaType type, JavaType site) {
    if (site.isParameterized()) {
      return applySubstitution(type, ((ParametrizedTypeJavaType) site).typeSubstitution);
    }
    return type;
  }

  JavaType applySiteSubstitution(@Nullable JavaType resolvedType, JavaType callSite, JavaType resolvedTypeDefinition) {
    if (resolvedType == null) {
      // case of constructors
      return null;
    }
    if(resolvedType.isTagged(JavaType.METHOD)) {
      MethodJavaType methodType = (MethodJavaType) resolvedType;
      JavaType resultType = applySiteSubstitution(methodType.resultType, callSite, resolvedTypeDefinition);
      List<JavaType> argTypes = Lists.newArrayList();
      for (JavaType argType : methodType.argTypes) {
        argTypes.add(applySiteSubstitution(argType, callSite, resolvedTypeDefinition));
      }
      return new MethodJavaType(argTypes, resultType, methodType.thrown, methodType.symbol);
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

  JavaType applySubstitution(JavaType type, TypeSubstitution substitution) {
    JavaType substitutedType = substitution.substitutedType(type);
    if (substitutedType != null) {
      return substitutedType;
    }
    if (type.isParameterized()) {
      return substituteInParametrizedType((ParametrizedTypeJavaType) type, substitution);
    }
    if (type.isTagged(JavaType.WILDCARD)) {
      return substituteInWildCardType((WildCardType) type, substitution);
    }
    if (type.isArray()) {
      return substituteInArrayType((ArrayJavaType) type, substitution);
    }
    return type;
  }

  private JavaType substituteInParametrizedType(ParametrizedTypeJavaType type, TypeSubstitution substitution) {
    TypeSubstitution newSubstitution = new TypeSubstitution();
    for (Map.Entry<TypeVariableJavaType, JavaType> entry : type.typeSubstitution.substitutionEntries()) {
      newSubstitution.add(entry.getKey(), applySubstitution(entry.getValue(), substitution));
    }
    return parametrizedTypeCache.getParametrizedTypeType(type.rawType.getSymbol(), newSubstitution);
  }

  private JavaType substituteInWildCardType(WildCardType wildcard, TypeSubstitution substitution) {
    JavaType substitutedType = applySubstitution(wildcard.bound, substitution);
    if (substitutedType != wildcard.bound) {
      return parametrizedTypeCache.getWildcardType(substitutedType, wildcard.boundType);
    }
    return wildcard;
  }

  private JavaType substituteInArrayType(ArrayJavaType arrayType, TypeSubstitution substitution) {
    JavaType rootElementType = arrayType.elementType;
    int nbDimensions = 1;
    while (rootElementType.isArray()) {
      rootElementType = ((ArrayJavaType) rootElementType).elementType;
      nbDimensions++;
    }
    JavaType substitutedType = applySubstitution(rootElementType, substitution);
    if (substitutedType != rootElementType) {
      // FIXME SONARJAVA-1574 a new array type should not be created but reused if already existing for the current element type
      for (int i = 0; i < nbDimensions; i++) {
        substitutedType = new ArrayJavaType(substitutedType, symbols.arrayClass);
      }
      return substitutedType;
    }
    return arrayType;
  }

  TypeSubstitution getSubstitutionFromTypeParams(List<TypeVariableJavaType> typeVariableTypes, List<JavaType> typeParams) {
    TypeSubstitution substitution = new TypeSubstitution();
    if (typeVariableTypes.size() == typeParams.size()) {
      // create naive substitution
      for (int i = 0; i < typeVariableTypes.size(); i++) {
        TypeVariableJavaType typeVariableType = typeVariableTypes.get(i);
        JavaType typeParam = typeParams.get(i);
        substitution.add(typeVariableType, typeParam);
      }
    }
    return substitution;
  }

  private boolean isValidSubtitution(TypeSubstitution substitutions, JavaType site) {
    for (Map.Entry<TypeVariableJavaType, JavaType> substitution : substitutions.substitutionEntries()) {
      if (!isValidSubstitution(substitutions, substitution.getKey(), substitution.getValue(), site)) {
        return false;
      }
    }
    return true;
  }

  private boolean isValidSubstitution(TypeSubstitution candidate, TypeVariableJavaType typeVar, JavaType typeParam, JavaType site) {
    for (JavaType bound : typeVar.bounds) {
      JavaType currentBound = applySubstitution(bound, candidate);
      while (currentBound.isTagged(JavaType.TYPEVAR) && !currentBound.symbol().owner().isMethodSymbol()) {
        JavaType newBound = candidate.substitutedType(currentBound);
        if (currentBound.equals(newBound)) {
          break;
        }
        if (newBound == null && site.isParameterized()) {
          newBound = ((ParametrizedTypeJavaType) site).typeSubstitution.substitutedType(currentBound);
        }
        if (newBound == null) {
          return ((JavaSymbol.TypeJavaSymbol) site.symbol()).typeVariableTypes.contains(currentBound);
        }
        currentBound = newBound;
      }
      if (!isUnboundedWildcard(typeParam) && !typeParam.isSubtypeOf(currentBound)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isUnboundedWildcard(JavaType type) {
    return type.isTagged(JavaType.WILDCARD) && ((WildCardType) type).boundType == WildCardType.BoundType.UNBOUNDED;
  }

  JavaType erasureSubstitution(ParametrizedTypeJavaType type) {
    TypeSubstitution substitution = new TypeSubstitution();
    for (Map.Entry<TypeVariableJavaType, JavaType> entry : type.typeSubstitution.substitutionEntries()) {
      TypeVariableJavaType typeVar = entry.getKey();
      JavaType subs = entry.getValue();
      if (typeVar == subs) {
        subs = subs.erasure();
      }
      substitution.add(typeVar, subs);
    }
    return parametrizedTypeCache.getParametrizedTypeType(type.symbol, substitution);
  }

  static TypeSubstitution substitutionFromSuperType(ParametrizedTypeJavaType target, ParametrizedTypeJavaType source) {
    TypeSubstitution result = new TypeSubstitution(target.typeSubstitution);
    if (target.rawType != source.rawType) {
      TypeJavaSymbol targetSymbol = target.symbol;
      Type superClass = targetSymbol.superClass();
      if (superClass != null && ((JavaType) superClass).isParameterized()) {
        TypeSubstitution newSub = substitutionFromSuperType((ParametrizedTypeJavaType) superClass, source);
        result = result.combine(newSub);
      }
      for (Type superInterface : targetSymbol.interfaces()) {
        if (((JavaType) superInterface).isParameterized()) {
          TypeSubstitution newSub = substitutionFromSuperType((ParametrizedTypeJavaType) superInterface, source);
          result = result.combine(newSub);
        }
      }
    } else {
      result = target.typeSubstitution.combine(source.typeSubstitution);
    }
    return result;
  }
}
