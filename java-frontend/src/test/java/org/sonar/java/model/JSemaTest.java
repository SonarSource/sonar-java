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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.semantic.Sema;

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
  void resolvePackageAnnotations() {
    assertThat(sema.getClassType("org.sonar.java.resolve.targets.ClassWithConstants").symbol().owner().metadata().annotations())
      .hasSize(1);
  }

  @Test
  void getClassType() {
    assertAll(
      () ->
        assertThat(sema.getClassType("Nonexistent"))
          .isSameAs(Type.UNKNOWN),
      () ->
        assertThat(sema.getClassType("void"))
          .isNotEqualTo(Type.UNKNOWN),
      () ->
        assertThat(sema.getClassType("java.util.Map"))
          .isNotEqualTo(Type.UNKNOWN),
      () ->
        assertThat(sema.getClassType("java.util.Map$Entry"))
          .isNotEqualTo(Type.UNKNOWN),
      () ->
        assertThat(sema.getClassType("java.util.Map$Entry[][]"))
          .isNotEqualTo(Type.UNKNOWN)
    );
  }

  private Sema sema;

  @BeforeEach
  void setup() {
    sema = JParserTestUtils.sema(JParserTestUtils.parse(""));
  }

}
