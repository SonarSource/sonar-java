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
package org.sonar.java.regex.ast;

import org.sonar.plugins.java.api.tree.Tree;

public class Location {

  private final Tree javaTree;

  private final IndexRange indexRange;

  public Location(Tree javaTree, int beginningOffset, int endingOffset) {
    this.javaTree = javaTree;
    this.indexRange = new IndexRange(beginningOffset, endingOffset);
  }

  public Tree getJavaTree() {
    return javaTree;
  }

  public IndexRange getIndexRange() {
    return indexRange;
  }

  public int getBeginningOffset() {
    return indexRange.getBeginningOffset();
  }

  public int getEndingOffset() {
    return indexRange.getEndingOffset();
  }

  public boolean isEmpty() {
    return indexRange.isEmpty();
  }

}
