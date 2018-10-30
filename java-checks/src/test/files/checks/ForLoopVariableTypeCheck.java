class A {}
class B extends A {}
class C {
  static java.util.Collection<B> getBs() {
    return java.util.Collections.singleton(new B());
  }
}
public class CheckForLoop {
  void doStuff() {
    java.util.List<B> listOfB = java.util.Collections.singletonList(new B());
    java.util.Collection unparameterized = java.util.Collections.emptySet();
    for (Object o: unparameterized) {}
    for (B b: listOfB) {}
    for (B b: C.getBs()) {}
    for (B b: java.util.Collections.singletonList(new B())) {}
    for (A a: listOfB) {} // Noncompliant {{Change "A" by the type handled by the Collection.}}
    for (A b: C.getBs()) {} // Noncompliant {{Change "A" by the type handled by the Collection.}}
    for (A a: listOfB) {} // Noncompliant
    for (Object o: listOfB) {} // Noncompliant {{Change "Object" by the type handled by the Collection.}}
    for (Object o: java.util.Collections.singletonList(new B())) {} // Noncompliant {{Change "Object" by the type handled by the Collection.}}
    for (Object o; listOfB.hasNext(); o = listOfB.next()) {}
    while (listOfB.hasNext()) { listOfB.next(); }
  }
}
