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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.java.test.TestResourceScope;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestResourceScope.Scoped
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
    sema = EcjTestSupport.sema(SemanticFixture.scoped("", java.util.List.of(new java.io.File("target/test-classes"))).tree());
  }

}
