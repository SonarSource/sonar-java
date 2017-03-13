class A {
  void m1() {
    for (int i = 0; i < 10; i++) {
      foo();
      break;  // Noncompliant {{Remove this "break" statement or make it conditional.}}
    }
  }

  void m2() {
    forLabel: for (int i = 0; i < 10; ++i) {
      foo();
      continue forLabel;  // Noncompliant {{Remove this "continue" statement or make it conditional.}}
    }
  }

  void m3() throws Exception {
    int i = 0;
    while (i++ < 10) {
      foo();
      throw new Exception("BOUM!");  // Noncompliant {{Remove this "throw" statement or make it conditional.}}
    }
  }

  void m4() {
    int i = 0;
    do {
      foo();
      break;  // Noncompliant {{Remove this "break" statement or make it conditional.}}
    } while (i++ < 10);
  }

  void m5(java.util.List<Object> myList) {
    for (Object object : myList) {
      foo(object);
      continue; // Noncompliant {{Remove this "continue" statement or make it conditional.}}
    }
  }

  void m6() {
    for (int i = 0; i < 10; ++i) {
      foo();
      return;  // Noncompliant {{Remove this "return" statement or make it conditional.}}
    }
  }

  void m7() {
    int i = 0;
    for (; i < 10;) {
      foo();
      break; // Noncompliant {{Remove this "break" statement or make it conditional.}}
    }
  }

  void m8() throws Exception {
    int i = 0;
    while (i++ < 10) {
      if (foo(i)) {
        continue;
      }
      throw new Exception("BOUM!");  // Compliant  - can continue
    }
  }

  void m9(boolean b) {
    int i = 0;
    if (b)
      while (i++ < 10) {
        foo();
        return;  // Noncompliant {{Remove this "return" statement or make it conditional.}}
      }
  }

  void m10(boolean b) throws Exception {
    int i = 0;
    while (i++ < 10) {
      foo(i);
      throw new Exception("BOUM!");  // Noncompliant {{Remove this "throw" statement or make it conditional.}}
    }
  }

  void bar(java.util.List<Object> myList, String target) {
    int i;
    for (i = 0; i < 10; ++i) {
      foo();
      // no jump statement
    }

    i = 0;
    while (i < 10) {
      foo(i++);
      // no jump statement
    }

    boolean found = false;
    search: for (Object object : myList) {
      if (target.equals(object)) {
        found = true;
        break search; // Compliant
      }
    }

    return;
  }
}

class B {

  static {
    while (foo()) {
      bar();
      if (baz()) {
        break;
      }
    }
    while (foo()) {
      bar();
      break; // Noncompliant
    }
  }

  void qix() throws Exception {
    while(foo()) {
      bar();
      if (baz()) {
        break;
      }
    }

    while(foo()) {
      bar();
      break; // Noncompliant
    }

    while(foo()) {
      bar();
      continue; // Noncompliant
    }

    while(foo()) {
      bar();
      throw new Exception(); // Noncompliant
    }
  }

  void gul(java.util.List<Object> myList) {

    while(foo())
      break; // Noncompliant

    do {
      bar();
      break; // Noncompliant
    } while (foo());

    for (int i = 0; foo(); i++) {
      bar();
      break; // Noncompliant
    }

    for (Object o : myList) {
      bar();
      break; // Noncompliant
    }

    for (Object o : myList) {
      foo();
      continue; // Noncompliant
    }

    while(foo()) {
      if (baz()) {
        break;
      }
      baz();
      break; // Noncompliant
    }

    while(foo()) {
      if (baz()) {
        continue;
      }
      baz();
      break; // Compliant - the loop can execute more than once
    }

    while(foo()) {
      if (baz()) {
        continue;
      }
      baz();
      continue; // Noncompliant
    }

    for (int i = 0; foo(); i++) {
      if (baz()) {
        continue;
      }
      baz();
      break; // compliant
    }

    for (int i = 0; foo();) {
      baz();
      break; // Noncompliant
    }

    for (int i = 0; foo(); i++) {
      baz();
      continue; // Noncompliant
    }
  }

  private static boolean baz() { return false; }
  private static void bar() { }
  private static boolean foo() { return false; }
}

