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

import static org.assertj.core.api.Assertions.assertThat;

class FunctionalInferenceTest {

  private final JTypeFixtures fixtures = new JTypeFixtures();
  private final FunctionalInference functional = fixtures.sema.functionalInference();

  @Test
  void functional_types_are_unknown_until_inference_lands() {
    JMethodSymbol valueOf = fixtures.method("java.lang.String", "valueOf", "java.lang.Object");
    JType string = fixtures.string();

    assertThat(functional.functionProducing(string, string)).isNull();
    assertThat(functional.returnsClassVariable(valueOf)).isFalse();
    assertThat(functional.unboundReferenceResult(valueOf, string)).isNull();
    assertThat(functional.targetedParameters(valueOf, string)).isNull();
  }
}
