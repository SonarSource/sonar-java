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

import com.google.common.collect.ImmutableMap;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.impl.Lexer;
import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;

import java.util.Map;

/**
 * An equivalent of {@link com.sonar.sslr.impl.channel.IdentifierAndKeywordChannel} with regular expression "\p{javaJavaIdentifierStart}++\p{javaJavaIdentifierPart}*+".
 * However provides better performance, since implemented without regular expression.
 */
public class JavaIdentifierAndKeywordChannel extends Channel<Lexer> {

  private final Map<String, TokenType> keywordsMap;
  private final StringBuilder tmpBuilder = new StringBuilder();
  private final Token.Builder tokenBuilder = Token.builder();

  public JavaIdentifierAndKeywordChannel(TokenType[]... keywordSets) {
    ImmutableMap.Builder<String, TokenType> keywordsMapBuilder = ImmutableMap.builder();
    for (TokenType[] keywords : keywordSets) {
      for (TokenType keyword : keywords) {
        keywordsMapBuilder.put(keyword.getValue(), keyword);
      }
    }
    this.keywordsMap = keywordsMapBuilder.build();
  }

  @Override
  public boolean consume(CodeReader code, Lexer lexer) {
    if (!Character.isJavaIdentifierStart(code.peek())) {
      return false;
    }

    int line = code.getCursor().getLine();
    int column = code.getCursor().getColumn();
    while (Character.isJavaIdentifierPart(code.peek())) {
      tmpBuilder.append((char) code.pop());
    }

    String word = tmpBuilder.toString();

    TokenType keywordType = keywordsMap.get(word);
    Token token = tokenBuilder
        .setType(keywordType == null ? GenericTokenType.IDENTIFIER : keywordType)
        .setValueAndOriginalValue(word, word)
        .setURI(lexer.getURI())
        .setLine(line)
        .setColumn(column)
        .build();

    lexer.addToken(token);

    tmpBuilder.delete(0, tmpBuilder.length());

    return true;
  }
}
