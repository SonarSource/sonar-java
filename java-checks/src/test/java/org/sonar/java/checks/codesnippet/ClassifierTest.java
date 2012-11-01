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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.impl.channel.BlackHoleChannel;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;

import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.regexp;
import static org.fest.assertions.Assertions.assertThat;

public class ClassifierTest {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void getMatchingRules() {
    Parser<MyGrammar> p = getParser();
    MyGrammar g = p.getGrammar();

    assertThat(new Classifier(p, Collections.EMPTY_SET).getMatchingRules(Lists.newArrayList("foo"))).containsOnly();
    assertThat(new Classifier(p, Sets.newHashSet(g.foo)).getMatchingRules(Lists.newArrayList("bla"))).containsOnly(g.foo);
    assertThat(new Classifier(p, Sets.newHashSet(g.foo, g.bar)).getMatchingRules(Lists.newArrayList("bla"))).containsOnly(g.foo, g.bar);
    assertThat(new Classifier(p, Sets.newHashSet(g.foo, g.bar, g.baz)).getMatchingRules(Lists.newArrayList("bla"))).containsOnly(g.foo, g.bar);
    assertThat(new Classifier(p, Sets.newHashSet(g.foo)).getMatchingRules(Lists.newArrayList("bla", " "))).containsOnly();
    assertThat(new Classifier(p, Sets.newHashSet(g.foo, g.baz)).getMatchingRules(Lists.newArrayList("bla bla"))).containsOnly(g.baz);

  }

  @Test
  public void should_fail_with_empty_inputs() {
    thrown.expect(IllegalArgumentException.class);

    Parser<MyGrammar> p = getParser();

    assertThat(new Classifier(p, Collections.EMPTY_SET).getMatchingRules(Collections.EMPTY_LIST)).containsOnly();
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

    public MyGrammar() {
      foo.is(GenericTokenType.IDENTIFIER, GenericTokenType.EOF);
      bar.is(GenericTokenType.IDENTIFIER, GenericTokenType.EOF);
      baz.is(GenericTokenType.IDENTIFIER, GenericTokenType.IDENTIFIER, GenericTokenType.EOF);
    }

    @Override
    public Rule getRootRule() {
      return foo;
    }

  }

}
