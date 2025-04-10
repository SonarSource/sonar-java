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
package org.sonar.plugins.java.api;

import java.util.Optional;
import java.util.function.Function;

/**
 * Implementing this interface allows a check to be executed - or not - during analysis, depending on the
 * version of desired libraries among dependencies of the current module.
 *
 * <p> For example, if a rule is to be used only on modules of a project that use spring-web version at least 4.3,
 * it should implement {@link DependencyVersionAware} with the following method:
 * <pre>
 *  {@literal @}Override
 *   public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
 *     return dependencyFinder.apply("spring-web")
 *       .map(v -> v.isGreaterThanOrEqualTo("4.3"))
 *       .orElse(false);
 *   }
 * </pre>
 */
public interface DependencyVersionAware {

  /**
   * Control whether the check is compatible with the dependencies of the project being analysed.
   *
   * @param dependencyFinder is a function that takes in the name of an artifact that may be found in the classpath
   *                         of the project. It returns the version of that artifact that was detected, or an empty
   *                         optional if it is not detected in the classpath.
   *                         Note that we cannot guarantee that a dependency will always be detected in the case were
   *                         the classpath doesn't come from a standard build system like maven or gradle.
   * @return true if the check is compatible with the detected dependencies and should be executed on sources, false otherwise.
   */
  boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder);

}
