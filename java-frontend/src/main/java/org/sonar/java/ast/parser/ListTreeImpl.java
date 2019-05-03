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
package org.sonar.java.ast.parser;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.sslr.grammar.GrammarRuleKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public abstract class ListTreeImpl<T extends Tree> extends JavaTree implements ListTree<T> {

  private final List<T> list;
  private final List<SyntaxToken> separators;

  public ListTreeImpl(GrammarRuleKey grammarRuleKey, List<T> list) {
    super(grammarRuleKey);
    this.list = list;
    this.separators = new ArrayList<>();
  }

  public ListTreeImpl(GrammarRuleKey grammarRuleKey, List<T> list, List<SyntaxToken> separators) {
    super(grammarRuleKey);
    this.list = list;
    this.separators = separators;
  }

  @Override
  public List<SyntaxToken> separators() {
    return separators;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    for (T t : list) {
      t.accept(visitor);
    }
  }

  @Override
  public Kind kind() {
    return Kind.LIST;
  }

  @Override
  public Iterable<Tree> children() {
    return new InterleaveIterable(list, separators);
  }

  private class InterleaveIterable implements Iterable<Tree> {

    private final ImmutableList<Iterator<? extends Tree>> iterators;

    public InterleaveIterable(List<T> list, List<SyntaxToken> separators) {
      iterators = ImmutableList.of(list.iterator(), separators.iterator());
    }

    @Override
    public Iterator<Tree> iterator() {
      return new InterleaveIterator<>(iterators);
    }
  }

  private static class InterleaveIterator<E> extends AbstractIterator<E> {

    private final LinkedList<Iterator<? extends E>> iterables;

    public InterleaveIterator(List<Iterator<? extends E>> iterables) {
      super();
      this.iterables = new LinkedList<>(iterables);
    }

    @Override
    protected E computeNext() {
      while (!iterables.isEmpty()) {
        Iterator<? extends E> topIter = iterables.poll();
        if (topIter.hasNext()) {
          E result = topIter.next();
          iterables.offer(topIter);
          return result;
        }
      }
      return endOfData();
    }
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return list.contains(o);
  }

  @Override
  public Iterator<T> iterator() {
    return list.iterator();
  }

  @Override
  public Object[] toArray() {
    return list.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return list.toArray(a);
  }

  @Override
  public boolean add(T e) {
    return list.add(e);
  }

  @Override
  public boolean remove(Object o) {
    return list.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return list.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    return list.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    return list.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return list.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return list.retainAll(c);
  }

  @Override
  public void clear() {
    list.clear();
  }

  @Override
  public T get(int index) {
    return list.get(index);
  }

  @Override
  public T set(int index, T element) {
    return list.set(index, element);
  }

  @Override
  public void add(int index, T element) {
    list.add(index, element);
  }

  @Override
  public T remove(int index) {
    return list.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return list.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return list.lastIndexOf(o);
  }

  @Override
  public ListIterator<T> listIterator() {
    return list.listIterator();
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    return list.listIterator(index);
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return list.subList(fromIndex, toIndex);
  }

}
