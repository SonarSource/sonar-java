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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ListUtilsTest {
  
  @Test
  void test_get_last_String() {
    List<String> list = Arrays.asList("A", "B", "Z");
    assertThat(ListUtils.getLast(list)).isEqualTo("Z");
  }

  @Test
  void test_construct_any_map() {

    List<SomeType> list = Arrays.asList(new SomeType("value1"), new SomeType("value2"));
    
    assertThat(ListUtils.getLast(list)).isEqualTo(new SomeType("value2"));
  }

  @Test
  void test_get_the_only_element() {
    List<String> list = Collections.singletonList("A");
    assertThat(ListUtils.getOnlyElement(list)).isEqualTo("A");
  }

  @Test
  void test_get_the_only_element_with_empty_list() {
    List<String> list = Collections.emptyList();
    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> ListUtils.getOnlyElement(list));
    assertThat(exception).hasMessage("Expected list of size 1, but was list of size 0.");
  }

  @Test
  void test_get_the_only_element_with_too_big_list() {
    List<String> list = Arrays.asList("A", "B");
    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> ListUtils.getOnlyElement(list));
    assertThat(exception).hasMessage("Expected list of size 1, but was list of size 2.");
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
