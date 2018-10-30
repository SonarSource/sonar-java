class A {}
class B extends A {}
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
  class E {}
  static java.util.Collection<E> getEs() {
    return java.util.Collections.singleton(new E());
  }
}
public class CheckForLoop {
  void doStuff() {
    java.util.Collection unparameterized = java.util.Collections.emptySet();
    for (Object o: unparameterized) {}

    java.util.List<B> listOfB = java.util.Collections.singletonList(new B());
    for (B b: listOfB) {}
    for (A a: listOfB) {} // Noncompliant [[sc=10;ec=11;secondary=27]] {{Change "A" to the type handled by the Collection.}}
    for (A a: listOfB) {} // Noncompliant
    for (Object o: listOfB) {} // Noncompliant {{Change "Object" to the type handled by the Collection.}}

    for (B b: C.getBs()) {}
    for (A b: C.getBs()) {} // Noncompliant {{Change "A" to the type handled by the Collection.}}

    for (B b: C.getExtendedBs()) {}
    for (A a: C.getExtendedBs()) {} // Noncompliant
    for (A a: C.getExtendedAs()) {}

    for (B b: java.util.Collections.singletonList(new B())) {}
    for (A a: java.util.Collections.singletonList(new B())) {} // Noncompliant {{Change "A" to the type handled by the Collection.}}

    for (D.E e: D.getEs()) {}
    for (Object o; listOfB.hasNext(); o = listOfB.next()) {}
    while (listOfB.hasNext()) { listOfB.next(); }
  }
}
