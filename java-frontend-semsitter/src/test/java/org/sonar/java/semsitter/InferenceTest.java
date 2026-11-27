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

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InferenceTest {

  private final JTypeFixtures fixtures = new JTypeFixtures();
  private final Inference inference = fixtures.sema.inference();

  @Test
  void declared_types_stand_until_inference_lands() {
    JMethodSymbol valueOf = fixtures.method("java.lang.String", "valueOf", "java.lang.Object");
    JType string = fixtures.string();

    assertThat(inference.invocationReturn(valueOf, null, List.of(string))).isSameAs(valueOf.returnType().type());
    assertThat(inference.rawReturn(valueOf)).isNull();
    assertThat(inference.invocationThrown(valueOf, null, List.of(string))).isNull();
    assertThat(inference.targetedReturn(valueOf, string)).isNull();
    assertThat(inference.retargetedElement(valueOf, string, string, string)).isNull();
    assertThat(inference.inferDiamond(string, string)).isSameAs(string);
    assertThat(inference.inferDiamondFromArguments(valueOf, string, List.of(string))).isSameAs(string);
    assertThat(inference.groundType(string)).isSameAs(string);
    assertThat(inference.captureWildcards(string)).isSameAs(string);
    assertThat(inference.declaresTypeVariables(valueOf, true)).isFalse();
  }
}
