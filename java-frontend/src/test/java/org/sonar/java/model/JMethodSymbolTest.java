/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class JMethodSymbolTest {

  @Test
  void test() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { C m(C p) throws Exception { return null; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));
    assertThat(symbol.parameterTypes())
      .containsOnly(cu.sema.type(Objects.requireNonNull(c.typeBinding)));
    assertThat(symbol.returnType())
      .isSameAs(cu.sema.typeSymbol(Objects.requireNonNull(c.typeBinding)));
    assertThat(symbol.thrownTypes())
      .containsOnly(cu.sema.type(Objects.requireNonNull(cu.sema.resolveType("java.lang.Exception"))));
  }

  @Test
  void testSingleInheritance() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface A { void a(); }  class C implements A { public void a() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));
    assertThat(symbol.overriddenSymbols()).containsOnly(retrieveMethodSymbol("A", "a", cu));
  }

  @Test
  void testClassInheritanceChainOnlyFindsDirectOverride() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface AInt { void a() {} } class A implements AInt { public void a() {} } class B extends A { public void a() {}}  class C extends B { public void a() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(3);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));
    assertThat(symbol.overriddenSymbols()).containsOnly(retrieveMethodSymbol("AInt", "a", cu), retrieveMethodSymbol("B", "a", cu));
  }

  @Test
  void testObjectExtension() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface A { boolean equals(Object other); }  interface B { boolean equals(Object other); } interface Z extends B { } class C { public boolean equals(Object other) { return false;} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(3);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));
    assertThat(symbol.overriddenSymbols()).containsOnly(new JMethodSymbol(cu.sema, Arrays.stream(cu.sema.resolveType("java.lang.Object").getDeclaredMethods()).filter(a -> a.getName().equals("equals")).findFirst().orElseThrow(() -> new IllegalStateException("Could not find Object#equals"))));
  }

  @Test
  void testMultipleInheritance() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface A { void a(); }  interface B { void a(); } class C implements A, B { public void a() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(2);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));
    assertThat(symbol.overriddenSymbols()).containsOnly(
      retrieveMethodSymbol("A", "a", cu),
      retrieveMethodSymbol("B", "a", cu));
  }

  @Test
  void testMultipleInheritanceWithExtensionOfObject() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface A { boolean equals(Object other); }  interface B { boolean equals(Object other); } interface Z extends B { } class C implements A, Z { public boolean equals(Object other) { return false;} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(3);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));
    assertThat(symbol.overriddenSymbols()).containsOnly(
      new JMethodSymbol(cu.sema, Arrays.stream(cu.sema.resolveType("java.lang.Object").getDeclaredMethods()).filter(a -> a.getName().equals("equals")).findFirst().orElseThrow(() -> new IllegalStateException("Could not find Object#equals"))),
      retrieveMethodSymbol("A", "equals", cu),
      retrieveMethodSymbol("B", "equals", cu));
  }

  private static JMethodSymbol retrieveMethodSymbol(String className, String methodName, CompilationUnitTreeImpl cu) {
    return cu.sema.methodSymbol(cu.types().stream().filter(a -> a instanceof ClassTreeImpl)
      .map(a -> (ClassTreeImpl) a)
      .filter(c -> className.equals(c.simpleName().name()))
      .flatMap(c -> c.children().stream())
      .filter(c -> c.is(Tree.Kind.METHOD))
      .map(m -> (MethodTreeImpl) m)
      .filter(m -> m.simpleName().name().equals(methodName))
      .map(m -> m.methodBinding)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No method could be found with the given name in the given class")));
  }

  @Test
  void signature() {
    JavaTree.CompilationUnitTreeImpl cu = test("package org.example; class C { C() {} <T> Object m(Object p1, Object[] p2, T p3) { return m(null, null, 42); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl constructor = (MethodTreeImpl) c.members().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(1);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) Objects.requireNonNull(method.block()).body().get(0);
    MethodInvocationTreeImpl methodInvocation = Objects.requireNonNull((MethodInvocationTreeImpl) s.expression());
    assertThat(cu.sema.methodSymbol(Objects.requireNonNull(constructor.methodBinding)).signature())
      .isEqualTo(constructor.symbol().signature())
      .isEqualTo("org.example.C#<init>()V");
    assertThat(cu.sema.methodSymbol(Objects.requireNonNull(method.methodBinding)).signature())
      .isEqualTo(method.symbol().signature())
      .isEqualTo("org.example.C#m(Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    assertThat(cu.sema.methodSymbol(Objects.requireNonNull(methodInvocation.methodBinding)).signature())
      .isEqualTo(method.symbol().signature())
      .isEqualTo("org.example.C#m(Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
  }

  private static CompilationUnitTreeImpl test(String source) {
    return (CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
