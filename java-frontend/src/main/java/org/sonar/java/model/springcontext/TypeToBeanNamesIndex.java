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
 * Index mapping fully-qualified bean type names to the names of all beans of that type
 * discovered during Spring context scanning.
 *
 * <p>A single type may have multiple bean names registered (e.g. when the same class is
 * declared as several distinct beans). Lookup returns an empty set for types with no
 * registered beans.
 */
public class TypeToBeanNamesIndex {
  /** Bean names indexed by fully-qualified type name. */
  private final Map<String, Set<String>> beanNamesByType = new HashMap<>();

  /**
   * Registers a bean name under the given type.
   *
   * @param beanType fully-qualified class name of the bean's type
   * @param beanName the bean name to associate with that type
   */
  public void addBeanForType(String beanType, String beanName) {
    beanNamesByType.computeIfAbsent(beanType, k -> new HashSet<>()).add(beanName);
  }

  /**
   * Returns an immutable set of all bean names registered for the given type.
   *
   * @param beanType fully-qualified class name of the bean's type
   * @return an unmodifiable set of bean names, or an empty set if none were registered
   */
  public Set<String> getNamesForType(String beanType) {
    return Collections.unmodifiableSet(beanNamesByType.getOrDefault(beanType, Set.of()));
  }
}