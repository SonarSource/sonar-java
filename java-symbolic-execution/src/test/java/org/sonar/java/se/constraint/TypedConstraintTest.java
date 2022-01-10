/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.sonar.java.model.Symbols;

import static org.assertj.core.api.Assertions.assertThat;

class TypedConstraintTest {

  @Test
  void test_equals_hashcode() {
    TypedConstraint object1 = new TypedConstraint("java.lang.String");
    TypedConstraint object2 = new TypedConstraint("java.lang.String");
    assertThat(object1)
      .isEqualTo(object1)
      .isEqualTo(object2)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .hasSameHashCodeAs(object2);

    TypedConstraint nullTC1 = new TypedConstraint(Symbols.unknownType.fullyQualifiedName());
    TypedConstraint nullTC2 = new TypedConstraint(Symbols.unknownType.fullyQualifiedName());
    assertThat(nullTC1).isNotEqualTo(nullTC2);
    assertThat(object1).isNotEqualTo(nullTC1);
  }
}
