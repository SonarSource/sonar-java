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
package org.sonar.java.regex.ast;

import javax.annotation.Nonnull;

public interface CharacterClassElementTree extends RegexSyntaxElement {

  enum Kind {
    INTERSECTION,
    UNION,
    NEGATION,
    CHARACTER_RANGE,
    ESCAPED_CHARACTER_CLASS,
    PLAIN_CHARACTER,
    UNICODE_CODE_POINT,
    MISC_ESCAPE_SEQUENCE,
    NESTED_CHARACTER_CLASS
  }

  @Nonnull
  Kind characterClassElementKind();

  void accept(RegexVisitor visitor);

  default boolean is(Kind... kinds) {
    Kind thisKind = characterClassElementKind();
    for (Kind kind : kinds) {
      if (thisKind == kind) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  FlagSet activeFlags();

}
