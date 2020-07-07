package checks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class ClassWithoutHashCodeInHashStructureCheck {
  class Test<K> {
    Map<A, Object> m1 = new HashMap<>(); // Compliant
    Map<B, Object> m2 = new Hashtable<>(); // Noncompliant {{Add a "hashCode()" method to "B" or remove it from this hash.}}
    Map<C, Object> m3 = new LinkedHashMap<>(); // Compliant
    Map<D, Object> m4 = new LinkedHashMap<>(); // Compliant

    Set<A> s1 = new HashSet<>(); // Compliant
    Set<B> s2 = new TreeSet<>(); // Compliant
    Set<B> s3 = new HashSet<>(); // Noncompliant {{Add a "hashCode()" method to "B" or remove it from this hash.}}
    Set<C> s4 = new HashSet<>(); // Compliant
    Set<K> s5 = new HashSet<K>(); // Compliant
    Set s6 = new HashSet(); // Compliant
  }

  class A {
    boolean equals = false;

    boolean equals(Object o1, Object o2) {
      return false;
    }

    boolean equals(A a) {
      return false;
    }
  }

  class B {
    @Override
    public boolean equals(Object obj) {
      return false;
    }

    int hashCode = 1;

    int hashCode(int i) {
      return 0;
    }
  }

  class C {
    @Override
    public int hashCode() {
      return 0;
    }
  }

  class D {
    @Override
    public boolean equals(Object obj) {
      return false;
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }

  class NotOverridenEquals {
    private Set<Equality> set = new HashSet<>(); // Compliant
  }

  interface Equality {
    @Override
    boolean equals(Object other); // does not have a default implementation
  }
}
