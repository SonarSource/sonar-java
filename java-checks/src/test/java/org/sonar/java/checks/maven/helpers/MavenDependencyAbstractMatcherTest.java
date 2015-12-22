/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks.maven.helpers;

import org.junit.Test;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.maven2.Dependency;

import java.util.regex.Pattern;

import static org.fest.assertions.Assertions.assertThat;

public class MavenDependencyAbstractMatcherTest {

  @Test
  public void wildcard() {
    assertThat(MavenDependencyAbstractMatcher.isWildCard("")).isTrue();
    assertThat(MavenDependencyAbstractMatcher.isWildCard("*")).isTrue();
    assertThat(MavenDependencyAbstractMatcher.isWildCard("?")).isFalse();
  }

  @Test
  public void matchPattern() {
    assertThat(MavenDependencyAbstractMatcher.attributeMatchesPattern(null, null)).isFalse();
    assertThat(MavenDependencyAbstractMatcher.attributeMatchesPattern(new LocatedAttribute("test"), null)).isTrue();
    assertThat(MavenDependencyAbstractMatcher.attributeMatchesPattern(null, Pattern.compile(""))).isFalse();
    assertThat(MavenDependencyAbstractMatcher.attributeMatchesPattern(new LocatedAttribute("test"), Pattern.compile("[a-z]*"))).isTrue();
    assertThat(MavenDependencyAbstractMatcher.attributeMatchesPattern(new LocatedAttribute("0123"), Pattern.compile("[a-z]*"))).isFalse();
  }

  @Test
  public void no_effect_matcher_should_always_match() {
    MavenDependencyAbstractMatcher matcher = MavenDependencyAbstractMatcher.alwaysMatchingMatcher();

    Dependency dependency = new Dependency();
    dependency.setVersion(null);
    assertThat(matcher.matches(dependency)).isTrue();

    dependency.setVersion(new LocatedAttribute(""));
    assertThat(matcher.matches(dependency)).isTrue();

    dependency.setVersion(new LocatedAttribute("1234"));
    assertThat(matcher.matches(dependency)).isTrue();
  }

  @Test
  public void should_compile_valid_regex() {
    assertThat(MavenDependencyAbstractMatcher.compileRegex(".*")).isNotNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_on_invalid_regex() {
    MavenDependencyAbstractMatcher.compileRegex("*");
  }

}
