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
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.channel.CommentRegexpChannel;
import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;

import static com.sonar.sslr.api.GenericTokenType.COMMENT;

/**
 * An equivalent of {@link CommentRegexpChannel} with regular expression "/\*[\s\S]*?\*\/".
 * However provides better performance, since implemented without regular expression.
 */
public class MultilineCommentChannel extends Channel<Lexer> {

  private final StringBuilder sb = new StringBuilder();
  private final Token.Builder tokenBuilder = Token.builder();

  @Override
  public boolean consume(CodeReader code, Lexer lexer) {
    if ((code.charAt(0) != '/') || (code.charAt(1) != '*')) {
      return false;
    }
    int column = code.getCursor().getColumn();
    int line = code.getCursor().getLine();

    sb.append((char) code.pop());
    sb.append((char) code.pop());
    int prev;
    do {
      prev = code.pop();
      sb.append((char) prev);
    } while (prev != '*' || code.peek() != '/');
    sb.append((char) code.pop());

    String value = sb.toString();

    Token token = tokenBuilder
        .setType(COMMENT)
        .setValueAndOriginalValue(value)
        .setURI(lexer.getURI())
        .setLine(line)
        .setColumn(column)
        .build();
    lexer.addTrivia(Trivia.createComment(token));

    sb.delete(0, sb.length());

    return true;
  }

}
