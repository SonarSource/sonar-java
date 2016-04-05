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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class TypeSubstitutionSolverTest {

  private Symbols symbols;
  private TypeSubstitutionSolver typeSubstitutionSolver;

  private static final JavaType.TypeVariableJavaType T = getTypeVariable("T");

  @Before
  public void setUp() {
    ParametrizedTypeCache ptc = new ParametrizedTypeCache();
    symbols = new Symbols(new BytecodeCompleter(Lists.<java.io.File>newArrayList(), ptc));
    typeSubstitutionSolver = new TypeSubstitutionSolver(ptc, symbols);
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
  public void applySubstitution_on_nested_parametrized_types() {
    JavaType.TypeVariableJavaType k = getTypeVariable("K");

    TypeSubstitution aSubs = new TypeSubstitution();
    aSubs.add(k, T);
    JavaSymbol.TypeJavaSymbol aSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "A", Symbols.rootPackage);
    // A<{K=T}>
    JavaType.ParametrizedTypeJavaType aRoot = new JavaType.ParametrizedTypeJavaType(aSymbol, aSubs);

    // A<...n-1...<A<T>>...>
    JavaType last = aRoot;
    int n = 10;
    for (int i = 0; i < n; i++) {
      TypeSubstitution newSubs = new TypeSubstitution();
      newSubs.add(k, last);
      last = new JavaType.ParametrizedTypeJavaType(aSymbol, newSubs);
    }

    List<JavaType> formals = Lists.newArrayList(last);
    TypeSubstitution substitution = new TypeSubstitution();
    substitution.add(T, symbols.stringType);
    List<JavaType> substitutedFormals = typeSubstitutionSolver.applySubstitutionToFormalParameters(formals, substitution);

    JavaType type = substitutedFormals.get(0);
    int nbNestedGenerics = 0;
    while (type instanceof JavaType.ParametrizedTypeJavaType) {
      type = ((JavaType.ParametrizedTypeJavaType) type).substitution(k);
      nbNestedGenerics++;
    }
    assertThat(nbNestedGenerics).isEqualTo(n + 1);
    assertThat(type).isEqualTo(symbols.stringType);
  }

  @Test
  public void getSubstitutionFromTypeParams_does_not_provide_substitution_if_arity_of_params_is_not_matching() {
    ArrayList<JavaType.TypeVariableJavaType> typeVariableTypes = Lists.newArrayList(T, T);
    ArrayList<JavaType> typeParams = Lists.newArrayList(symbols.stringType);

    TypeSubstitution substitution = typeSubstitutionSolver.getSubstitutionFromTypeParams(typeVariableTypes, typeParams);
    assertThat(substitution.size()).isEqualTo(0);
  }

  @Test
  public void getSubstitutionFromTypeParams_provide_substitution() {
    JavaType.TypeVariableJavaType j = getTypeVariable("J");
    JavaType.TypeVariableJavaType k = getTypeVariable("K");
    ArrayList<JavaType.TypeVariableJavaType> typeVariableTypes = Lists.newArrayList(j, k);
    ArrayList<JavaType> typeParams = Lists.newArrayList(symbols.stringType, symbols.intType.primitiveWrapperType);

    TypeSubstitution substitution = typeSubstitutionSolver.getSubstitutionFromTypeParams(typeVariableTypes, typeParams);
    assertThat(substitution.size()).isEqualTo(2);
    assertThat(substitution.substitutedType(j)).isSameAs(symbols.stringType);
    assertThat(substitution.substitutedType(k)).isSameAs(symbols.intType.primitiveWrapperType);
  }

  private static JavaType.TypeVariableJavaType getTypeVariable(String variableName) {
    return new JavaType.TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol(variableName, Symbols.unknownSymbol));
  }
}
