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

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractIteratorTest {
  
  @Test
  void test_iterator() { 
    AbstractIterator<Integer> iterator = new IteratorImpl();
    int test = 10;
    while (iterator.hasNext()) {
      assertThat(iterator.next()).isEqualTo(test);
      test--;
    }
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void test_has_no_next() {
    AbstractIterator<Integer> iterator = new IteratorImpl();
    assertThat(iterator.hasNext()).isTrue();
    exhaustIterator(iterator);
    assertThat(iterator.hasNext()).isFalse();
    Assertions.assertThrows(NoSuchElementException.class, iterator::peek);
  }

  @Test
  void test_failed_next() {
    AbstractIterator<Integer> iterator = new IteratorImpl();
    exhaustIterator(iterator);
    Assertions.assertThrows(NoSuchElementException.class, iterator::next);
  }

  private void exhaustIterator(AbstractIterator<Integer> iterator) {
    for (int i = 0; i < 10; i++) {
      iterator.next();
    }
  }


  @Test
  void test_peek() {
    AbstractIterator<Integer> iterator = new IteratorImpl();
    assertThat(iterator.peek()).isEqualTo(10);
  }
  

  private static class IteratorImpl extends AbstractIterator<Integer> {
      private int counter = 10;

    @Override
    protected Integer computeNext() {
      if (counter > 0) {
        return counter--;
      }
      return endOfData();
    }
  }
}

