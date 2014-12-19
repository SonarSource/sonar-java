class A {
  
  void myMethod(int x, int y) {
    x % 2 == 1; // Noncompliant
    x % 2 == -1; // Noncompliant
    2 % x == 1; // Noncompliant
    1 == x % 2; // Noncompliant
    x % 2 == y;
    x % 2 == 0;
    x % 2 != 1;
    int i = 5;
    i % 2 == 1;
  }
  
}