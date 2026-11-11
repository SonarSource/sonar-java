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
package org.sonar.java.jdt;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.Symbols;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.java.jdt.JDTTestUtils.methodBinding;
import static org.sonar.java.jdt.JDTTestUtils.sema;

class JDTMethodSymbolTest {

  @Test
  void testBindingReturnNullSuperClass() {
    CompilationUnitTreeImpl cu = test(""
      + "class A { public void a() { } }");
    ClassTreeImpl a = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) a.members().get(0);
    JDTMethodSymbol symbol = (JDTMethodSymbol) m.symbol();

    ITypeBinding iTypeBinding = mock(ITypeBinding.class);
    when(iTypeBinding.getSuperclass()).thenReturn(null);
    ITypeBinding[] interfaces = {null};
    when(iTypeBinding.getInterfaces()).thenReturn(interfaces);

    Collection<Symbol.MethodSymbol> overrides = new ArrayList<>();
    assertDoesNotThrow(() ->
      symbol.findOverridesInParentTypes(overrides, methodBinding -> false, iTypeBinding));
    assertThat(overrides).isEmpty();
  }

  @Test
  void support_unexpected_IMethodBinding_null_return_type() {
    CompilationUnitTreeImpl cu = test("class C { C m() { return null; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    IMethodBinding brokenMethodBinding = spy(JDTTestUtils.methodBinding(m));
    when(brokenMethodBinding.getReturnType()).thenReturn(null);
    JDTMethodSymbol methodSymbol = new JDTMethodSymbol(sema(cu), brokenMethodBinding);
    assertThat(methodSymbol.returnType().isUnknown()).isTrue();
    assertThat(methodSymbol.isNativeMethod()).isFalse();
    assertThat(methodSymbol.metadata()).isSameAs(Symbols.EMPTY_METADATA);
  }

  @Test
  void isLambda() {
    CompilationUnitTreeImpl cu = test("""
        class A {
          void foo() { }
          java.util.function.IntConsumer bar = i -> { };
        }
        """);
    ClassTreeImpl a = (ClassTreeImpl) cu.types().get(0);

    MethodTreeImpl foo = (MethodTreeImpl) a.members().get(0);
    var fooSymbol = (JDTMethodSymbol) foo.symbol();
    assertFalse(fooSymbol.isLambda());
    assertThat(fooSymbol.toString()).isNotEmpty();

    var barAssignment = (VariableTree) a.members().get(1);
    var initializer = (LambdaExpressionTree) barAssignment.initializer();
    var lambdaSymbol = (JDTMethodSymbol) initializer.symbol();
    assertTrue(lambdaSymbol.isLambda());
    assertThat(lambdaSymbol.toString()).isNotEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "class C { C m() { return null; } }",
    "class C { <T> C m() { return null; } }",
    "interface I { default C m() { return null; } }",
    "class C { synchronized C m() { return null; } }",
    "class C { C m(String... s) { return null; } }"
  })
  void recovered_method_modifiers(String source) {
    CompilationUnitTreeImpl cu = test(source);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    IMethodBinding binding = spy(methodBinding(m));
    when(binding.getReturnType()).thenReturn(null);
    when(binding.isRecovered()).thenReturn(true);
    JDTMethodSymbol symbol = new JDTMethodSymbol(sema(cu), binding);

    assertThat(symbol.isOverridable()).isFalse();
    assertThat(symbol.isParametrizedMethod()).isFalse();
    assertThat(symbol.isDefaultMethod()).isFalse();
    assertThat(symbol.isSynchronizedMethod()).isFalse();
    assertThat(symbol.isVarArgsMethod()).isFalse();
  }

  private static CompilationUnitTreeImpl test(String source) {
    return (CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }
}
