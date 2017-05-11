class A {
  void foo() {
    boolean a,b;
    if(a == b) { }
    if(a == a) { } // Noncompliant [[sc=13;ec=14;secondary=5]] {{Correct one of the identical sub-expressions on both sides of operator "=="}}
    if(a != a) { } // Noncompliant [[sc=13;ec=14]] {{Correct one of the identical sub-expressions on both sides of operator "!="}}
    if(a || a) { } // Noncompliant
    if(a && a) { } // Noncompliant
    if(a == b || a == b) {} // Noncompliant [[sc=18;ec=24]]
    if(a || b || a) {} // Noncompliant
    if(a || a || b) {} // Noncompliant
    if(a || b || c || e && a) {}
    if(a && b && c && e && a) {} // Noncompliant [[sc=28;ec=29]]
    if(b
        || a
        || a) {} // Noncompliant [[sc=12;ec=13;secondary=15]]

    double d = 0.0d;
    float f = 0.0f;
    if(f != f) {} //valid test for NaN
    if(d != d) {} //valid test for NaN
    int j,l;
    int k = 1 << 1; //exclude this case for bit masks
    j = 12 - k -k; //case why minus is excluded.
    j = k - k; // Noncompliant
    j = k*3/12%2 - k*3/12%2; // Noncompliant
  }

  void fun(Object a, Object b) {
    a.equals(a);  // Noncompliant {{Correct one of the identical sub-expressions on both sides of equals.}}
    a.equals(b);
    equals(a);
    java.util.Objects.equals(a, a); // Noncompliant {{Correct one of the identical argument sub-expressions.}}
    java.util.Objects.equals(a, b);
    java.util.Objects.deepEquals(a, a); // Noncompliant {{Correct one of the identical argument sub-expressions.}}
    java.util.Objects.deepEquals(a, b);
    java.util.Objects.isNull(a);
  }
}
