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

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class MavenDependencyMatcherTest {

  Dependency dependency;

  @Before
  public void setup() {
    dependency = new Dependency();
  }

  @Test(expected=IllegalArgumentException.class)
  public void no_name_should_fail() {
    MavenDependencyMatcher.fromString("");
  }

  @Test(expected=IllegalArgumentException.class)
  public void invalid_format_should_fail() {
    MavenDependencyMatcher.fromString(":");
  }

  @Test
  public void should_retrieve_matchers_comma_separated() {
    List<MavenDependencyMatcher> matchers = MavenDependencyMatcher.fromString("*:*");
    assertThat(matchers).hasSize(1);

    matchers = MavenDependencyMatcher.fromString("*:*,*:*");
    assertThat(matchers).hasSize(2);
  }

  @Test
  public void should_handle_wildcards_for_groupId() {
    List<MavenDependencyMatcher> matchers = MavenDependencyMatcher.fromString("*:log");
    MavenDependencyMatcher matcher = matchers.get(0);

    assertThat(matcher.matches(dependency)).isFalse();

    setArtifactIdAndGroupId("a.b.c", "log");
    assertThat(matcher.matches(dependency)).isTrue();

    setArtifactIdAndGroupId("log", "d");
    assertThat(matcher.matches(dependency)).isFalse();
  }

  @Test
  public void should_handle_wildcards_for_artifactId() {
    List<MavenDependencyMatcher> matchers = MavenDependencyMatcher.fromString("log:*");
    MavenDependencyMatcher matcher = matchers.get(0);

    assertThat(matcher.matches(dependency)).isFalse();

    setArtifactIdAndGroupId("log", "a");
    assertThat(matcher.matches(dependency)).isTrue();

    setArtifactIdAndGroupId("a.b.c", "log");
    assertThat(matcher.matches(dependency)).isFalse();
  }

  @Test
  public void should_handle_exact_values() {
    List<MavenDependencyMatcher> matchers = MavenDependencyMatcher.fromString("log:log");
    MavenDependencyMatcher matcher = matchers.get(0);

    assertThat(matcher.matches(dependency)).isFalse();

    setArtifactIdAndGroupId("log", "a");
    assertThat(matcher.matches(dependency)).isFalse();

    setArtifactIdAndGroupId("a.b.c", "log");
    assertThat(matcher.matches(dependency)).isFalse();

    setArtifactIdAndGroupId("log", "log");
    assertThat(matcher.matches(dependency)).isTrue();
  }

  @Test
  public void should_handle_versions() {
    List<MavenDependencyMatcher> matchers = MavenDependencyMatcher.fromString("log:log:1.3");
    MavenDependencyMatcher matcher = matchers.get(0);

    assertThat(matcher.matches(dependency)).isFalse();

    setArtifactIdAndGroupId("log", "a");
    assertThat(matcher.matches(dependency)).isFalse();

    setArtifactIdAndGroupId("a.b.c", "log");
    assertThat(matcher.matches(dependency)).isFalse();

    setArtifactIdAndGroupId("log", "log");
    assertThat(matcher.matches(dependency)).isFalse();

    setArtifactIdAndGroupId("log", "log", "1.2");
    assertThat(matcher.matches(dependency)).isFalse();

    setArtifactIdAndGroupId("log", "log", "1.3");
    assertThat(matcher.matches(dependency)).isTrue();
  }

  private void setArtifactIdAndGroupId(String groupId, String artifactId) {
    this.dependency.setGroupId(new LocatedAttribute(groupId));
    this.dependency.setArtifactId(new LocatedAttribute(artifactId));
  }

  private void setArtifactIdAndGroupId(String groupId, String artifactId, String version) {
    setArtifactIdAndGroupId(groupId, artifactId);
    this.dependency.setVersion(new LocatedAttribute(version));
  }

}
