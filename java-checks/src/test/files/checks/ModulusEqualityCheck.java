import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

class A {

  void myMethod(int x, int y, Integer z) {
     x % 2 == 1; // Noncompliant [[sc=8;ec=9]] {{The results of this modulus operation may not be positive.}}
     x % 2 == -1; // Noncompliant {{The results of this modulus operation may not be negative.}}
     2 % x == 1; // Noncompliant {{The results of this modulus operation may not be positive.}}
     1 == x % 2; // Noncompliant {{The results of this modulus operation may not be positive.}}
     z.intValue() % 2 == 1; // Noncompliant {{The results of this modulus operation may not be positive.}}
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

    b = a.hashCode() % 2 == 1; // Noncompliant {{The results of this modulus operation may not be positive.}}
    b = c.hashCode() % 2 == 1; // Noncompliant {{The results of this modulus operation may not be positive.}}
    b = s.indexOf("") % 2 == -1; // Noncompliant {{The results of this modulus operation may not be negative.}}
  }
}
