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
package org.sonar.java.model.springcontext;

import java.util.List;
import org.sonar.plugins.java.api.JavaCheck;

/**
 * Registry of all {@link SpringContextModelGatherer} visitors that populate the {@link SpringContextModel}
 * during a module analysis.
 *
 * <p>Use {@link #getAllGatherers()} to obtain the full list of gatherers to be registered with the scanner.
 * New gatherers should be added here as the set of Spring context data we collect grows.
 */
public class SpringContextModelGatherers {

  private SpringContextModelGatherers() {
    // utility class, should not be instantiated
  }

  /**
   * Returns all gatherers that contribute data to the {@link SpringContextModel}.
   *
   * @return a list of {@link JavaCheck} instances, each implementing {@link SpringContextModelGatherer}
   */
  public static List<JavaCheck> getAllGatherers() {
    return List.of(
      new ComponentScanPackageGatherer()
    );
  }

}
