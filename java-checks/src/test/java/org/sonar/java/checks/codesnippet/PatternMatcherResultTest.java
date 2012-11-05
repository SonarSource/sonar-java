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

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatternMatcherResultTest {

  @Test
  public void isMatching() {
    PatternMatcherResult patternMatcherResult = new PatternMatcherResult(0);
    assertThat(patternMatcherResult.isMatching()).isEqualTo(false);

    patternMatcherResult = new PatternMatcherResult(1);
    assertThat(patternMatcherResult.isMatching()).isEqualTo(true);

    PatternMatcherResult nextPatternMatcherResult = mock(PatternMatcherResult.class);
    when(nextPatternMatcherResult.isMatching()).thenReturn(true);
    patternMatcherResult = new PatternMatcherResult(0, nextPatternMatcherResult);
    assertThat(patternMatcherResult.isMatching()).isEqualTo(true);

    nextPatternMatcherResult = mock(PatternMatcherResult.class);
    when(nextPatternMatcherResult.isMatching()).thenReturn(false);
    patternMatcherResult = new PatternMatcherResult(0, nextPatternMatcherResult);
    assertThat(patternMatcherResult.isMatching()).isEqualTo(false);

    nextPatternMatcherResult = mock(PatternMatcherResult.class);
    when(nextPatternMatcherResult.isMatching()).thenReturn(false);
    patternMatcherResult = new PatternMatcherResult(42, nextPatternMatcherResult);
    assertThat(patternMatcherResult.isMatching()).isEqualTo(false);
  }

  @Test
  public void getMatchingToIndex() {
    PatternMatcherResult patternMatcherResult = new PatternMatcherResult(0);
    assertThat(patternMatcherResult.getMatchingToIndex()).isEqualTo(0);

    patternMatcherResult = new PatternMatcherResult(42);
    assertThat(patternMatcherResult.getMatchingToIndex()).isEqualTo(42);

    patternMatcherResult = new PatternMatcherResult(99, mock(PatternMatcherResult.class));
    assertThat(patternMatcherResult.getMatchingToIndex()).isEqualTo(99);
  }

  @Test
  public void getNextPatternMatcherResult() {
    assertThat(new PatternMatcherResult(0).getNextPatternMatcherResult()).isNull();

    PatternMatcherResult nextPatternMatcherResult = mock(PatternMatcherResult.class);
    assertThat(new PatternMatcherResult(0, nextPatternMatcherResult).getNextPatternMatcherResult()).isEqualTo(nextPatternMatcherResult);
  }

  @Test
  public void getMismatch() {
    assertThat(PatternMatcherResult.getMismatch().isMatching()).isEqualTo(false);
    assertThat(PatternMatcherResult.getMismatch().getMatchingToIndex()).isEqualTo(0);
    assertThat(PatternMatcherResult.getMismatch().getNextPatternMatcherResult()).isNull();
  }

}
