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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.sonar.java.resolve.WildCardType.BoundType;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class TypeSubstitutionSolverTest {

  private Symbols symbols;
  private TypeSubstitutionSolver typeSubstitutionSolver;
  private ParametrizedTypeCache parametrizedTypeCache;

  private static TypeVariableJavaType T;

  @Before
  public void setUp() {
    ParametrizedTypeCache ptc = new ParametrizedTypeCache();
    symbols = new Symbols(new BytecodeCompleter(Lists.<java.io.File>newArrayList(), ptc));
    typeSubstitutionSolver = new TypeSubstitutionSolver(ptc, symbols);
    parametrizedTypeCache = new ParametrizedTypeCache();
    T = getTypeVariable("T");
  }

  @Test
  public void applySubstitution_on_empty_list_of_parameters_has_no_effect() {
    List<JavaType> formals = new ArrayList<>();
    TypeSubstitution substitution = new TypeSubstitution();
    substitution.add(T, symbols.stringType);
    List<JavaType> substitutedFormals = typeSubstitutionSolver.applySubstitutionToFormalParameters(formals, substitution);

    assertThat(substitutedFormals).isEmpty();
    assertThat(substitutedFormals).isSameAs(formals);
  }

  @Test
  public void applySubstitution_on_list_of_parameters_has_no_effect_with_empty_substitution() {
    List<JavaType> formals = Lists.newArrayList(symbols.stringType);
    List<JavaType> substitutedFormals = typeSubstitutionSolver.applySubstitutionToFormalParameters(formals, new TypeSubstitution());

    assertThat(substitutedFormals).isSameAs(formals);
  }

  @Test
  public void applySubstitution_on_simple_types() {
    List<JavaType> formals = Lists.newArrayList((JavaType) T);
    TypeSubstitution substitution = new TypeSubstitution();
    substitution.add(T, symbols.stringType);
    List<JavaType> substitutedFormals = typeSubstitutionSolver.applySubstitutionToFormalParameters(formals, substitution);

    assertThat(substitutedFormals).hasSize(formals.size());
    assertThat(substitutedFormals).containsExactly(symbols.stringType);
  }

  @Test
  public void applySubstitution_on_array_with_generic() {
    TypeVariableJavaType X = getTypeVariable("X");
    JavaSymbol.TypeJavaSymbol aSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "A", symbols.defaultPackage);

    // A<{X=T}>
    JavaType aXT = parametrizedTypeCache.getParametrizedTypeType(aSymbol, new TypeSubstitution().add(X, T));
    // A<T>[]
    JavaType formal1 = new ArrayJavaType(aXT, symbols.arrayClass);
    // A<T>[][]
    JavaType formal2 = new ArrayJavaType(new ArrayJavaType(aXT, symbols.arrayClass), symbols.arrayClass);

    List<JavaType> substitutedTypes = typeSubstitutionSolver.applySubstitutionToFormalParameters(ImmutableList.of(formal1, formal2),
      new TypeSubstitution().add(T, symbols.stringType));

    JavaType substituted1 = substitutedTypes.get(0);
    assertThat(substituted1).isInstanceOf(ArrayJavaType.class);
    JavaType elementType = ((ArrayJavaType) substituted1).elementType;
    assertThat(elementType).isInstanceOf(ParametrizedTypeJavaType.class);
    ParametrizedTypeJavaType ptt = (ParametrizedTypeJavaType) elementType;
    assertThat(ptt.substitution(X)).isSameAs(symbols.stringType);

    JavaType substituted2 = substitutedTypes.get(1);
    assertThat(substituted2).isInstanceOf(ArrayJavaType.class);
    elementType = ((ArrayJavaType) ((ArrayJavaType) substituted2).elementType).elementType;
    assertThat(elementType).isInstanceOf(ParametrizedTypeJavaType.class);
    ptt = (ParametrizedTypeJavaType) elementType;
    assertThat(ptt.substitution(X)).isSameAs(symbols.stringType);
  }

  @Test
  public void applySubstitution_on_nested_parametrized_types() {
    TypeVariableJavaType k = getTypeVariable("K");

    TypeSubstitution aSubs = new TypeSubstitution();
    aSubs.add(k, T);
    JavaSymbol.TypeJavaSymbol aSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "A", Symbols.rootPackage);
    // A<{K=T}>
    ParametrizedTypeJavaType aRoot = new ParametrizedTypeJavaType(aSymbol, aSubs);

    // A<...n-1...<A<T>>...>
    JavaType last = aRoot;
    int n = 10;
    for (int i = 0; i < n; i++) {
      TypeSubstitution newSubs = new TypeSubstitution();
      newSubs.add(k, last);
      last = new ParametrizedTypeJavaType(aSymbol, newSubs);
    }

    List<JavaType> formals = Lists.newArrayList(last);
    TypeSubstitution substitution = new TypeSubstitution();
    substitution.add(T, symbols.stringType);
    List<JavaType> substitutedFormals = typeSubstitutionSolver.applySubstitutionToFormalParameters(formals, substitution);

    JavaType type = substitutedFormals.get(0);
    int nbNestedGenerics = 0;
    while (type instanceof ParametrizedTypeJavaType) {
      type = ((ParametrizedTypeJavaType) type).substitution(k);
      nbNestedGenerics++;
    }
    assertThat(nbNestedGenerics).isEqualTo(n + 1);
    assertThat(type).isEqualTo(symbols.stringType);
  }

  @Test
  public void getSubstitutionFromTypeParams_does_not_provide_substitution_if_arity_of_params_is_not_matching() {
    ArrayList<TypeVariableJavaType> typeVariableTypes = Lists.newArrayList(T, T);
    ArrayList<JavaType> typeParams = Lists.newArrayList(symbols.stringType);

    TypeSubstitution substitution = typeSubstitutionSolver.getSubstitutionFromTypeParams(typeVariableTypes, typeParams);
    assertThat(substitution.size()).isEqualTo(0);
  }

  @Test
  public void getTypeSubstitution_always_return_first_type_match() {
    TypeVariableJavaType U = getTypeVariable("U");
    List<JavaType> formals = Lists.<JavaType>newArrayList(T, T, U);

    List<JavaType> args = Lists.newArrayList(symbols.stringType, symbols.objectType, symbols.intType.primitiveWrapperType);
    TypeSubstitution substitution = typeSubstitutionForTypeParameters(formals, args, T, U);
    assertThat(substitution.substitutedType(T)).isSameAs(symbols.stringType);
    assertThat(substitution.substitutedType(U)).isSameAs(symbols.intType.primitiveWrapperType);

    args = Lists.newArrayList(symbols.objectType, symbols.stringType, symbols.intType.primitiveWrapperType);
    substitution = typeSubstitutionForTypeParameters(formals, args, T, U);
    assertThat(substitution.substitutedType(T)).isSameAs(symbols.objectType);
    assertThat(substitution.substitutedType(U)).isSameAs(symbols.intType.primitiveWrapperType);
  }

  @Test
  public void getTypeSubstitution_missing_varargs() {
    TypeVariableJavaType U = getTypeVariable("U");
    List<JavaType> formals = Lists.<JavaType>newArrayList(T, new ArrayJavaType(U, symbols.arrayClass));

    // 2nd parameter not provided, U is infered as Object
    List<JavaType> args = Lists.newArrayList(symbols.stringType);
    TypeSubstitution substitution = typeSubstitutionForTypeParametersWithVarargs(formals, args, T, U);
    assertThat(substitution.substitutedType(T)).isSameAs(symbols.stringType);
    assertThat(substitution.substitutedType(U)).isSameAs(symbols.objectType);

    // 2nd parameter provided
    args = Lists.newArrayList(symbols.stringType, symbols.intType.primitiveWrapperType);
    substitution = typeSubstitutionForTypeParametersWithVarargs(formals, args, T, U);
    assertThat(substitution.substitutedType(T)).isSameAs(symbols.stringType);
    assertThat(substitution.substitutedType(U)).isSameAs(symbols.intType.primitiveWrapperType);
  }

  @Test
  public void getTypeSubstitution_varargs_and_generics() {
    TypeVariableJavaType X = getTypeVariable("X");
    JavaSymbol.TypeJavaSymbol aSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "A", symbols.defaultPackage);
    ClassJavaType aType = (ClassJavaType) aSymbol.type;
    aType.interfaces = ImmutableList.of();
    aType.supertype = symbols.objectType;

    // A<{X=X}>
    JavaType aXType = parametrizedTypeCache.getParametrizedTypeType(aSymbol, new TypeSubstitution().add(X, X));
    // A
    JavaType aRawType = aXType.erasure();
    // A<{X=? extends T}>
    JavaType aWCextendsTType = parametrizedTypeCache.getParametrizedTypeType(aSymbol, new TypeSubstitution().add(X, new WildCardType(T, BoundType.EXTENDS)));

    // A<{X=? extends T}>[]
    List<JavaType> formals = Lists.<JavaType>newArrayList(new ArrayJavaType(aWCextendsTType, symbols.arrayClass));

    // only raw types
    List<JavaType> args = Lists.<JavaType>newArrayList(aRawType, aRawType);
    TypeSubstitution substitution = typeSubstitutionForTypeParametersWithVarargs(formals, args, T);
    assertThat(substitution.substitutedType(T)).isSameAs(symbols.objectType);

    // raw type with generic type
    args = Lists.<JavaType>newArrayList(aRawType, parametrizedTypeCache.getParametrizedTypeType(aSymbol, new TypeSubstitution().add(X, symbols.stringType)));
    substitution = typeSubstitutionForTypeParametersWithVarargs(formals, args, T);
    assertThat(substitution.substitutedType(T)).isSameAs(symbols.objectType);
  }

  private TypeSubstitution typeSubstitutionForTypeParametersWithVarargs(List<JavaType> formals, List<JavaType> args, TypeVariableJavaType... typeParameters) {
    return typeSubstitutionForTypeParameters(formals, args, true, typeParameters);
  }

  private TypeSubstitution typeSubstitutionForTypeParameters(List<JavaType> formals, List<JavaType> args, TypeVariableJavaType... typeParameters) {
    return typeSubstitutionForTypeParameters(formals, args, false, typeParameters);
  }

  private TypeSubstitution typeSubstitutionForTypeParameters(List<JavaType> formals, List<JavaType> args, boolean varargs, TypeVariableJavaType... typeParameters) {
    MethodJavaType methodType = new MethodJavaType(formals, symbols.voidType, ImmutableList.<JavaType>of(), symbols.objectType.symbol);
    int flags = Flags.PUBLIC;
    if (varargs) {
      flags |= Flags.VARARGS;
    }
    JavaSymbol.MethodJavaSymbol methodSymbol = new JavaSymbol.MethodJavaSymbol(flags, "foo", methodType, symbols.objectType.symbol);
    for (TypeVariableJavaType typeParameter : typeParameters) {
      methodSymbol.addTypeParameter(typeParameter);
    }
    return typeSubstitutionSolver.getTypeSubstitution(methodSymbol, symbols.objectType, ImmutableList.<JavaType>of(), args);
  }

  @Test
  public void getSubstitutionFromTypeParams_provide_substitution() {
    TypeVariableJavaType j = getTypeVariable("J");
    TypeVariableJavaType k = getTypeVariable("K");
    ArrayList<TypeVariableJavaType> typeVariableTypes = Lists.newArrayList(j, k);
    ArrayList<JavaType> typeParams = Lists.newArrayList(symbols.stringType, symbols.intType.primitiveWrapperType);

    TypeSubstitution substitution = typeSubstitutionSolver.getSubstitutionFromTypeParams(typeVariableTypes, typeParams);
    assertThat(substitution.size()).isEqualTo(2);
    assertThat(substitution.substitutedType(j)).isSameAs(symbols.stringType);
    assertThat(substitution.substitutedType(k)).isSameAs(symbols.intType.primitiveWrapperType);
  }

  private TypeVariableJavaType getTypeVariable(String variableName) {
    TypeVariableJavaType typeVariableJavaType = new TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol(variableName, Symbols.unknownSymbol));
    typeVariableJavaType.bounds = ImmutableList.of(symbols.objectType);
    return typeVariableJavaType;
  }
}
