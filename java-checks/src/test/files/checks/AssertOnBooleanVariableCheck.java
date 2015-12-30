
public class A {
  boolean removed;
  void foo() {
    List myList;
    assert myList.remove(myList.get(0)); // Noncompliant [[sc=12;ec=40]] {{Move this "assert" side effect to another statement.}}
    assert(myList.remove(myList.get(0))); // Noncompliant
    this.removed = myList.remove(myList.get(0));
    assert this.removed;
    boolean removed = myList.remove(myList.get(0));
    assert removed;
    assert(removed);
    int i = 0;
    assert (removed = i); // Noncompliant
    assert bar() == 1; // Noncompliant
    assert i++; // Noncompliant
  }

  int bar() {
    return 1;
  }
}
