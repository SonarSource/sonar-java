/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nullable;

public final class CollectionUtils {

  private CollectionUtils() {
  }
  
  private static int size(Iterator<?> iterator) {
    int count;
    for(count = 0; iterator.hasNext(); ++count) {
      iterator.next();
    }

    return count;
  }

  @Nullable
  public static <T> T getFirst(Iterable<T> iterable, @Nullable T defaultValue) {
    Iterator<T> iterator = iterable.iterator();
    return iterator.hasNext() ? iterator.next() : defaultValue;
  }

  public static int size(Iterable<?> iterable) {
    return iterable instanceof Collection<?> collection ? collection.size() : size(iterable.iterator());
  }

}
