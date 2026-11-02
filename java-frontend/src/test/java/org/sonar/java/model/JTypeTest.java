/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.test.TestResourceScope;
import org.sonar.java.testing.ThreadLocalLogTester;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.java.model.assertions.TypeAssert.assertThat;

@TestResourceScope.Scoped
class JTypeTest {

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.DEBUG);

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

    assertThat(SemanticTestSupport.type(e).isParameterized())
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

    assertThat(SemanticTestSupport.type(e).typeArguments())
      .hasToString(e.symbolType().typeArguments().toString())
      .hasToString("[Integer, String]");

    assertThat(SemanticTestSupport.type(c).typeArguments())
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
  void primitiveWrapperType(){
    Type booleanType = type("boolean");
    assertThat(booleanType.primitiveWrapperType()).isEqualTo(type("java.lang.Boolean"));
    assertThat(booleanType.primitiveWrapperType()).isNotEqualTo(type("java.lang.Byte"));
  }

  @Test
  void non_primitive_type_has_no_primitive() {
    Type stringType = type("java.lang.String");
    assertThat(stringType.primitiveType()).isNull();
  }

  @Test
  void non_primitive_wrapper_tpe_has_no_primitive_wrapper() {
    Type stringType = type("java.lang.String");
    assertThat(stringType.primitiveWrapperType()).isNull();
  }

  @Test
  void primitiveWrapperType_returns_null_when_semantic_cannot_resolve_wrapper_type() {
    var original = (JSema) test("class C {}").sema;
    var broken = spy(original);
    when(broken.getClassType("java.lang.Byte")).thenReturn(Type.UNKNOWN);
    var primitive = (JType) original.getClassType("byte");
    assertThat(new JType(broken, primitive.psi, primitive.key).primitiveWrapperType()).isNull();
  }

  @Test
  void primitiveType_returns_null_when_semantic_cannot_resolve_primitive_type() {
    var original = (JSema) test("class C {}").sema;
    var broken = spy(original);
    when(broken.getClassType("byte")).thenReturn(Type.UNKNOWN);
    var wrapper = (JType) original.getClassType("java.lang.Byte");
    assertThat(new JType(broken, wrapper.psi, wrapper.key).primitiveType()).isNull();
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
  @MethodSource("fullyQualifiedNamesToNames")
  void names(String expectedFullyQualifiedName, String expectedName) {
    assertThat(type(expectedFullyQualifiedName))
      .is(expectedFullyQualifiedName)
      .hasName(expectedName)
      .hasToString(expectedName);
  }

  private static Stream<Arguments> fullyQualifiedNamesToNames() {
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
    Type parameterizedType = Objects.requireNonNull(SemanticTestSupport.type(e));

    assertThat(parameterizedType)
      .hasSameNameAs(e.symbolType())
      .hasSameNameAs(SemanticTestSupport.type(e).symbol().type())
      .is("java.util.List")
      .hasName("List");
  }

  @Test
  void name_of_type_variable() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { <T> T m(); }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    AbstractTypedTree e = Objects.requireNonNull((AbstractTypedTree) m.returnType());
    Type typeVariable = Objects.requireNonNull(SemanticTestSupport.type(e));
    assertThat(typeVariable)
      .hasSameNameAs(e.symbolType())
      .hasSameNameAs(SemanticTestSupport.type(e).symbol().type())
      .hasName("T")
      .is(e.symbolType().fullyQualifiedName())
      .is("T");
  }

  @Test
  void symbol() {
    Type typeBinding = sema.getClassType("java.lang.Object");
    assertThat(typeBinding.symbol())
      .isSameAs(sema.getClassType("java.lang.Object").symbol());
  }

  @Test
  void erasure() {
    assertThat(type("java.lang.Object").erasure())
      .isSameAs(type("java.lang.Object"));
  }

  @Test
  void elementType() {
    assertThat(((Type.ArrayType) type("int[][]")).elementType())
      .isSameAs(type("int[]"));
  }

  @Test
  void capture_type() {
    CompilationUnitTreeImpl cu = test("""
      class A {
        Object foo(java.util.List<? extends A> list) {
          return list.get(0);
        }
      }
      """);
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

    Type nullType = Objects.requireNonNull(SemanticTestSupport.type(e));
    Type classType = Objects.requireNonNull(SemanticTestSupport.type(c));
    Type primitiveType = Objects.requireNonNull(SemanticTestSupport.type(primitive));
    Type arrayType = Objects.requireNonNull(SemanticTestSupport.type(array));

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
    var original = (JSema) test("class C {}").sema;
    var string = (JType) original.getClassType("java.lang.String");
    var broken = spy((com.intellij.psi.PsiClassType) string.psi);
    when(broken.resolveGenerics()).thenThrow(new NullPointerException("test NPE"));
    var stringType = new JType(original, broken, string.key);
    assertThat(stringType.isSubtypeOf(original.getClassType("java.lang.Object"))).isFalse();
    assertThat(logTester.logs(Level.DEBUG)).containsExactly("NullPointerException while resolving isSubTypeCompatible()");
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
    Type wildcardType1 = Objects.requireNonNull(SemanticTestSupport.type(w1));
    Type wildcardType2 = Objects.requireNonNull(SemanticTestSupport.type(w2));
    Type wildcardType3 = Objects.requireNonNull(SemanticTestSupport.type(w3));

    assertThat(wildcardType1)
      .isEqualTo(wildcardType2)
      .isNotEqualTo(wildcardType3);
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

  private Type type(String name) {
    return fixture.type(name);
  }

  private org.sonar.plugins.java.api.semantic.Sema sema;
  private SemanticFixture fixture;

  @BeforeEach
  void setup() {
    fixture = SemanticFixture.scoped("", java.util.List.of());
    sema = fixture.tree().sema;
  }

}
