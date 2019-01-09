/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.se.constraint;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConstraintTest {

  @Test
  public void ObjectConstraint_should_be_valid_with_correct_constraints() throws Exception {
    assertThat(ObjectConstraint.NULL.isValidWith(null)).isTrue();
    assertThat(ObjectConstraint.NOT_NULL.isValidWith(null)).isTrue();
    assertThat(ObjectConstraint.NULL.isValidWith(ObjectConstraint.NULL)).isTrue();
    assertThat(ObjectConstraint.NOT_NULL.isValidWith(ObjectConstraint.NOT_NULL)).isTrue();
    assertThat(ObjectConstraint.NOT_NULL.isValidWith(ObjectConstraint.NULL)).isFalse();
    assertThat(ObjectConstraint.NULL.isValidWith(ObjectConstraint.NOT_NULL)).isFalse();
    assertThat(ObjectConstraint.NULL.isValidWith(BooleanConstraint.TRUE)).isFalse();
  }

  @Test
  public void default_methods_result() {
    Constraint c = new Constraint() {};
    assertThat(c.hasPreciseValue()).isFalse();
    assertThat(c.valueAsString()).isEmpty();
    assertThat(c.inverse()).isNull();
  }
}
