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
package org.sonar.plugins.java.api.location;

import java.util.Comparator;
import org.sonar.java.model.location.InternalPosition;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

public interface Position extends Comparable<Position> {

  Comparator<Tree> TREE_START_POSITION_COMPARATOR = (t1, t2) -> startOf(t1).compareTo(startOf(t2));
  int FIRST_LINE = 1;
  int FIRST_COLUMN = 1;

  /**
   * The line number in a file. First line number is 1.
   */
  int line();

  /**
   * The line offset in a file. First line offset is 0. (lineOffset() == line() - 1)
   */
  int lineOffset();

  /**
   * The column number at the specified line. First column number is 1. (column() == columnOffset() + 1)
   */
  int column();

  /**
   * The column offset at the specified line. First column offset is 0. (columnOffset() == column() - 1)
   */
  int columnOffset();

  static Position at(int line, int column) {
    return new InternalPosition(line, column);
  }

  boolean isBefore(Position other);

  boolean isAfter(Position other);

  static Position startOf(Tree tree) {
    return startOf(tree.firstToken());
  }

  static Position endOf(Tree tree) {
    return endOf(tree.lastToken());
  }

  static Position startOf(SyntaxTrivia trivia) {
    return trivia.range().start();
  }

  static Position endOf(SyntaxTrivia trivia) {
    return trivia.range().end();
  }

  static Position startOf(SyntaxToken token) {
    return token.range().start();
  }

  static Position endOf(SyntaxToken token) {
    return token.range().end();
  }

}
