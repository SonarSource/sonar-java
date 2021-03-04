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

import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.regex.ast.AutomatonState;
import org.sonar.java.regex.ast.BoundaryTree;
import org.sonar.java.regex.ast.LookAroundTree;
import org.sonar.java.regex.ast.RepetitionTree;

import javax.annotation.CheckForNull;
import java.util.HashMap;

import static org.sonar.java.regex.ast.AutomatonState.TransitionType.*;

public abstract class AbstractAutomataChecker {

  private final OrderedAutomataPairCache<Boolean> cache = new OrderedAutomataPairCache<>();
  private final boolean defaultAnswer;

  protected AbstractAutomataChecker(boolean defaultAnswer) {
    this.defaultAnswer = defaultAnswer;
  }

  public boolean check(SubAutomaton auto1, SubAutomaton auto2) {
    return check(auto1, auto2, false);
  }

  protected boolean check(SubAutomaton auto1, SubAutomaton auto2, boolean hasConsumedInput) {
    if (hasUnsupportedTransitionType(auto1) || hasUnsupportedTransitionType(auto2)) {
      return defaultAnswer;
    }
    OrderedAutomataPair entry = new OrderedAutomataPair(auto1, auto2);
    Boolean cachedValue = cache.startCalculation(entry, defaultAnswer);
    if (cachedValue != null) {
      return cachedValue;
    }
    boolean answer = hasConsumedInput || defaultAnswer;
    if (auto1.isAtEnd() && auto2.isAtEnd()) {
      return cache.save(entry, answer);
    } else if (auto1.isAtEnd() && auto2.incomingTransitionType() != EPSILON) {
      return cache.save(entry, auto2.allowPrefix && answer);
    } else if (auto2.isAtEnd() && auto1.incomingTransitionType() != EPSILON) {
      return cache.save(entry, auto1.allowPrefix && answer);
    } else if (auto2.incomingTransitionType() == EPSILON && !auto2.isAtEnd()) {
      return cache.save(entry, checkAuto2Successors(auto1, auto2, defaultAnswer, hasConsumedInput));
    } else if (auto1.incomingTransitionType() == EPSILON) {
      // In this branch auto1 can't be at the end
      return cache.save(entry, checkAuto1Successors(auto1, auto2, defaultAnswer, hasConsumedInput));
    } else {
      return cache.save(entry, checkAuto1AndAuto2Successors(auto1, auto2, defaultAnswer, hasConsumedInput));
    }
  }

  public void clearCache() {
    cache.clear();
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

  @VisibleForTesting
  static class OrderedAutomataPairCache<T> extends HashMap<OrderedAutomataPair, T> {

    public static final int MAX_CACHE_SIZE = 5_000;

    /**
     * If a cached value exists in the cache return it. Otherwise return null and
     * put in the cache defaultAnswer while we are in the process of calculating it
     * @param statePair to look for and return the cached value
     * @param defaultAnswer to put in the cache while we are in the process of calculating the value
     * @return cached value if exists or null if it need to be computed
     */
    @CheckForNull
    T startCalculation(OrderedAutomataPair statePair, T defaultAnswer) {
      T cachedResult = get(statePair);
      if (cachedResult != null) {
        return cachedResult;
      } else if (size() >= MAX_CACHE_SIZE) {
        return defaultAnswer;
      }
      // cache contains 'defaultAnswer' because we're currently in the process of calculating it
      put(statePair, defaultAnswer);
      return null;
    }

    T save(OrderedAutomataPair statePair, T value) {
      put(statePair, value);
      return value;
    }

  }

  @VisibleForTesting
  static class OrderedAutomataPair {
    public final SubAutomaton auto1;
    public final SubAutomaton auto2;

    public OrderedAutomataPair(SubAutomaton auto1, SubAutomaton auto2) {
      this.auto1 = auto1;
      this.auto2 = auto2;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      OrderedAutomataPair that = (OrderedAutomataPair) o;
      return auto1.equals(that.auto1) && auto2.equals(that.auto2);
    }

    @Override
    public int hashCode() {
      return 31 * auto1.hashCode() + auto2.hashCode();
    }
  }
}
