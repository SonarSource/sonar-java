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
package org.sonar.java.collections;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used for Java < 9 to simplify the creation of maps.
 * After moving to Java > 9, should be replaced by Immutable Map Static Factory Methods
 * @see <a href="https://docs.oracle.com/javase/9/docs/api/java/util/Map.html#immutable">Immutable Map Static Factory Methods</a>
 */
public final class MapBuilder<K, V> {

  public static <K, V> MapBuilder<K,V> newMap() {
    return new MapBuilder<>();
  }

  private final Map<K, V> map;

  private MapBuilder() {
    this.map = new HashMap<>();
  }

  public MapBuilder<K, V> put(K key, V value) {
    map.put(key, value);
    return this;
  }

  public Map<K, V> build() {
    return Collections.unmodifiableMap(map);
  }
}
