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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MapBuilder<K, V> {

  public static <K, V> MapBuilder<K,V> newMap() {
    return new MapBuilder<>();
  }

  private final Map<K, V> map;

  private MapBuilder() {
    this.map = new HashMap<>();
  }

  public MapBuilder<K, V> add(K key, V value) {
    map.put(key, value);
    return this;
  }

  public Map<K, V> build() {
    return Collections.unmodifiableMap(map);
  }
}
