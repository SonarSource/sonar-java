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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "interface A { void a(); }\n"
      + "class Clazz implements A { public void a() {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));

    assertThat(symbol.overriddenSymbols()).containsOnly(retrieveMethodSymbol("A", "a", cu));
  }

  @Test
  void testOverridenSymbolVSOverridenSymbols() {
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "interface A { void a(); }\n"
      + "interface B { void a(); }\n"
      + "class Clazz implements A, B { public void a() {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(2);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));

    JMethodSymbol aA = retrieveMethodSymbol("A", "a", cu);
    JMethodSymbol aB = retrieveMethodSymbol("B", "a", cu);

    assertThat(symbol.overriddenSymbol()).isEqualTo(aA);
    // call it again to not recompute overridden symbols
    assertThat(symbol.overriddenSymbol()).isEqualTo(aA);

    assertThat(symbol.overriddenSymbols()).containsExactly(aA, aB);
  }

  @Test
  void testClassInheritanceChainOnlyFindsDirectOverride() {
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "interface Interface { void a(); }\n"
      + "class A implements Interface { public void a() {} }\n"
      + "class B extends A { public void a() {} }\n"
      + "class C extends B { public void a() {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(3);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));

    assertThat(symbol.overriddenSymbols()).containsExactly(
      retrieveMethodSymbol("B", "a", cu),
      retrieveMethodSymbol("A", "a", cu),
      retrieveMethodSymbol("Interface", "a", cu));
  }

  @Test
  void testObjectExtensionFromClass() {
    JavaTree.CompilationUnitTreeImpl cu = test("class Clazz { public boolean equals(Object other) { return false; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));

    assertThat(symbol.overriddenSymbols()).containsExactly(objectEqualsMethod(cu.sema));
  }

  @Test
  void testObjectExtensionFromInterface() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface Interface { boolean equals(Object other) { return false; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));

    assertThat(symbol.overriddenSymbols()).containsExactly(objectEqualsMethod(cu.sema));
  }

  private static JMethodSymbol objectEqualsMethod(JSema sema) {
    return Arrays.stream(sema.resolveType("java.lang.Object").getDeclaredMethods())
      .filter(method -> "equals".equals(method.getName()))
      .findFirst()
      .map(sema::methodSymbol)
      .orElseThrow(() -> new IllegalStateException("Could not find Object#equals"));
  }

  @Test
  void testMultipleInheritance() {
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "interface A { void a(); }\n"
      + "interface B { void a(); }\n"
      + "class C implements A, B { public void a() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(2);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));

    assertThat(symbol.overriddenSymbols()).containsExactly(
      retrieveMethodSymbol("A", "a", cu),
      retrieveMethodSymbol("B", "a", cu));
  }

  @Test
  void testUnknownInheritance() {
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "interface KnownInterface { void a(); }\n"
      + "class C implements KnownInterface, UnknownInterface { public void a() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));

    assertThat(symbol.overriddenSymbols()).containsExactly(
      retrieveMethodSymbol("KnownInterface", "a", cu));
  }

  @Test
  void testUnknownExtends() {
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "class B extends Unknown { public void a() { } }");
    ClassTreeImpl a = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) a.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));
    assertThat(symbol.overriddenSymbols()).isEmpty();
  }

  @Test
  void testBindingReturnNullSuperClass() {
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "class A { public void a() { } }");
    ClassTreeImpl a = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) a.members().get(0);
    // We create a real JMethodSymbol because it is not possible to mock JMethodSymbol (it is final), and not easy
    // to mock the arguments either, because the constructor is using an external helper method.
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));

    ITypeBinding iTypeBinding = mock(ITypeBinding.class);
    // In some rare situation, the "getSuperclass" can return null. It is not easy to write code reproducing the issue, we test it by "hand".
    when(iTypeBinding.getSuperclass()).thenReturn(null);
    ITypeBinding[] interfaces = {null};
    when(iTypeBinding.getInterfaces()).thenReturn(interfaces);

    Collection<Symbol.MethodSymbol> overrides = new ArrayList<>();
    assertDoesNotThrow(() ->
      symbol.findOverridesInParentTypes(overrides, methodBinding -> false, iTypeBinding));
    assertThat(overrides).isEmpty();
  }

  @Test
  void testMultipleInheritanceWithExtensionOfObject() {
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "interface A { boolean equals(Object other); }\n"
      + "interface B { boolean equals(Object other); }\n"
      + "interface C extends B { }\n"
      + "class Clazz implements A, C { public boolean equals(Object other) { return false; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(3);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));

    assertThat(symbol.overriddenSymbols()).containsExactly(
      objectEqualsMethod(cu.sema),
      retrieveMethodSymbol("A", "equals", cu),
      retrieveMethodSymbol("B", "equals", cu));
  }

  private static JMethodSymbol retrieveMethodSymbol(String className, String methodName, CompilationUnitTreeImpl cu) {
    MethodMatchers methodMatcher = MethodMatchers.create()
      .ofTypes(className)
      .names(methodName)
      .withAnyParameters()
      .build();
    return cu.types().stream()
      .filter(ClassTreeImpl.class::isInstance)
      .map(ClassTreeImpl.class::cast)
      .map(ClassTreeImpl::symbol)
      .map(Symbol.TypeSymbol::memberSymbols)
      .flatMap(Collection::stream)
      .filter(methodMatcher::matches)
      .map(JMethodSymbol.class::cast)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No method could be found with the given name in the given class"));
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
