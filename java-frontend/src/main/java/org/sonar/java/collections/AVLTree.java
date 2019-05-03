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

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * AVL Tree.
 *
 * https://en.wikipedia.org/wiki/AVL_tree
 *
 * @author Evgeny Mandrikov
 */
abstract class AVLTree<K, V> implements PMap<K, V>, PSet<K> {

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
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    return put(key, value, this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public AVLTree<K, V> remove(K key) {
    Objects.requireNonNull(key);
    return remove(key, this);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public V get(K key) {
    Objects.requireNonNull(key);
    final int h = key.hashCode();
    AVLTree t = this;
    while (!t.isEmpty()) {
      final int c = t.key().hashCode();
      if (h == c) {
        t = searchInBucket(key, t);
        return t == null ? null : (V) t.value();
      } else if (h < c) {
        t = t.left();
      } else {
        t = t.right();
      }
    }
    return null;
  }

  @Override
  public void forEach(Consumer<K> action) {
    forEach(this, action);
  }

  @SuppressWarnings("unchecked")
  private void forEach(AVLTree t, Consumer<K> action) {
    if (t.isEmpty()) {
      return;
    }
    forEach(t.left(), action);
    forEach(t.right(), action);
    while (t != null) {
      action.accept((K) t.key());
      t = t.nextInBucket();
    }
  }

  @Override
  public void forEach(BiConsumer<K, V> consumer) {
    forEach(this, consumer);
  }

  @SuppressWarnings("unchecked")
  private void forEach(AVLTree t, BiConsumer<K, V> action) {
    if (t.isEmpty()) {
      return;
    }
    forEach(t.left(), action);
    forEach(t.right(), action);
    while (t != null) {
      action.accept((K) t.key(), (V) t.value());
      t = t.nextInBucket();
    }
  }

  protected abstract AVLTree left();

  protected abstract AVLTree right();

  @Nullable
  protected abstract AVLTree nextInBucket();

  protected abstract Object key();

  protected abstract Object value();

  protected abstract int height();

  private static AVLTree put(Object key, Object value, AVLTree t) {
    if (t.isEmpty()) {
      return createNode(t, key, value, null, t);
    }
    final int h = key.hashCode();
    final int c = t.key().hashCode();
    if (h == c) {
      final AVLTree nextInBucket = t.nextInBucket();
      if (key.equals(t.key())) {
        if (value.equals(t.value())) {
          return t;
        }
        return createNode(t.left(), key, value, nextInBucket, t.right());
      }
      final AVLTree nodeToReplace = searchInBucket(key, nextInBucket);
      if (nodeToReplace != null && value.equals(nodeToReplace.value())) {
        return t;
      }
      return createNode(t.left(), key, value, createBucket(t.key(), t.value(), removeFromBucket(nextInBucket, nodeToReplace)), t.right());
    } else if (h < c) {
      AVLTree left = put(key, value, t.left());
      if (left == t.left()) {
        return t;
      }
      return balance(left, t, t.right());
    } else {
      AVLTree right = put(key, value, t.right());
      if (right == t.right()) {
        return t;
      }
      return balance(t.left(), t, right);
    }
  }

  private static AVLTree remove(Object key, AVLTree t) {
    if (t.isEmpty()) {
      return t;
    }
    final int h = key.hashCode();
    final int c = t.key().hashCode();
    if (h == c) {
      final AVLTree nextInBucket = t.nextInBucket();
      if (key.equals(t.key())) {
        if (nextInBucket != null) {
          return createNode(t.left(), nextInBucket.key(), nextInBucket.value(), nextInBucket.nextInBucket(), t.right());
        }
        return combineTrees(t.left(), t.right());
      }
      final AVLTree nodeToRemove = searchInBucket(key, nextInBucket);
      if (nodeToRemove == null) {
        return t;
      }
      return createNode(t.left(), t.key(), t.value(), removeFromBucket(nextInBucket, nodeToRemove), t.right());
    } else if (h < c) {
      AVLTree left = remove(key, t.left());
      if (left == t.left()) {
        return t;
      }
      return balance(left, t, t.right());
    } else {
      AVLTree right = remove(key, t.right());
      if (right == t.right()) {
        return t;
      }
      return balance(t.left(), t, right);
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
    return balance(l, oldNode.node, newRight);
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
    return balance(removeMinBinding(t.left(), noderemoved), t, t.right());
  }

  private static AVLTree balance(AVLTree l, AVLTree oldNode, AVLTree r) {
    if (l.height() > r.height() + 2) {
      assert !l.isEmpty();
      AVLTree ll = l.left();
      AVLTree lr = l.right();
      if (ll.height() >= lr.height()) {
        return createNode(ll, l, createNode(lr, oldNode, r));
      }
      assert !lr.isEmpty();
      AVLTree lrl = lr.left();
      AVLTree lrr = lr.right();
      return createNode(createNode(ll, l, lrl), lr, createNode(lrr, oldNode, r));
    }
    if (r.height() > l.height() + 2) {
      assert !r.isEmpty();
      AVLTree rl = r.left();
      AVLTree rr = r.right();
      if (rr.height() >= rl.height()) {
        return createNode(createNode(l, oldNode, rl), r, rr);
      }
      assert !rl.isEmpty();
      AVLTree rll = rl.left();
      AVLTree rlr = rl.right();
      return createNode(createNode(l, oldNode, rll), rl, createNode(rlr, r, rr));
    }
    return createNode(l, oldNode, r);
  }

  private static AVLTree createNode(AVLTree newLeft, AVLTree oldTree, AVLTree newRight) {
    return new Node(newLeft, newRight, oldTree.key(), oldTree.value(), oldTree.nextInBucket(), incrementHeight(newLeft, newRight));
  }

  private static Node createNode(AVLTree l, Object key, Object value, @Nullable AVLTree nextInBucket, AVLTree r) {
    return new Node(l, r, key, value, nextInBucket, incrementHeight(l, r));
  }

  private static int incrementHeight(AVLTree l, AVLTree r) {
    return (l.height() > r.height() ? l.height() : r.height()) + 1;
  }

  /**
   * @return node from the given bucket, which contains given key, null if not found
   */
  @Nullable
  private static AVLTree searchInBucket(Object key, @Nullable AVLTree bucketStart) {
    AVLTree n = bucketStart;
    while (n != null) {
      if (key.equals(n.key())) {
        return n;
      }
      n = n.nextInBucket();
    }
    return null;
  }

  /**
   * Given "b" as a node to remove and bucket "a -> b -> c" result will be "c -> a".
   *
   * @return null if bucket is null
   */
  @Nullable
  private static AVLTree removeFromBucket(@Nullable AVLTree bucketStart, @Nullable AVLTree nodeToRemove) {
    AVLTree c = bucketStart;
    AVLTree result = null;
    while (c != null) {
      if (/* not the instance to remove: */ c != nodeToRemove) {
        result = createBucket(c.key(), c.value(), result);
      }
      c = c.nextInBucket();
    }
    return result;
  }

  private static Node createBucket(Object key, Object value, @Nullable AVLTree bucket) {
    return new Node(AVLTree.EMPTY, AVLTree.EMPTY, key, value, bucket, 0);
  }

  @VisibleForTesting
  static class Node extends AVLTree {
    private final AVLTree left;
    private final AVLTree right;
    private final int height;

    private final Object key;
    private final Object value;
    @Nullable
    private final AVLTree nextInBucket;

    private int hashCode;

    public Node(AVLTree left, AVLTree right, Object key, Object value, @Nullable AVLTree nextInBucket, int height) {
      this.left = left;
      this.right = right;
      this.key = key;
      this.value = value;
      this.nextInBucket = nextInBucket;
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
    protected AVLTree nextInBucket() {
      return nextInBucket;
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

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        // the key is multiplied by 31 to avoid K ^ V == 0 when K == V in case of set
        this.hashCode = left.hashCode() + ((31 * key.hashCode()) ^ value.hashCode()) + right.hashCode() + (nextInBucket == null ? 0 : nextInBucket.hashCode());
      }
      return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof Node) {
        final Node other = (Node) obj;
        return this.hashCode() == other.hashCode()
          && Equals.compute(this, other);
      }
      return false;
    }

    @Override
    public String toString() {
      return left.toString() + " " + key + "->" + value + (nextInBucket == null ? "" : nextInBucket.toString()) + right.toString();
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
    protected AVLTree nextInBucket() {
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

  private static class Equals implements BiConsumer {
    private final AVLTree tree;
    private int sizeDifference;

    public static boolean compute(AVLTree first, AVLTree second) {
      Equals state = new Equals(first);
      if (!state.supersetOf(second)) {
        return false;
      }
      first.forEach(state);
      return state.sizeDifference == 0;
    }

    private Equals(AVLTree tree) {
      this.tree = tree;
    }

    /**
     * @return true if {@link #tree} is superset of given tree
     */
    private boolean supersetOf(@Nullable AVLTree node) {
      if (node == null || node.isEmpty()) {
        return true;
      }
      sizeDifference++;
      return Objects.equals(node.value(), tree.get(node.key()))
        && supersetOf(node.nextInBucket())
        && supersetOf(node.left())
        && supersetOf(node.right());
    }

    @Override
    public void accept(Object key, Object value) {
      sizeDifference--;
    }
  }

}
