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

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JSemaTest {

  private JSema sema;

  @BeforeEach
  void setup() {
    sema = new JSema();
    sema.ingestTypes(new JsonObject());
  }

  @Test
  void testGetClassTypeUnknown() {
    assertThat(sema.getClassType("com.google.common.collect.ImmutableMap").isUnknown()).isTrue();
    assertThat(sema.getClassType("java.lang.Object").isUnknown()).isFalse();
  }

  @Test
  void resolveType() {
    assertAll(
      () -> assertThat(sema.getClassType("void").isUnknown()).isFalse(),
      () -> assertThat(sema.getClassType("java.util.Map").isUnknown()).isTrue(),
      () -> assertThat(sema.getClassType("java.lang.Object[]").isArray()).isTrue()
    );
  }

  @Test
  void typeIsStable() {
    Type first = sema.getClassType("int");
    assertThat(first).isNotNull().isSameAs(sema.getClassType("int"));
  }

}
