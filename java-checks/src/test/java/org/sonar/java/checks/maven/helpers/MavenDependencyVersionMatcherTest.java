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

import org.junit.Before;
import org.junit.Test;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.maven2.Dependency;

import static org.fest.assertions.Assertions.assertThat;

public class MavenDependencyVersionMatcherTest {

  Dependency dependency;

  @Before
  public void setup() {
    dependency = new Dependency();
  }

  @Test
  public void no_effect_matcher_should_always_match() {
    MavenDependencyVersionMatcher matcher = MavenDependencyVersionMatcher.alwaysMatchingVersionMatcher();

    this.dependency.setVersion(null);
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1234");
    assertThat(matcher.matches(dependency)).isTrue();
  }

  @Test
  public void no_version() {
    MavenDependencyVersionMatcher matcher = MavenDependencyVersionMatcher.fromString("", "myCheck");

    this.dependency.setVersion(null);
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1234");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.2.3-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isTrue();
  }

  @Test
  public void wildcard() {
    MavenDependencyVersionMatcher matcher = MavenDependencyVersionMatcher.fromString("*", "myCheck");

    this.dependency.setVersion(null);
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1234");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.2.3-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isTrue();
  }

  @Test
  public void fixed_version() {
    MavenDependencyVersionMatcher matcher = MavenDependencyVersionMatcher.fromString("1", "myCheck");

    this.dependency.setVersion(null);
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.2.3-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isFalse();

    matcher = MavenDependencyVersionMatcher.fromString("(1.2.3).*", "myCheck");
    assertThat(matcher.matches(dependency)).isTrue();
  }

  @Test
  public void fixed_version_with_pattern() {
    MavenDependencyVersionMatcher matcher = MavenDependencyVersionMatcher.fromString("(1.2).*", "myCheck");

    this.dependency.setVersion(null);
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.2.3-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.2.3-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isTrue();
  }

  @Test(expected=IllegalArgumentException.class)
  public void should_fail_to_create_ranged_version_because_of_upper_bound()  {
    MavenDependencyVersionMatcher.fromString("1.2-1.invalid", "myCheck");
  }

  @Test(expected=IllegalArgumentException.class)
  public void should_fail_to_create_ranged_version_because_of_lower_bound()  {
    MavenDependencyVersionMatcher.fromString("1.invalid-2.0", "myCheck");
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_to_create_ranged_version_because_of_invalid_wildcard() {
    MavenDependencyVersionMatcher.fromString("*-*", "myCheck");
  }

  @Test
  public void ranged_version_is_after() {
    MavenDependencyVersionMatcher matcher = MavenDependencyVersionMatcher.fromString("1.2-1.5.6", "myCheck");

    dependency.setVersion(null);
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.5.7-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.5.7");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.6");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("2");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("invalid.0");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.invalid");
    assertThat(matcher.matches(dependency)).isFalse();

    matcher = MavenDependencyVersionMatcher.fromString("1.2.3-1.5", "myCheck");
    setDependencyVersion("1.3");
    assertThat(matcher.matches(dependency)).isTrue();
  }

  @Test
  public void ranged_version_with_wildcard_for_lower_bound() {
    MavenDependencyVersionMatcher matcher = MavenDependencyVersionMatcher.fromString("*-1.5.6", "myCheck");

    dependency.setVersion(null);
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.5.7-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.5.7");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.5");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.5.6");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.5.5");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("0.1-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isTrue();
  }

  @Test
  public void ranged_version_with_wildcard_for_upper_bound() {
    MavenDependencyVersionMatcher matcher = MavenDependencyVersionMatcher.fromString("1.5.6-*", "myCheck");

    dependency.setVersion(null);
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.5.7-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.5.6");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.5");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.6");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("2.0");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("0.1-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isFalse();
  }

  @Test
  public void ranged_version_is_before() {
    MavenDependencyVersionMatcher matcher = MavenDependencyVersionMatcher.fromString("1.2-1.5.6", "myCheck");

    dependency.setVersion(null);
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.1.9-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.1.9");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.1");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("0");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("invalid.0");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.invalid");
    assertThat(matcher.matches(dependency)).isFalse();
  }

  @Test
  public void ranged_version_is_in_range() {
    MavenDependencyVersionMatcher matcher = MavenDependencyVersionMatcher.fromString("1.2-1.5.6", "myCheck");

    setDependencyVersion("1.2");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.2.1.1");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.2.1-SNAPSHOT");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.3.4");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("1.5.6");
    assertThat(matcher.matches(dependency)).isTrue();

    setDependencyVersion("invalid.0");
    assertThat(matcher.matches(dependency)).isFalse();

    setDependencyVersion("1.invalid");
    assertThat(matcher.matches(dependency)).isFalse();
  }

  private void setDependencyVersion(String version) {
    this.dependency.setVersion(new LocatedAttribute(version));
  }

}
