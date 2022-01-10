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
package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.regex.ast.FinalState;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SubAutomatonTest {

  @Test
  void testEqualsAndHashcode() {
    SubAutomaton subAutomaton1 = new SubAutomaton(new FinalState(new FlagSet()), new FinalState(new FlagSet()), false);
    SubAutomaton subAutomaton2 = new SubAutomaton(new FinalState(new FlagSet()), new FinalState(new FlagSet()), false);
    SubAutomaton subAutomaton3 = new SubAutomaton(subAutomaton1.start, subAutomaton1.end, true);
    SubAutomaton subAutomaton4 = new SubAutomaton(subAutomaton1.start, subAutomaton1.end, false);
    SubAutomaton subAutomaton5 = new SubAutomaton(subAutomaton1.start, subAutomaton2.end, false);
    SubAutomaton subAutomaton6 = new SubAutomaton(subAutomaton2.start, subAutomaton1.end, false);

    assertThat(subAutomaton1)
      .isNotEqualTo(null)
      .isNotEqualTo("null")
      .isNotEqualTo(subAutomaton2)
      .isNotEqualTo(subAutomaton3)
      .isNotEqualTo(subAutomaton5)
      .isNotEqualTo(subAutomaton6)
      .isEqualTo(subAutomaton4)
      .isEqualTo(subAutomaton1)
      .hasSameHashCodeAs(subAutomaton4);

    // isEqualTo() in this case doesn't actually call .equals() method
    assertThat(subAutomaton1.equals(subAutomaton1)).isTrue();
    assertThat(subAutomaton1.equals("subAutomaton1")).isFalse();
    assertThat(subAutomaton1.equals(null)).isFalse();
  }
}
