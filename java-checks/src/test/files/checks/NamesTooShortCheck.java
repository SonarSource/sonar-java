class A { // Non-Compliant

}

class Foo { // Compliant

  void fo() { // Non-Compliant
  }

  void foo() { // Compliant
    int a; // Compliant
    int index; // Compliant

    for (int i = 0; i < 42; i++) { // Compliant - exception
      int b; // Compliant
    }

    System.out.println(a + b); // Compliant
    fo(); // Compliant
  }

  int a; // Compliant

}

interface B { // Non-Compliant
  int a = 0; // Non-Compliant
  int aaaa = 0; // Compliant

  void f(); // Non-Compliant
  void foo(); // Compliant
}

interface Bar {
}

enum A { // Non-Compliant
  U, // Non-Compliant
  HUHU // Compliant
}

class Aaaa {
  private void fooo(int a, int b) { // Non-Compliant
    try {

    } catch (Exception e) { // Compliant - exception

    }
  }
}
