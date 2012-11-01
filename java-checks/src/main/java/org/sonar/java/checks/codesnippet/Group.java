/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks.codesnippet;

import com.google.common.collect.Lists;

import java.util.List;

public class Group {

  private final List<Integer> indexesI = Lists.newLinkedList();
  private final List<Integer> indexesJ = Lists.newLinkedList();

  public Group prepend(int i, int j) {
    indexesI.add(0, i);
    indexesJ.add(0, j);

    return this;
  }

  public Group append(int i, int j) {
    indexesI.add(i);
    indexesJ.add(j);

    return this;
  }

  public List<Integer> getIndexesI() {
    return indexesI;
  }

  public List<Integer> getIndexesJ() {
    return indexesJ;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + indexesI.hashCode();
    result = prime * result + indexesJ.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Group other = (Group) obj;
    if (!indexesI.equals(other.indexesI)) {
      return false;
    }
    if (!indexesJ.equals(other.indexesJ)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "i = " + indexesI + ", j = " + indexesJ;
  }

}
