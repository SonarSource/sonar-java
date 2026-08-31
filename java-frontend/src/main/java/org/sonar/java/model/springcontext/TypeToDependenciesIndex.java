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
  /** Dependencies (name, location) indexed by fully-qualified type. */
  private final Map<String, Set<String>> beanDependenciesByType = new HashMap<>();

  /**
   * Registers a dependency under the given type.
   *
   * @param dependencyType fully-qualified name of the dependency's type
   * @param dependencyName the dependency name to associate with that type
   */
  public void addBeanForType(String dependencyType, String dependencyName) {
    beanDependenciesByType.computeIfAbsent(dependencyType, k -> new HashSet<>()).add(dependencyName);
  }

  /**
   * Returns an immutable set of all bean names registered for the given type.
   *
   * @param dependencyType fully-qualified class name of the dependency's type
   * @return an unmodifiable set of bean names, or an empty set if none were registered
   */
  public Set<String> getNamesForType(String dependencyType) {
    return Collections.unmodifiableSet(beanDependenciesByType.getOrDefault(dependencyType, Set.of()));
  }
}
