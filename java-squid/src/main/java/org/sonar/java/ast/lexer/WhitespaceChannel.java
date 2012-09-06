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

import com.sonar.sslr.impl.Lexer;
import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;

/**
 * An equivalent of {@link com.sonar.sslr.impl.channel.BlackHoleChannel} with regular expression "\s++".
 * However provides better performance, since implemented without regular expression.
 */
public class WhitespaceChannel extends Channel<Lexer> {

  @Override
  public boolean consume(CodeReader code, Lexer output) {
    if (!Character.isWhitespace(code.peek())) {
      return false;
    }
    do {
      code.pop();
    } while (Character.isWhitespace(code.peek()));
    return true;
  }

}
