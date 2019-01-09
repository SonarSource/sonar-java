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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol;
import org.sonar.plugins.java.api.semantic.Type;

public class TypeSubstitutionSolver {

  private final ParametrizedTypeCache parametrizedTypeCache;
  private final Symbols symbols;
  private final LeastUpperBound leastUpperBound;
  private final TypeInferenceSolver typeInferenceSolver;
  private Deque<JavaSymbol.TypeVariableJavaSymbol> typevarExplored = new LinkedList<>();

  public TypeSubstitutionSolver(ParametrizedTypeCache parametrizedTypeCache, Symbols symbols) {
    this.parametrizedTypeCache = parametrizedTypeCache;
    this.symbols = symbols;
    this.leastUpperBound = new LeastUpperBound(this, parametrizedTypeCache, symbols);
    this.typeInferenceSolver = new TypeInferenceSolver(leastUpperBound, symbols, this);
    parametrizedTypeCache.setTypeSubstitutionSolver(this);
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
      if (!isValidSubstitution(substitution, site)) {
        // check for valid substitution in supertypes, null if no valid substitution is found
        return getTypeSubstitutionFromSuperTypes(method, site, typeParams, argTypes);
      }
    }
    return substitution;
  }

  @CheckForNull
  private TypeSubstitution getTypeSubstitutionFromSuperTypes(JavaSymbol.MethodJavaSymbol method, JavaType site, List<JavaType> typeParams, List<JavaType> argTypes) {
    return site.directSuperTypes().stream()
      .filter(Objects::nonNull)
      .map(superType -> getTypeSubstitution(method, superType, typeParams, argTypes))
      .filter(Objects::nonNull)
      .findFirst().orElse(null);
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
    // As per getClass javadoc:
    // The actual result type [of getClass] is Class<? extends |X|> where |X| is the erasure of the static type of the expression on which getClass is called.
    if(defSite == symbols.objectType && "getClass".equals(method.name())) {
      TypeJavaSymbol classSymbol = symbols.classType.symbol;
      JavaType wildcardType = parametrizedTypeCache.getWildcardType(callSite.erasure(), WildCardType.BoundType.EXTENDS);
      resultType = parametrizedTypeCache.getParametrizedTypeType(classSymbol, new TypeSubstitution().add(classSymbol.typeVariableTypes.get(0), wildcardType));
    }
    resultType = applySiteSubstitution(resultType, defSite);
    if (callSite != defSite) {
      resultType = applySiteSubstitution(resultType, callSite);
    }
    resultType = applySubstitution(resultType, substitution);
    if (!isReturnTypeCompletelySubstituted(resultType, method.typeVariableTypes)
      || (method.isConstructor() && !isReturnTypeCompletelySubstituted(resultType, defSite.symbol.typeVariableTypes))) {
      resultType = symbols.deferedType(resultType);
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
    if(formals.isEmpty()) {
      return formals;
    }
    Set<Type> visited = new HashSet<>();
    visited.add(site);
    return applySiteSubstitutionToFormalParameters(formals, site, visited);
  }

  @VisibleForTesting
  List<JavaType> applySiteSubstitutionToFormalParameters(List<JavaType> formals, JavaType site, Set<Type> visited) {
    TypeSubstitution typeSubstitution = new TypeSubstitution();
    if (site.isParameterized()) {
      typeSubstitution = ((ParametrizedTypeJavaType) site).typeSubstitution;
    }
    TypeJavaSymbol siteSymbol = site.getSymbol();
    List<JavaType> newFormals = formals;
    Type superClass = siteSymbol.superClass();
    if (superClass != null) {
      JavaType newSuperClass = applySubstitution((JavaType) superClass, typeSubstitution);
      if(visited.add(newSuperClass)) {
        newFormals = applySiteSubstitutionToFormalParameters(newFormals, newSuperClass, visited);
      }
    }
    for (Type interfaceType : siteSymbol.interfaces()) {
      JavaType newInterfaceType = applySubstitution((JavaType) interfaceType, typeSubstitution);
      if(visited.add(newInterfaceType)) {
        newFormals = applySiteSubstitutionToFormalParameters(newFormals, newInterfaceType, visited);
      }
    }
    return applySubstitutionToFormalParameters(newFormals, typeSubstitution);
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
      List<JavaType> argTypes = methodType.argTypes.stream().map(argType -> applySiteSubstitution(argType, callSite, resolvedTypeDefinition)).collect(Collectors.toList());
      return new MethodJavaType(argTypes, resultType, methodType.thrown, methodType.symbol);
    }

    return applySiteSubstitution(applySiteSubstitution(resolvedType, resolvedTypeDefinition), callSite);
  }

  List<JavaType> applySubstitutionToFormalParameters(List<JavaType> types, TypeSubstitution substitution) {
    if(substitution.isUnchecked()) {
      return types.stream().map(JavaType::erasure).collect(Collectors.toList());
    }
    if (substitution.size() == 0 || types.isEmpty()) {
      return types;
    }
    return types.stream().map(type -> applySubstitution(type, substitution)).collect(Collectors.toList());
  }

  JavaType applySubstitution(JavaType type, TypeSubstitution substitution) {
    JavaType substitutedType = substitution.substitutedType(type);
    if (substitutedType != null) {
      return substitutedType;
    }
    if (type.isTagged(JavaType.TYPEVAR)) {
      return substituteInTypeVar((TypeVariableJavaType) type, substitution);
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

  private JavaType substituteInTypeVar(TypeVariableJavaType typevar, TypeSubstitution substitution) {
    // completing owner of type var to ensure type var's bounds have been computed
    typevar.symbol.owner().complete();
    if(typevarExplored.contains(typevar.symbol) || typevar.bounds == null) {
      return typevar;
    }
    typevarExplored.push((JavaSymbol.TypeVariableJavaSymbol) typevar.symbol);
    List<JavaType> substitutedBounds = typevar.bounds.stream().map(t -> applySubstitution(t, substitution)).collect(Collectors.toList());
    typevarExplored.pop();
    if(substitutedBounds.equals(typevar.bounds)) {
      return typevar;
    }
    TypeVariableJavaType typeVariableJavaType = new TypeVariableJavaType((JavaSymbol.TypeVariableJavaSymbol) typevar.symbol);
    typeVariableJavaType.bounds = substitutedBounds;
    return typeVariableJavaType;
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

  private boolean isValidSubstitution(TypeSubstitution substitutions, JavaType site) {
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
        if (newBound == null && site.isParameterized()) {
          newBound = ((ParametrizedTypeJavaType) site).typeSubstitution.substitutedType(currentBound);
        }
        if (newBound == null) {
          return ((JavaSymbol.TypeJavaSymbol) site.symbol()).typeVariableTypes.contains(currentBound);
        }
        if (currentBound.equals(newBound)) {
          // exploring the same substitution, we cannot deduce anything
          break;
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

  JavaType functionType(ParametrizedTypeJavaType type) {
    TypeSubstitution substitution = new TypeSubstitution();
    for (Map.Entry<TypeVariableJavaType, JavaType> entry : type.typeSubstitution.substitutionEntries()) {
      JavaType value = entry.getValue();
      if(value.isTagged(JavaType.WILDCARD)) {
        // JLS8 9.9 function types
        WildCardType wildcardType = (WildCardType) value;
        switch (wildcardType.boundType) {
          case UNBOUNDED:
            // This is only an approximation of the real bound type (the case with multiple bounds is not covered).
            value = entry.getKey().bounds.get(0);
            break;
          case EXTENDS:
            value = (JavaType) LeastUpperBound.greatestLowerBound(Lists.newArrayList(wildcardType.bound, entry.getKey().bounds.get(0)));
            break;
          case SUPER:
            value = wildcardType.bound;
            break;
        }
      }
      substitution.add(entry.getKey(), value);
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
