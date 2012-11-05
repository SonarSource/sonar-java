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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.Parser;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Cardinality;
import org.sonar.check.Priority;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.lexer.JavaLexer;
import org.sonar.java.ast.visitors.JavaAstCheck;
import org.sonar.java.checks.codesnippet.CommonPatternMatcher;
import org.sonar.java.checks.codesnippet.Group;
import org.sonar.java.checks.codesnippet.JavaPatternGrammar;
import org.sonar.java.checks.codesnippet.Lcs;
import org.sonar.java.checks.codesnippet.PatternMatcher;
import org.sonar.java.checks.codesnippet.PatternMatcherBuilder;
import org.sonar.java.checks.codesnippet.PrefixParser;
import org.sonar.java.checks.codesnippet.TokenElementSequence;
import org.sonar.java.checks.codesnippet.TokenOriginalValueComparator;

import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@org.sonar.check.Rule(
  key = "Snippet",
  priority = Priority.MAJOR,
  cardinality = Cardinality.MULTIPLE)
public final class SnippetCheck extends JavaAstCheck implements AstAndTokenVisitor {

  private static final String DEFAULT_DONT_EXAMPLE1 = "";
  private static final String DEFAULT_DO_EXAMPLE1 = "";

  private static final String DEFAULT_DONT_EXAMPLE2 = "";

  @RuleProperty(
    key = "dontExample1",
    defaultValue = "" + DEFAULT_DONT_EXAMPLE1,
    type = "TEXT")
  public String dontExample1 = DEFAULT_DONT_EXAMPLE1;
  @RuleProperty(
    key = "doExample1",
    defaultValue = "" + DEFAULT_DO_EXAMPLE1,
    type = "TEXT")
  public String doExample1 = DEFAULT_DO_EXAMPLE1;

  @RuleProperty(
    key = "dontExample2",
    defaultValue = "" + DEFAULT_DONT_EXAMPLE2,
    type = "TEXT")
  public String dontExample2 = DEFAULT_DONT_EXAMPLE2;

  private final List<Token> tokens = Lists.newLinkedList();
  private PatternMatcher patternMatcher = null;

  @Override
  public void init() {
    Lexer lexer = JavaLexer.create(Charset.forName("UTF-8"));
    Comparator<Token> comparator = new TokenOriginalValueComparator();

    if (!StringUtils.isEmpty(dontExample1) && !StringUtils.isEmpty(dontExample2)) {
      TokenElementSequence inputI = new TokenElementSequence(getTokensWithoutEof(lexer.lex(dontExample1)));
      TokenElementSequence inputJ = new TokenElementSequence(getTokensWithoutEof(lexer.lex(dontExample2)));

      JavaPatternGrammar g = new JavaPatternGrammar();
      Parser<JavaPatternGrammar> parser = g.getParser(lexer);
      Set<Rule> rules = ImmutableSet.of(
          g.characterLiteral,
          g.stringLiteral,
          g.nullLiteral,
          g.booleanLiteral,
          g.integerLiteral,
          g.floatingLiteral,
          g.qualifiedIdentifier,
          g.methodCall);

      PrefixParser prefixParser = new PrefixParser(parser);

      PatternMatcherBuilder patternMatcherBuilder = new PatternMatcherBuilder(inputI, inputJ, comparator, prefixParser, rules);

      Lcs<Token> lcs = new Lcs<Token>(inputI, inputJ, comparator);
      List<Group> groups = lcs.getGroups();

      patternMatcher = patternMatcherBuilder.getPatternMatcher(groups);
    } else if (!StringUtils.isEmpty(dontExample1)) {
      List<Token> tokensToMatch = getTokensWithoutEof(lexer.lex(dontExample1));

      patternMatcher = new CommonPatternMatcher(tokensToMatch, comparator);
    }
  }

  private List<Token> getTokensWithoutEof(List<Token> tokens) {
    return tokens.subList(0, tokens.size() - 1);
  }

  @Override
  public void visitFile(AstNode node) {
    tokens.clear();
  }

  public void visitToken(Token token) {
    tokens.add(token);
  }

  @Override
  public void leaveFile(AstNode node) {
    if (patternMatcher != null) {
      while (!tokens.isEmpty()) {
        if (patternMatcher.isMatching(tokens)) {
          getContext().createLineViolation(this, "This should be rewritten as: {0}", tokens.get(0), doExample1);
        }

        tokens.remove(0);
      }
    }
  }

}
