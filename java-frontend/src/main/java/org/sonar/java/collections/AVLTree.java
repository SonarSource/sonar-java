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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * AVL Tree.
 *
 * https://en.wikipedia.org/wiki/AVL_tree
 *
 * @author Evgeny Mandrikov
 */
public abstract class AVLTree<K, V> implements PMap<K, V>, PSet<K> {

  /**
   * @return empty tree
   */
  @SuppressWarnings("unchecked")
  public static <K, V> AVLTree<K, V> create() {
    return EMPTY;
  }

  @SuppressWarnings("unchecked")
  @Override
  public AVLTree<K, V> add(K e) {
    return put(e, e, this);
  }

  @Override
  public boolean contains(K k) {
    return get(k) != null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public AVLTree<K, V> put(K key, V value) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(value);
    return put(key, value, this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public AVLTree<K, V> remove(K key) {
    Preconditions.checkNotNull(key);
    return remove(key, this);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public V get(K key) {
    Preconditions.checkNotNull(key);
    AVLTree t = this;
    while (!t.isEmpty()) {
      int c = KEY_COMPARATOR.compare(key, t.key());
      if (c == 0) {
        return (V) t.value();
      } else if (c < 0) {
        t = t.left();
      } else {
        t = t.right();
      }
    }
    return null;
  }

  @Override
  public void forEach(PSet.Consumer<K> action) {
    forEach(this, action);
  }

  @SuppressWarnings("unchecked")
  private void forEach(AVLTree t, PSet.Consumer<K> action) {
    if (t.isEmpty()) {
      return;
    }
    forEach(t.left(), action);
    action.accept((K) t.key());
    forEach(t.right(), action);
  }

  @Override
  public void forEach(PMap.Consumer<K, V> consumer) {
    forEach(this, consumer);
  }

  @SuppressWarnings("unchecked")
  private void forEach(AVLTree t, PMap.Consumer<K, V> action) {
    if (t.isEmpty()) {
      return;
    }
    forEach(t.left(), action);
    action.accept((K) t.key(), (V) t.value());
    forEach(t.right(), action);
  }

  @Override
  public Iterator<Map.Entry<K, V>> entriesIterator() {
    return new NodeIterator(this);
  }

  private class NodeIterator implements Iterator<Map.Entry<K, V>> {
    private Deque<AVLTree> stack = new ArrayDeque<>();

    public NodeIterator(AVLTree<K, V> node) {
      descendLeft(node);
    }

    @Override
    public boolean hasNext() {
      return !stack.isEmpty();
    }

    private void descendLeft(AVLTree node) {
      if (node != EMPTY) {
        stack.addLast(node);
        descendLeft(node.left());
      }
    }

    @Override
    public Map.Entry<K, V> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      AVLTree node = stack.removeLast();
      if (node.right() != EMPTY) {
        descendLeft(node.right());
      }
      return new AbstractMap.SimpleImmutableEntry<>((K) node.key(), (V) node.value());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public interface Visitor<K, V> {
    void visit(K key, V value);
  }

  protected abstract AVLTree left();

  protected abstract AVLTree right();

  protected abstract Object key();

  protected abstract Object value();

  protected abstract int height();

  private static AVLTree put(Object key, Object value, AVLTree t) {
    if (t.isEmpty()) {
      return createNode(t, key, value, t);
    }
    int result = KEY_COMPARATOR.compare(key, t.key());
    if (result == 0) {
      if (value.equals(t.value())) {
        return t;
      }
      return createNode(t.left(), key, value, t.right());
    } else if (result < 0) {
      AVLTree left = put(key, value, t.left());
      if (left == t.left()) {
        return t;
      }
      return balance(left, t.key(), t.value(), t.right());
    } else {
      AVLTree right = put(key, value, t.right());
      if (right == t.right()) {
        return t;
      }
      return balance(t.left(), t.key(), t.value(), right);
    }
  }

  private static AVLTree remove(Object key, AVLTree t) {
    if (t.isEmpty()) {
      return t;
    }
    int result = KEY_COMPARATOR.compare(key, t.key());
    if (result == 0) {
      return combineTrees(t.left(), t.right());
    } else if (result < 0) {
      AVLTree left = remove(key, t.left());
      if (left == t.left()) {
        return t;
      }
      return balance(left, t.key(), t.value(), t.right());
    } else {
      AVLTree right = remove(key, t.right());
      if (right == t.right()) {
        return t;
      }
      return balance(t.left(), t.key(), t.value(), right);
    }
  }

  private static AVLTree combineTrees(AVLTree l, AVLTree r) {
    if (l.isEmpty()) {
      return r;
    }
    if (r.isEmpty()) {
      return l;
    }
    NodeRef oldNode = new NodeRef();
    AVLTree newRight = removeMinBinding(r, oldNode);
    return balance(l, oldNode.node.key(), oldNode.node.value(), newRight);
  }

  private static class NodeRef {
    AVLTree node;
  }

  private static AVLTree removeMinBinding(AVLTree t, NodeRef noderemoved) {
    assert !t.isEmpty();
    if (t.left().isEmpty()) {
      noderemoved.node = t;
      return t.right();
    }
    return balance(removeMinBinding(t.left(), noderemoved), t.key(), t.value(), t.right());
  }

  private static AVLTree balance(AVLTree l, Object key, Object value, AVLTree r) {
    if (l.height() > r.height() + 2) {
      assert !l.isEmpty();
      AVLTree ll = l.left();
      AVLTree lr = l.right();
      if (ll.height() >= lr.height()) {
        return createNode(ll, l, createNode(lr, key, value, r));
      }
      assert !lr.isEmpty();
      AVLTree lrl = lr.left();
      AVLTree lrr = lr.right();
      return createNode(createNode(ll, l, lrl), lr, createNode(lrr, key, value, r));
    }
    if (r.height() > l.height() + 2) {
      assert !r.isEmpty();
      AVLTree rl = r.left();
      AVLTree rr = r.right();
      if (rr.height() >= rl.height()) {
        return createNode(createNode(l, key, value, rl), r, rr);
      }
      assert !rl.isEmpty();
      AVLTree rll = rl.left();
      AVLTree rlr = rl.right();
      return createNode(createNode(l, key, value, rll), rl, createNode(rlr, r, rr));
    }
    return createNode(l, key, value, r);
  }

  private static AVLTree createNode(AVLTree newLeft, AVLTree oldTree, AVLTree newRight) {
    return createNode(newLeft, oldTree.key(), oldTree.value(), newRight);
  }

  private static AVLTree createNode(AVLTree l, Object key, Object value, AVLTree r) {
    return new Node(l, r, key, value, incrementHeight(l, r));
  }

  private static int incrementHeight(AVLTree l, AVLTree r) {
    return (l.height() > r.height() ? l.height() : r.height()) + 1;
  }

  private static class Node extends AVLTree {
    private final AVLTree left;
    private final AVLTree right;
    private final int height;

    private final Object key;
    private final Object value;
    private int hashCode;

    public Node(AVLTree left, AVLTree right, Object key, Object value, int height) {
      this.left = left;
      this.right = right;
      this.key = key;
      this.value = value;
      this.height = height;
    }

    @Override
    protected AVLTree left() {
      return left;
    }

    @Override
    protected AVLTree right() {
      return right;
    }

    @Override
    protected Object key() {
      return key;
    }

    @Override
    protected Object value() {
      return value;
    }

    @Override
    protected int height() {
      return height;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    private static class Hasher implements PMap.Consumer<Object, Object> {
      int result = 0;

      @Override
      public void accept(Object key, Object value) {
        // the key is multiplied by 31 to avoid K ^ V == 0 when K and V are the same element
        result += (31 * key.hashCode()) ^ value.hashCode();
      }
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        Hasher hasher = new Hasher();
        forEach(hasher);
        hashCode = hasher.result;
      }
      return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof Node) {
        Node other = (Node) obj;
        int c = 0;
        for (Iterator<Map.Entry> iter = entriesIterator(); iter.hasNext();) {
          Map.Entry next = iter.next();
          Object otherValue = other.get(next.getKey());
          if (otherValue == null || !Objects.equals(next.getValue(), otherValue)) {
            return false;
          }
          c++;
        }
        return c == Iterators.size(other.entriesIterator());
      }
      return false;
    }

    @Override
    public String toString() {
      return left.toString() + " " + key + "->" + value + right.toString();
    }
  }

  private static final AVLTree EMPTY = new AVLTree() {
    @Override
    protected AVLTree left() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected AVLTree right() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected Object key() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected Object value() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected int height() {
      return 0;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof AVLTree) && ((AVLTree) obj).isEmpty();
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public String toString() {
      return "";
    }
  };

  /**
   * Not total ordering, but stable.
   */
  private static final Comparator KEY_COMPARATOR = new Comparator() {
    @Override
    public int compare(Object o1, Object o2) {
      int h1 = o1.hashCode();
      int h2 = o2.hashCode();
      if (h1 == h2) {
        return o1.equals(o2) ? 0 : 1;
      }
      return h1 - h2;
    }
  };
}
