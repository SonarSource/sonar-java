import java.util.ArrayList;
import java.util.List;

class A {
  void fun() {
    List<String> strings = new ArrayList<String>();
    strings.add("Hello");
    strings.addAll(strings); // Noncompliant {{Remove or correct this "addAll" call.}}
    strings.containsAll(strings); // Noncompliant {{Remove or correct this "containsAll" call.}}
    strings.removeAll(strings); // Noncompliant {{Remove or correct this "removeAll" call.}}
    strings.retainAll(strings); // Noncompliant {{Remove or correct this "retainAll" call.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^
    strings.wait();
    strings.foo();
  }
}
