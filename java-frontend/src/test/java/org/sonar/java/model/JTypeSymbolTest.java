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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;

import java.util.Objects;
import org.sonar.plugins.java.api.semantic.Symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JTypeSymbolTest {

  @Test
  void superClass() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { } class C2 extends C { }");
    ITypeBinding javaLangObject = Objects.requireNonNull(cu.sema.resolveType("java.lang.Object"));
    ITypeBinding i = Objects.requireNonNull(((ClassTreeImpl) cu.types().get(0)).typeBinding);
    ITypeBinding c = Objects.requireNonNull(((ClassTreeImpl) cu.types().get(1)).typeBinding);
    ITypeBinding c2 = Objects.requireNonNull(((ClassTreeImpl) cu.types().get(2)).typeBinding);
    assertAll(
      // for java.lang.Object
      () ->
        assertThat(cu.sema.typeSymbol(javaLangObject).superClass())
          .isNull(),
      // for interfaces
      () ->
        assertThat(cu.sema.typeSymbol(i).superClass())
          .isSameAs(cu.sema.type(javaLangObject)),
      // for classes
      () ->
        assertThat(cu.sema.typeSymbol(c).superClass())
          .isSameAs(cu.sema.type(javaLangObject)),
      () ->
        assertThat(cu.sema.typeSymbol(c2).superClass())
          .isSameAs(cu.sema.type(c)),
      // for arrays
      () ->
        assertThat(cu.sema.typeSymbol(javaLangObject.createArrayType(1)).superClass())
          .isSameAs(cu.sema.type(javaLangObject))
    );
  }

  @Test
  void interfaces() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { }");
    ITypeBinding i = Objects.requireNonNull(((ClassTreeImpl) cu.types().get(0)).typeBinding);
    ITypeBinding c = Objects.requireNonNull(((ClassTreeImpl) cu.types().get(1)).typeBinding);
    assertThat(cu.sema.typeSymbol(i).interfaces())
      .isEmpty();
    assertThat(cu.sema.typeSymbol(c).interfaces())
      .containsOnly(cu.sema.type(i));
  }

  @Test
  void memberSymbols() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; C() {} class N {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(1);
    ClassTreeImpl n = (ClassTreeImpl) c.members().get(2);
    assertThat(cu.sema.typeSymbol(c.typeBinding).memberSymbols())
      .containsOnly(
        cu.sema.variableSymbol(f.variableBinding),
        cu.sema.methodSymbol(m.methodBinding),
        cu.sema.typeSymbol(n.typeBinding)
      );
  }

  @Test
  void sealedClass() {
    JavaTree.CompilationUnitTreeImpl cu = test(
      "sealed class A permits B,C,D {}\n" +
        "final class B extends A {}\n" +
        "non-sealed class C extends A {}\n" +
        "sealed class D extends A permits E {}\n" +
        "non-sealed class E extends D {}");
    ClassTreeImpl a = (ClassTreeImpl) cu.types().get(0);
    ClassTreeImpl b = (ClassTreeImpl) cu.types().get(1);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(2);
    ClassTreeImpl d = (ClassTreeImpl) cu.types().get(3);
    ClassTreeImpl e = (ClassTreeImpl) cu.types().get(4);
    Symbol.TypeSymbol aSymbol = a.symbol();
    Symbol.TypeSymbol bSymbol = b.symbol();
    Symbol.TypeSymbol cSymbol = c.symbol();
    Symbol.TypeSymbol dSymbol = d.symbol();
    Symbol.TypeSymbol eSymbol = e.symbol();

    assertThat(aSymbol.isSealed()).isTrue();
    assertThat(aSymbol.isNonSealed()).isFalse();
    assertThat(aSymbol.permitsTypes()).containsExactly();

    assertThat(bSymbol.isSealed()).isFalse();
    assertThat(bSymbol.isNonSealed()).isFalse();

    assertThat(cSymbol.isSealed()).isFalse();
    assertThat(cSymbol.isNonSealed()).isTrue();

    assertThat(dSymbol.isSealed()).isTrue();
    assertThat(dSymbol.isNonSealed()).isFalse();
    assertThat(aSymbol.permitsTypes()).containsExactly();

    assertThat(eSymbol.isSealed()).isFalse();
    assertThat(eSymbol.isNonSealed()).isTrue();
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
