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
package org.sonar.plugins.java.api.classpath;

public interface DependencyVersion {

  String getGroupId();

  String getArtifactId();

  String getVersion();

  /**
   * Returns whether the dependency version has a higher or equal version number than the one provided
   * @param version The version number we want to check against e.g. "1.8.0"
   */
  boolean isGreaterThanOrEqualTo(String version);

  /**
   * Returns whether the dependency version has a higher version number than the one provided
   * @param version The version number we want to check against e.g. "1.8.0"
   */
  boolean isGreaterThan(String version);

  /**
   * Returns whether the dependency version has a lower or equal version number than the one provided
   * @param version The version number we want to check against e.g. "1.8.0"
   */
  boolean isLowerThanOrEqualTo(String version);

  /**
   * Returns whether the dependency version has a lower version number than the one provided
   * @param version The version number we want to check against e.g. "1.8.0"
   */
  boolean isLowerThan(String version);

}
