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
package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Test;
import org.sonar.java.regex.ast.FinalState;

import static org.assertj.core.api.Assertions.assertThat;

class OrderedStatePairCacheTest {

  OrderedStatePairCache<String> cache = new OrderedStatePairCache<>();

  @Test
  void test() {
    for (int i = 0; i < OrderedStatePairCache.MAX_CACHE_SIZE; i++) {
      OrderedStatePair pair = createPair();
      assertThat(cache.startCalculation(pair, "default")).isNull();
      assertThat(cache.startCalculation(pair, "default")).isEqualTo("default");
      assertThat(cache.save(pair, "foo")).isEqualTo("foo");
      assertThat(cache.startCalculation(pair, "default")).isEqualTo("foo");
    }
    assertThat(cache.startCalculation(createPair(), "default")).isEqualTo("default");
    assertThat(cache.startCalculation(createPair(), "default")).isEqualTo("default");
  }

  private static OrderedStatePair createPair() {
    return new OrderedStatePair(new FinalState(), new FinalState());
  }

}
