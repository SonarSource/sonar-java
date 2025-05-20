/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JSemaTest {

  @Test
  void testGetClassTypeUnknown() {
    assertThat(sema.getClassType("com.google.common.collect.ImmutableMap")).
      isEqualTo(Type.UNKNOWN);
    assertThat(sema.getClassType("java.lang.Object"))
      .isNotEqualTo(Type.UNKNOWN);
  }

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
    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
    String version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION.effectiveJavaVersionAsString();
    JavaCore.setComplianceOptions(version, JavaCore.getOptions());
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
