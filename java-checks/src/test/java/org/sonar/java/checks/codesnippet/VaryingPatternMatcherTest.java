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

import com.google.common.collect.Lists;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.java.checks.codesnippet.PrefixParser.PrefixParseResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VaryingPatternMatcherTest {

  @Test
  public void isMatching_no_hit() {
    Token token1 = mockToken("token1");
    Token token2 = mockToken("token2");
    Token token3 = mockToken("token3");

    Comparator<Token> comparator = mock(Comparator.class);
    when(comparator.compare(Mockito.any(Token.class), Mockito.any(Token.class))).thenReturn(-1);
    when(comparator.compare(token2, token2)).thenReturn(0);

    CommonPatternMatcher nextCommonPatternMatcher = mock(CommonPatternMatcher.class);
    when(nextCommonPatternMatcher.getTokensToMatch()).thenReturn(Lists.newArrayList(token2));
    when(nextCommonPatternMatcher.getComparator()).thenReturn(comparator);

    Rule rule = mock(Rule.class);

    PrefixParser prefixParser = mock(PrefixParser.class);

    List<Token> tokens = Lists.newArrayList(token1, token3, token1);

    assertThat(new VaryingPatternMatcher(nextCommonPatternMatcher, prefixParser, rule).isMatching(tokens)).isEqualTo(false);

    verify(comparator, times(2)).compare(token1, token2);
    verify(comparator).compare(token3, token2);
  }

  @Test
  public void isMatching_mismatch() {
    Token token1 = mockToken("token1");
    Token token2 = mockToken("token2");
    Token token3 = mockToken("token3");

    Comparator<Token> comparator = mock(Comparator.class);
    when(comparator.compare(Mockito.any(Token.class), Mockito.any(Token.class))).thenReturn(-1);
    when(comparator.compare(token2, token2)).thenReturn(0);

    CommonPatternMatcher nextCommonPatternMatcher = mock(CommonPatternMatcher.class);
    when(nextCommonPatternMatcher.getTokensToMatch()).thenReturn(Lists.newArrayList(token2));
    when(nextCommonPatternMatcher.getComparator()).thenReturn(comparator);
    when(nextCommonPatternMatcher.isMatching(Lists.newArrayList(token2, token3, token2))).thenReturn(true);

    Rule rule = mock(Rule.class);

    PrefixParser prefixParser = mock(PrefixParser.class);
    when(prefixParser.parse(rule, Lists.newArrayList(token1))).thenReturn(PrefixParseResult.MISMATCH);

    List<Token> tokens = Lists.newArrayList(token1, token2, token3, token2);

    assertThat(new VaryingPatternMatcher(nextCommonPatternMatcher, prefixParser, rule).isMatching(tokens)).isEqualTo(false);

    verify(prefixParser).parse(rule, Lists.newArrayList(token1));
    verify(comparator).compare(token1, token2);
    verify(comparator).compare(token2, token2);
    verify(nextCommonPatternMatcher, never()).isMatching(Lists.newArrayList(token2, token3, token2));
  }

  @Test
  public void isMatching_match_first_time_with_full_match() {
    Token token1 = mockToken("token1");
    Token token2 = mockToken("token2");
    Token token3 = mockToken("token3");

    Comparator<Token> comparator = mock(Comparator.class);
    when(comparator.compare(Mockito.any(Token.class), Mockito.any(Token.class))).thenReturn(-1);
    when(comparator.compare(token2, token2)).thenReturn(0);

    CommonPatternMatcher nextCommonPatternMatcher = mock(CommonPatternMatcher.class);
    when(nextCommonPatternMatcher.getTokensToMatch()).thenReturn(Lists.newArrayList(token2));
    when(nextCommonPatternMatcher.getComparator()).thenReturn(comparator);
    when(nextCommonPatternMatcher.isMatching(Lists.newArrayList(token2, token3, token2))).thenReturn(true);

    Rule rule = mock(Rule.class);

    PrefixParser prefixParser = mock(PrefixParser.class);
    when(prefixParser.parse(rule, Lists.newArrayList(token1))).thenReturn(PrefixParseResult.FULL_MATCH);

    List<Token> tokens = Lists.newArrayList(token1, token2, token3, token2);

    assertThat(new VaryingPatternMatcher(nextCommonPatternMatcher, prefixParser, rule).isMatching(tokens)).isEqualTo(true);

    verify(prefixParser).parse(rule, Lists.newArrayList(token1));
    verify(comparator).compare(token1, token2);
    verify(comparator).compare(token2, token2);
    verify(nextCommonPatternMatcher).isMatching(Lists.newArrayList(token2, token3, token2));
  }

  @Test
  public void isMatching_match_second_time_with_full_match() {
    Token token1 = mockToken("token1");
    Token token2 = mockToken("token2");
    Token token3 = mockToken("token3");

    Comparator<Token> comparator = mock(Comparator.class);
    when(comparator.compare(Mockito.any(Token.class), Mockito.any(Token.class))).thenReturn(-1);
    when(comparator.compare(token2, token2)).thenReturn(0);

    CommonPatternMatcher nextCommonPatternMatcher = mock(CommonPatternMatcher.class);
    when(nextCommonPatternMatcher.getTokensToMatch()).thenReturn(Lists.newArrayList(token2));
    when(nextCommonPatternMatcher.getComparator()).thenReturn(comparator);
    when(nextCommonPatternMatcher.isMatching(Lists.newArrayList(token2))).thenReturn(true);

    Rule rule = mock(Rule.class);

    PrefixParser prefixParser = mock(PrefixParser.class);
    when(prefixParser.parse(rule, Lists.newArrayList(token1))).thenReturn(PrefixParseResult.FULL_MATCH);
    when(prefixParser.parse(rule, Lists.newArrayList(token1, token2, token3))).thenReturn(PrefixParseResult.FULL_MATCH);

    List<Token> tokens = Lists.newArrayList(token1, token2, token3, token2);

    assertThat(new VaryingPatternMatcher(nextCommonPatternMatcher, prefixParser, rule).isMatching(tokens)).isEqualTo(true);

    verify(prefixParser).parse(rule, Lists.newArrayList(token1));
    verify(prefixParser).parse(rule, Lists.newArrayList(token1, token2, token3));
    verify(comparator).compare(token1, token2);
    verify(comparator, times(2)).compare(token2, token2);
    verify(comparator).compare(token3, token2);
    verify(nextCommonPatternMatcher).isMatching(Lists.newArrayList(token2));
  }

  @Test
  public void isMatching_match_second_time_with_partial_match() {
    Token token1 = mockToken("token1");
    Token token2 = mockToken("token2");
    Token token3 = mockToken("token3");

    Comparator<Token> comparator = mock(Comparator.class);
    when(comparator.compare(Mockito.any(Token.class), Mockito.any(Token.class))).thenReturn(-1);
    when(comparator.compare(token2, token2)).thenReturn(0);

    CommonPatternMatcher nextCommonPatternMatcher = mock(CommonPatternMatcher.class);
    when(nextCommonPatternMatcher.getTokensToMatch()).thenReturn(Lists.newArrayList(token2));
    when(nextCommonPatternMatcher.getComparator()).thenReturn(comparator);
    when(nextCommonPatternMatcher.isMatching(Lists.newArrayList(token2))).thenReturn(true);

    Rule rule = mock(Rule.class);

    PrefixParser prefixParser = mock(PrefixParser.class);
    when(prefixParser.parse(rule, Lists.newArrayList(token1))).thenReturn(PrefixParseResult.PREFIX_MATCH);
    when(prefixParser.parse(rule, Lists.newArrayList(token1, token2, token3))).thenReturn(PrefixParseResult.FULL_MATCH);

    List<Token> tokens = Lists.newArrayList(token1, token2, token3, token2);

    assertThat(new VaryingPatternMatcher(nextCommonPatternMatcher, prefixParser, rule).isMatching(tokens)).isEqualTo(true);

    verify(prefixParser).parse(rule, Lists.newArrayList(token1));
    verify(prefixParser).parse(rule, Lists.newArrayList(token1, token2, token3));
    verify(comparator).compare(token1, token2);
    verify(comparator, times(2)).compare(token2, token2);
    verify(comparator).compare(token3, token2);
    verify(nextCommonPatternMatcher).isMatching(Lists.newArrayList(token2));
  }

  @Test
  public void getNextCommonPatternMatcherTokenToMatch() {
    Token token1 = mockToken("token1");
    Token token2 = mockToken("token2");

    CommonPatternMatcher nextCommonPatternMatcher = mock(CommonPatternMatcher.class);
    when(nextCommonPatternMatcher.getTokensToMatch()).thenReturn(Lists.newArrayList(token1, token2));

    assertThat(new VaryingPatternMatcher(nextCommonPatternMatcher, mock(PrefixParser.class), mock(Rule.class)).getNextCommonPatternMatcherTokenToMatch()).isEqualTo(token1);
  }

  @Test
  public void getNextCommonPatternMatcherComparator() {
    Comparator<Token> comparator = mock(Comparator.class);

    CommonPatternMatcher nextCommonPatternMatcher = mock(CommonPatternMatcher.class);
    when(nextCommonPatternMatcher.getComparator()).thenReturn(comparator);

    assertThat(new VaryingPatternMatcher(nextCommonPatternMatcher, mock(PrefixParser.class), mock(Rule.class)).getNextCommonPatternMatcherComparator()).isEqualTo(comparator);
  }

  private Token mockToken(String value) {
    try {
      return Token.builder()
          .setType(mock(TokenType.class))
          .setValueAndOriginalValue(value)
          .setURI(new URI("test://unit"))
          .setLine(1)
          .setColumn(1)
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
