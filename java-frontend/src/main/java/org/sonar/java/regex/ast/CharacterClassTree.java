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

public class CharacterClassTree extends RegexTree implements CharacterClassElementTree {

  private final JavaCharacter openingBracket;

  private final CharacterClassElementTree contents;

  private final boolean negated;

  public CharacterClassTree(RegexSource source, IndexRange range, JavaCharacter openingBracket, boolean negated,
    CharacterClassElementTree contents, FlagSet activeFlags) {
    super(source, range, activeFlags);
    this.negated = negated;
    this.contents = contents;
    this.openingBracket = openingBracket;
  }

  public CharacterClassElementTree getContents() {
    return contents;
  }

  public boolean isNegated() {
    return negated;
  }

  public JavaCharacter getOpeningBracket() {
    return openingBracket;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitCharacterClass(this);
  }

  @Override
  public RegexTree.Kind kind() {
    return RegexTree.Kind.CHARACTER_CLASS;
  }

  @Nonnull
  @Override
  public CharacterClassElementTree.Kind characterClassElementKind() {
    return CharacterClassElementTree.Kind.NESTED_CHARACTER_CLASS;
  }

  @Nonnull
  @Override
  public TransitionType incomingTransitionType() {
    return TransitionType.CHARACTER;
  }

}
