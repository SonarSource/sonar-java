package checks;

import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

class ForLoopVariableTypeCheck {
  static class A {
  }

  static class B extends A {
  }

  class C {
    static java.util.Collection<B> getBs() {
      return java.util.Collections.singleton(new B());
    }

    static java.util.Collection<? extends B> getExtendedBs() {
      return java.util.Collections.singleton(new B());
    }

    static java.util.Collection<? extends A> getExtendedAs() {
      return java.util.Collections.singleton(new B());
    }
  }

  class D {
    static class E {
    }

    static java.util.Collection<E> getEs() {
      return java.util.Collections.singleton(new E());
    }
  }

  public class CheckForLoop {
    void doStuff() {
      java.util.Collection unparameterized = java.util.Collections.emptySet();
      for (Object o : unparameterized) {
      }

      java.util.List<B> listOfB = java.util.Collections.singletonList(new B());
      for (B b : listOfB) {
      }
      for (A a : listOfB) {
      }
      for (A a : listOfB) { // Noncompliant [[sc=12;ec=13;secondary=+0]] {{Change "A" to the type handled by the Collection.}}
        B b = (B) a;
      }
      for (Object o : listOfB) {
      }
      for (Object o : listOfB) { // Noncompliant {{Change "Object" to the type handled by the Collection.}}
        B b = (B) o;
      }

      for (B b : C.getBs()) {
      }
      for (A b : C.getBs()) {
      }
      for (A a : C.getBs()) { // Noncompliant {{Change "A" to the type handled by the Collection.}}
        B b = (B) a;
      }

      for (B b : C.getExtendedBs()) {
      }
      for (A a : C.getExtendedBs()) {
      }
      for (A a : C.getExtendedBs()) { // Noncompliant {{Change "A" to the type handled by the Collection.}}
        B b = (B) a;
      }
      for (A a : C.getExtendedAs()) {
      }

      for (B b : java.util.Collections.singletonList(new B())) {
      }
      for (A a : java.util.Collections.singletonList(new B())) {
      }
      for (A a : java.util.Collections.singletonList(new B())) { // Noncompliant {{Change "A" to the type handled by the Collection.}}
        B b = (B) a;
      }

      for (D.E e : D.getEs()) {
      }
      for (Iterator<B> iterator = listOfB.iterator(); iterator.hasNext(); iterator.next()) {
      }
      Iterator<B> iterator = listOfB.iterator();
      while (iterator.hasNext()) {
        iterator.next();
      }

      java.util.Set<java.util.Set<B>> setOfSetOfB = java.util.Collections.emptySet();
      for (java.util.Set<B> s : setOfSetOfB) {
      }
      for (java.util.Set s : setOfSetOfB) {
      }
      for (Object s : setOfSetOfB) {
      }
      for (Object s : setOfSetOfB) { // Noncompliant
        B b = (B) s;
      }

      java.util.Map t = new java.util.HashMap();
      for (java.util.Map.Entry e : ((java.util.Map<?, ?>) t).entrySet()) {
      }

      java.util.List l = null;
      for (Object o : l) {
        B b = (B) o;
      }

      class Foo implements java.util.Collection<String> {
        @Override
        public int size() {
          return 0;
        }

        @Override
        public boolean isEmpty() {
          return false;
        }

        @Override
        public boolean contains(Object o) {
          return false;
        }

        @NotNull
        @Override
        public Iterator<String> iterator() {
          return null;
        }

        @Override
        public void forEach(Consumer<? super String> action) {
          Collection.super.forEach(action);
        }

        @NotNull
        @Override
        public Object[] toArray() {
          return new Object[0];
        }

        @Override
        public <String> String[] toArray(IntFunction<String[]> generator) {
          return Collection.super.toArray(generator);
        }

        @NotNull
        @Override
        public <String> String[] toArray(@NotNull String[] strings) {
          return null;
        }

        @Override
        public boolean add(String s) {
          return false;
        }

        @Override
        public boolean remove(Object o) {
          return false;
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> collection) {
          return false;
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends String> collection) {
          return false;
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> collection) {
          return false;
        }

        @Override
        public boolean removeIf(Predicate<? super String> filter) {
          return Collection.super.removeIf(filter);
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> collection) {
          return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public Spliterator<String> spliterator() {
          return Collection.super.spliterator();
        }

        @Override
        public Stream<String> stream() {
          return Collection.super.stream();
        }

        @Override
        public Stream<String> parallelStream() {
          return Collection.super.parallelStream();
        }
      }
      for (Object o : new Foo()) { // Compliant: ignoring raw subtypes of j.u.Collection
        String s = (String) o;
      }

      int[] arrayOfInt = new int[0];
      for (Object e : arrayOfInt) { // Noncompliant
        int i = (int) e;
      }
      for (Object o : arrayOfInt) {
      }
      for (int i : arrayOfInt) {
      }
    }
  }

  class MyMap<K, V> extends java.util.AbstractMap<K, V> {
    @Override
    public void putAll(java.util.Map<? extends K, ? extends V> m) {
      for (java.util.Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
        put(e.getKey(), e.getValue());
      }
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
      return null;
    }
  }

  class Wildcard {
    public void method(java.util.Collection<?> c) {
      for (Object o : c) { // Compliant
        java.util.Map.Entry<?, ?> entry = (java.util.Map.Entry<?, ?>) o;
      }
    }
  }

  class I {
  }

  class J extends I {
  }

  class K extends J {
  }

  class L extends I {
  }

  class Test {
    java.util.Collection<K> collectionOfK;
    java.util.Collection<J> collectionOfJ;
    java.util.Collection<I> collectionOfI;
    java.util.Map<String, java.util.Set<K>> multiMapOfK;
    Object other;

    void doStuff() {
      for (K k : collectionOfK) {
      }
      for (J k : collectionOfK) {
      }
      for (I i : collectionOfK) {
      }
      for (J j : collectionOfK) { // Noncompliant
        K k = (K) j;
      }
      for (J j : collectionOfK) {
        K k = (K) other;
      }
      for (I i : collectionOfK) { // Noncompliant
        J j = (J) i;
        K k = (K) i;
      }
      for (I i : collectionOfK) { // Noncompliant
        L l = (L) i;
      }
      for (I i : collectionOfK) { // Noncompliant
        B b = (B) foo((L) i);
      }
      for (I i : collectionOfK) { // Noncompliant
        I i2 = (K) i;
      }
      for (J j : collectionOfJ) {
        K k = (K) j;
      }
      for (I i : collectionOfJ) { // Noncompliant
        J j = (J) i;
      }
      for (J j : collectionOfJ) {
        if (j instanceof K) {
          K k = (K) j;
        }
      }
      for (I i : collectionOfI) {
        if (i instanceof K) {
          K k = (K) i;
        }
      }
    }
  }

  private Object foo(L i) {
    return null;
  }
}
