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
import com.google.common.collect.Lists;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.java.checks.codesnippet.PrefixParser.PrefixParseResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatternMatcherBuilderTest {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_fail_with_empty_rules() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("rules must contain at least one element");

    new PatternMatcherBuilder(mock(ElementSequence.class), mock(ElementSequence.class), mock(Comparator.class),
        mock(PrefixParser.class), Collections.EMPTY_SET);
  }

  @Test
  public void getPatternMatcher() {
    ElementSequence<Token> inputI = mock(ElementSequence.class);
    ElementSequence<Token> inputJ = mock(ElementSequence.class);
    Comparator<Token> comparator = mock(Comparator.class);
    PrefixParser prefixParser = mock(PrefixParser.class);
    Rule rule1 = mock(Rule.class);
    Rule rule2 = mock(Rule.class);
    Set<Rule> rules = ImmutableSet.of(rule1, rule2);

    PatternMatcherBuilder patternMatcherBuilder = new PatternMatcherBuilder(inputI, inputJ, comparator, prefixParser, rules);

    CommonGroup commonGroup1 = mock(CommonGroup.class);
    when(commonGroup1.getIndexesI()).thenReturn(Lists.newArrayList(0));
    when(commonGroup1.getIndexesJ()).thenReturn(Lists.newArrayList(0));

    VaryingGroup varyingGroup = mock(VaryingGroup.class);
    when(varyingGroup.getIndexesI()).thenReturn(Lists.newArrayList(1));
    when(varyingGroup.getIndexesJ()).thenReturn(Lists.newArrayList(1, 2));

    CommonGroup commonGroup2 = mock(CommonGroup.class);
    when(commonGroup2.getIndexesI()).thenReturn(Lists.newArrayList(2));
    when(commonGroup2.getIndexesJ()).thenReturn(Lists.newArrayList(3));

    final List<Token> tokensI = Lists.newArrayList(mockToken("i0"), mockToken("i1"), mockToken("i2"));
    final List<Token> tokensJ = Lists.newArrayList(mockToken("j0"), mockToken("j1"), mockToken("j2"), mockToken("j3"));

    when(inputI.elementAt(Mockito.anyInt())).thenAnswer(new Answer<Token>() {
      public Token answer(InvocationOnMock invocation) throws Throwable {
        return tokensI.get((Integer) invocation.getArguments()[0]);
      }
    });
    when(inputI.length()).thenReturn(tokensI.size());

    when(inputJ.elementAt(Mockito.anyInt())).thenAnswer(new Answer<Token>() {
      public Token answer(InvocationOnMock invocation) throws Throwable {
        return tokensJ.get((Integer) invocation.getArguments()[0]);
      }
    });
    when(inputJ.length()).thenReturn(tokensJ.size());

    when(prefixParser.parse(rule1, tokensI.subList(1, 2))).thenReturn(PrefixParseResult.FULL_MATCH);
    when(prefixParser.parse(rule2, tokensI.subList(1, 2))).thenReturn(PrefixParseResult.MISMATCH);
    when(prefixParser.parse(rule1, tokensJ.subList(1, 3))).thenReturn(PrefixParseResult.FULL_MATCH);
    when(prefixParser.parse(rule2, tokensI.subList(1, 2))).thenReturn(PrefixParseResult.PREFIX_MATCH);

    PatternMatcher patternMatcher = patternMatcherBuilder.getPatternMatcher(Lists.newArrayList(commonGroup1, varyingGroup, commonGroup2));

    assertThat(patternMatcher).isInstanceOf(CommonPatternMatcher.class);
    CommonPatternMatcher commonPatternMatcher1 = (CommonPatternMatcher) patternMatcher;
    assertThat(commonPatternMatcher1.getNextPatternMatcher()).isInstanceOf(VaryingPatternMatcher.class);
    VaryingPatternMatcher varyingPatternMatcher = (VaryingPatternMatcher) commonPatternMatcher1.getNextPatternMatcher();
    assertThat(varyingPatternMatcher.getNextPatternMatcher()).isInstanceOf(CommonPatternMatcher.class);
    CommonPatternMatcher commonPatternMatcher2 = (CommonPatternMatcher) varyingPatternMatcher.getNextPatternMatcher();
    assertThat(commonPatternMatcher2.getNextPatternMatcher()).isNull();

    assertThat(commonPatternMatcher1.getComparator()).isEqualTo(comparator);
    assertThat(commonPatternMatcher1.getTokensToMatch()).isEqualTo(tokensI.subList(0, 1));

    assertThat(varyingPatternMatcher.getNextCommonPatternMatcherComparator()).isEqualTo(comparator);
    assertThat(varyingPatternMatcher.getNextCommonPatternMatcherTokenToMatch()).isEqualTo(tokensI.get(2));

    assertThat(commonPatternMatcher2.getComparator()).isEqualTo(comparator);
    assertThat(commonPatternMatcher2.getTokensToMatch()).isEqualTo(tokensI.subList(2, 3));
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
