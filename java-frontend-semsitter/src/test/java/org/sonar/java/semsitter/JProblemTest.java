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
import org.sonar.plugins.java.api.diagnostics.ProblemKind;

import static org.assertj.core.api.Assertions.assertThat;

class JProblemTest {

  @Test
  void undefined_type_names_the_type() {
    JProblem problem = JProblem.undefinedType("p.Missing");

    assertThat(problem.type()).isEqualTo(ProblemKind.UNDEFINED_TYPE);
    assertThat(problem.message()).isEqualTo("p.Missing cannot be resolved to a type");
    assertThat(problem).hasToString("p.Missing cannot be resolved to a type");
  }

  @Test
  void problems_are_equal_by_kind_and_message() {
    JProblem problem = new JProblem("m", ProblemKind.UNDEFINED_TYPE);

    assertThat(problem)
      .isEqualTo(new JProblem("m", ProblemKind.UNDEFINED_TYPE))
      .hasSameHashCodeAs(new JProblem("m", ProblemKind.UNDEFINED_TYPE))
      .isNotEqualTo(new JProblem("other", ProblemKind.UNDEFINED_TYPE))
      .isNotEqualTo(new JProblem("m", ProblemKind.UNUSED_IMPORT))
      .isNotEqualTo(new JProblem("m", ProblemKind.UNDEFINED_TYPE) {
      })
      .isNotEqualTo("m");
  }
}
