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

import javax.annotation.Nullable;

public class BoundaryTree extends RegexTree {

  public enum Type {
    LINE_START('^'),
    LINE_END('$'),
    WORD('b'),
    // requires brackets as well
    UNICODE_EXTENDED_GRAPHEME_CLUSTER('b'),
    NON_WORD('B'),
    INPUT_START('A'),
    PREVIOUS_MATCH_END('G'),
    INPUT_END_FINAL_TERMINATOR('Z'),
    INPUT_END('z');

    private final char key;

    Type(char key) {
      this.key = key;
    }

    @Nullable
    public static Type forKey(char k) {
      for (Type type : Type.values()) {
        if (type.key == k) {
          return type;
        }
      }
      return null;
    }
  }

  private final Type type;

  public BoundaryTree(RegexSource source, Type type, IndexRange range) {
    super(source, range);
    this.type = type;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitBoundary(this);
  }

  @Override
  public RegexTree.Kind kind() {
    return RegexTree.Kind.BOUNDARY;
  }

  Type type() {
    return type;
  }

}
