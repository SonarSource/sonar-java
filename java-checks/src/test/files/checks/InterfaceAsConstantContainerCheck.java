interface A { // Compliant
}

interface A { // Non-Compliant
  int a = 0;
}

interface A { // Non-Compliant
  int a = 0;
  int b = 0;
}

interface A { // Compliant
  void f();
}

interface A { // Compliant
  int a = 0;
  void f();
}

interface A { // Compliant
  void f();
  int a = 0;
}

interface A { // Compliant
  int a = 0;
  int f();
}

interface A { // Compliant
  int f();
  int a = 0;
}
