/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.collections;

import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class SetUtilsTest {
  
  @Test
  void test_returns_unmodifiable_set() {
    Set<String> set = SetUtils.immutableSetOf();
    Assertions.assertThrows(UnsupportedOperationException.class, () -> set.add("value"));
  }

  @Test
  void test_construct_strings_set() {
    Set<String> set = SetUtils.immutableSetOf("value1", "value2");
    
    assertThat(set).containsExactlyInAnyOrder("value1", "value2");
  }

  @Test
  void test_construct_any_set() {
    Set<SomeType> set = SetUtils.immutableSetOf(new SomeType("value1"), new SomeType("value2"));
    
    assertThat(set).containsExactlyInAnyOrder(new SomeType("value1"), new SomeType("value2"));
  }

  @Test
  void test_concat_any_set() {
    Set<SomeType> set1 = SetUtils.immutableSetOf(new SomeType("value1"), new SomeType("value2"));
    Set<SomeType> set2 = SetUtils.immutableSetOf(new SomeType("value3"), new SomeType("value4"));
    
    assertThat(SetUtils.concat(set1, set2))
      .containsExactlyInAnyOrder(new SomeType("value1"), new SomeType("value2"), 
        new SomeType("value3"), new SomeType("value4"));
  }

  @Test
  void test_concat_many_sets() {
    Set<SomeType> set1 = SetUtils.immutableSetOf(new SomeType("value1"), new SomeType("value2"));
    Set<SomeType> set2 = SetUtils.immutableSetOf(new SomeType("value3"), new SomeType("value4"));
    Set<SomeType> set3 = SetUtils.immutableSetOf(new SomeType("value5"), new SomeType("value6"));
    Set<SomeType> set4 = SetUtils.immutableSetOf(new SomeType("value7"), new SomeType("value8"));
    Set<SomeType> set5 = SetUtils.immutableSetOf(new SomeType("value9"), new SomeType("value10"));
    Set<SomeType> set6 = SetUtils.immutableSetOf(new SomeType("value11"), new SomeType("value12"));
    Set<SomeType> set7 = SetUtils.immutableSetOf(new SomeType("value13"), new SomeType("value14"));
    
    assertThat(SetUtils.concat(set1, set2, set3, set4, set5, set6, set7))
      .containsExactlyInAnyOrder(
        new SomeType("value1"), new SomeType("value2"), 
        new SomeType("value3"), new SomeType("value4"),
        new SomeType("value5"), new SomeType("value6"),
        new SomeType("value7"), new SomeType("value8"),
        new SomeType("value9"), new SomeType("value10"),
        new SomeType("value11"), new SomeType("value12"),
        new SomeType("value13"), new SomeType("value14")
      );
  }
  
  private static class SomeType {
    final String value;

    private SomeType(String value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SomeType value1 = (SomeType) o;
      return Objects.equals(value, value1.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }
}
