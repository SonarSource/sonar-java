/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.xml.maven.helpers;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PatternMatcherTest {

  private PatternMatcher matcher;

  @Test
  public void should_match_patterns() {
    matcher = new PatternMatcher("[a-z]*");
    assertThat(matcher.test("test")).isTrue();
    assertThat(matcher.test("012")).isFalse();
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_on_invalid_regex() {
    new PatternMatcher("*");
  }

}
