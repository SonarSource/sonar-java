interface A {
}

interface B { // Noncompliant
  int a = 0;
}

interface C { // Noncompliant
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

interface J { // Compliant
  int f();

  interface K { // Noncompliant
    void f();
    int a = 0;
  }
}

interface L {
  ;
}
