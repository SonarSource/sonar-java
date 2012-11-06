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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class NumericLiteralChannel extends Channel<Lexer> {

  private final Matcher matcher;
  private final StringBuilder tmpBuilder = new StringBuilder();

  public NumericLiteralChannel(String regexp) {
    matcher = Pattern.compile(regexp).matcher("");
  }

  protected abstract void consume(String value, CodeReader code, Lexer lexer);

  @Override
  public boolean consume(CodeReader code, Lexer lexer) {
    if (code.popTo(matcher, tmpBuilder) > 0) {
      consume(tmpBuilder.toString(), code, lexer);
      tmpBuilder.delete(0, tmpBuilder.length());
      return true;
    }
    return false;
  }

}
