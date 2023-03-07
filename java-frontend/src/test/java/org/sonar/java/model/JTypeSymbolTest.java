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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;

import java.util.Objects;

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

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
