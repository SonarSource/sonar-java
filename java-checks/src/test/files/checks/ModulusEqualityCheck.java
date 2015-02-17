import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

class A {

  void myMethod(int x, int y, Integer z) {
     x % 2 == 1; // Noncompliant
     x % 2 == -1; // Noncompliant
     2 % x == 1; // Noncompliant
     1 == x % 2; // Noncompliant
     z.intValue() % 2 == 1; // Noncompliant
     x % 2 == y;
     x % 2 == 0;
     x % 2 != 1;
     int i = 5;
     i % 2 == 1;
  }

  void myOtherMethod(Collection c, List l, String s, int[] a, Queue<String> q) {
    boolean b;
    b = c.size() % 2 == 1; // Compliant
    b = s.length() % 2 == 1; // Compliant
    b = a.length % 2 == 1; // Compliant
    b = q.size() % 2 == 1; // Compliant
    b = 2 % l.size() == 1; // Compliant

    b = a.hashCode() % 2 == 1; // Noncompliant
    b = c.hashCode() % 2 == 1; // Noncompliant
    b = s.indexOf("") % 2 == -1; // Noncompliant
  }
}