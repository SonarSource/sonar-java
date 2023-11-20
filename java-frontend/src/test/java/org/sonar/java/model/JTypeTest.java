/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.model;

import java.util.Objects;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.java.model.assertions.TypeAssert.assertThat;

class JTypeTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void isArray() {
    assertThat(type("int[]").isArray())
      .isTrue();
    assertThat(type("int").isArray())
      .isFalse();
  }

  @Test
  void isClass() {
    assertAll(
      () ->
        assertThat(type("java.util.HashMap").isClass())
          .as("for classes")
          .isTrue(),
      () ->
        assertThat(type("java.util.Map").isClass())
          .as("for interfaces")
          .isTrue(),
      () ->
        assertThat(type("java.lang.annotation.RetentionPolicy").isClass())
          .as("for enums")
          .isTrue()
    );
  }

  @Test
  void isVoid() {
    assertAll(
      () ->
        assertThat(type("void").isVoid())
          .isTrue(),
      () ->
        assertThat(type("int").isVoid())
          .isFalse()
    );
  }

  @Test
  void isParametrizedType() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m() { new java.util.ArrayList<String>(); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTreeImpl s = (ExpressionStatementTreeImpl) Objects.requireNonNull(m.block()).body().get(0);
    AbstractTypedTree e = Objects.requireNonNull((AbstractTypedTree) s.expression());

    assertThat(cu.sema.type(e.typeBinding).isParameterized())
      .isEqualTo(e.symbolType().isParameterized())
      .isTrue();
  }

  @Test
  void typeArguments() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m() { new java.util.HashMap<Integer, String>(); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTreeImpl s = (ExpressionStatementTreeImpl) Objects.requireNonNull(m.block()).body().get(0);
    AbstractTypedTree e = Objects.requireNonNull((AbstractTypedTree) s.expression());

    assertThat(cu.sema.type(e.typeBinding).typeArguments())
      .hasToString(e.symbolType().typeArguments().toString())
      .hasToString("[Integer, String]");

    assertThat(cu.sema.type(c.typeBinding).typeArguments())
      .isEqualTo(c.symbol().type().typeArguments())
      .isEmpty();
  }

  @Test
  void isPrimitive() {
    assertAll(
      () ->
        assertThat(type("byte").isPrimitive())
          .isTrue(),
      () ->
        assertThat(type("void").isPrimitive())
          .isFalse(),
      () ->
        assertThat(type("byte").isPrimitive(Type.Primitives.BYTE))
          .isTrue(),
      () ->
        assertThat(type("byte").isPrimitive(Type.Primitives.INT))
          .isFalse(),
      () ->
        assertThat(type("void").isPrimitive(Type.Primitives.BYTE))
          .isFalse()
    );
  }

  @Test
  void primitiveType(){
    Type byteType = type("java.lang.Byte");
    assertThat(byteType.primitiveType()).isEqualTo(type("byte"));
    assertThat(byteType.primitiveType()).isNotEqualTo(type("boolean"));
  }

  @Test
  void declaringType(){
    Type byteType = type("java.lang.Byte");
    assertThat(byteType.declaringType()).isEqualTo(byteType);
    assertThat(byteType.declaringType()).isNotEqualTo(type("java.lang.Boolean"));
  }

  @Test
  void isNumerical() {
    assertAll(
      () -> assertThat(type("byte").isNumerical()).isTrue(),
      () -> assertThat(type("char").isNumerical()).isTrue(),
      () -> assertThat(type("short").isNumerical()).isTrue(),
      () -> assertThat(type("int").isNumerical()).isTrue(),
      () -> assertThat(type("long").isNumerical()).isTrue(),
      () -> assertThat(type("float").isNumerical()).isTrue(),
      () -> assertThat(type("double").isNumerical()).isTrue()
    );
  }

  @ParameterizedTest
  @MethodSource("names")
  void names(String expectedFullyQualifiedName, String expectedName) {
    assertThat(type(expectedFullyQualifiedName))
      .is(expectedFullyQualifiedName)
      .hasName(expectedName)
      .hasToString(expectedName);
  }

  private static Stream<Arguments> names() {
    return Stream.of(
      Arguments.of("int", "int"),
      Arguments.of("int[][]", "int[][]"),
      Arguments.of("java.util.Map", "Map"),
      Arguments.of("java.util.Map[][]", "Map[][]"),
      Arguments.of("java.util.Map$Entry", "Entry"),
      Arguments.of("java.util.Map$Entry[][]", "Entry[][]")
    );
  }

  @Test
  void name_of_parameterized_type() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { java.util.List<String> m(); }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    AbstractTypedTree e = Objects.requireNonNull((AbstractTypedTree) m.returnType());
    JType parameterizedType = cu.sema.type(Objects.requireNonNull(e.typeBinding));

    assertThat(parameterizedType)
      .hasSameNameAs(e.symbolType())
      .hasSameNameAs(cu.sema.typeSymbol(e.typeBinding).type())
      .is("java.util.List")
      .hasName("List");
  }

  @Test
  void name_of_type_variable() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { <T> T m(); }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    AbstractTypedTree e = Objects.requireNonNull((AbstractTypedTree) m.returnType());
    JType typeVariable = cu.sema.type(Objects.requireNonNull(e.typeBinding));
    assertThat(typeVariable)
      .hasSameNameAs(e.symbolType())
      .hasSameNameAs(cu.sema.typeSymbol(e.typeBinding).type())
      .hasName("T")
      .is(e.symbolType().fullyQualifiedName())
      .is("T");
  }

  @Test
  void symbol() {
    ITypeBinding typeBinding = Objects.requireNonNull(sema.resolveType("java.lang.Object"));
    assertThat(sema.type(typeBinding).symbol())
      .isSameAs(sema.typeSymbol(typeBinding));
  }

  @Test
  void erasure() {
    assertThat(type("java.lang.Object").erasure())
      .isSameAs(type("java.lang.Object"));
  }

  @Test
  void elementType() {
    assertThat(type("int[][]").elementType())
      .isSameAs(type("int[]"));
  }

  @Test
  void capture_type() {
    CompilationUnitTreeImpl cu = test("class A {\n" +
      "  Object foo(java.util.List<? extends A> list) {\n" +
      "    return list.get(0);\n" +
      "  }\n" +
      "}");
    cu.types().get(0);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) Objects.requireNonNull(m.block()).body().get(0);
    AbstractTypedTree e = Objects.requireNonNull((AbstractTypedTree) s.expression());
    Type captureType = e.symbolType();

    assertThat(captureType.fullyQualifiedName()).isEqualTo("!capture!");
  }

  @Test
  void null_type() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m(int p, int[] a) { return null; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) Objects.requireNonNull(m.block()).body().get(0);

    AbstractTypedTree e = Objects.requireNonNull((AbstractTypedTree) s.expression());
    AbstractTypedTree primitive = (AbstractTypedTree) m.parameters().get(0).type();
    AbstractTypedTree array = (AbstractTypedTree) m.parameters().get(1).type();

    JType nullType = cu.sema.type(Objects.requireNonNull(e.typeBinding));
    JType classType = cu.sema.type(Objects.requireNonNull(c.typeBinding));
    JType primitiveType = cu.sema.type(Objects.requireNonNull(primitive.typeBinding));
    JType arrayType = cu.sema.type(Objects.requireNonNull(array.typeBinding));

    assertThat(nullType)
      .hasSameNameAs(e.symbolType())
      .hasName("<nulltype>")
      .is(e.symbolType().fullyQualifiedName())
      .is("<nulltype>")
      .isSubtypeOf(classType)
      .isSubtypeOf(arrayType)
      .isNotSubtypeOf(primitiveType);
  }

  @Test
  void is_subtype_of_should_not_throw_NPE() {
    JType objectType = type("java.lang.Object");
    ITypeBinding brokenStringBinding = spy(Objects.requireNonNull(sema.resolveType("java.lang.String")));
    // simulate the NullPointerException described in SONARJAVA-4390
    when(brokenStringBinding.isSubTypeCompatible(any()))
      .thenThrow(new NullPointerException("test NPE"));

    JType stringType = new JType(sema, brokenStringBinding);
    // should catch the NullPointerException
    assertThat(stringType.isSubtypeOf(objectType)).isFalse();

    assertThat(logTester.logs(Level.DEBUG))
      .containsExactly("NullPointerException while resolving isSubTypeCompatible()");
  }

  @Test
  void wildcard() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C<T1, T2, T3> { C<? extends String, ? extends String, ? super String> f; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c.members().get(0);
    JavaTree.ParameterizedTypeTreeImpl p = (JavaTree.ParameterizedTypeTreeImpl) f.type();
    JavaTree.WildcardTreeImpl w1 = (JavaTree.WildcardTreeImpl) p.typeArguments().get(0);
    JavaTree.WildcardTreeImpl w2 = (JavaTree.WildcardTreeImpl) p.typeArguments().get(1);
    JavaTree.WildcardTreeImpl w3 = (JavaTree.WildcardTreeImpl) p.typeArguments().get(2);
    JType wildcardType1 = cu.sema.type(Objects.requireNonNull(w1.typeBinding));
    JType wildcardType2 = cu.sema.type(Objects.requireNonNull(w2.typeBinding));
    JType wildcardType3 = cu.sema.type(Objects.requireNonNull(w3.typeBinding));

    assertThat(wildcardType1)
      .isEqualTo(wildcardType2)
      .isNotEqualTo(wildcardType3);
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

  private JType type(String name) {
    ITypeBinding typeBinding = Objects.requireNonNull(sema.resolveType(name));
    return sema.type(typeBinding);
  }

  private JSema sema;

  @BeforeEach
  void setup() {
    ASTParser astParser = ASTParser.newParser(AST.JLS14);
    astParser.setEnvironment(
      new String[]{},
      new String[]{},
      new String[]{},
      true
    );
    astParser.setResolveBindings(true);
    astParser.setUnitName("File.java");
    astParser.setSource("".toCharArray());
    AST ast = astParser.createAST(null).getAST();
    sema = new JSema(ast);
  }

}
