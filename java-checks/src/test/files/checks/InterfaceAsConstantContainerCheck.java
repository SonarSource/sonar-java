interface A {
}

interface B {
}

interface C { // Noncompliant [[sc=11;ec=12]] {{Move constants to a class or enum.}}
  int a = 0;
  int b = 0;
}

interface D {
  void f();
}

interface E { // Noncompliant
  int a = 0;
  void f();
}

interface F { // Noncompliant
  void f();
  int a = 0;
}

interface G { // Noncompliant
  int a = 0;
  int f();
}

interface H { // Noncompliant
  int f();
  int a = 0;
}

interface I {
  int f();
  void g();
}

interface J {
  int f();

  interface K { // Noncompliant
    void f();
    int a = 0;
  }
}

interface L {
  ;
}
