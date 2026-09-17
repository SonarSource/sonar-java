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

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Estimates the heap memory retained by a {@link SpringContextModel}, in bytes.
 *
 * <p>The figure is an <strong>estimate</strong>, not a measurement. It is computed by walking the model's indexes and
 * summing shallow object sizes derived from the JVM layout constants below, which assume a 64-bit HotSpot JVM with
 * compressed object pointers and 8-byte object alignment. Strings are assumed to use the Latin-1 coder, which holds for
 * the fully-qualified type names, bean names and package names the model stores. Immutable collections
 * ({@code Set.copyOf}, {@code Collectors.toUnmodifiableMap}) are accounted for with the {@link java.util.HashMap}
 * formulas; their real layout differs in detail.
 *
 * <p>The result is a deterministic, self-consistent indicator suitable for comparing projects and tracking growth over
 * time. It is accurate to an order of magnitude and must not be read as an exact retained size; establishing that
 * requires a heap dump.
 */
final class SpringContextModelSizeEstimator {

  private static final int OBJECT_HEADER = 12;
  private static final int ARRAY_HEADER = 16;
  private static final int REFERENCE = 4;
  private static final int INT = 4;
  private static final int ALIGNMENT = 8;

  private static final float HASH_TABLE_LOAD_FACTOR = 0.75f;
  private static final int HASH_TABLE_MIN_CAPACITY = 16;
  private static final int HASH_TABLE_MAX_CAPACITY = 1 << 30;

  private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

  private SpringContextModelSizeEstimator() {
  }

  static long estimate(SpringContextModel model) {
    var estimator = new SpringContextModelSizeEstimator();
    return estimator.sizeOfBeanDefinitions(model.getBeanDefinitionRegistry().beanDefinitions())
      + estimator.sizeOfBeanEntriesByString(model.getTypeToBeansIndex().entriesByType())
      + estimator.sizeOfInjectionPointsByString(model.getTypeToDependenciesIndex().injectionPointsByType())
      + estimator.sizeOfEntriesByString(model.getEntityClassToPropertiesIndex().propertiesByEntityClass())
      + estimator.sizeOfStringsByString(model.getProjectPackageScan().packagesScannedBySpringPerModule());
  }

  private long sizeOfBeanDefinitions(Map<String, List<BeanDefinitionHolder>> beanDefinitions) {
    long size = shallowSizeOfMap(beanDefinitions.size());
    for (var entry : beanDefinitions.entrySet()) {
      size += sizeOfString(entry.getKey()) + shallowSizeOfList(entry.getValue().size());
      for (var holder : entry.getValue()) {
        size += sizeOfBeanDefinitionHolder(holder);
      }
    }
    return size;
  }

  private long sizeOfStringsByString(Map<String, Set<String>> index) {
    long size = shallowSizeOfMap(index.size());
    for (var entry : index.entrySet()) {
      size += sizeOfString(entry.getKey()) + sizeOfStringSet(entry.getValue());
    }
    return size;
  }

  private long sizeOfBeanEntriesByString(Map<String, Set<TypeToBeansIndex.BeanEntry>> index) {
    long size = shallowSizeOfMap(index.size());
    for (var entry : index.entrySet()) {
      size += sizeOfString(entry.getKey()) + shallowSizeOfSet(entry.getValue().size());
      for (var beanEntry : entry.getValue()) {
        if (charge(beanEntry)) {
          size += align(OBJECT_HEADER + 3L * REFERENCE)
            + sizeOfString(beanEntry.name())
            + sizeOfString(beanEntry.module())
            + sizeOfString(beanEntry.beanPackage());
        }
      }
    }
    return size;
  }

  private long sizeOfInjectionPointsByString(Map<String, Set<InjectionPoint>> index) {
    long size = shallowSizeOfMap(index.size());
    for (var entry : index.entrySet()) {
      size += sizeOfString(entry.getKey()) + shallowSizeOfSet(entry.getValue().size());
      for (var injectionPoint : entry.getValue()) {
        size += sizeOfInjectionPoint(injectionPoint);
      }
    }
    return size;
  }

  private long sizeOfEntriesByString(Map<String, Set<Map.Entry<String, String>>> index) {
    long size = shallowSizeOfMap(index.size());
    for (var entry : index.entrySet()) {
      size += sizeOfString(entry.getKey()) + shallowSizeOfSet(entry.getValue().size());
      for (var property : entry.getValue()) {
        if (charge(property)) {
          size += align(OBJECT_HEADER + 2L * REFERENCE) + sizeOfString(property.getKey()) + sizeOfString(property.getValue());
        }
      }
    }
    return size;
  }

  private long sizeOfBeanDefinitionHolder(BeanDefinitionHolder holder) {
    if (!charge(holder)) {
      return 0;
    }
    long size = align(OBJECT_HEADER + 6 * REFERENCE + 1L)
      + sizeOfString(holder.getType())
      + sizeOfString(holder.getModule())
      + sizeOfString(holder.getBeanPackage())
      + sizeOfString(holder.getProfiles())
      + sizeOfBeanLocation(holder.getLocation());
    var dependingBeans = holder.getDependingBeans();
    if (charge(dependingBeans)) {
      size += shallowSizeOfMap(dependingBeans.size());
      for (var entry : dependingBeans.entrySet()) {
        size += sizeOfString(entry.getKey()) + sizeOfStringSet(entry.getValue());
      }
    }
    return size;
  }

  private long sizeOfInjectionPoint(InjectionPoint injectionPoint) {
    if (!charge(injectionPoint)) {
      return 0;
    }
    return align(OBJECT_HEADER + 2L * REFERENCE)
      + sizeOfString(injectionPoint.name())
      + sizeOfBeanLocation(injectionPoint.location());
  }

  private long sizeOfBeanLocation(@Nullable BeanLocation location) {
    if (location == null || !charge(location)) {
      return 0;
    }
    long size = align(OBJECT_HEADER + 2L * REFERENCE);
    var textSpan = location.mainLocation();
    if (charge(textSpan)) {
      size += align(OBJECT_HEADER + 4L * INT);
    }
    return size;
  }

  private long sizeOfStringSet(Collection<String> strings) {
    long size = shallowSizeOfSet(strings.size());
    for (var string : strings) {
      size += sizeOfString(string);
    }
    return size;
  }

  private long sizeOfString(@Nullable String value) {
    if (value == null || !charge(value)) {
      return 0;
    }
    return align(OBJECT_HEADER + REFERENCE + INT + 2L) + align((long) ARRAY_HEADER + value.length());
  }

  /**
   * @return {@code true} the first time the given instance is seen, so that shared objects are counted exactly once.
   */
  private boolean charge(Object object) {
    return visited.add(object);
  }

  /**
   * Size of a {@link java.util.HashMap} with the given number of entries, excluding the keys and values themselves.
   * An empty map has no backing table, which {@link java.util.HashMap} only allocates on the first insertion.
   */
  private static long shallowSizeOfMap(int entryCount) {
    long size = align(OBJECT_HEADER + 8L * REFERENCE);
    if (entryCount > 0) {
      size += align(ARRAY_HEADER + REFERENCE * (long) hashTableCapacity(entryCount)) + entryCount * align(OBJECT_HEADER + INT + 3L * REFERENCE);
    }
    return size;
  }

  /**
   * Size of a {@link java.util.HashSet} with the given number of elements, excluding the elements themselves.
   */
  private static long shallowSizeOfSet(int elementCount) {
    return shallowSizeOfMap(elementCount) + align((long) OBJECT_HEADER + REFERENCE);
  }

  /**
   * Size of an {@link java.util.ArrayList} with the given number of elements, excluding the elements themselves.
   */
  private static long shallowSizeOfList(int elementCount) {
    return align(OBJECT_HEADER + REFERENCE + 2L * INT) + align(ARRAY_HEADER + (long) REFERENCE * elementCount);
  }

  /**
   * Capacity of the backing table a {@link java.util.HashMap} grows to for the given number of entries.
   */
  private static int hashTableCapacity(int entryCount) {
    int capacity = HASH_TABLE_MIN_CAPACITY;
    while (capacity < HASH_TABLE_MAX_CAPACITY && capacity * HASH_TABLE_LOAD_FACTOR < entryCount) {
      capacity <<= 1;
    }
    return capacity;
  }

  private static long align(long size) {
    return (size + ALIGNMENT - 1) / ALIGNMENT * ALIGNMENT;
  }
}
