package org.foo;

import java.io.IOException;

class A {
  IOException lastException;

  private interface MyInterface {
    void bar() throws IOException;
  }

  public A foo(MyInterface mi, int j) {

    for (int i = 0; i < j; i++) {
      try {
        mi.bar();
      } catch (IOException x) {
        if (x != null) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
          lastException = x;
        }
      }
    }
    return this;
  }
}
