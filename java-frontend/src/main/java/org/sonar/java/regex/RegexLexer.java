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
package org.sonar.java.regex;

import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.JavaCharacter;
import org.sonar.java.regex.ast.RegexSource;

public class RegexLexer {

  public static final int EOF = -1;

  private final RegexSource source;

  private final JavaCharacterParser characters;

  private CharacterBuffer buffer = new CharacterBuffer(2);

  private boolean freeSpacingMode = false;

  private boolean escaped = false;

  public RegexLexer(RegexSource source) {
    this.source = source;
    this.characters = new JavaCharacterParser(source);
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
  public JavaCharacter getCurrent() {
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
      return new IndexRange(source.length(), source.length());
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
    return buffer.isEmpty() && characters.isAtEnd();
  }

  public boolean isNotAtEnd() {
    return !isAtEnd();
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

  private void emptyBuffer() {
    if (!buffer.isEmpty()) {
      characters.resetTo(buffer.get(0).getRange().getBeginningOffset());
      buffer = new CharacterBuffer(2);
    }
  }

  private void fillBuffer(int size) {
    if (freeSpacingMode) {
      skipCommentsAndWhiteSpace();
    }
    while (buffer.size() < size && characters.isNotAtEnd()) {
      JavaCharacter javaCharacter = characters.getCurrent();
      consumeCharacter();
      buffer.add(javaCharacter);
      if (freeSpacingMode) {
        skipCommentsAndWhiteSpace();
      }
    }
  }

  private void skipCommentsAndWhiteSpace() {
    while (characters.isNotAtEnd() && isSkippable(characters.getCurrent().getCharacter())) {
      if (characters.getCurrent().getCharacter() == '#') {
        while (characters.isNotAtEnd() && characters.getCurrent().getCharacter() != '\n') {
          consumeCharacter();
        }
      } else {
        consumeCharacter();
      }
    }
  }

  private void consumeCharacter() {
    escaped = !escaped && characters.getCurrent().getCharacter() == '\\';
    characters.moveNext();
  }

  private boolean isSkippable(char ch) {
    return Character.isWhitespace(ch) || (!escaped && ch == '#');
  }

}
