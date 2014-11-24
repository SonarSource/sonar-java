class A{
  void foo() {
    long l1 = 1000*3600*24*365; // Noncompliant
    long l2 = 1000L*3600*24*365;
    float f1 = 2/3; // Noncompliant
    float f2 = 2f/3;
  }
}