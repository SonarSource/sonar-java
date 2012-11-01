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
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TokenElementSequenceTest {

  @Test
  public void elementAt() {
    Token token1 = mockToken();
    Token token2 = mockToken();

    assertThat(new TokenElementSequence(Lists.newArrayList(token1, token2)).elementAt(0)).isEqualTo(token1);
    assertThat(new TokenElementSequence(Lists.newArrayList(token1, token2)).elementAt(1)).isEqualTo(token2);
  }

  @Test
  public void length() {
    Token token = mockToken();

    assertThat(new TokenElementSequence(Collections.EMPTY_LIST).length()).isEqualTo(0);
    assertThat(new TokenElementSequence(Lists.newArrayList(token, token, token)).length()).isEqualTo(3);
  }

  private Token mockToken() {
    try {
      return Token.builder()
          .setType(mock(TokenType.class))
          .setValueAndOriginalValue("")
          .setURI(new URI("test://unit"))
          .setLine(1)
          .setColumn(1)
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
