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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol;
import org.sonar.java.resolve.WildCardType.BoundType;
import org.sonar.plugins.java.api.semantic.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TypeInferenceSolverTest {

  private Symbols symbols;
  private ParametrizedTypeCache parametrizedTypeCache;
  private TypeInferenceSolver typeInferenceSolver;

  private static TypeVariableJavaType T;

  @Before
  public void setUp() {
    parametrizedTypeCache = new ParametrizedTypeCache();
    symbols = new Symbols(new BytecodeCompleter(new SquidClassLoader(Collections.emptyList()), parametrizedTypeCache));
    TypeSubstitutionSolver typeSubstitutionSolver = new TypeSubstitutionSolver(parametrizedTypeCache, symbols);
    LeastUpperBound lub = new LeastUpperBound(typeSubstitutionSolver, parametrizedTypeCache, symbols);
    typeInferenceSolver = new TypeInferenceSolver(lub, symbols, typeSubstitutionSolver);
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
    JavaType aType = createType("A", symbols.objectType);

    // A<{X=X}>
    JavaType aXType = parametrizedTypeCache.getParametrizedTypeType(aType.symbol, new TypeSubstitution().add(X, X));
    // A
    JavaType aRawType = aXType.erasure();
    // A<{X=? extends T}>
    JavaType aWCextendsTType = parametrizedTypeCache.getParametrizedTypeType(aType.symbol, new TypeSubstitution().add(X, new WildCardType(T, BoundType.EXTENDS)));

    // formals = A<{X=? extends T}>[]
    List<JavaType> formals = Lists.<JavaType>newArrayList(new ArrayJavaType(aWCextendsTType, symbols.arrayClass));

    // only raw types: args = A, A
    List<JavaType> args = Lists.<JavaType>newArrayList(aRawType, aRawType);
    TypeSubstitution substitution = typeSubstitutionForTypeParametersWithVarargs(formals, args, T);
    assertThat(substitution.substitutedType(T)).isNull();
    assertThat(substitution.isUnchecked()).isTrue();

    // raw type with generic type : A, A<String>
    args = Lists.<JavaType>newArrayList(aRawType, parametrizedTypeCache.getParametrizedTypeType(aType.symbol, new TypeSubstitution().add(X, symbols.stringType)));
    substitution = typeSubstitutionForTypeParametersWithVarargs(formals, args, T);
    assertThat(substitution.substitutedType(T)).isNull();
    assertThat(substitution.isUnchecked()).isTrue();
  }

  @Test
  public void inferTypeSubstitution_varargs() {
    JavaType aType = createType("A", symbols.objectType);

    // B <: A
    JavaType bType = createType("B", aType);

    // C <: A
    JavaType cType = createType("C", aType);

    // formals = T[] (varargs)
    List<JavaType> formals = Lists.<JavaType>newArrayList(new ArrayJavaType(T, symbols.arrayClass));

    // args = B, C
    List<JavaType> args = Lists.<JavaType>newArrayList(bType, cType);
    TypeSubstitution substitution = typeSubstitutionForTypeParametersWithVarargs(formals, args, T);
    assertThat(substitution.substitutedType(T)).isSameAs(aType);

    // args = int, long
    args = Lists.<JavaType>newArrayList(symbols.intType, symbols.longType);
    substitution = typeSubstitutionForTypeParametersWithVarargs(formals, args, T);
    assertThat(substitution.substitutedType(T).is("java.lang.Number")).isTrue();
  }

  private JavaType createType(String string, JavaType superType) {
    JavaSymbol.TypeJavaSymbol symbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "A", symbols.defaultPackage);
    ClassJavaType type = (ClassJavaType) symbol.type;
    type.interfaces = Collections.emptyList();
    type.supertype = superType;
    return type;
  }

  @Test
  public void typeSubstitution_with_varargs_and_generics() {
    Result result = Result.createForJavaFile("src/test/files/resolve/ParametrizedMethodAndVarargs");

    JavaType childB = (JavaType) result.symbol("childB").type();
    JavaType childC = (JavaType) result.symbol("childC").type();

    JavaSymbol.MethodJavaSymbol variadicMethod;
    List<JavaType> args;
    TypeSubstitution typeSubstitution;

    variadicMethod = (JavaSymbol.MethodJavaSymbol) result.symbol("bar");
    args = Lists.newArrayList(childB, childB);
    typeSubstitution = inferTypeSubstitution(variadicMethod, args);
    assertThat(typeSubstitution.substitutedType(variadicMethod.typeVariableTypes.get(0)).is("B")).isTrue();

    variadicMethod = (JavaSymbol.MethodJavaSymbol) result.symbol("foo");
    args = Lists.newArrayList(childB, childC);
    typeSubstitution = inferTypeSubstitution(variadicMethod, args);
    assertThat(variadicMethod.usages()).hasSize(1);
    assertThat(typeSubstitution.substitutedType(variadicMethod.typeVariableTypes.get(0)).is("A")).isTrue();
  }

  private TypeSubstitution inferTypeSubstitution(MethodJavaSymbol method, List<JavaType> args) {
    return typeInferenceSolver.inferTypeSubstitution(method, toJavaTypes(method.parameterTypes()), args);
  }

  private List<JavaType> toJavaTypes(List<Type> types) {
    List<JavaType> result = new ArrayList<>(types.size());
    for (Type type : types) {
      result.add((JavaType) type);
    }
    return result;
  }

  private TypeSubstitution typeSubstitutionForTypeParametersWithVarargs(List<JavaType> formals, List<JavaType> args, TypeVariableJavaType... typeParameters) {
    return typeSubstitutionForTypeParameters(formals, args, true, typeParameters);
  }

  private TypeSubstitution typeSubstitutionForTypeParameters(List<JavaType> formals, List<JavaType> args, TypeVariableJavaType... typeParameters) {
    return typeSubstitutionForTypeParameters(formals, args, false, typeParameters);
  }

  private TypeVariableJavaType getTypeVariable(String variableName) {
    TypeVariableJavaType typeVariableJavaType = new TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol(variableName, Symbols.unknownSymbol));
    typeVariableJavaType.bounds = Collections.singletonList(symbols.objectType);
    return typeVariableJavaType;
  }

  private TypeSubstitution typeSubstitutionForTypeParameters(List<JavaType> formals, List<JavaType> args, boolean varargs, TypeVariableJavaType... typeParameters) {
    MethodJavaType methodType = new MethodJavaType(formals, symbols.voidType, Collections.emptyList(), symbols.objectType.symbol);
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
