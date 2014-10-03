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



    double d1 = 0.1d;
    double d2 = 0.1d;
    if(d1 == d2) {} //Non-Compliant
    if( d1 != d2 ){}//Non-Compliant
    if( d1 == i ){}//Non-Compliant
    if( i == d1 ){}//Non-Compliant
    if(d1 != d1){} //compliant NaN test
  }
}