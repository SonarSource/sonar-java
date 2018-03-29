class A {
  void m1() {
    for (int i = 0; i < 10; i++) {
      foo();
      break;  // Noncompliant {{Remove this "break" statement or make it conditional.}}
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
      continue;
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

  boolean foo(int last, char[] b, char[] src) {
    boolean hasClassPathAttribute = false;
    int i = 0;
    next: while (i <= last) {
      for (int j = 0; j < 10; j++) {
        char c = b[i];
        if (c != src[j]) {
          continue next;
        }
      }
      hasClassPathAttribute = true;
      break; // compliant, there is another way to get back to the 'while' loop from the inner 'for' loop
    }
    return hasClassPathAttribute;
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
      continue;
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

  }

  private static boolean baz() { return false; }
  private static void bar() { }
  private static boolean foo() { return false; }
}

abstract class C {
  void m1(java.util.Iterator<String> itr) {
    String s = null;
    while(itr.hasNext()) {
      s = itr.next();
      break; // Compliant
    }
  }

  String m2(java.util.Enumeration<String> e) {
    while((((e.hasMoreElements())))) {
      String s = e.nextElement();
      return s; // Compliant
    }
    return null;
  }

  void m4() {
    while(true) {
      if (isItTrue()) {
        // ...
        break;
      }
      break; // Compliant
    }

    while(isItTrue()) {
      if (isItTrue()) {
        // ...
        break;
      }
      break; // Noncompliant
    }
  }

  void m5() {
    for(;;) {
      if (isItTrue()) {
        // ...
        break;
      }
      break; // Compliant
    }

    for(int i = 0;;) {
      if (isItTrue()) {
        // ...
        break;
      }
      break; // Noncompliant
    }

    for(;isItTrue();) {
      if (isItTrue()) {
        // ...
        break;
      }
      break; // Noncompliant
    }

    int i = 0;
    for(;;i++) {
      if (isItTrue()) {
        // ...
        break;
      }
      break; // Noncompliant
    }
  }

  void m6() {
    do {
      if (isItTrue()) {
        // ...
        break;
      }
      break; // Noncompliant
    } while (false);

    do {
      if (isItTrue()) {
        // ...
        break;
      }
      break; // Compliant
    } while ((((true))));
  }

  abstract boolean isItTrue();
}

abstract class D {
  void foo(Iterable<D> s) {
    s.forEach(d -> {
      for (Object o : d.bar()) {
        if (o.equals("")) {
          continue;
        }
        break;
      }
    });
  }

  abstract Iterable<Object> bar();
}
