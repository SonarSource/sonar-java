/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.collections;

import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nullable;

public final class CollectionUtils {

  private CollectionUtils() {
  }
  
  @Nullable
  public static <T> T getFirst(Iterable<T> iterable, @Nullable T defaultValue) {
    Iterator<T> iterator = iterable.iterator();
    return iterator.hasNext() ? iterator.next() : defaultValue;
  }

  public static int size(Iterable<?> iterable) {
    return iterable instanceof Collection<?> collection ? collection.size() : size(iterable.iterator());
  }

  private static int size(Iterator<?> iterator) {
    int count;
    for(count = 0; iterator.hasNext(); ++count) {
      iterator.next();
    }

    return count;
  }

}
