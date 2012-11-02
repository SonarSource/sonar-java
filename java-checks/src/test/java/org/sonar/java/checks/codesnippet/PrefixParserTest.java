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
import org.sonar.java.checks.codesnippet.PrefixParser.PrefixParseResult;

import java.util.Collections;
import java.util.List;

import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.regexp;
import static org.fest.assertions.Assertions.assertThat;

public class PrefixParserTest {

  @Test
  public void parse() {
    Parser<MyGrammar> p = getParser();
    MyGrammar g = p.getGrammar();
    List<Token> tokens = getLexer().lex("foo");
    tokens = tokens.subList(0, tokens.size() - 1); // Remove EOF

    PrefixParser prefixParser = new PrefixParser(p);
    assertThat(prefixParser.parse(g.foo, tokens)).isEqualTo(PrefixParser.PrefixParseResult.MISMATCH);
    assertThat(prefixParser.parse(g.bar, tokens)).isEqualTo(PrefixParser.PrefixParseResult.FULL_MATCH);
    assertThat(prefixParser.parse(g.baz, tokens)).isEqualTo(PrefixParser.PrefixParseResult.PREFIX_MATCH);

    tokens = getLexer().lex("foo bar");
    tokens = tokens.subList(0, tokens.size() - 1); // Remove EOF

    assertThat(prefixParser.parse(g.bar, tokens)).isEqualTo(PrefixParser.PrefixParseResult.MISMATCH);
    assertThat(prefixParser.parse(g.baz, tokens)).isEqualTo(PrefixParser.PrefixParseResult.FULL_MATCH);
  }

  @Test
  public void should_restore_previous_parse_root_rule_upon_success() {
    Parser<MyGrammar> p = getParser();
    MyGrammar g = p.getGrammar();
    List<Token> tokens = getLexer().lex("foo");
    tokens = tokens.subList(0, tokens.size() - 1); // Remove EOF

    p.setRootRule(g.foo);

    PrefixParser prefixParser = new PrefixParser(p);
    assertThat(p.getRootRule()).isEqualTo(g.foo);

    assertThat(prefixParser.parse(g.bar, tokens)).isEqualTo(PrefixParseResult.FULL_MATCH);
    assertThat(p.getRootRule()).isEqualTo(g.foo);
  }

  @Test
  public void should_restore_previous_parse_root_rule_upon_error() {
    Parser<MyGrammar> p = getParser();
    MyGrammar g = p.getGrammar();

    p.setRootRule(g.foo);

    PrefixParser prefixParser = new PrefixParser(p);
    assertThat(p.getRootRule()).isEqualTo(g.foo);

    assertThat(prefixParser.parse(g.bar, Collections.EMPTY_LIST)).isEqualTo(PrefixParseResult.PREFIX_MATCH);
    assertThat(p.getRootRule()).isEqualTo(g.foo);
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
