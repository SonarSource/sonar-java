import java.util.ArrayList;
import java.util.List;

class A {
  void fun() {
    List<String> strings = new ArrayList<String>();
    strings.add("Hello");
    strings.add(strings); // Noncompliant; StackOverflowException if strings.hashCode() called
    strings.addAll(strings); // Noncompliant; behavior undefined
    strings.containsAll(strings); // Noncompliant; always true
    strings.removeAll(strings); // Noncompliant; confusing. Use clear() instead
    strings.retainAll(strings); // Noncompliant; NOOP
    strings.wait();
    strings.foo();
  }
}