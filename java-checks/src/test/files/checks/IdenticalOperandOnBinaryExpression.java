class A {
  void foo() {
    boolean a,b;
    if(a == b) { }
    if(a == a) { } // Noncompliant [[sc=13;ec=14;secondary=5]] {{Identical sub-expressions on both sides of operator "=="}}
    if(a != a) { } // Noncompliant [[sc=13;ec=14]] {{Identical sub-expressions on both sides of operator "!="}}
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
}
