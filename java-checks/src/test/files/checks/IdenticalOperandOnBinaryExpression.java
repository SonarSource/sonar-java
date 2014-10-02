class A {
  void foo() {
    boolean a,b;
    if(a == b) { }
    if(a == a) { }
    if(a != a) { }
    if(a || a) { }
    if(a && a) { }
    if(a == b || a == b) {}
    if(a || b || a) {}
    if(a || a || b) {}
    if(a || b || c || e && a) {}
    if(a && b && c && e && a) {}
    if(b
        || a
        || a) {}

    double d = 0.0d;
    float f = 0.0f;
    if(f != f) {} //valid test for NaN
    if(d != d) {} //valid test for NaN
    int j,k;
    int k = 1 << 1; //exclude this case for bit masks
    j = 12 - k -k; //case why minus is excluded.
    j = k - k;
    j = k*3/12%2 - k*3/12%2;
  }
}