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

import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.Parser;
import org.junit.Test;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.lexer.JavaLexer;

import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class GlobalTest {

  @Test
  public void foo() {
    String eg1 = "A est de India,";
    String eg2 = "B est de 42,";

    Lexer lexer = JavaLexer.create(Charset.forName("UTF-8"));

    List<Token> tokens1 = lexer.lex(eg1);
    tokens1 = tokens1.subList(0, tokens1.size() - 1);
    List<Token> tokens2 = lexer.lex(eg2);
    tokens2 = tokens2.subList(0, tokens2.size() - 1);

    TokenElementSequence inputI = new TokenElementSequence(tokens1);
    TokenElementSequence inputJ = new TokenElementSequence(tokens2);

    Parser<JavaSnippetGrammar> p = getParser();
    PrefixParser prefixParser = new PrefixParser(p);
    Set<Rule> rules = ImmutableSet.of(
        p.getGrammar().integer,
        p.getGrammar().identifier);

    Comparator<Token> comparator = new TokenOriginalValueComparator();
    PatternMatcherBuilder patternMatcherBuilder = new PatternMatcherBuilder(inputI, inputJ, comparator, prefixParser, rules);

    Lcs<Token> lcs = new Lcs<Token>(inputI, inputJ, comparator);
    List<Group> groups = lcs.getGroups();

    PatternMatcher patternMatcher = patternMatcherBuilder.getPatternMatcher(groups);

    List<Token> tokens = lexer.lex("B est de 2012,");

    System.out.println(patternMatcher.isMatching(tokens));
  }

  public Parser<JavaSnippetGrammar> getParser() {
    return Parser.builder(new JavaSnippetGrammar())
        .build();
  }

  private static class JavaSnippetGrammar extends Grammar {

    public Rule integer;
    public Rule identifier;

    public JavaSnippetGrammar() {
      integer.is(JavaTokenType.INTEGER_LITERAL);
      identifier.is(GenericTokenType.IDENTIFIER);
    }

    @Override
    public Rule getRootRule() {
      return null;
    }

  }

}
