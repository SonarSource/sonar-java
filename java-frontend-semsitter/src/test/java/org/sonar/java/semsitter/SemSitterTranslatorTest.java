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
package org.sonar.java.semsitter;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;

import static org.assertj.core.api.Assertions.assertThat;

class SemSitterTranslatorTest {

  @Test
  void an_unresolved_method_is_owned_by_the_unknown_type() {
    JMethodSymbol method = new JMethodSymbol(new JSema(), null, "log");
    method.unknown(true);

    assertThat(method.owner()).isSameAs(Symbol.TypeSymbol.UNKNOWN_TYPE);
    assertThat(((Symbol.TypeSymbol) method.owner()).memberSymbols()).isEmpty();
  }
}
