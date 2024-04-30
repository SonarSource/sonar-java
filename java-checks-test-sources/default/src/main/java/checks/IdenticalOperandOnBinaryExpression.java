package checks;

class IdenticalOperandOnBinaryExpressionCheck {
  void foo(boolean a, boolean b, boolean c, boolean e) {
    if(a == b) { }
    if(a == a) { } // Noncompliant {{Correct one of the identical sub-expressions on both sides of operator "=="}}
//     ^>   ^

    if(a != a) { } // Noncompliant {{Correct one of the identical sub-expressions on both sides of operator "!="}}
//          ^
    if(a || a) { } // Noncompliant
    if(a && a) { } // Noncompliant
    if(a == b || a == b) {} // Noncompliant
//               ^^^^^^
    if(a || b || a) {} // Noncompliant
    if(a || a || b) {} // Noncompliant
    if(a || b || c || e && a) {}
    if(a && b && c && e && a) {} // Noncompliant
//     ^>                  ^
    if(b || a || a) {} // Noncompliant
//               ^

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
    a.equals(a); // Noncompliant {{Correct one of the identical sub-expressions on both sides of equals.}}
    a.equals(b);
    equals(a);
    java.util.Objects.equals(a, a); // Noncompliant {{Correct one of the identical argument sub-expressions.}}
    java.util.Objects.equals(a, b);
    java.util.Objects.deepEquals(a, a); // Noncompliant {{Correct one of the identical argument sub-expressions.}}
    java.util.Objects.deepEquals(a, b);
    java.util.Objects.isNull(a);
  }
}
