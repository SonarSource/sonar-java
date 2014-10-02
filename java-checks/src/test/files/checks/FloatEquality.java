class A {
  void foo() {
    float f1 = 0.1f;
    float f2 = 0.1f;
    if(f1 == f2) {} //Non-Compliant
    if( f1 != f2 ){}//Non-Compliant
    if(f1 != f1){} //compliant NaN test
  }
}