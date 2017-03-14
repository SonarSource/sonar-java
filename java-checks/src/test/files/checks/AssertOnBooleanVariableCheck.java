import java.util.Collections;
import java.util.List;

public class A {
  void foo(A a, boolean b, List<Object> myList) {
    assert myList.remove(myList.get(0)); // Noncompliant [[sc=19;ec=25]] {{Move this "assert" side effect to another statement.}}
    assert myList.remove(myList.remove(1)); // Noncompliant [[sc=19;ec=25]] {{Move this "assert" side effect to another statement.}}

    assert myList.add(new Object()); // Noncompliant
    assert myList.retainAll(Collections.singleton(new Object())); // Noncompliant
    assert deleteStuff(); // Noncompliant
    assert stuffToRemove(); // Compliant - does not start with 'remove'
    assert updateIfValid(false); // Noncompliant
    assert setValue(); // Noncompliant

    assert bar() > 1 ? doNothing() : deleteStuff(); // Noncompliant [[sc=38;ec=49]]

    assert bar() == 0; // Compliant
    assert new A() { // Compliant
      @Override
      boolean deleteStuff() {
        // do nothing
        return false;
      }
    }.bar() != 14;

    boolean removed = myList.remove(myList.get(0));
    assert removed;

    int i = 0;
    assert (removed = b); // Compliant - no call to side-effect method
    assert i++ == 14; // Compliant
  }

  int bar() {
    return 1;
  }

  boolean setValue() { return false; }
  boolean doNothing() { return false; }
  boolean deleteStuff() { return false; }
  boolean stuffToRemove() { return false; }
  boolean updateIfValid(boolean b) { return false; }
}
