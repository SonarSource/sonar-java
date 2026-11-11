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
package org.sonar.java.jdt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.testing.ClassLayoutTestUtils.countFields;

class JDTClassesLayoutTest {

  @Test
  void type() {
    assertThat(countFields(JDTType.class)).isEqualTo(7);
  }

  @Test
  void symbol_type() {
    assertThat(countFields(JDTTypeSymbol.class)).isEqualTo(11);
  }

  @Test
  void symbol_method() {
    assertThat(countFields(JDTMethodSymbol.class)).isEqualTo(11);
  }

  @Test
  void symbol_variable() {
    assertThat(countFields(JDTVariableSymbol.class)).isEqualTo(7);
  }

  @Test
  void annotation() {
    assertThat(countFields(JDTSymbolMetadata.JAnnotationInstance.class)).isEqualTo(3);
  }
}
