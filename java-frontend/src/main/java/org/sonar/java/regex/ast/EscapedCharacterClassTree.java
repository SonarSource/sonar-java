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
import javax.annotation.Nullable;
import org.sonar.java.regex.RegexSource;

public class EscapedCharacterClassTree extends RegexTree implements CharacterClassElementTree {

  private final char type;

  @Nullable
  private final String property;

  private EscapedCharacterClassTree(RegexSource source, IndexRange range, SourceCharacter marker, @Nullable String property, FlagSet activeFlags) {
    super(source, range, activeFlags);
    this.type = marker.getCharacter();
    this.property = property;
  }

  public EscapedCharacterClassTree(RegexSource source, SourceCharacter backslash, SourceCharacter marker, SourceCharacter openingCurlyBrace,
    SourceCharacter closingCurlyBrace, FlagSet activeFlags) {
    this(
      source,
      backslash.getRange().merge(closingCurlyBrace.getRange()),
      marker,
      source.substringAt(
        new IndexRange(
          openingCurlyBrace.getRange().getBeginningOffset() + 1,
          closingCurlyBrace.getRange().getBeginningOffset())),
      activeFlags
    );
    if (!isProperty()) {
      throw new IllegalArgumentException("Only \\p can have a property string");
    }
  }

  public EscapedCharacterClassTree(RegexSource source, SourceCharacter backslash, SourceCharacter marker, FlagSet activeFlags) {
    this(source, backslash.getRange().merge(marker.getRange()), marker, null, activeFlags);
    if (isProperty()) {
      throw new IllegalArgumentException("\\p needs a property string");
    }
  }

  @Nonnull
  @Override
  public CharacterClassElementTree.Kind characterClassElementKind() {
    return CharacterClassElementTree.Kind.ESCAPED_CHARACTER_CLASS;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitEscapedCharacterClass(this);
  }

  @Override
  public RegexTree.Kind kind() {
    return RegexTree.Kind.ESCAPED_CHARACTER_CLASS;
  }

  public boolean isNegation() {
    return Character.isUpperCase(type);
  }

  public boolean isProperty() {
    return Character.toLowerCase(getType()) == 'p';
  }

  /**
   * Non-null if and only if isProperty returns true
   */
  @Nullable
  public String property() {
    return property;
  }

  public char getType() {
    return type;
  }

  @Nonnull
  @Override
  public TransitionType incomingTransitionType() {
    return TransitionType.CHARACTER;
  }
}
