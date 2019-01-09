/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.collections;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class AVLTreeTest {

  private static final class Key {
    private final int hashCode;
    private final String toString;

    private Key(int hashCode, String toString) {
      this.hashCode = hashCode;
      this.toString = toString;
    }

    @Override
    public final int hashCode() {
      return hashCode;
    }

    @Override
    public final String toString() {
      return toString;
    }
  }

  @Test
  public void buckets() {
    Object k1 = new Key(42, "k1");
    Object k2 = new Key(42, "k2");
    Object k3 = new Key(42, "k3");
    AVLTree<Object, Object> t = AVLTree.create()
      .put(k1, "v1")
      .put(k2, "v2");

    assertThat(t.toString())
      .as("should create bucket")
      .isEqualTo(" k2->v2 k1->v1");

    AVLTree<Object, Object> t2 = AVLTree.create()
      .put(k2, "v2")
      .put(k1, "v1");
    assertThat(t2.toString())
      .as("toString depends on order of operations")
      .isEqualTo(" k1->v1 k2->v2");

    assertThat(t.equals(t2))
      .as("should compare buckets")
      .isTrue();
    assertThat(t2.equals(t))
      .as("should compare buckets")
      .isTrue();

    assertThat(t.hashCode())
      .isEqualTo(((31 * k1.hashCode()) ^ "v1".hashCode()) + ((31 * k2.hashCode()) ^ "v2".hashCode()));
    assertThat(t2.hashCode())
      .as("hashCode doesn't depend on order of operations")
      .isEqualTo(t.hashCode());

    assertThat(t.get(k1))
      .isEqualTo("v1");
    assertThat(t.get(k2))
      .isEqualTo("v2");
    assertThat(t.get(k3))
      .as("not such key")
      .isNull();

    assertThat(t.put(k2, "new v2").toString())
      .as("should replace head of bucket")
      .isEqualTo(" k2->new v2 k1->v1");
    assertThat(t.put(k1, "new v1").toString())
      .as("should replace element of bucket")
      .isEqualTo(" k1->new v1 k2->v2");
    assertThat(t.put(k1, "v1"))
      .as("should not change")
      .isSameAs(t);
    assertThat(t.put(k2, "v2"))
      .as("should not change")
      .isSameAs(t);
    assertThat(t.put(k3, "v3").toString())
      .as("should add to bucket")
      .isEqualTo(" k3->v3 k2->v2 k1->v1");

    assertThat(t.remove(k2).toString())
      .as("should remove head of bucket")
      .isEqualTo(" k1->v1");
    assertThat(t.remove(k1).toString())
      .as("should remove element of bucket")
      .isEqualTo(" k2->v2");
    assertThat(t.remove(k1).remove(k2).toString())
      .as("should remove bucket")
      .isEqualTo("");
    assertThat(t.remove(k3))
      .as("should not change")
      .isSameAs(t);

    HashMap<Object, Object> biConsumer = new HashMap<>();
    t.forEach((k, v) -> assertThat(biConsumer.put(k, v)).as("unique key-value").isNull());
    assertThat(biConsumer)
      .isEqualTo(ImmutableMap.of(k1, "v1", k2, "v2"));

    HashSet<Object> consumer = new HashSet<>();
    t.forEach(k -> assertThat(consumer.add(k)).as("unique key").isTrue());
    assertThat(consumer)
      .containsOnly(k1, k2);
  }

  @Test
  public void balancing_should_preserve_buckets() {
    Object k1 = new Key(1, "k1");
    Object k2 = new Key(2, "k2");
    Object k3 = new Key(3, "k3");
    Object k4 = new Key(4, "k4");
    AVLTree<Object, Object> t = AVLTree.create()
      .put(k1, "v1")
      .put(k2, "v2")
      .put(k3, "v3");

    Object k1_1 = new Key(1, "k1_1");
    t = t.put(k1_1, "v1_1");

    t = t.put(k4, "v4");
    assertThat(t.height()).as("height after balancing").isEqualTo(3);
    assertThat(t.get(k1_1)).isEqualTo("v1_1");
  }

  /**
   * Subtraction must not be used for comparison of keys due to possibility of integer overflow,
   * this for example will be the case for sequence below, which was generated using random number generator.
   */
  @Test
  public void do_not_use_subtraction_for_comparison_of_keys() {
    Key[] keys = {
      new Key(2043979982, ""),
      new Key(-36348207, ""),
      new Key(-1864559204, ""),
      new Key(-2018458363, ""),
      new Key(-152409201, ""),
      new Key(-1786252453, ""),
      new Key(-1853960690, "")
    };
    AVLTree<Object, Object> t = AVLTree.create();
    for (Key key : keys) {
      t = t.add(key);
    }
    for (Key key : keys) {
      assertThat(t.get(key)).as("found").isNotNull();
      assertThat(t.remove(key)).as("removed").isNotSameAs(t);
    }
  }

  @Test
  public void hashCode_and_equals_should_not_depend_on_order_of_construction() {
    Object o1 = new Key(21, "o1");
    Object o2 = new Key(45, "o2");
    AVLTree<Object, Object> t1 = AVLTree.create().add(o1).add(o2);
    AVLTree<Object, Object> t2 = AVLTree.create().add(o2).add(o1);
    assertThat(t1.key()).as("shape is different").isNotEqualTo(t2.key());

    assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    assertThat(t1).isEqualTo(t2);
    assertThat(t2).isEqualTo(t1);

    Object o3 = new Key(0, "o3");
    AVLTree<Object, Object> t3 = t1.add(o3);
    assertThat(t1.hashCode()).isEqualTo(t3.hashCode());
    assertThat(t1).isNotEqualTo(t3);
    assertThat(t3).isNotEqualTo(t1);
  }

  @Test
  public void test_empty() {
    AVLTree<String, String> t = AVLTree.create();
    assertThat(t).as("singleton").isSameAs(AVLTree.create());
    assertThat(t.get("anything")).isNull();
    assertThat(t.remove("anything")).isSameAs(t);
    assertThat(t.toString()).isEqualTo("");
    assertThat(t.hashCode()).isEqualTo(0);
  }

  @Test
  public void test_one_element() {
    AVLTree<String, String> t0 = AVLTree.create();
    AVLTree<String, String> t1 = t0.put("1", "a");
    AVLTree<String, String> t2 = t0.put("2", "b");

    assertThat(t0).isNotSameAs(t1).isNotSameAs(t2);
    assertThat(t1).isNotSameAs(t2);

    assertThat(t0.get("1")).isNull();
    assertThat(t0.get("2")).isNull();
    assertThat(t0.get("3")).isNull();

    assertThat(t1.get("1")).isEqualTo("a");
    assertThat(t1.get("2")).isNull();
    assertThat(t2.get("3")).isNull();

    assertThat(t2.get("1")).isNull();
    assertThat(t2.get("2")).isEqualTo("b");
    assertThat(t2.get("3")).isNull();
  }

  @Test
  public void test_replace_root() {
    AVLTree<String, String> t0 = AVLTree.create();
    AVLTree<String, String> t1 = t0.put("1", "a");
    AVLTree<String, String> t2 = t1.put("1", "b");

    assertThat(t1).isNotSameAs(t2);
    assertThat(t1.get("1")).isEqualTo("a");
    assertThat(t2.get("1")).isEqualTo("b");
  }

  @Test
  public void no_change() {
    AVLTree<String, String> t0 = AVLTree.create();
    AVLTree<String, String> t1 = t0.put("1", "1");
    assertThat(t1.put("1", "1")).isSameAs(t1);
    assertThat(t1.remove("3")).isSameAs(t1);
    AVLTree<String, String> t2 = t0.put("2", "2");
    assertThat(t2.put("2", "2")).isSameAs(t2);
    assertThat(t2.remove("3")).isSameAs(t2);
  }

  @Test
  public void test() {
    List<Integer> keys = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      keys.add(i);
    }
    Collections.shuffle(keys);

    AVLTree<Integer, Object> t = AVLTree.create();
    for (Integer key : keys) {
      t = t.add(key);
      assertThat(t.add(key)).isSameAs(t);
    }
    assertThat(Counter.countSet(t)).isEqualTo(100);
    assertThat(Counter.countMap(t)).isEqualTo(100);
    assertThat(t.height())
      .isGreaterThanOrEqualTo(8)
      .isLessThanOrEqualTo(10);

    for (Integer key : keys) {
      assertThat(t.contains(key)).isTrue();
      t = t.remove(key);
      assertThat(t.remove(key)).isSameAs(t);
    }
    assertThat(Counter.countSet(t)).isEqualTo(0);
    assertThat(Counter.countMap(t)).isEqualTo(0);
  }

  private static class Counter<K, V> implements BiConsumer<K, V>, Consumer<K> {
    int count;

    public static <K> int countSet(PSet<K> set) {
      Counter<K, K> counter = new Counter<>();
      set.forEach(counter);
      return counter.count;
    }

    public static <K, V> int countMap(PMap<K, V> map) {
      Counter<K, V> counter = new Counter<>();
      map.forEach(counter);
      return counter.count;
    }

    @Override
    public void accept(K key, V value) {
      count++;
    }

    @Override
    public void accept(K k) {
      count++;
    }
  }

}
