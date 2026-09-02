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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Index mapping fully-qualified bean type names to the names of all dependencies of that type
 * discovered during Spring context scanning.
 *
 * <p>A single type may have multiple dependencies registered. Lookup returns an empty set for types with no
 * registered dependencies.
 *
 * <p>Dependencies are represented by a name (either the field/parameter name at the injection point or the value of
 * the `@Qualifier` annotation if present)
 */
public class TypeToDependenciesIndex {
  /** Injection points stored by dependency name (either field/parameter name or qualifier annotation value) and location*/
  public record InjectionPoint(String name, BeanLocation location) {}

  /** Dependencies indexed by fully-qualified required type. */
  private final Map<String, Set<InjectionPoint>> injectionPointsByType = new HashMap<>();

  /**
   * Registers a dependency under the given type.
   *
   * @param dependencyType fully-qualified name of the dependency's type
   * @param dependencyName the dependency name to associate with that type
   * @param location       the source location of the injection point
   */
  public void addDependencyForType(String dependencyType, String dependencyName, BeanLocation location) {
    injectionPointsByType.computeIfAbsent(dependencyType, k -> new HashSet<>())
      .add(new InjectionPoint(dependencyName, location));
  }

  /**
   * Returns an unmodifiable set of all injection points registered for the given type.
   *
   * @param dependencyType fully-qualified class name of the dependency's type
   * @return an unmodifiable set of injection points, or an empty set if none were registered
   */
  public Set<InjectionPoint> getDependenciesForType(String dependencyType) {
    return Collections.unmodifiableSet(injectionPointsByType.getOrDefault(dependencyType, Set.of()));
  }
}
