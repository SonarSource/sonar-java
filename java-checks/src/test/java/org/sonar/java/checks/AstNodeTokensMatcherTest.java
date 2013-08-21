/*
 * SonarQube Java
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

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AstNodeTokensMatcherTest {

  @Test
  public void matches() {
    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens(), "")).isTrue();
    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens(), "foo")).isFalse();

    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens("a"), "a")).isTrue();
    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens("a"), "abc")).isFalse();

    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens("a", "bc"), "abc")).isTrue();
    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens("a", "b", "c"), "abc")).isTrue();

    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens("!", "bc"), "abc")).isFalse();
    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens("a", "!c"), "abc")).isFalse();
    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens("a", "b!"), "abc")).isFalse();
    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens("!", "b", "c"), "abc")).isFalse();
    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens("a", "!", "c"), "abc")).isFalse();
    assertThat(AstNodeTokensMatcher.matches(getAstNodeWithTokens("a", "b", "!"), "abc")).isFalse();
  }

  private static AstNode getAstNodeWithTokens(String... originalValues) {
    ImmutableList.Builder<Token> builder = ImmutableList.builder();
    for (String originalValue : originalValues) {
      Token token = mock(Token.class);
      when(token.getOriginalValue()).thenReturn(originalValue);
      builder.add(token);
    }

    AstNode result = mock(AstNode.class);
    when(result.getTokens()).thenReturn(builder.build());

    if (originalValues.length >= 1) {
      when(result.hasToken()).thenReturn(true);
      when(result.getTokenOriginalValue()).thenReturn(originalValues[0]);
    } else {
      when(result.hasToken()).thenReturn(false);
    }

    return result;
  }

}
