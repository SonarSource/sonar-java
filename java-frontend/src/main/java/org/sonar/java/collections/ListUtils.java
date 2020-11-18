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
package org.sonar.java.collections;

import java.util.List;

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
}
