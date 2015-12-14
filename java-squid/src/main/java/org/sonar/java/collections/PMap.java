/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

/**
 * Persistent (functional) Map.
 *
 * @author Evgeny Mandrikov
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public interface PMap<K, V> {

  /**
   * @return new map with added key-value pair
   */
  PMap<K, V> put(K key, V value);

  /**
   * @return new map with removed key, or this if map does not contain given key
   */
  PMap<K, V> remove(K key);

  /**
   * @return value associated with given key, or null if not found
   */
  @Nullable
  V get(K key);

  /**
   * Performs the given action for each entry in this map until all entries have been processed or the action throws an exception.
   */
  void forEach(Consumer<K, V> action);

  /**
   * Iterates over all elements.
   */
  Iterator<Map.Entry<K, V>> entriesIterator();

  /**
   * @return true if this map contains no elements
   */
  boolean isEmpty();

  /**
   * Represents an operation that accepts two input arguments and returns no result.
   *
   * This interface intended to be a <i>functional interface</i> as defined by the Java Language Specification.
   */
  interface Consumer<K, V> {

    void accept(K key, V value);

  }

}
