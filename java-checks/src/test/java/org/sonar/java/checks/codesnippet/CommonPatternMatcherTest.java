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
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommonPatternMatcherTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_fail_with_empty_tokens_to_match() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("tokensToMatch must contain at least one element");

    new CommonPatternMatcher(Collections.EMPTY_LIST, mock(Comparator.class));
  }

  @Test
  public void getTokensToMatch() {
    List<Token> tokensToMatch = mock(List.class);
    when(tokensToMatch.size()).thenReturn(42);

    assertThat(new CommonPatternMatcher(tokensToMatch, mock(Comparator.class)).getTokensToMatch()).isEqualTo(tokensToMatch);
  }

  @Test
  public void getComparator() {
    List<Token> tokensToMatch = mock(List.class);
    when(tokensToMatch.size()).thenReturn(42);

    Comparator<Token> comparator = mock(Comparator.class);

    assertThat(new CommonPatternMatcher(tokensToMatch, comparator).getComparator()).isEqualTo(comparator);
  }

  @Test
  public void isMatching_should_return_false_when_list_size_smaller() {
    List<Token> tokensToMatch = mock(List.class);
    when(tokensToMatch.size()).thenReturn(42);

    List<Token> tokens = mock(List.class);
    when(tokensToMatch.size()).thenReturn(1);

    assertThat(new CommonPatternMatcher(tokensToMatch, mock(Comparator.class)).isMatching(tokens)).isEqualTo(false);
  }

  @Test
  public void isMatching_should_succeed_without_next_pattern_matcher() {
    Token token1 = mockToken("token1");
    Token token2 = mockToken("token2");
    Token token3 = mockToken("token3");

    List<Token> tokensToMatch = Lists.newArrayList(token1, token2);

    Comparator<Token> comparator = mock(Comparator.class);

    CommonPatternMatcher patternMatcher = new CommonPatternMatcher(tokensToMatch, comparator);

    assertThat(patternMatcher.isMatching(Lists.newArrayList(token1, token2, token3))).isEqualTo(true);
    verify(comparator).compare(token1, token1);
    verify(comparator).compare(token2, token2);
    verify(comparator, never()).compare(token3, token3);
  }

  @Test
  public void isMatching_should_fail_without_next_pattern_matcher() {
    Token token1 = mockToken("token1");
    Token token2 = mockToken("token2");
    Token token3 = mockToken("token3");

    List<Token> tokensToMatch = Lists.newArrayList(token1, token3);

    Comparator<Token> comparator = mock(Comparator.class);
    when(comparator.compare(token1, token1)).thenReturn(0);
    when(comparator.compare(token3, token2)).thenReturn(-1);

    CommonPatternMatcher patternMatcher = new CommonPatternMatcher(tokensToMatch, comparator);

    assertThat(patternMatcher.isMatching(Lists.newArrayList(token1, token2, token3))).isEqualTo(false);
    verify(comparator).compare(token1, token1);
    verify(comparator).compare(token3, token2);
    verify(comparator, never()).compare(token2, token2);
    verify(comparator, never()).compare(token3, token3);
  }

  @Test
  public void isMatching_should_call_next_pattern_matcher_with_right_tokens() {
    Token token1 = mockToken("token1");
    Token token2 = mockToken("token2");
    Token token3 = mockToken("token3");

    List<Token> tokensToMatch = Lists.newArrayList(token1);

    Comparator<Token> comparator = mock(Comparator.class);

    PatternMatcher nextPatternMatcher = mock(PatternMatcher.class);
    when(nextPatternMatcher.isMatching(Lists.newArrayList(token2, token3))).thenReturn(true);
    CommonPatternMatcher patternMatcher = new CommonPatternMatcher(nextPatternMatcher, tokensToMatch, comparator);

    assertThat(patternMatcher.isMatching(Lists.newArrayList(token1, token2, token3))).isEqualTo(true);
    assertThat(patternMatcher.isMatching(Lists.newArrayList(token1, token2))).isEqualTo(false);
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
