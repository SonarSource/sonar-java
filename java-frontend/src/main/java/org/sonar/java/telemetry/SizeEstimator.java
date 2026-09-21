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
package org.sonar.java.telemetry;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Estimates object sizes for a 64-bit HotSpot JVM with compressed object pointers and 8-byte object alignment. Strings
 * are assumed to use the Latin-1 coder. Immutable collections are approximated with the {@link java.util.HashMap}
 * formulas used for the mutable indexes.
 *
 * <p>A single instance must be shared by all parts of an object graph so that shared instances are charged once. The
 * result is a deterministic indicator suitable for comparing projects and tracking growth over time, not an exact
 * retained-size measurement.
 */
public final class SizeEstimator {

  private static final int OBJECT_HEADER = 12;
  private static final int ARRAY_HEADER = 16;
  private static final int REFERENCE = 4;
  private static final int INT = 4;
  private static final int ALIGNMENT = 8;

  private static final double HASH_TABLE_LOAD_FACTOR = 0.75d;
  private static final int HASH_TABLE_MIN_CAPACITY = 16;
  private static final int HASH_TABLE_MAX_CAPACITY = 1 << 30;

  private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

  private SizeEstimator() {
  }

  /**
   * Estimates several roots as one object graph, charging shared instances once.
   *
   * @param values The roots to estimate.
   * @return The estimated size in bytes.
   */
  public static long estimate(SizeEstimable... values) {
    var estimator = new SizeEstimator();
    long size = 0;
    for (var value : values) {
      size += estimator.estimate(value);
    }
    return size;
  }

  /**
   * Estimates a domain object unless the same instance has already been estimated.
   *
   * @param value The object to estimate.
   * @return The estimated size in bytes, or zero for a null or previously visited instance.
   */
  public long estimate(@Nullable SizeEstimable value) {
    if (value == null || !visited.add(value)) {
      return 0;
    }
    return value.estimateSize(this);
  }

  /**
   * Estimates an object's shallow size unless the same instance has already been estimated.
   *
   * @param object         The object to estimate.
   * @param referenceCount The number of reference fields.
   * @param primitiveSize  The total size of primitive fields in bytes.
   * @return The estimated shallow size in bytes, or zero for a previously visited instance.
   */
  public long estimateShallowObject(Object object, int referenceCount, int primitiveSize) {
    return visited.add(object) ? shallowSizeOfObject(referenceCount, primitiveSize) : 0;
  }

  /**
   * Estimates a compact Latin-1 string and its backing byte array.
   *
   * @param value The string to estimate.
   * @return The estimated size in bytes, or zero for a null or previously visited instance.
   */
  public long estimateString(@Nullable String value) {
    if (value == null || !visited.add(value)) {
      return 0;
    }
    return shallowSizeOfObject(1, INT + 2) + align((long) ARRAY_HEADER + value.length());
  }

  /**
   * Estimates a map and its internal storage without estimating its keys or values.
   *
   * @param map The map to estimate.
   * @return The estimated size in bytes, or zero for a previously visited instance.
   */
  public long estimateMap(Map<?, ?> map) {
    return visited.add(map) ? shallowSizeOfMap(map.size()) : 0;
  }

  /**
   * Estimates a set and its internal storage without estimating its elements.
   *
   * @param set The set to estimate.
   * @return The estimated size in bytes, or zero for a previously visited instance.
   */
  public long estimateSet(Set<?> set) {
    return visited.add(set) ? shallowSizeOfSet(set.size()) : 0;
  }

  /**
   * Estimates a list and its internal storage without estimating its elements.
   *
   * @param list The list to estimate.
   * @return The estimated size in bytes, or zero for a previously visited instance.
   */
  public long estimateList(List<?> list) {
    return visited.add(list) ? shallowSizeOfList(list.size()) : 0;
  }

  private static long shallowSizeOfObject(int referenceCount, int primitiveSize) {
    return align(OBJECT_HEADER + (long) referenceCount * REFERENCE + primitiveSize);
  }

  private static long shallowSizeOfMap(int entryCount) {
    long size = shallowSizeOfObject(8, 0);
    if (entryCount > 0) {
      size += align(ARRAY_HEADER + REFERENCE * (long) hashTableCapacity(entryCount))
        + entryCount * shallowSizeOfObject(3, INT);
    }
    return size;
  }

  private static long shallowSizeOfSet(int elementCount) {
    return shallowSizeOfMap(elementCount) + shallowSizeOfObject(1, 0);
  }

  private static long shallowSizeOfList(int elementCount) {
    return shallowSizeOfObject(1, 2 * INT) + align(ARRAY_HEADER + (long) REFERENCE * elementCount);
  }

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
