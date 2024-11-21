/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.se.constraint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintTest {

  @Test
  void ObjectConstraint_should_be_valid_with_correct_constraints() {
    assertThat(ObjectConstraint.NULL.isValidWith(null)).isTrue();
    assertThat(ObjectConstraint.NOT_NULL.isValidWith(null)).isTrue();
    assertThat(ObjectConstraint.NULL.isValidWith(ObjectConstraint.NULL)).isTrue();
    assertThat(ObjectConstraint.NOT_NULL.isValidWith(ObjectConstraint.NOT_NULL)).isTrue();
    assertThat(ObjectConstraint.NOT_NULL.isValidWith(ObjectConstraint.NULL)).isFalse();
    assertThat(ObjectConstraint.NULL.isValidWith(ObjectConstraint.NOT_NULL)).isFalse();
    assertThat(ObjectConstraint.NULL.isValidWith(BooleanConstraint.TRUE)).isFalse();
  }

  @Test
  void default_methods_result() {
    Constraint c = new Constraint() {};
    assertThat(c.hasPreciseValue()).isFalse();
    assertThat(c.valueAsString()).isEmpty();
    assertThat(c.inverse()).isNull();
  }
}
