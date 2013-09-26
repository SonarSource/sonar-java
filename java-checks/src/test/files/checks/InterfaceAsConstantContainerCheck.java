interface A {
}

interface A {
  int a = 0; // Non-Compliant
}

interface A {
  int a = 0; // Non-Compliant
  int b = 0; // Non-Compliant
}

interface A {
  void f();
}

interface A {
  int a = 0; // Non-Compliant
  void f();
}

interface A {
  void f();
  int a = 0; // Non-Compliant
}

interface A {
  int a = 0; // Non-Compliant
  int f();
}

interface A {
  int f();
  int a = 0; // Non-Compliant
}
