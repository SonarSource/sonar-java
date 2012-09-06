/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.ast.lexer;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.impl.Lexer;
import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;

public class CharacterLiteralChannel extends Channel<Lexer> {

  private final StringBuilder sb = new StringBuilder();
  private final char ch;
  private final TokenType tokenType;
  private final Token.Builder tokenBuilder = Token.builder();

  public CharacterLiteralChannel(char ch, TokenType tokenType) {
    this.ch = ch;
    this.tokenType = tokenType;
  }

  @Override
  public boolean consume(CodeReader code, Lexer lexer) {
    if (code.peek() != ch) {
      return false;
    }
    int line = code.getCursor().getLine();
    int column = code.getCursor().getColumn();
    char prev;
    do {
      prev = (char) code.pop();
      sb.append(prev);
      if (prev == '\\') {
        prev = (char) code.pop();
        sb.append(prev);
      }
    } while (code.peek() != ch);

    sb.append((char) code.pop());
    String value = sb.toString();

    Token token = tokenBuilder
        .setType(tokenType)
        .setValueAndOriginalValue(value)
        .setURI(lexer.getURI())
        .setLine(line)
        .setColumn(column)
        .build();

    lexer.addToken(token);

    sb.delete(0, sb.length());

    return true;
  }

}
