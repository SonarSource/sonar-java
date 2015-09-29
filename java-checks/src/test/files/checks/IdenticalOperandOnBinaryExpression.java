class A {
  void foo() {
    boolean a,b;
    if(a == b) { }
    if(a == a) { } // Noncompliant {{Identical sub-expressions on both sides of operator "=="}}
    if(a != a) { } // Noncompliant {{Identical sub-expressions on both sides of operator "!="}}
    if(a || a) { } // Noncompliant
    if(a && a) { } // Noncompliant
    if(a == b || a == b) {} // Noncompliant
    if(a || b || a) {} // Noncompliant
    if(a || a || b) {} // Noncompliant
    if(a || b || c || e && a) {}
    if(a && b && c && e && a) {} // Noncompliant
    if(b
        || a
        || a) {} // Noncompliant

    double d = 0.0d;
    float f = 0.0f;
    if(f != f) {} //valid test for NaN
    if(d != d) {} //valid test for NaN
    int j,k;
    int k = 1 << 1; //exclude this case for bit masks
    j = 12 - k -k; //case why minus is excluded.
    j = k - k; // Noncompliant
    j = k*3/12%2 - k*3/12%2; // Noncompliant
  }
}
