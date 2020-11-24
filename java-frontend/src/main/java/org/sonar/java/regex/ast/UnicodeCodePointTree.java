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

import javax.annotation.Nonnull;

/**
 * Represents the \\x{N...N} sequence in a regular expression, which specifies a single Unicode code point.
 * This differs from PlainCharacterTree in that it will match a single code point even if it consists of
 * multiple multiple UTF-16 code units (i.e. multiple Java chars).
 */
public class UnicodeCodePointTree extends CharacterTree {

  private final int codePoint;

  public UnicodeCodePointTree(RegexSource source, IndexRange range, int codePoint, FlagSet activeFlags) {
    super(source, range, activeFlags);
    this.codePoint = codePoint;
  }

  @Override
  public int codePointOrUnit() {
    return codePoint;
  }

  @Override
  public boolean isEscapeSequence() {
    return true;
  }

  @Override
  public String characterAsString() {
    return String.valueOf(Character.toChars(codePoint));
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitUnicodeCodePoint(this);
  }

  @Override
  public RegexTree.Kind kind() {
    return RegexTree.Kind.UNICODE_CODE_POINT;
  }

  @Nonnull
  @Override
  public CharacterClassElementTree.Kind characterClassElementKind() {
    return CharacterClassElementTree.Kind.UNICODE_CODE_POINT;
  }

}
