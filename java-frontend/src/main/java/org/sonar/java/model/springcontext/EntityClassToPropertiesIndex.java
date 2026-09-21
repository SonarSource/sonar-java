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
import org.sonar.java.telemetry.SizeEstimable;
import org.sonar.java.telemetry.SizeEstimator;

/**
 * Index mapping JPA / Hibernate {@code @Entity} class names to their associated
 * key-value properties collected during scanning.
 *
 * <p>Multiple properties can be registered for the same entity class.
 * Lookup returns an empty set for entity classes that have no registered properties.
 */
public class EntityClassToPropertiesIndex implements SizeEstimable {
  /**
   * Properties indexed by fully-qualified {@code @Entity} class name.
   */
  private final Map<String, Set<Map.Entry<String, String>>> propertiesByEntityClass = new HashMap<>();

  /**
   * Registers a property for the given {@code @Entity} class.
   *
   * @param entityClass   fully-qualified name of the {@code @Entity} class
   * @param propertyKey   the property name
   * @param propertyValue the property value
   */
  public void addProperty(String entityClass, String propertyKey, String propertyValue) {
    propertiesByEntityClass.computeIfAbsent(entityClass, k -> new HashSet<>()).add(Map.entry(propertyKey, propertyValue));
  }

  /**
   * Returns an immutable set of all properties registered for the given {@code @Entity} class.
   *
   * @param entityClass fully-qualified name of the {@code @Entity} class
   * @return an unmodifiable set of key-value property entries, or an empty set if none were registered
   */
  public Set<Map.Entry<String, String>> getPropertiesForEntity(String entityClass) {
    return Collections.unmodifiableSet(propertiesByEntityClass.getOrDefault(entityClass, Set.of()));
  }

  @Override
  public long estimateSize(SizeEstimator estimator) {
    long size = estimator.estimateMap(propertiesByEntityClass);
    for (var entry : propertiesByEntityClass.entrySet()) {
      size += estimator.estimateString(entry.getKey()) + estimator.estimateSet(entry.getValue());
      for (var property : entry.getValue()) {
        size += estimator.estimateShallowObject(property, 2, 0)
          + estimator.estimateString(property.getKey())
          + estimator.estimateString(property.getValue());
      }
    }
    return size;
  }
}
