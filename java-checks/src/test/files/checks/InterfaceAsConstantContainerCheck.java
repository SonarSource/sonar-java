interface A {
}

interface A { // Noncompliant
  int a = 0;
}

interface A { // Noncompliant
  int a = 0;
  int b = 0;
}

interface A {
  void f();
}

interface A { // Noncompliant
  int a = 0;
  void f();
}

interface A { // Noncompliant
  void f();
  int a = 0;
}

interface A { // Noncompliant
  int a = 0;
  int f();
}

interface A { // Noncompliant
  int f();
  int a = 0;
}

interface A {
  int f();
  void g();
}

interface A { // Compliant
  int f();

  interface B { // Noncompliant
    void f();
    int a = 0;
  }
}

interface A {
  ;
}
