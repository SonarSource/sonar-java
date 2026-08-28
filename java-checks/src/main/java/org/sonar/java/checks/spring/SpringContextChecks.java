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
package org.sonar.java.checks.spring;

import java.util.List;

/**
 * Registry of all {@link SpringContextCheck}s to be run against the {@code SpringContextModel}.
 *
 * <p>Use {@link #getAllChecks()} to obtain the full list of checks to be run by the scanner.
 * New checks should be added here as the set of Spring context issues we detect grows.
 */
public final class SpringContextChecks {

  private SpringContextChecks() {
    // utility class, should not be instantiated
  }

  /**
   * Returns all checks that reason over the {@code SpringContextModel}.
   */
  public static List<SpringContextCheck> getAllChecks() {
    return List.of(
      new AmbiguousDependencyCheck()
    );
  }

}
