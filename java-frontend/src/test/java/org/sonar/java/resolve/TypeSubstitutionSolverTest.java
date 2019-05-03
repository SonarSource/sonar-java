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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TypeSubstitutionSolverTest {

  private Symbols symbols;
  private TypeSubstitutionSolver typeSubstitutionSolver;
  private ParametrizedTypeCache parametrizedTypeCache;

  private static TypeVariableJavaType T;

  @Before
  public void setUp() {
    parametrizedTypeCache = new ParametrizedTypeCache();
    symbols = new Symbols(new BytecodeCompleter(new SquidClassLoader(Collections.emptyList()), parametrizedTypeCache));
    typeSubstitutionSolver = new TypeSubstitutionSolver(parametrizedTypeCache, symbols);
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
    ParametrizedTypeJavaType aRoot = new ParametrizedTypeJavaType(aSymbol, aSubs, typeSubstitutionSolver);

    // A<...n-1...<A<T>>...>
    JavaType last = aRoot;
    int n = 10;
    for (int i = 0; i < n; i++) {
      TypeSubstitution newSubs = new TypeSubstitution();
      newSubs.add(k, last);
      last = new ParametrizedTypeJavaType(aSymbol, newSubs, typeSubstitutionSolver);
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
    typeVariableJavaType.bounds = Collections.singletonList(symbols.objectType);
    return typeVariableJavaType;
  }

  @Test
  public void substitutionFromSuperType_from_same_type() {
    Result result = Result.createForJavaFile("src/test/files/sym/TypeSubstitutionSolver");
    JavaType stringType = (JavaType) result.symbol("string").type();
    ParametrizedTypeJavaType aString = (ParametrizedTypeJavaType) result.symbol("aString").type();

    ParametrizedTypeJavaType aX = (ParametrizedTypeJavaType) result.symbol("aX").type();
    TypeVariableJavaType x = aX.typeParameters().get(0);

    TypeSubstitution substitution = TypeSubstitutionSolver.substitutionFromSuperType(aX, aString);
    assertThat(substitution.size()).isEqualTo(1);
    assertThat(substitution.typeVariables()).containsExactly(x);
    assertThat(substitution.substitutedTypes()).containsExactly(stringType);
  }

  @Test
  public void substitutionFromSuperType_direct_inheritance() {
    Result result = Result.createForJavaFile("src/test/files/sym/TypeSubstitutionSolver");
    JavaType stringType = (JavaType) result.symbol("string").type();
    ParametrizedTypeJavaType iString = (ParametrizedTypeJavaType) result.symbol("iString").type();

    ParametrizedTypeJavaType aX = (ParametrizedTypeJavaType) result.symbol("aX").type();
    TypeVariableJavaType x = aX.typeParameters().get(0);

    TypeSubstitution substitution = TypeSubstitutionSolver.substitutionFromSuperType(aX, iString);
    assertThat(substitution.size()).isEqualTo(1);
    assertThat(substitution.typeVariables()).containsExactly(x);
    assertThat(substitution.substitutedTypes()).containsExactly(stringType);
  }

  @Test
  public void substitutionFromSuperType_concrete_type() {
    Result result = Result.createForJavaFile("src/test/files/sym/TypeSubstitutionSolver");
    ParametrizedTypeJavaType lNumber = (ParametrizedTypeJavaType) result.symbol("lNumber").type();

    ParametrizedTypeJavaType aX = (ParametrizedTypeJavaType) result.symbol("aX").type();
    TypeVariableJavaType x = aX.typeParameters().get(0);

    TypeSubstitution substitution = TypeSubstitutionSolver.substitutionFromSuperType(aX, lNumber);
    assertThat(substitution.size()).isEqualTo(1);
    assertThat(substitution.typeVariables()).containsExactly(x);
    assertThat(substitution.substitutedTypes()).containsExactly(x);
  }

  @Test
  public void substitutionFromSuperType_from_non_related_types_does_nothing() {
    Result result = Result.createForJavaFile("src/test/files/sym/TypeSubstitutionSolver");
    ParametrizedTypeJavaType jStringInteger = (ParametrizedTypeJavaType) result.symbol("jStringInteger").type();

    ParametrizedTypeJavaType bXY = (ParametrizedTypeJavaType) result.symbol("bXY").type();
    TypeVariableJavaType x = bXY.typeParameters().get(0);
    TypeVariableJavaType y = bXY.typeParameters().get(1);

    TypeSubstitution substitution = TypeSubstitutionSolver.substitutionFromSuperType(bXY, jStringInteger);
    assertThat(substitution.size()).isEqualTo(2);
    assertThat(substitution.typeVariables()).containsExactly(x, y);
    assertThat(substitution.substitutedTypes()).containsExactly(x, y);
  }

  @Test
  public void substitutionFromSuperType_with_multiple_variables() {
    Result result = Result.createForJavaFile("src/test/files/sym/TypeSubstitutionSolver");
    Type stringType = result.symbol("string").type();
    Type integerType = result.symbol("integer").type();
    ParametrizedTypeJavaType jStringInteger = (ParametrizedTypeJavaType) result.symbol("jStringInteger").type();

    ParametrizedTypeJavaType cXY = (ParametrizedTypeJavaType) result.symbol("cXY").type();
    TypeVariableJavaType x = cXY.typeParameters().get(0);
    TypeVariableJavaType y = cXY.typeParameters().get(1);

    TypeSubstitution substitution = TypeSubstitutionSolver.substitutionFromSuperType(cXY, jStringInteger);
    assertThat(substitution.size()).isEqualTo(2);
    assertThat(substitution.typeVariables()).containsExactly(x, y);
    assertThat(substitution.substitutedType(x)).isSameAs(stringType);
    assertThat(substitution.substitutedType(y)).isSameAs(integerType);
  }

  @Test
  public void substitutionFromSuperType_with_2_level_inheritance() {
    Result result = Result.createForJavaFile("src/test/files/sym/TypeSubstitutionSolver");
    JavaType stringType = (JavaType) result.symbol("string").type();
    ParametrizedTypeJavaType iString = (ParametrizedTypeJavaType) result.symbol("iString").type();

    ParametrizedTypeJavaType dV = (ParametrizedTypeJavaType) result.symbol("dX").type();
    TypeVariableJavaType v = dV.typeParameters().get(0);

    TypeSubstitution substitution = TypeSubstitutionSolver.substitutionFromSuperType(dV, iString);
    assertThat(substitution.size()).isEqualTo(1);
    assertThat(substitution.typeVariables()).containsExactly(v);
    assertThat(substitution.substitutedTypes()).containsExactly(stringType);
  }

  @Test
  public void substitutionFromSuperType_complex_inheritance_and_multiple_variables() {
    Result result = Result.createForJavaFile("src/test/files/sym/TypeSubstitutionSolver");
    Type stringType = result.symbol("string").type();
    Type integerType = result.symbol("integer").type();
    ParametrizedTypeJavaType jStringInteger = (ParametrizedTypeJavaType) result.symbol("jStringInteger").type();

    ParametrizedTypeJavaType fWXYZ = (ParametrizedTypeJavaType) result.symbol("fWXYZ").type();
    TypeVariableJavaType w = fWXYZ.typeParameters().get(0);
    TypeVariableJavaType x = fWXYZ.typeParameters().get(1);
    TypeVariableJavaType y = fWXYZ.typeParameters().get(2);
    TypeVariableJavaType z = fWXYZ.typeParameters().get(3);

    TypeSubstitution substitution = TypeSubstitutionSolver.substitutionFromSuperType(fWXYZ, jStringInteger);
    assertThat(substitution.size()).isEqualTo(4);
    assertThat(substitution.typeVariables()).containsExactly(w, x, y, z);
    assertThat(substitution.substitutedType(w)).isSameAs(stringType);
    assertThat(substitution.substitutedType(x)).isSameAs(x);
    assertThat(substitution.substitutedType(y)).isSameAs(y);
    assertThat(substitution.substitutedType(z)).isSameAs(integerType);
  }

  @Test
  public void test_no_infinite_recursion_on_validation() throws Exception {
    Result result = Result.createForJavaFile("src/test/files/sym/TypeSubstitutionSolver");
    JavaType inst = result.symbol("inst").type;
    assertThat(inst.isParameterized()).isTrue();
    List<TypeVariableJavaType> typeVariableJavaTypes = ((ParametrizedTypeJavaType) inst).typeParameters();
    assertThat(typeVariableJavaTypes).hasSize(2);
    assertThat(((ParametrizedTypeJavaType) inst).substitution(typeVariableJavaTypes.get(0)).is("java.lang.Object")).isTrue();
    assertThat(((ParametrizedTypeJavaType) inst).substitution(typeVariableJavaTypes.get(1)).is("java.lang.Boolean")).isTrue();

  }

  @Test
  public void inference_on_parameters_supertypes() throws Exception {
    Result result = Result.createForJavaFile("src/test/files/sym/TypeInferenceOnSupertypes");
    assertThat(result.reference(6, 5)).isSameAs(result.symbol("foo"));
    Type type = result.referenceTree(6, 5).symbolType();
    assertThat(((MethodJavaType) type).resultType.is("java.lang.String")).isTrue();
  }

  @Test
  public void compute_function_types() throws Exception {
    Result result = Result.createForJavaFile("src/test/files/sym/FunctionTypes");
    ParametrizedTypeJavaType lowerBound = (ParametrizedTypeJavaType) result.symbol("lowerBound").type;
    ParametrizedTypeJavaType upperBound = (ParametrizedTypeJavaType) result.symbol("upperBound").type;
    ParametrizedTypeJavaType unbounded = (ParametrizedTypeJavaType) result.symbol("unbounded").type;
    ParametrizedTypeJavaType ref = (ParametrizedTypeJavaType) result.symbol("ref").type;

    ParametrizedTypeJavaType lowerBoundFuncType = (ParametrizedTypeJavaType) typeSubstitutionSolver.functionType(lowerBound);
    assertThat(lowerBoundFuncType.rawType).isEqualTo(ref.rawType);
    assertThat(lowerBoundFuncType.typeSubstitution.substitutedTypes()).containsExactly(ref.typeSubstitution.substitutedTypes().get(0));

    ParametrizedTypeJavaType upperBoundFuncType = (ParametrizedTypeJavaType) typeSubstitutionSolver.functionType(upperBound);
    assertThat(upperBoundFuncType.rawType).isEqualTo(ref.rawType);
    assertThat(upperBoundFuncType.typeSubstitution.substitutedTypes()).containsExactly(ref.typeSubstitution.substitutedTypes().get(0));

    ParametrizedTypeJavaType unboundFuncType = (ParametrizedTypeJavaType) typeSubstitutionSolver.functionType(unbounded);
    assertThat(unboundFuncType.rawType).isEqualTo(ref.rawType);
    assertThat(unboundFuncType.typeSubstitution.substitutedTypes()).hasSize(1);
    assertThat(unboundFuncType.typeSubstitution.substitutedTypes().get(0).is("java.lang.Object")).isTrue();

    ParametrizedTypeJavaType emptySubstitution = (ParametrizedTypeJavaType) parametrizedTypeCache.getParametrizedTypeType(ref.symbol, new TypeSubstitution());
    assertThat(typeSubstitutionSolver.functionType(emptySubstitution)).isEqualTo(emptySubstitution);
  }

  @Test
  public void substitute_thrown_types() throws Exception {
    Result result = Result.createForJavaFile("src/test/files/sym/ThrownTypeVariables");
    IdentifierTree fooInvocation = result.referenceTree(6, 34);
    assertThat(((MethodJavaType) fooInvocation.symbolType()).thrown).hasSize(1);
    assertThat(((MethodJavaType) fooInvocation.symbolType()).thrown.get(0).is("java.io.IOException")).isTrue();

    IdentifierTree barInvocation1 = result.referenceTree(23, 21);
    assertThat(((MethodJavaType) barInvocation1.symbolType()).thrown).hasSize(1);
    assertThat(((MethodJavaType) barInvocation1.symbolType()).thrown.get(0).is("java.util.NoSuchElementException")).isTrue();

    IdentifierTree barInvocation2 = result.referenceTree(24, 21);
    assertThat(((MethodJavaType) barInvocation2.symbolType()).thrown).hasSize(1);
    assertThat(((MethodJavaType) barInvocation2.symbolType()).thrown.get(0).is("java.io.IOException")).isTrue();
  }

  @Test
  public void type_hierarchy_visit_should_be_limited() {
    ParametrizedTypeCache parametrizedTypeCache = new ParametrizedTypeCache();
    BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(new SquidClassLoader(new ArrayList<>()), parametrizedTypeCache);
    Symbols symbols = new Symbols(bytecodeCompleter);

    ActionParser<Tree> parser = JavaParser.createParser();
    SemanticModel semanticModel = new SemanticModel(bytecodeCompleter);
    Resolve resolve = new Resolve(symbols, bytecodeCompleter, parametrizedTypeCache);
    TypeAndReferenceSolver typeAndReferenceSolver = new TypeAndReferenceSolver(semanticModel, symbols, resolve, parametrizedTypeCache);
    CompilationUnitTree tree = (CompilationUnitTree) parser.parse(new File("src/test/files/sym/ComplexHierarchy.java"));
    new FirstPass(semanticModel, symbols, resolve, parametrizedTypeCache, typeAndReferenceSolver).visitCompilationUnit(tree);
    typeAndReferenceSolver.visitCompilationUnit(tree);

    ClassTree classTree = (ClassTree) tree.types().get(tree.types().size() - 1);
    JavaType site = (JavaType) classTree.symbol().type();
    MethodInvocationTree mit = (MethodInvocationTree) ((ExpressionStatementTree) ((MethodTree) classTree.members().get(0)).block().body().get(0)).expression();

    TypeSubstitutionSolver typeSubstitutionSolver = Mockito.spy(new TypeSubstitutionSolver(parametrizedTypeCache, symbols));
    // call with empty formals should return.
    typeSubstitutionSolver.applySiteSubstitutionToFormalParameters(new ArrayList<>(), site);
    verify(typeSubstitutionSolver, times(0)).applySiteSubstitutionToFormalParameters(anyList(), any(JavaType.class), anySet());

    JavaSymbol.MethodJavaSymbol methodJavaSymbol = (JavaSymbol.MethodJavaSymbol) mit.symbol();
    typeSubstitutionSolver.applySiteSubstitutionToFormalParameters(((MethodJavaType) methodJavaSymbol.type).argTypes, site);
    verify(typeSubstitutionSolver, times(11)).applySiteSubstitutionToFormalParameters(anyList(), any(JavaType.class), anySet());
  }
}
