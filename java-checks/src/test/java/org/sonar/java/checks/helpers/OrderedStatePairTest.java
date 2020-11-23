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
import org.sonar.java.regex.ast.AutomatonState;
import org.sonar.java.regex.ast.FinalState;

import static org.assertj.core.api.Assertions.assertThat;

class OrderedStatePairTest {

  @Test
  void equals_and_hashCode() {
    AutomatonState state1 = new FinalState();
    AutomatonState state2 = new FinalState();

    OrderedStatePair pairS1S2A = new OrderedStatePair(state1, state2);
    assertThat(pairS1S2A.equals(pairS1S2A)).isTrue();
    assertThat(pairS1S2A.equals(null)).isFalse();
    assertThat(pairS1S2A.equals("")).isFalse();
    assertThat(pairS1S2A).isEqualTo(pairS1S2A).hasSameHashCodeAs(pairS1S2A);

    OrderedStatePair pairS1S2B = new OrderedStatePair(state1, state2);
    assertThat(pairS1S2B).isEqualTo(pairS1S2A).hasSameHashCodeAs(pairS1S2A);

    OrderedStatePair pairS2S1 = new OrderedStatePair(state2, state1);
    assertThat(pairS2S1).isNotEqualTo(pairS1S2A);

    OrderedStatePair pairS1S1 = new OrderedStatePair(state1, state1);
    assertThat(pairS1S1).isNotEqualTo(pairS1S2A);
  }

}
