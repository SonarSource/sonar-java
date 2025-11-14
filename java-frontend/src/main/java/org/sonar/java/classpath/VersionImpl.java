/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.Version;

/**
 * Class to parse and compare versions of library jars.
 */
public record VersionImpl(Integer major, Integer minor, @Nullable Integer patch, @Nullable String qualifier) implements Comparable<Version>, Version {

  public static String VERSION_REGEX = "([0-9]+).([0-9]+)(.[0-9]+)?([^0-9].*)?";

  private static final Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEX);

  /**
   * matcher must come from a match against a pattern that contains {@link #VERSION_REGEX} and no other groups.
   */
  public static VersionImpl matcherToVersion(Matcher matcher) {
    return new VersionImpl(
      Integer.parseInt(matcher.group(1)),
      Integer.parseInt(matcher.group(2)),
      matcher.group(3) != null ? Integer.parseInt(matcher.group(3).substring(1)) : null,
      matcher.group(4));
  }

  public static VersionImpl parse(String versionString) {
    Matcher matcher = VERSION_PATTERN.matcher(versionString);
    if (matcher.matches()) {
      return matcherToVersion(matcher);
    }
    throw new IllegalArgumentException("Not a valid version string: " + versionString);
  }

  /**
   * Warning: this is a partial order: 2.5 and 2.5.1 are incomparable.
   * Qualifiers are ignored.
   */
  @Override
  public int compareTo(Version o) {
    if (!Objects.equals(major, o.major())) {
      return major - o.major();
    }
    if (!Objects.equals(minor, o.minor())) {
      return minor - o.minor();
    }
    if (!Objects.equals(patch, o.patch())) {
      if (patch == null || o.patch() == null) return 0;
      return patch - o.patch();
    }
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Version version)) return false;
    return Objects.equals(major, version.major())
      && Objects.equals(minor, version.minor())
      && Objects.equals(patch, version.patch())
      && Objects.equals(qualifier, version.qualifier());
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch, qualifier);
  }

  @Override
  public String toString() {
    return major + "." + minor +
      (patch == null ? "" : ("." + patch)) +
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

  public boolean isGreaterThanOrEqualTo(String version) {
    return isGreaterThanOrEqualTo(VersionImpl.parse(version));
  }

  public boolean isGreaterThan(String version) {
    return isGreaterThan(VersionImpl.parse(version));
  }

  public boolean isLowerThanOrEqualTo(String version) {
    return isLowerThanOrEqualTo(VersionImpl.parse(version));
  }

  public boolean isLowerThan(String version) {
    return isLowerThan(VersionImpl.parse(version));
  }

}
