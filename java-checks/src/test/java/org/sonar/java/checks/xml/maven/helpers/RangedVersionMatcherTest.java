/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import javax.annotation.Nullable;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RangedVersionMatcherTest {

  private RangedVersionMatcher matcher;

  @Test
  public void no_version_never_match() {
    matcher = new RangedVersionMatcher("1.2", "1.5.6");
    assertNotMatch(null);
    assertNotMatch("");
  }

  @Test
  public void invalid_version_never_match() {
    matcher = new RangedVersionMatcher("1.2", "1.5.6");
    assertNotMatch("invalid.0");
    assertNotMatch("1.invalid");
  }

  @Test
  public void fail_with_double_wildcard() {
    assertThrows(IllegalArgumentException.class,
      () -> new RangedVersionMatcher("*", "*"));
  }

  @Test
  public void fail_with_invalid_version() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> new RangedVersionMatcher("1.2.3", "invalid"));
    assertThat(e.getMessage()).isEqualTo("Provided version does not match expected pattern: <major version>.<minor version>.<incremental version> (recieved: invalid)");
  }

  @Test
  public void version_after_range_never_match() {
    matcher = new RangedVersionMatcher("1.2", "1.5.6");
    assertNotMatch("1.5.7-SNAPSHOT");
    assertNotMatch("1.5.7");
    assertNotMatch("1.6");
    assertNotMatch("2");
  }

  @Test
  public void version_before_range_never_match() {
    matcher = new RangedVersionMatcher("1.2", "1.5.6");
    assertNotMatch("1.1.9-SNAPSHOT");
    assertNotMatch("1.1.9");
    assertNotMatch("1.1");
    assertNotMatch("1");
    assertNotMatch("0");
  }

  @Test
  public void version_in_range_always_match() {
    matcher = new RangedVersionMatcher("1.2", "1.5.6");
    assertMatches("1.2.1.1");
    assertMatches("1.2.1-SNAPSHOT");
    assertMatches("1.3.4");
    assertMatches("1.3");

    // bounds are included
    assertMatches("1.2");
    assertMatches("1.5.6");
  }

  @Test
  public void version_with_wildcard_for_lower_bound() {
    matcher = new RangedVersionMatcher("*", "1.5.6");
    assertNotMatch("1.5.7");
    assertNotMatch("1.6");
    assertNotMatch("2");

    assertMatches("1.5");
    assertMatches("1.5.5");
    assertMatches("1");
    assertMatches("0.1-SNAPSHOT");
  }

  @Test
  public void version_with_wildcard_for_upper_bound() {
    matcher = new RangedVersionMatcher("1.5.6", "*");
    assertNotMatch("1.5");
    assertNotMatch("1.5.5");
    assertNotMatch("1");
    assertNotMatch("0.1-SNAPSHOT");

    assertMatches("1.5.7-SNAPSHOT");
    assertMatches("1.5.7");
    assertMatches("1.6");
    assertMatches("2");
  }

  private void assertMatches(@Nullable String version) {
    assertWithMatcher(version).isTrue();
  }

  private void assertNotMatch(@Nullable String version) {
    assertWithMatcher(version).isFalse();
  }

  private AbstractBooleanAssert<?> assertWithMatcher(@Nullable String version) {
    if (version != null) {
      return assertThat(matcher.test(version));
    }
    return assertThat(matcher.test(null));
  }
}
