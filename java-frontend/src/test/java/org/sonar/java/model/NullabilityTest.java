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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.CLASS;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.METHOD;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.PACKAGE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.VARIABLE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.NON_NULL;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.STRONG_NULLABLE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.UNKNOWN;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.WEAK_NULLABLE;

class NullabilityTest {

  @Test
  void non_null() {
    SymbolMetadata.NullabilityData data = new Nullability.Data(NON_NULL, VARIABLE, null, null, false);
    assertThat(data.isNonNull(VARIABLE, true, false)).isTrue();
    assertThat(data.isStrongNullable(VARIABLE, true, false)).isFalse();
    assertThat(data.isNullable(VARIABLE, true, false)).isFalse();
  }

  @Test
  void weak_nullable() {
    SymbolMetadata.NullabilityData data = new Nullability.Data(WEAK_NULLABLE, VARIABLE, null, null, false);
    assertThat(data.isNonNull(VARIABLE, true, false)).isFalse();
    assertThat(data.isStrongNullable(VARIABLE, true, false)).isFalse();
    assertThat(data.isNullable(VARIABLE, true, false)).isTrue();
  }

  @Test
  void strong_nullable() {
    SymbolMetadata.NullabilityData data = new Nullability.Data(STRONG_NULLABLE, VARIABLE, null, null, false);
    assertThat(data.isNonNull(VARIABLE, true, false)).isFalse();
    assertThat(data.isStrongNullable(VARIABLE, true, false)).isTrue();
    assertThat(data.isNullable(VARIABLE, true, false)).isTrue();
  }

  @Test
  void variable_level() {
    SymbolMetadata.NullabilityData data = new Nullability.Data(NON_NULL, VARIABLE, null, null, false);
    assertThat(data.isNonNull(VARIABLE, true, false)).isTrue();
    assertThat(data.isNonNull(METHOD, true, false)).isTrue();
    assertThat(data.isNonNull(CLASS, true, false)).isTrue();
    assertThat(data.isNonNull(PACKAGE, true, false)).isTrue();
  }

  @Test
  void method_level() {
    SymbolMetadata.NullabilityData data = new Nullability.Data(NON_NULL, METHOD, null, null, false);
    assertThat(data.isNonNull(VARIABLE, true, false)).isFalse();
    assertThat(data.isNonNull(METHOD, true, false)).isTrue();
    assertThat(data.isNonNull(CLASS, true, false)).isTrue();
    assertThat(data.isNonNull(PACKAGE, true, false)).isTrue();
  }

  @Test
  void class_level() {
    SymbolMetadata.NullabilityData data = new Nullability.Data(NON_NULL, CLASS, null, null, false);
    assertThat(data.isNonNull(VARIABLE, true, false)).isFalse();
    assertThat(data.isNonNull(METHOD, true, false)).isFalse();
    assertThat(data.isNonNull(CLASS, true, false)).isTrue();
    assertThat(data.isNonNull(PACKAGE, true, false)).isTrue();
  }

  @Test
  void package_level() {
    SymbolMetadata.NullabilityData data = new Nullability.Data(NON_NULL, PACKAGE, null, null, false);
    assertThat(data.isNonNull(VARIABLE, true, false)).isFalse();
    assertThat(data.isNonNull(METHOD, true, false)).isFalse();
    assertThat(data.isNonNull(CLASS, true, false)).isFalse();
    assertThat(data.isNonNull(PACKAGE, true, false)).isTrue();
  }

  @Test
  void ignored_meta_annotation() {
    SymbolMetadata.NullabilityData data = new Nullability.Data(NON_NULL, VARIABLE, null, null, true);
    assertThat(data.isNonNull(VARIABLE, true, false)).isFalse();
    assertThat(data.isNonNull(METHOD, true, false)).isFalse();
    assertThat(data.isNonNull(CLASS, true, false)).isFalse();
    assertThat(data.isNonNull(PACKAGE, true, false)).isFalse();
  }

  @Test
  void retained_meta_annotation() {
    SymbolMetadata.NullabilityData data = new Nullability.Data(NON_NULL, CLASS, null, null, true);
    assertThat(data.isNonNull(VARIABLE, false, false)).isFalse();
    assertThat(data.isNonNull(METHOD, false, false)).isFalse();
    assertThat(data.isNonNull(CLASS, false, false)).isTrue();
    assertThat(data.isNonNull(PACKAGE, false, false)).isTrue();
  }

  @Test
  void unknown_type() {
    SymbolMetadata.NullabilityData data = new Nullability.Data(UNKNOWN, VARIABLE, null, null, false);
    assertThat(data.isStrongNullable(VARIABLE, true, false)).isFalse();
    assertThat(data.isNullable(VARIABLE, true, false)).isFalse();
    assertThat(data.isNonNull(VARIABLE, true, false)).isFalse();
    assertThat(data.isNonNull(METHOD, true, false)).isFalse();
    assertThat(data.isNonNull(CLASS, true, false)).isFalse();
    assertThat(data.isNonNull(PACKAGE, true, false)).isFalse();
  }
}
