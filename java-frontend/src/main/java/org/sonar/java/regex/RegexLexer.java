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
package org.sonar.java.regex;

import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.SourceCharacter;

public class RegexLexer {

  public static final int EOF = -1;

  private final RegexSource source;

  private final CharacterParser characters;

  private CharacterBuffer buffer = new CharacterBuffer(2);

  private boolean freeSpacingMode = false;

  private boolean escaped = false;

  private boolean hasComments = false;

  private boolean quotingMode = false;

  public RegexLexer(RegexSource source, CharacterParser characters) {
    this.source = source;
    this.characters = characters;
    moveNext();
  }

  public boolean getFreeSpacingMode() {
    return freeSpacingMode;
  }

  public void setFreeSpacingMode(boolean freeSpacingMode) {
    if (this.freeSpacingMode != freeSpacingMode) {
      this.freeSpacingMode = freeSpacingMode;
      // After changing the spacing mode, the buffer contents are no longer valid as they may contain contents that should
      // be skipped or may be missing contents that should no longer be skipped
      emptyBuffer();
    }
  }

  public void moveNext(int amount) {
    for (int i = 0; i < amount; i++) {
      moveNext();
    }
  }

  public void moveNext() {
    if (!buffer.isEmpty()) {
      buffer.removeFirst();
    }
    if (buffer.isEmpty()) {
      fillBuffer(1);
    }
  }

  @Nonnull
  public SourceCharacter getCurrent() {
    fillBuffer(1);
    if (buffer.isEmpty()) {
      throw new NoSuchElementException();
    }
    return buffer.get(0);
  }

  public int getCurrentChar() {
    if (isNotAtEnd()) {
      return getCurrent().getCharacter();
    } else {
      return EOF;
    }
  }

  public IndexRange getCurrentIndexRange() {
    if (isNotAtEnd()) {
      return getCurrent().getRange();
    } else {
      // When we're at the end of the regex, the end index extends one past the end of the regex, so that the closing
      // quote will be the character that's marked as the offending character.
      return new IndexRange(source.length(), source.length() + 1);
    }
  }

  public int getCurrentStartIndex() {
    if (isAtEnd()) {
      return source.length();
    } else {
      return getCurrent().getRange().getBeginningOffset();
    }
  }

  public boolean isAtEnd() {
    fillBuffer(1);
    return buffer.isEmpty() && characters.isAtEnd();
  }

  public boolean isNotAtEnd() {
    return !isAtEnd();
  }

  public boolean isInQuotingMode() {
    return quotingMode;
  }

  public boolean currentIs(char ch) {
    return getCurrentChar() == ch;
  }

  public boolean currentIs(String str) {
    fillBuffer(str.length());
    if (buffer.size() < str.length()) {
      return false;
    }
    for (int i = 0; i < str.length(); i++) {
      if (buffer.get(i).getCharacter() != str.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  public int lookAhead(int offset) {
    fillBuffer(offset + 1);
    if (buffer.size() <= offset) {
      return EOF;
    }
    return buffer.get(offset).getCharacter();
  }

  public boolean hasComments() {
    return hasComments;
  }

  private void emptyBuffer() {
    if (!buffer.isEmpty()) {
      characters.resetTo(buffer.get(0).getRange().getBeginningOffset());
      buffer = new CharacterBuffer(2);
    }
  }

  private void fillBuffer(int size) {
    skipCommentsAndWhiteSpace();
    while (buffer.size() < size && characters.isNotAtEnd()) {
      SourceCharacter sourceCharacter = characters.getCurrent();
      characters.moveNext();
      if (!escaped && sourceCharacter.getCharacter() == '\\') {
        if (readQuotingDelimiter()) {
          skipCommentsAndWhiteSpace();
          continue;
        } else {
          escaped = !quotingMode;
        }
      } else {
        escaped = false;
      }
      buffer.add(sourceCharacter);
      skipCommentsAndWhiteSpace();
    }
  }

  private boolean readQuotingDelimiter() {
    if (characters.isAtEnd()) {
      return false;
    }
    char ch = characters.getCurrent().getCharacter();
    if ((!quotingMode && ch == 'Q') || (quotingMode && ch == 'E')) {
      quotingMode = !quotingMode;
      characters.moveNext();
      return true;
    } else {
      return false;
    }
  }

  private void skipCommentsAndWhiteSpace() {
    if (!freeSpacingMode) {
      return;
    }
    while (characters.isNotAtEnd() && isSkippable(characters.getCurrent().getCharacter())) {
      if (characters.getCurrent().getCharacter() == '#') {
        hasComments = true;
        while (characters.isNotAtEnd() && characters.getCurrent().getCharacter() != '\n') {
          characters.moveNext();
        }
      } else {
        characters.moveNext();
      }
    }
  }

  private boolean isSkippable(char ch) {
    return !quotingMode && !escaped && (Character.isWhitespace(ch) || ch == '#');
  }

}
