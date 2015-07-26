class A {
  void foo() {
    float f1 = 0.1f;
    float f2 = 0.1f;
    int i = 1;
    if(f1 == f2) {} // Noncompliant {{Equality tests should not be made with floating point values.}}
    if( f1 != f2 ){}// Noncompliant {{Equality tests should not be made with floating point values.}}
    if( f1 == i ){}// Noncompliant {{Equality tests should not be made with floating point values.}}
    if( i == f1 ){}// Noncompliant
    if(f1 != f1){} //compliant NaN test



    double a = 0.1d;
    double c = 0.1d;
    if( a == c ) {} // Noncompliant
    if( a != c ){}// Noncompliant
    if( a == i ){}// Noncompliant
    if( i == a ){}// Noncompliant
    if( a != a ){} //compliant NaN test

    if(c <= a && a <= c) {} // Noncompliant
    if(a <= c && c <= a) {} // Noncompliant
    if(c <= a && c >= a) {} // Noncompliant
    if(c >= a && c <= a) {} // Noncompliant
    if(a >= c && a <= c) {} // Noncompliant
    if(a <= c && a >= c) {} // Noncompliant
    if(a >= c && c >= a) {} // Noncompliant
    if(c >= a && a >= c) {} // Noncompliant


    if(c < a || a < c) {} // Noncompliant
    if(a < c || c < a) {} // Noncompliant
    if(c < a || c > a) {} // Noncompliant
    if(c > a || c < a) {} // Noncompliant
    if(a > c || a < c) {} // Noncompliant
    if(a < c || a > c) {} // Noncompliant
    if(a > c || c > a) {} // Noncompliant
    if(c > a || a > c) {} // Noncompliant

    if(c < a || a > c) {} //Compliant
    if(c > a || a < c) {} //Compliant

  }
}