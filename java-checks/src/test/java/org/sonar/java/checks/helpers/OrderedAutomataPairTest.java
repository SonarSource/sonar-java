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
import org.sonarsource.analyzer.commons.regex.ast.AutomatonState;
import org.sonarsource.analyzer.commons.regex.ast.FinalState;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;

import static org.assertj.core.api.Assertions.assertThat;

class OrderedAutomataPairTest {

  @Test
  void equals_and_hashCode() {
    AutomatonState state11 = new FinalState(new FlagSet());
    AutomatonState state12 = new FinalState(new FlagSet());
    AutomatonState state21 = new FinalState(new FlagSet());
    AutomatonState state22 = new FinalState(new FlagSet());

    SubAutomaton auto1 = new SubAutomaton(state11, state12, false);
    SubAutomaton auto2 = new SubAutomaton(state21, state22, false);

    AbstractAutomataChecker.OrderedAutomataPair pairS1S2A = new AbstractAutomataChecker.OrderedAutomataPair(auto1, auto2, false);
    assertThat(pairS1S2A)
      .isNotNull()
      .isNotEqualTo("")
      .isEqualTo(pairS1S2A)
      .hasSameHashCodeAs(pairS1S2A);

    // isEqualTo() in this case doesn't actually call .equals() method
    assertThat(pairS1S2A.equals(pairS1S2A)).isTrue();
    assertThat(pairS1S2A.equals("pairS1S2A")).isFalse();
    assertThat(pairS1S2A.equals(null)).isFalse();

    AbstractAutomataChecker.OrderedAutomataPair pairS1S2B = new AbstractAutomataChecker.OrderedAutomataPair(auto1, auto2, false);
    assertThat(pairS1S2B).isEqualTo(pairS1S2A).hasSameHashCodeAs(pairS1S2A);

    AbstractAutomataChecker.OrderedAutomataPair pairS1S2BWithConsumedInput = new AbstractAutomataChecker.OrderedAutomataPair(auto1, auto2, true);
    assertThat(pairS1S2BWithConsumedInput).isNotEqualTo(pairS1S2A);


    AbstractAutomataChecker.OrderedAutomataPair pairS2S1 = new AbstractAutomataChecker.OrderedAutomataPair(auto2, auto1, false);
    assertThat(pairS2S1).isNotEqualTo(pairS1S2A);

    AbstractAutomataChecker.OrderedAutomataPair pairS1S1 = new AbstractAutomataChecker.OrderedAutomataPair(auto1, auto1, false);
    assertThat(pairS1S1).isNotEqualTo(pairS1S2A);
  }

}
