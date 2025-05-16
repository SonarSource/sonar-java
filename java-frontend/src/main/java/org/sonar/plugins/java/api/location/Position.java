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

  Position relativeTo(Position other);
}
