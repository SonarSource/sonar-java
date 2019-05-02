import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

class A {

  boolean b;

  void myMethod(int x, int y, Integer z) {
     b = x % 2 == 1; // Noncompliant [[sc=12;ec=13]] {{The results of this modulus operation may not be positive.}}
     b = x % 2 == -1; // Noncompliant {{The results of this modulus operation may not be negative.}}
     b = 2 % x == 1; // Noncompliant {{The results of this modulus operation may not be positive.}}
     b = 1 == x % 2; // Noncompliant {{The results of this modulus operation may not be positive.}}
     b = z.intValue() % 2 == 1; // Noncompliant {{The results of this modulus operation may not be positive.}}
     b = x % 2 == y;
     b = x % 2 == 0;
     b = x % 2 != 1;
     int i = 5;
     b = i % 2 == 1;
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
