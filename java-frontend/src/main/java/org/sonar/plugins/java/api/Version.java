/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.plugins.java.api;

/** Versions of libraries. This provides methods to compare the version with other ones. */
public interface Version {

  Integer major();
  Integer minor();
  Integer patch();
  String qualifier();

  boolean isGreaterThanOrEqualTo(Version version);
  boolean isGreaterThanOrEqualTo(String version);
  boolean isGreaterThan(Version version);
  boolean isLowerThanOrEqualTo(Version version);
  boolean isLowerThan(Version version);
  boolean isGreaterThan(String version);
  boolean isLowerThanOrEqualTo(String version);
  boolean isLowerThan(String version);
}
