package org.sonar.java.ecj;

import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @see org.sonar.java.ast.parser.ListTreeImpl
 */
@MethodsAreNonnullByDefault
@ParametersAreNonnullByDefault
class EList<T extends Tree> extends ETree implements ListTree<T> {
  List<T> elements = new ArrayList<>();

  @Override
  public List<SyntaxToken> separators() {
    throw new NotImplementedException();
  }

  @Override
  public int size() {
    return elements.size();
  }

  @Override
  public boolean isEmpty() {
    return elements.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return elements.contains(o);
  }

  @Override
  public Iterator<T> iterator() {
    return elements.iterator();
  }

  @Override
  public Object[] toArray() {
    throw new UnexpectedAccessException();
  }

  @Override
  public <T1> T1[] toArray(T1[] a) {
    throw new UnexpectedAccessException();
  }

  @Deprecated
  @Override
  public boolean add(T t) {
    throw new UnexpectedAccessException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnexpectedAccessException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnexpectedAccessException();
  }

  @Deprecated
  @Override
  public boolean addAll(Collection<? extends T> c) {
    throw new UnexpectedAccessException();
  }

  @Deprecated
  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    throw new UnexpectedAccessException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnexpectedAccessException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnexpectedAccessException();
  }

  @Override
  public void clear() {
    throw new UnexpectedAccessException();
  }

  @Override
  public T get(int index) {
    return elements.get(index);
  }

  @Override
  public T set(int index, T element) {
    throw new UnexpectedAccessException();
  }

  @Override
  public void add(int index, T element) {
    throw new UnexpectedAccessException();
  }

  @Override
  public T remove(int index) {
    throw new UnexpectedAccessException();
  }

  @Override
  public int indexOf(Object o) {
    return elements.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    throw new UnexpectedAccessException();
  }

  @Override
  public ListIterator<T> listIterator() {
    return elements.listIterator();
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    return elements.listIterator(index);
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return elements.subList(fromIndex, toIndex);
  }

  @Override
  public void accept(TreeVisitor visitor) {
    for (T element : elements) {
      element.accept(visitor);
    }
  }

  @Override
  public Kind kind() {
    return Kind.LIST;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return elements.iterator();
  }
}
