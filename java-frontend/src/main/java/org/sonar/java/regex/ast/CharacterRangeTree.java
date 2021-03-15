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

public class CharacterRangeTree extends AbstractRegexSyntaxElement implements CharacterClassElementTree {

  private final CharacterTree lowerBound;

  private final CharacterTree upperBound;

  private final FlagSet activeFlags;

  public CharacterRangeTree(RegexSource source, IndexRange range, CharacterTree lowerBound, CharacterTree upperBound, FlagSet activeFlags) {
    super(source, range);
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    this.activeFlags = activeFlags;
  }

  public CharacterTree getLowerBound() {
    return lowerBound;
  }

  public CharacterTree getUpperBound() {
    return upperBound;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitCharacterRange(this);
  }

  @Nonnull
  @Override
  public Kind characterClassElementKind() {
    return Kind.CHARACTER_RANGE;
  }

  @Nonnull
  @Override
  public FlagSet activeFlags() {
    return activeFlags;
  }

}
