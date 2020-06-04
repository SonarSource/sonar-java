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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.JavaCharacter;
import org.sonar.java.regex.ast.RegexSource;

/**
 * Parse the contents of string literals and provide the individual characters of the string after processing escape
 * sequences
 */
public class JavaCharacterParser {

  public static final int EOF = -1;

  private final RegexSource source;

  private final JavaUnicodeEscapeParser unicodeProcessedCharacters;

  /**
   * Will be null if and only if the end of input has been reached
   */
  @CheckForNull
  private JavaCharacter current;

  public JavaCharacterParser(RegexSource source) {
    this.source = source;
    this.unicodeProcessedCharacters = new JavaUnicodeEscapeParser(source);
    moveNext();
  }

  public void moveNext() {
    current = parseJavaCharacter();
  }

  @Nonnull
  public JavaCharacter getCurrent() {
    if (current == null) {
      throw new NoSuchElementException();
    }
    return current;
  }

  public int getCurrentChar() {
    if (current != null) {
      return current.getCharacter();
    } else {
      return EOF;
    }
  }

  public IndexRange getCurrentIndexRange() {
    if (current != null) {
      return current.getRange();
    } else {
      return new IndexRange(source.length(), source.length());
    }
  }

  public int getCurrentStartIndex() {
    if (current == null) {
      return source.length();
    } else {
      return current.getRange().getBeginningOffset();
    }
  }

  public boolean isAtEnd() {
    return current == null;
  }

  public boolean isNotAtEnd() {
    return current != null;
  }

  public boolean currentIs(char ch) {
    return current != null && current.getCharacter() == ch;
  }

  @CheckForNull
  private JavaCharacter parseJavaCharacter() {
    JavaCharacter javaCharacter = unicodeProcessedCharacters.getCurrent();
    if (javaCharacter == null) {
      return null;
    }
    if (javaCharacter.getCharacter() == '\\') {
      return parseJavaEscapeSequence(javaCharacter);
    }
    unicodeProcessedCharacters.moveNext();
    return javaCharacter;
  }

  private JavaCharacter parseJavaEscapeSequence(JavaCharacter backslash) {
    unicodeProcessedCharacters.moveNext();
    JavaCharacter javaCharacter = unicodeProcessedCharacters.getCurrent();
    if (javaCharacter == null) {
      // Should only happen in case of syntactically invalid string literals
      return backslash;
    }
    char ch = javaCharacter.getCharacter();
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
          StringBuilder codeUnit = new StringBuilder(3);
          for (int i = 0; i < 3 && javaCharacter != null && isOctalDigit(javaCharacter.getCharacter()); i++) {
            codeUnit.append(javaCharacter.getCharacter());
            unicodeProcessedCharacters.moveNext();
            javaCharacter = unicodeProcessedCharacters.getCurrent();
          }
          ch = (char) Integer.parseInt(codeUnit.toString(), 8);
          return new JavaCharacter(source, backslash.getRange().extendTo(getCurrentStartIndex()), ch);
        }
        break;
    }
    unicodeProcessedCharacters.moveNext();
    return new JavaCharacter(source, backslash.getRange().extendTo(getCurrentStartIndex()), ch);
  }

  private static boolean isOctalDigit(int c) {
    return '0' <= c && c <= '7';
  }

}
