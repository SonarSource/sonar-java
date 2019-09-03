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
package org.sonar.java.model;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JTypeTest {

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

  @Test
  void fullyQualifiedName() {
    assertAll(
      () ->
        assertThat(type("int").fullyQualifiedName())
          .isEqualTo("int"),
      () ->
        assertThat(type("int[][]").fullyQualifiedName())
          .isEqualTo("int[][]"),
      () ->
        assertThat(type("java.util.Map").fullyQualifiedName())
          .isEqualTo("java.util.Map"),
      () ->
        assertThat(type("java.util.Map[][]").fullyQualifiedName())
          .isEqualTo("java.util.Map[][]"),
      () ->
        assertThat(type("java.util.Map$Entry").fullyQualifiedName())
          .isEqualTo("java.util.Map$Entry"),
      () ->
        assertThat(type("java.util.Map$Entry[][]").fullyQualifiedName())
          .isEqualTo("java.util.Map$Entry[][]")
    );
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

  private JType type(String name) {
    ITypeBinding typeBinding = Objects.requireNonNull(sema.resolveType(name));
    return sema.type(typeBinding);
  }

  private JSema sema;

  @BeforeEach
  void setup() {
    ASTParser astParser = ASTParser.newParser(AST.JLS12);
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
