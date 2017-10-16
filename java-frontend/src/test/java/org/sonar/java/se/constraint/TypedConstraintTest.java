/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TypedConstraintTest {

  @Test
  public void test_equals_hashcode() {
    Type type = mock(Type.class);
    TypedConstraint object1 = new TypedConstraint(type);
    TypedConstraint object2 = new TypedConstraint(type);
    assertThat(object1.equals(object1)).isTrue();
    assertThat(object1.equals(object2)).isTrue();
    assertThat(object1.equals(null)).isFalse();
    assertThat(object1.equals("")).isFalse();
    assertThat(object1.hashCode()).isEqualTo(object2.hashCode());

    TypedConstraint nullTC1 = new TypedConstraint(Symbols.unknownType);
    TypedConstraint nullTC2 = new TypedConstraint(Symbols.unknownType);
    assertThat(nullTC1.equals(nullTC2)).isFalse();
    assertThat(object1.equals(nullTC1)).isFalse();
  }
}
