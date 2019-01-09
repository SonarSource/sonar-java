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
import java.util.function.Predicate;

/**
 * Persistent (functional) Stack.
 *
 * @param <E> the type of elements maintained by this stack
 */
public interface PStack<E> {

  /**
   * @return new stack with added element
   */
  PStack<E> push(E e);

  /**
   * @return element at the top of this stack
   * @throws IllegalStateException if this stack is empty.
   */
  E peek();

  /**
   *
   * @param i - index of element to be returned, 0 means top of the stack
   * @return i-th element from top of the stack
   * @throws IllegalStateException if stack has less than i elements
   */
  E peek(int i);

  /**
   * @return new stack with removed element
   * @throws IllegalStateException if this stack is empty.
   */
  PStack<E> pop();

  /**
   * @return true if this stack contains no elements
   */
  boolean isEmpty();

  /**
   * Performs the given action for each element in this stack until all elements have been processed or the action throws an exception.
   */
  void forEach(Consumer<E> action);

  /**
   * Test given predicate on elements and return true if any of elements matches the predicate
   * @param predicate predicate to be tested
   * @return true if any of the stack elements satisfies the predicate
   */
  boolean anyMatch(Predicate<E> predicate);

  /**
   * Naive implementation has O(n) time complexity, where n is number of elements. More clever implementation could take advantage of PStack's immutability
   * @return number of elements in the stack
   */
  int size();

  /**
   * @return a string representation of this stack
   */
  @Override
  String toString();

}
