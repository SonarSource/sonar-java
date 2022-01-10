/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JSemaTest {

  @Test
  void type() {
    ITypeBinding typeBinding = Objects.requireNonNull(sema.resolveType("int"));
    assertThat(sema.type(typeBinding))
      .isNotNull()
      .isSameAs(sema.type(typeBinding));
  }

  @Test
  void resolveType() {
    assertAll(
      () ->
        assertThat(sema.resolveType("Nonexistent"))
          .isNull(),
      () ->
        assertThat(sema.resolveType("void"))
          .isNotNull(),
      () ->
        assertThat(sema.resolveType("java.util.Map"))
          .isNotNull(),
      () ->
        assertThat(sema.resolveType("java.util.Map$Entry"))
          .isNotNull(),
      () ->
        assertThat(sema.resolveType("java.util.Map$Entry[][]"))
          .isNotNull()
    );
  }

  @Test
  void resolvePackageAnnotations() {
    assertThat(sema.resolvePackageAnnotations("org.sonar.java.resolve.targets"))
      .hasSize(1);
  }

  private JSema sema;

  @BeforeEach
  void setup() {
    ASTParser astParser = ASTParser.newParser(AST.JLS14);
    astParser.setEnvironment(
      new String[]{"target/test-classes"},
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
