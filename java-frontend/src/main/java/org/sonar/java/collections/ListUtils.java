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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class ListUtils {

  private ListUtils() {
  }
  
  public static <T> T getLast(List<T> list) {
    return list.get(list.size() - 1);
  }

  public static <T> T getOnlyElement(List<T> list) {
    if (list.size() == 1) {
      return list.get(0);
    }
    throw new IllegalArgumentException(String.format("Expected list of size 1, but was list of size %d.", list.size()));
  }

  public static <T> List<T> reverse(List<T> list) {
    List<T> reversed = new ArrayList<>(list);
    Collections.reverse(reversed);
    return reversed;
  }

  @SafeVarargs
  public static <T> List<T> concat(List<? extends T>... lists) {
    return Arrays.stream(lists)
      .flatMap(List::stream)
      .collect(Collectors.toList());
  }

  @SafeVarargs
  public static <T> List<T> concat(Iterable<? extends T>... iterables) {
    return Arrays.stream(iterables)
      .flatMap(it -> StreamSupport.stream(it.spliterator(), false))
      .collect(Collectors.toList());
  }

  public static <T> List<T> alternate(List<? extends T> list1, List<? extends T> list2) {
    int listSize = list1.size();
    int separatorsSize = list2.size();
    List<T> result = new ArrayList<>(listSize + separatorsSize);
    Iterator<? extends T> list1Iterator = list1.iterator();
    Iterator<? extends T> list2Iterator = list2.iterator();
    while (list1Iterator.hasNext() || list2Iterator.hasNext()) {
      if (list1Iterator.hasNext()){
        result.add(list1Iterator.next());
      }
      if (list2Iterator.hasNext()){
        result.add(list2Iterator.next());
      }
    }
    return result;
  }
}
