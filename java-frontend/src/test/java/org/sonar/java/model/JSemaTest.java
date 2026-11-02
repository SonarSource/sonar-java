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
    Type type = sema.getClassType("int");
    assertThat(type).isNotNull().isSameAs(sema.getClassType("int"));
  }

  @Test
  void resolveType() {
    assertAll(
      () ->
        assertThat(sema.getClassType("Nonexistent"))
          .isSameAs(Type.UNKNOWN),
      () ->
        assertThat(sema.getClassType("void"))
          .isNotSameAs(Type.UNKNOWN),
      () ->
        assertThat(sema.getClassType("java.util.Map"))
          .isNotSameAs(Type.UNKNOWN),
      () ->
        assertThat(sema.getClassType("java.util.Map$Entry"))
          .isNotSameAs(Type.UNKNOWN),
      () ->
        assertThat(sema.getClassType("java.util.Map$Entry[][]"))
          .isNotSameAs(Type.UNKNOWN)
    );
  }

  @Test
  void resolvePackageAnnotations() {
    assertThat(sema.getClassType("org.sonar.java.resolve.targets.ClassWithConstants").symbol().owner().metadata().annotations())
      .hasSize(1);
  }

  private org.sonar.plugins.java.api.semantic.Sema sema;

  @BeforeEach
  void setup() {
    sema = SemanticFixture.scoped("", java.util.List.of(new java.io.File("target/test-classes"))).tree().sema;
  }

}
