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

import org.sonar.plugins.java.api.classpath.DependencyVersion;

public class DependencyVersionImpl implements DependencyVersion {

  private final String groupId;
  private final String artifactId;
  private final Version version;

  public DependencyVersionImpl(String groupId, String artifactId, Version version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public Version getVersion() {
    return version;
  }

  @Override
  public boolean isGreaterThanOrEqualTo(String version) {
    return Version.parse(version).map(this.version::isGreaterThanOrEqualTo).orElse(false);
  }

  @Override
  public boolean isGreaterThan(String version) {
    return Version.parse(version).map(this.version::isGreaterThan).orElse(false);
  }

  @Override
  public boolean isLowerThanOrEqualTo(String version) {
    return Version.parse(version).map(this.version::isLowerThanOrEqualTo).orElse(false);
  }

  @Override
  public boolean isLowerThan(String version) {
    return Version.parse(version).map(this.version::isLowerThan).orElse(false);
  }

  public record CacheKey(String groupId, String artifactId) {
  }

}
