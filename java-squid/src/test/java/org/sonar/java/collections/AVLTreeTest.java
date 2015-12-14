/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.Iterators;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class AVLTreeTest {

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
    assertThat(t.entriesIterator()).hasSize(100);
    assertThat(t.height())
      .isGreaterThanOrEqualTo(8)
      .isLessThanOrEqualTo(10);
    AVLTree<Integer, Object> t1 = t;
    t = t.remove(45);
    t = t.remove(21);
    t = t.add(21);
    t = t.add(45);
    assertThat(t).isEqualTo(t1);
    assertThat(t.hashCode()).isEqualTo(t1.hashCode());

    for (Integer key : keys) {
      assertThat(t.contains(key)).isTrue();
      t = t.remove(key);
      assertThat(t.remove(key)).isSameAs(t);
    }
    assertThat(Counter.countSet(t)).isEqualTo(0);
    assertThat(Counter.countMap(t)).isEqualTo(0);
  }

  @Test
  public void hashCode_equals_test() {
    AVLTree<Integer, Object> t = AVLTree.create();
    t = t.add(1);
    t = t.add(2);
    AVLTree<Integer, Object> t2 = AVLTree.create();
    t2 = t2.add(2);
    t2 = t2.add(1);
    assertThat(t).isEqualTo(t2);
    assertThat(t.hashCode()).isEqualTo(t2.hashCode());
    assertThat(t.entriesIterator()).hasSize(2);
    assertTrue(Iterators.elementsEqual(t.entriesIterator(), t2.entriesIterator()));
    assertThat(t.entriesIterator()).containsOnly(new AbstractMap.SimpleImmutableEntry(1, 1), new AbstractMap.SimpleImmutableEntry(2, 2));
    t2 = t2.add(3);
    assertThat(t.hashCode()).isNotEqualTo(t2.hashCode());
    assertThat(t).isNotEqualTo(t2);
    assertThat(t2.entriesIterator()).hasSize(3);
    t = t.put(3, 33);
    assertThat(t).isNotEqualTo(t2);
    t = t.add(3);
    assertThat(t).isEqualTo(t2);
    t = t.add(4);
    assertThat(t).isNotEqualTo(t2);
    assertThat(t.entriesIterator()).hasSize(4);
    assertThat(t).isEqualTo(t);
    assertThat(t).isNotEqualTo(new Object());
  }

  @Test
  public void test_to_string() {
    AVLTree<Integer, Object> t = AVLTree.create();
    t = t.add(1);
    t = t.add(2);
    assertThat(t.toString()).isEqualTo(" 1->1 2->2");
  }

  @Test
  public void test_empty_iterator() {
    AVLTree<Integer, Object> t = AVLTree.create();
    Iterator<Map.Entry<Integer, Object>> iterator = t.entriesIterator();
    assertThat(iterator).isEmpty();
  }

  @Test(expected = NoSuchElementException.class)
  public void iterator_no_such_element_exception() {
    AVLTree<Integer, Object> t = AVLTree.create();
    t = t.add(1);
    Iterator<Map.Entry<Integer, Object>> iterator = t.entriesIterator();
    iterator.next();
    iterator.next();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void iterator_unsupported_remove() {
    AVLTree<Integer, Object> t = AVLTree.create();
    t = t.add(1);
    Iterator<Map.Entry<Integer, Object>> iterator = t.entriesIterator();
    iterator.remove();
  }

  private static class Counter<K, V> implements PMap.Consumer<K, V>, PSet.Consumer<K> {
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
