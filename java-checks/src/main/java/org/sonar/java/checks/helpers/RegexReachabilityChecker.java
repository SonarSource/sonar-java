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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.sonarsource.analyzer.commons.regex.ast.AutomatonState;

public class RegexReachabilityChecker {
  private static final int MAX_CACHE_SIZE = 5_000;

  private final boolean defaultAnswer;
  private final Map<OrderedStatePair, Boolean> cache = new HashMap<>();

  public RegexReachabilityChecker(boolean defaultAnswer) {
    this.defaultAnswer = defaultAnswer;
  }

  public void clearCache() {
    cache.clear();
  }

  public boolean canReach(AutomatonState start, AutomatonState goal) {
    if (start == goal) {
      return true;
    }
    OrderedStatePair pair = new OrderedStatePair(start, goal);
    if (cache.containsKey(pair)) {
      return cache.get(pair);
    }
    if (cache.size() >= MAX_CACHE_SIZE) {
      return defaultAnswer;
    }
    cache.put(pair, false);
    boolean result = false;
    for (AutomatonState successor : start.successors()) {
      if (canReach(successor, goal)) {
        result = true;
        break;
      }
    }
    cache.put(pair, result);
    return result;
  }

  private static class OrderedStatePair {
    private final AutomatonState source;
    private final AutomatonState target;

    OrderedStatePair(AutomatonState source, AutomatonState target) {
      this.source = source;
      this.target = target;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof OrderedStatePair)) return false;
      OrderedStatePair that = (OrderedStatePair) o;
      return source == that.source && target == that.target;
    }

    @Override
    public int hashCode() {
      return Objects.hash(source, target);
    }
  }
}
