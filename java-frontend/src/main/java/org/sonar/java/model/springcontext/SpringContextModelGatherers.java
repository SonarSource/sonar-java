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
import java.util.Optional;
import java.util.function.Function;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.plugins.java.api.Version;

/**
 * Registry of all {@link SpringContextModelGatherer} visitors that collect data for the {@link SpringContextModel}
 * during a module analysis.
 *
 * <p>Use {@link #getAllGatherers(SpringContextGatheringModel, Telemetry)} to obtain the full list of gatherers to be registered with the scanner.
 * New gatherers should be added here as the set of Spring context data we collect grows.
 */
public class SpringContextModelGatherers {

  private SpringContextModelGatherers() {
    // utility class, should not be instantiated
  }

  public static boolean hasSpringDependency(Function<String, Optional<Version>> dependencyFinder) {
    return dependencyFinder.apply("spring-context")
      .or(() -> dependencyFinder.apply("spring-beans"))
      .or(() -> dependencyFinder.apply("spring-boot-starter"))
      .or(() -> dependencyFinder.apply("spring-boot-starter-web"))
      .isPresent();
  }

  /**
   * Returns all gatherers that contribute data to the {@link SpringContextModel}.
   *
   * @return The gatherers to register with a scanner.
   */
  public static List<SpringContextModelGatherer> getAllGatherers(SpringContextGatheringModel springContextGatheringModel, Telemetry telemetry) {
    return List.of(
      new ComponentScanPackageGatherer(springContextGatheringModel, telemetry),
      new BeanDefinitionGatherer(springContextGatheringModel, telemetry)
    );
  }

}
