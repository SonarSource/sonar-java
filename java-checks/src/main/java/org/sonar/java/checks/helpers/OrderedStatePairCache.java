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

import java.util.HashMap;
import javax.annotation.CheckForNull;

public class OrderedStatePairCache<T> extends HashMap<OrderedStatePair, T> {

  public static final int MAX_CACHE_SIZE = 5_000;

  /**
   * If a cached value exists in the cache return it. Otherwise return null and
   * put in the cache defaultAnswer while we are in the process of calculating it
   * @param statePair to look for and return the cached value
   * @param defaultAnswer to put in the cache while we are in the process of calculating the value
   * @return cached value if exists or null if it need to be computed
   */
  @CheckForNull
  T startCalculation(OrderedStatePair statePair, T defaultAnswer) {
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

  T save(OrderedStatePair statePair, T value) {
    put(statePair, value);
    return value;
  }

}
