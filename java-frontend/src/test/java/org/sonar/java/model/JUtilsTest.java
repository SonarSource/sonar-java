/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.TypeCastExpressionTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;

class JUtilsTest {

  @Test
  void primitiveWrapperType() {
    JavaTree.CompilationUnitTreeImpl cu = test("");
    Type primitiveType = cu.sema.type(Objects.requireNonNull(cu.sema.resolveType("byte")));
    Type wrapperType = JUtils.primitiveWrapperType(primitiveType);

    assertThat(wrapperType).isNotNull();
    assertThat(JUtils.isPrimitiveWrapper(wrapperType)).isTrue();
    assertThat(wrapperType.fullyQualifiedName()).isEqualTo("java.lang.Byte");
  }

  @Test
  void primitiveType() {
    JavaTree.CompilationUnitTreeImpl cu = test("");
    Type wrapperType = cu.sema.type(Objects.requireNonNull(cu.sema.resolveType("java.lang.Byte")));
    Type primitiveType = JUtils.primitiveType(wrapperType);

    assertThat(primitiveType).isNotNull();
    assertThat(primitiveType.fullyQualifiedName()).isEqualTo("byte");
  }

  @Test
  void isNullType() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m() { return null; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) Objects.requireNonNull(method.block()).body().get(0);
    AbstractTypedTree e = (AbstractTypedTree) Objects.requireNonNull(s.expression());

    assertThat(JUtils.isNullType(cu.sema.type(e.typeBinding)))
      .isEqualTo(JUtils.isNullType(e.symbolType()))
      .isTrue();
  }

  @Test
  void isIntersectionType() {
    JavaTree.CompilationUnitTreeImpl cu = test(
      "class C { java.io.Serializable f = (java.util.Comparator<Object> & java.io.Serializable) (o1, o2) -> o1.toString().compareTo(o2.toString()); }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c.members().get(0);
    TypeCastExpressionTreeImpl e = (TypeCastExpressionTreeImpl) f.initializer();

    assertThat(JUtils.isIntersectionType(cu.sema.type(e.typeBinding)))
      .isEqualTo(JUtils.isIntersectionType(e.symbolType()))
      .isTrue();
  }

  @Test
  void isParametrizedMethod() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { <T> void m(T p) { m(42); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTreeImpl s = (ExpressionStatementTreeImpl) Objects.requireNonNull(method.block()).body().get(0);
    MethodInvocationTreeImpl methodInvocation = (MethodInvocationTreeImpl) s.expression();

    assertThat(JUtils.isParametrizedMethod(cu.sema.methodSymbol(method.methodBinding)))
      .isEqualTo(JUtils.isParametrizedMethod(method.symbol()))
      .isEqualTo(method.methodBinding.isGenericMethod())
      .isTrue();
    assertThat(JUtils.isParametrizedMethod(cu.sema.methodSymbol(methodInvocation.methodBinding)))
      .isEqualTo(JUtils.isParametrizedMethod((Symbol.MethodSymbol) methodInvocation.symbol()))
      .isEqualTo(methodInvocation.methodBinding.isParameterizedMethod())
      .isTrue();
  }

  @Test
  void isParametrizedType() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m() { new java.util.ArrayList<String>(); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTreeImpl s = (ExpressionStatementTreeImpl) Objects.requireNonNull(m.block()).body().get(0);
    AbstractTypedTree e = Objects.requireNonNull((AbstractTypedTree) s.expression());

    assertThat(JUtils.isParametrized(cu.sema.type(e.typeBinding)))
      .isEqualTo(JUtils.isParametrized(e.symbolType()))
      .isTrue();
  }

  @Test
  void isRawType() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C {} class D<T> { void foo(D d, Unknown u) {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);

    ClassTreeImpl dGeneric = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl m = (MethodTreeImpl) dGeneric.members().get(0);
    VariableTreeImpl dRaw = (VariableTreeImpl) m.parameters().get(0);
    VariableTreeImpl unknown = (VariableTreeImpl) m.parameters().get(1);

    assertThat(JUtils.isRawType(c.symbol().type()))
      .isSameAs(JUtils.isRawType(dGeneric.symbol().type()))
      .isSameAs(JUtils.isRawType(unknown.symbol().type()))
      .isFalse();

    assertThat(JUtils.isRawType(dRaw.type()
      .symbolType()))
      .isTrue();
  }

  @Test
  void declaringType() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C<T> { void foo(C d, Unknown u) {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);

    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl cRaw = (VariableTreeImpl) m.parameters().get(0);
    VariableTreeImpl unknown = (VariableTreeImpl) m.parameters().get(1);

    assertThat(JUtils.declaringType(unknown.symbol().type()))
      .isSameAs(unknown.symbol().type());

    assertThat(JUtils.declaringType(cRaw.type().symbolType()))
      .isSameAs(JUtils.declaringType(c.symbol().type()))
      .isSameAs(c.symbol().type());
  }

  @Test
  void typeArguments() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m() { new java.util.HashMap<Integer, String>(); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTreeImpl s = (ExpressionStatementTreeImpl) Objects.requireNonNull(m.block()).body().get(0);
    AbstractTypedTree e = Objects.requireNonNull((AbstractTypedTree) s.expression());

    assertThat(JUtils.typeArguments(cu.sema.type(e.typeBinding)).toString())
      .isEqualTo(JUtils.typeArguments(e.symbolType()).toString())
      .isEqualTo("[Integer, String]");
  }

  @Test
  void constantValue() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { short SHORT = 42; char CHAR = 42; byte BYTE = 42; boolean BOOLEAN = false; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    Symbol.VariableSymbol shortConstant = cu.sema.variableSymbol(((VariableTreeImpl) c.members().get(0)).variableBinding);
    assertThat(JUtils.constantValue(shortConstant).orElseThrow(AssertionError::new))
      .isInstanceOf(Integer.class)
      .isEqualTo(42);
    Symbol.VariableSymbol charConstant = cu.sema.variableSymbol(((VariableTreeImpl) c.members().get(1)).variableBinding);
    assertThat(JUtils.constantValue(charConstant).orElseThrow(AssertionError::new))
      .isInstanceOf(Integer.class)
      .isEqualTo(42);
    Symbol.VariableSymbol byteConstant = cu.sema.variableSymbol(((VariableTreeImpl) c.members().get(2)).variableBinding);
    assertThat(JUtils.constantValue(byteConstant).orElseThrow(AssertionError::new))
      .isInstanceOf(Integer.class)
      .isEqualTo(42);
    Symbol.VariableSymbol booleanConstant = cu.sema.variableSymbol(((VariableTreeImpl) c.members().get(3)).variableBinding);
    assertThat(JUtils.constantValue(booleanConstant).orElseThrow(AssertionError::new))
      .isInstanceOf(Boolean.class)
      .isEqualTo(false);
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
