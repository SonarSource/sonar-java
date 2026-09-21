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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SizeEstimatorTest {

  @ParameterizedTest
  @CsvSource({
    "0, 0, 16",
    "1, 0, 16",
    "2, 0, 24",
    "0, 4, 16",
    "0, 5, 24"
  })
  void estimates_aligned_shallow_object_size(int referenceCount, int primitiveSize, long expectedSize) {
    var value = new Object();

    assertThat(estimate(estimator -> estimator.estimateShallowObject(value, referenceCount, primitiveSize)))
      .isEqualTo(expectedSize);
  }

  @ParameterizedTest
  @CsvSource({
    "0, 40",
    "1, 48",
    "8, 48",
    "9, 56",
    "16, 56",
    "17, 64"
  })
  void estimates_string_and_backing_array_size(int length, long expectedSize) {
    String value = "a".repeat(length);

    assertThat(estimate(estimator -> estimator.estimateString(value))).isEqualTo(expectedSize);
  }

  @ParameterizedTest
  @CsvSource({
    "0, 48",
    "1, 160",
    "12, 512",
    "13, 608"
  })
  void estimates_map_storage_size(int entryCount, long expectedSize) {
    var map = mapWithSize(entryCount);

    assertThat(estimate(estimator -> estimator.estimateMap(map))).isEqualTo(expectedSize);
  }

  @ParameterizedTest
  @CsvSource({
    "0, 64",
    "1, 176",
    "12, 528",
    "13, 624"
  })
  void estimates_set_storage_size(int elementCount, long expectedSize) {
    Set<Integer> set = new HashSet<>(mapWithSize(elementCount).keySet());

    assertThat(estimate(estimator -> estimator.estimateSet(set))).isEqualTo(expectedSize);
  }

  @ParameterizedTest
  @CsvSource({
    "0, 40",
    "1, 48",
    "2, 48",
    "3, 56"
  })
  void estimates_list_storage_size(int elementCount, long expectedSize) {
    List<Object> list = new ArrayList<>();
    for (int i = 0; i < elementCount; i++) {
      list.add(new Object());
    }

    assertThat(estimate(estimator -> estimator.estimateList(list))).isEqualTo(expectedSize);
  }

  @Test
  void caps_map_capacity_and_uses_long_arithmetic() {
    Map<?, ?> map = mock(Map.class);
    when(map.size()).thenReturn(Integer.MAX_VALUE);

    assertThat(estimate(estimator -> estimator.estimateMap(map))).isEqualTo(73_014_444_064L);
  }

  @Test
  void handles_empty_and_null_roots_and_null_strings() {
    assertThat(SizeEstimator.estimate()).isZero();
    assertThat(SizeEstimator.estimate(new SizeEstimable[]{null})).isZero();
    assertThat(estimate(estimator -> estimator.estimateString(null))).isZero();
  }

  @Test
  void deduplicates_by_identity_not_equality() {
    String first = new String("value");
    String equalButDistinct = new String("value");

    assertThat(estimate(estimator -> estimator.estimateString(first)
      + estimator.estimateString(first)
      + estimator.estimateString(equalButDistinct)))
      .isEqualTo(96);
  }

  @Test
  void shares_identity_tracking_across_estimation_methods() {
    List<Object> list = new ArrayList<>();

    assertThat(estimate(estimator -> estimator.estimateList(list)
      + estimator.estimateShallowObject(list, 0, 0)))
      .isEqualTo(40);
  }

  @Test
  void collection_estimators_do_not_estimate_elements() {
    String element = new String("123456789");
    List<String> list = List.of(element);

    assertThat(estimate(estimator -> estimator.estimateList(list)
      + estimator.estimateString(element)))
      .isEqualTo(104);
  }

  @Test
  void estimates_shared_and_cyclic_graphs_once() {
    var first = new Node();
    var second = new Node();
    var shared = new Node();
    first.child = shared;
    second.child = shared;
    shared.child = first;

    assertThat(SizeEstimator.estimate(first, first, second)).isEqualTo(48);
  }

  private static long estimate(ToLongFunction<SizeEstimator> estimation) {
    return SizeEstimator.estimate(new SizeEstimable[]{estimation::applyAsLong});
  }

  private static Map<Integer, Integer> mapWithSize(int size) {
    Map<Integer, Integer> map = new HashMap<>();
    for (int i = 0; i < size; i++) {
      map.put(i, i);
    }
    return map;
  }

  private static class Node implements SizeEstimable {

    private Node child;

    @Override
    public long estimateSize(SizeEstimator estimator) {
      return estimator.estimateShallowObject(this, 1, 0) + estimator.estimate(child);
    }
  }
}
