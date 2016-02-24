import java.util.ArrayList;
import java.util.List;

class A {
  void fun() {
    List<String> strings = new ArrayList<String>();
    strings.add("Hello");
    strings.add(strings); // Compliant - does not compile, as there is no method "add(List<String>)" in List. The method invocation is not resolved
    strings.addAll(strings); // Noncompliant {{Remove or correct this "addAll" call.}}
    strings.containsAll(strings); // Noncompliant {{Remove or correct this "containsAll" call.}}
    strings.removeAll(strings); // Noncompliant {{Remove or correct this "removeAll" call.}}
    strings.retainAll(strings); // Noncompliant [[sc=5;ec=31]] {{Remove or correct this "retainAll" call.}}
    strings.wait();
    strings.foo();
  }
}
