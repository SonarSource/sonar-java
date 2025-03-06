/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.classpath;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public record Version(Integer major, @Nullable Integer minor, @Nullable Integer patch, @Nullable String qualifier) implements Comparable<Version> {

  public static String VERSION_REGEX = "([0-9]+).([0-9]+).([0-9]+)([^0-9].*)?";

  static Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEX);

  /**
   * matcher must come from a match again a pattern that contains {@link #VERSION_PATTERN} and no other groups.
   */
  static Version matcherToVersion(Matcher matcher) {
    return new Version(
      Integer.parseInt(matcher.group(1)),
      Integer.parseInt(matcher.group(2)),
      Integer.parseInt(matcher.group(3)),
      matcher.group(4));
  }

  static Optional<Version> parse(String versionString) {
    Matcher matcher = VERSION_PATTERN.matcher(versionString);
    if (matcher.matches()) {
      return Optional.of(matcherToVersion(matcher));
    }
    return Optional.empty();
  }

  @Override
  public int compareTo(Version o) {
    if (!Objects.equals(major, o.major)) {
      return major - o.major;
    }
    if (!Objects.equals(minor, o.minor)) {
      if (minor == null) return -1;
      if (o.minor == null) return 1;
      return minor - o.minor;
    }
    if (!Objects.equals(patch, o.patch)) {
      if (patch == null) return -1;
      if (o.patch == null) return 1;
      return patch - o.patch;
    }
    if (Objects.equals(qualifier, o.qualifier)) {
      return 0;
    }
    if (qualifier == null) return -1;
    if (o.qualifier == null) return 1;
    return qualifier.compareTo(o.qualifier);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Version version = (Version) o;
    return Objects.equals(major, version.major) && Objects.equals(minor, version.minor)
      && Objects.equals(patch, version.patch) && Objects.equals(qualifier, version.qualifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch, qualifier);
  }

  @Override
  public String toString() {
    return major +
      (minor == null ? "" : "." + minor) +
      (patch == null ? "" : "." + patch) +
      (qualifier == null ? "" : qualifier);
  }

  public boolean isGreaterThanOrEqualTo(Version version) {
    return compareTo(version) >= 0;
  }

  public boolean isGreaterThan(Version version) {
    return compareTo(version) > 0;
  }

  public boolean isLowerThanOrEqualTo(Version version) {
    return compareTo(version) <= 0;
  }

  public boolean isLowerThan(Version version) {
    return compareTo(version) < 0;
  }

}
