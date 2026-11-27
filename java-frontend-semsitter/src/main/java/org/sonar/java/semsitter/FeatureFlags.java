/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

/**
 * Remote switches that decide which behavior an analysis gets.
 */
public interface FeatureFlags {

  /** Opens a flag service for one project. */
  @FunctionalInterface
  interface Connector {
    FeatureFlags connect(String clientSideId, String projectKey);
  }

  /**
   * @return the value of the flag, or {@code defaultValue} when the service cannot tell
   */
  boolean isEnabled(String flagKey, boolean defaultValue);
}
