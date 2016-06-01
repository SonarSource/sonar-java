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

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class TypeInferenceSolverTest {

  private Symbols symbols;
  private ParametrizedTypeCache parametrizedTypeCache;
  private TypeInferenceSolver typeInferenceSolver;

  private static TypeVariableJavaType T;

  @Before
  public void setUp() {
    parametrizedTypeCache = new ParametrizedTypeCache();
    symbols = new Symbols(new BytecodeCompleter(Lists.<java.io.File>newArrayList(), parametrizedTypeCache));
    typeInferenceSolver = new TypeInferenceSolver(symbols);
    T = getTypeVariable("T");
  }

  @Test
  public void inferTypeSubstitution_always_return_first_type_match() {
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
  public void inferTypeSubstitution_missing_varargs() {
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
  public void inferTypeSubstitution_varargs_and_generics() {
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

    // formals = A<{X=? extends T}>[]
    List<JavaType> formals = Lists.<JavaType>newArrayList(new ArrayJavaType(aWCextendsTType, symbols.arrayClass));

    // only raw types: args = A, A
    List<JavaType> args = Lists.<JavaType>newArrayList(aRawType, aRawType);
    TypeSubstitution substitution = typeSubstitutionForTypeParametersWithVarargs(formals, args, T);
    assertThat(substitution.substitutedType(T)).isSameAs(symbols.objectType);

    // raw type with generic type : A, A<String>
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

  private TypeVariableJavaType getTypeVariable(String variableName) {
    TypeVariableJavaType typeVariableJavaType = new TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol(variableName, Symbols.unknownSymbol));
    typeVariableJavaType.bounds = ImmutableList.of(symbols.objectType);
    return typeVariableJavaType;
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
    return typeInferenceSolver.inferTypeSubstitution(methodSymbol, formals, args);
  }
}
