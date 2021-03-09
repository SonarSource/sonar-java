/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import org.sonar.java.regex.ast.AutomatonState;
import org.sonar.java.regex.ast.BoundaryTree;
import org.sonar.java.regex.ast.LookAroundTree;
import org.sonar.java.regex.ast.RepetitionTree;

import static org.sonar.java.regex.ast.AutomatonState.TransitionType.*;

public abstract class AbstractAutomataChecker {

  private final OrderedStatePairCache<Boolean> cache = new OrderedStatePairCache<>();
  private final boolean defaultAnswer;

  protected AbstractAutomataChecker(boolean defaultAnswer) {
    this.defaultAnswer = defaultAnswer;
  }

  public boolean check(SubAutomaton auto1, SubAutomaton auto2, boolean hasConsumedInput) {
    if (hasUnsupportedTransitionType(auto1) || hasUnsupportedTransitionType(auto2)) {
      return defaultAnswer;
    }
    OrderedStatePair entry = new OrderedStatePair(auto1.start, auto2.start);
    Boolean cachedValue = cache.startCalculation(entry, defaultAnswer);
    if (cachedValue != null) {
      return cachedValue;
    }
    if (auto1.isAtEnd() && auto2.isAtEnd()) {
      return cache.save(entry, hasConsumedInput || defaultAnswer);
    } else if (auto1.isAtEnd() && auto2.incomingTransitionType() != EPSILON) {
      return cache.save(entry, auto2.allowPrefix && (hasConsumedInput || defaultAnswer));
    } else if (auto2.isAtEnd() && auto1.incomingTransitionType() != EPSILON) {
      return cache.save(entry, auto1.allowPrefix && (hasConsumedInput || defaultAnswer));
    } else if (auto2.incomingTransitionType() == EPSILON && !auto2.isAtEnd()) {
      return cache.save(entry, checkAuto2Successors(auto1, auto2, defaultAnswer, hasConsumedInput));
    } else if (auto1.incomingTransitionType() == EPSILON && !auto1.isAtEnd()) {
      return cache.save(entry, checkAuto1Successors(auto1, auto2, defaultAnswer, hasConsumedInput));
    } else {
      return cache.save(entry, checkAuto1AndAuto2Successors(auto1, auto2, defaultAnswer, hasConsumedInput));
    }
  }

  protected abstract boolean checkAuto1AndAuto2Successors(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer, boolean hasConsumedInput);

  protected abstract boolean checkAuto1Successors(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer, boolean hasConsumedInput);

  protected abstract boolean checkAuto2Successors(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer, boolean hasConsumedInput);

  private static boolean hasUnsupportedTransitionType(SubAutomaton auto) {
    AutomatonState.TransitionType transition = auto.start.incomingTransitionType();
    return transition == LOOKAROUND_BACKTRACKING ||
      transition == NEGATION ||
      auto.start instanceof LookAroundTree ||
      auto.start instanceof BoundaryTree ||
      // We could support back-references by having a stack of sub-automata, onto which we push the referenced group,
      // but for now we'll simply bail here
      transition == BACK_REFERENCE ||
      // Properly supporting fixed-max loops would require unrolling the automaton, potentially making it huge
      // Technically the case where min > 1 is unsupported for the same reason, but in that case we treat it
      // as if min were 1, which should hopefully not produce a lot of FPs
      isMoreThanOneFiniteRepetition(auto.start);
  }

  private static boolean isMoreThanOneFiniteRepetition(AutomatonState state) {
    if (state instanceof RepetitionTree) {
      Integer maximumRepetitions = ((RepetitionTree) state).getQuantifier().getMaximumRepetitions();
      return maximumRepetitions != null && maximumRepetitions > 1;
    }
    return false;
  }

}
