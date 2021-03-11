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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.java.regex.ast.SourceCharacter;

/**
 * Parse the contents of string literals and provide the individual characters of the string after processing escape
 * sequences
 */
public class JavaCharacterParser implements CharacterParser {

  private final RegexSource source;

  private final JavaUnicodeEscapeParser unicodeProcessedCharacters;

  /**
   * Will be null if and only if the end of input has been reached
   */
  @CheckForNull
  private SourceCharacter current;

  public JavaCharacterParser(RegexSource source) {
    this.source = source;
    this.unicodeProcessedCharacters = new JavaUnicodeEscapeParser(source);
    moveNext();
  }

  public void resetTo(int index) {
    unicodeProcessedCharacters.resetTo(index);
    moveNext();
  }

  public void moveNext() {
    current = parseJavaCharacter();
  }

  @Nonnull
  public SourceCharacter getCurrent() {
    if (current == null) {
      throw new NoSuchElementException();
    }
    return current;
  }

  public boolean isAtEnd() {
    return current == null;
  }

  @CheckForNull
  private SourceCharacter parseJavaCharacter() {
    SourceCharacter sourceCharacter = unicodeProcessedCharacters.getCurrent();
    if (sourceCharacter == null) {
      return null;
    }
    if (sourceCharacter.getCharacter() == '\\') {
      return parseJavaEscapeSequence(sourceCharacter);
    }
    unicodeProcessedCharacters.moveNext();
    return sourceCharacter;
  }

  private SourceCharacter parseJavaEscapeSequence(SourceCharacter backslash) {
    unicodeProcessedCharacters.moveNext();
    SourceCharacter sourceCharacter = unicodeProcessedCharacters.getCurrent();
    if (sourceCharacter == null) {
      // Should only happen in case of syntactically invalid string literals
      return backslash;
    }
    char ch = sourceCharacter.getCharacter();
    switch (ch) {
      case 'n':
        ch = '\n';
        break;
      case 'r':
        ch = '\r';
        break;
      case 'f':
        ch = '\f';
        break;
      case 'b':
        ch = '\b';
        break;
      case 't':
        ch = '\t';
        break;
      default:
        if (isOctalDigit(ch)) {
          ch = 0;
          for (int i = 0; i < 3 && sourceCharacter != null && isOctalDigit(sourceCharacter.getCharacter()); i++) {
            int newValue = ch * 8 + sourceCharacter.getCharacter() - '0';
            if (newValue > 0xFF) {
              break;
            }
            ch = (char) newValue;
            unicodeProcessedCharacters.moveNext();
            sourceCharacter = unicodeProcessedCharacters.getCurrent();
          }
          int endIndex = sourceCharacter == null ? source.length() : sourceCharacter.getRange().getBeginningOffset();
          return new SourceCharacter(source, backslash.getRange().extendTo(endIndex), ch, true);
        }
        break;
    }
    unicodeProcessedCharacters.moveNext();
    return new SourceCharacter(source, backslash.getRange().merge(sourceCharacter.getRange()), ch, true);
  }

  private static boolean isOctalDigit(int c) {
    return '0' <= c && c <= '7';
  }

}
