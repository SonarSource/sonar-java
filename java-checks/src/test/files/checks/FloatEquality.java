class A {
  void foo() {
    float f1 = 0.1f;
    float f2 = 0.1f;
    int i = 1;
    if(f1 == f2) {} //Non-Compliant
    if( f1 != f2 ){}//Non-Compliant
    if( f1 == i ){}//Non-Compliant
    if( i == f1 ){}//Non-Compliant
    if(f1 != f1){} //compliant NaN test



    double a = 0.1d;
    double c = 0.1d;
    if( a == c ) {} //Non-Compliant
    if( a != c ){}//Non-Compliant
    if( a == i ){}//Non-Compliant
    if( i == a ){}//Non-Compliant
    if( a != a ){} //compliant NaN test

    if(c <= a && a <= c) {} //Non-Compliant
    if(a <= c && c <= a) {} //Non-Compliant
    if(c <= a && c >= a) {} //Non-Compliant
    if(c >= a && c <= a) {} //Non-Compliant
    if(a >= c && a <= c) {} //Non-Compliant
    if(a <= c && a >= c) {} //Non-Compliant
    if(a >= c && c >= a) {} //Non-Compliant
    if(c >= a && a >= c) {} //Non-Compliant


    if(c < a || a < c) {} //Non-Compliant
    if(a < c || c < a) {} //Non-Compliant
    if(c < a || c > a) {} //Non-Compliant
    if(c > a || c < a) {} //Non-Compliant
    if(a > c || a < c) {} //Non-Compliant
    if(a < c || a > c) {} //Non-Compliant
    if(a > c || c > a) {} //Non-Compliant
    if(c > a || a > c) {} //Non-Compliant

  }
}