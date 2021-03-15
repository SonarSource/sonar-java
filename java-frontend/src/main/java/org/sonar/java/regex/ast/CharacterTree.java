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
import org.sonar.java.regex.RegexSource;

public class CharacterTree extends RegexTree implements CharacterClassElementTree {

  private final int codePoint;
  private final boolean isEscapeSequence;

  public CharacterTree(RegexSource source, IndexRange range, int codePoint, boolean isEscapeSequence, FlagSet activeFlags) {
    super(source, range, activeFlags);
    this.codePoint = codePoint;
    this.isEscapeSequence = isEscapeSequence;
  }

  public int codePointOrUnit() {
    return codePoint;
  }

  public boolean isEscapeSequence() {
    return isEscapeSequence;
  }

  public String characterAsString() {
    return String.valueOf(Character.toChars(codePoint));
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitCharacter(this);
  }

  @Override
  public RegexTree.Kind kind() {
    return RegexTree.Kind.CHARACTER;
  }

  @Nonnull
  @Override
  public CharacterClassElementTree.Kind characterClassElementKind() {
    return CharacterClassElementTree.Kind.PLAIN_CHARACTER;
  }

  @Nonnull
  @Override
  public TransitionType incomingTransitionType() {
    return TransitionType.CHARACTER;
  }
}
