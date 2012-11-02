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
package org.sonar.java.checks.codesnippet;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.impl.channel.BlackHoleChannel;
import org.junit.Test;

import java.util.List;

import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.regexp;
import static org.fest.assertions.Assertions.assertThat;

public class PrefixParserTest {

  @Test
  public void parsePartially() {
    Parser<MyGrammar> p = getParser();
    MyGrammar g = p.getGrammar();
    List<Token> tokens = getLexer().lex("foo");
    tokens = tokens.subList(0, tokens.size() - 1); // Remove EOF

    PrefixParser partialParser = new PrefixParser(p);
    p.setRootRule(g.foo);
    assertThat(partialParser.parsePartially(tokens)).isEqualTo(PrefixParser.PrefixParseResult.MISMATCH);
    p.setRootRule(g.bar);
    assertThat(partialParser.parsePartially(tokens)).isEqualTo(PrefixParser.PrefixParseResult.FULL_MATCH);
    p.setRootRule(g.baz);
    assertThat(partialParser.parsePartially(tokens)).isEqualTo(PrefixParser.PrefixParseResult.PARTIAL_MATCH);
  }

  private Lexer getLexer() {
    return Lexer.builder()
        .withFailIfNoChannelToConsumeOneCharacter(true)
        .withCharset(Charsets.UTF_8)
        .withChannel(regexp(GenericTokenType.IDENTIFIER, "[a-z]++"))
        .withChannel(new BlackHoleChannel("[ \r\n]"))
        .build();
  }

  private Parser<MyGrammar> getParser() {
    return Parser.builder(new MyGrammar())
        .withLexer(getLexer())
        .build();
  }

  private class MyGrammar extends Grammar {

    public Rule foo;
    public Rule bar;
    public Rule baz;
    public Rule qux;

    public MyGrammar() {
      foo.is(GenericTokenType.LITERAL);
      bar.is(GenericTokenType.IDENTIFIER);
      baz.is(GenericTokenType.IDENTIFIER, GenericTokenType.IDENTIFIER);
      qux.is(GenericTokenType.IDENTIFIER, GenericTokenType.LITERAL);
    }

    @Override
    public Rule getRootRule() {
      return foo;
    }

  }

}
