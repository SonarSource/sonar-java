/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.function.Consumer;

/**
 * Persistent (functional) Set.
 *
 * @author Evgeny Mandrikov
 * @param <E> the type of elements maintained by this set
 */
public interface PSet<E> {

  /**
   * @return new set with added element, or this if element already in the set
   */
  PSet<E> add(E e);

  /**
   * @return new set with removed element, or this if set does not contain given element
   */
  PSet<E> remove(E e);

  /**
   * @return true if this set contains the specified element
   */
  boolean contains(E e);

  /**
   * Performs the given action for each entry in this set until all elements have been processed or the action throws an exception.
   */
  void forEach(Consumer<E> action);

  /**
   * @return true if this set contains no elements
   */
  boolean isEmpty();

  /**
   * The string representation consists of a list of elements in the ascending order of hash codes.
   * If two elements have same hash code, then their relative order is arbitrary, but stable.
   *
   * @return a string representation of this set
   */
  @Override
  String toString();

}
