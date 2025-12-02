/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class CollectionUtilsTest {
  
  @Test
  void test_get_first_String() {
    List<String> list = Arrays.asList("A", "B", "Z");
    assertThat(CollectionUtils.getFirst(list, null)).isEqualTo("A");
  }

  @Test
  void test_get_first_default_value() {
    assertThat(CollectionUtils.getFirst(Collections.emptySet(), "ABC")).isEqualTo("ABC");
  }

  @Test
  void test_get_collection_size() {
    assertThat(CollectionUtils.size(Arrays.asList("a", "b", "c"))).isEqualTo(3);
  }
  
  @Test
  void test_get_iterable_size() {
    assertThat(CollectionUtils.size(new SomeIterable<String>())).isEqualTo(3);
  }
  
  private static class SomeIterable<T> implements Iterable<T> {
    
    @Override
    public Iterator<T> iterator() {
      return new Iterator<T>() {
        private int count = 3;
        @Override
        public boolean hasNext() {
          return count > 0;
        }

        @Override
        @Nullable
        public T next() {
          count--;
          return null;
        }
      };
    }
  }
}
