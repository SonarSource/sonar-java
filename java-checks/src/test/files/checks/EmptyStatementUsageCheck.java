import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import arg.goo;  // Compliant
import arg.foo;; // Noncompliant [[sc=16;ec=17]] {{Remove this empty statement.}}
; // Noncompliant {{Remove this empty statement.}} 

abstract class Foo {
  int a; // Compliant
  int b;; // Noncompliant {{Remove this empty statement.}}
  ; // Noncompliant {{Remove this empty statement.}}
  
  public Foo() {
    ; // Noncompliant {{Remove this empty statement.}}
  }

  void foo(boolean condition) {
    for (int i = 0; i < 42; i++)
      ; // compliant unique statement of a loop
    int i = 0;; // Noncompliant {{Remove this empty statement.}}
    ; // Noncompliant {{Remove this empty statement.}}

    int a = 0; // Compliant
    a = 42; // Compliant

    for (;;) { // Compliant
      ; // Noncompliant {{Remove this empty statement.}}
      break;
    }

    if (i == 0)
      ; // Noncompliant {{Remove this empty statement.}}
    else
      ; // Noncompliant {{Remove this empty statement.}}

    if (a == 0)
      ; // Noncompliant {{Remove this empty statement.}}

    class myInnerClass {}; // Noncompliant {{Remove this empty statement.}}

    do ; while (condition); // compliant

    while (condition)
      ; // compliant

    for (Object object : getCollection())
      ; // compliant
    
    return; // Compliant
  }

  abstract void tul();

  Collection getCollection() {
    return new ArrayList();
  }; // Noncompliant {{Remove this empty statement.}}

  class Bar {
  }
}

static class Bar {
  public enum MyEnum { APPLICATION, HANDLER }; // Noncompliant {{Remove this empty statement.}}
  
  Closeable c = new Closeable() {
    @Override
    public void close() throws IOException {
    }; // Noncompliant {{Remove this empty statement.}}
  };
  
  void foo (MyEnum scope) {
    switch (scope) {
      case APPLICATION:
        break;
      default:
        ; // Noncompliant {{Remove this empty statement.}}
    }
  }; // Noncompliant {{Remove this empty statement.}}
}

; // Noncompliant {{Remove this empty statement.}}

enum EmptyEnum {
  // This is my empty enum full of emptyness
  ; // Compliant

  boolean foo() {
    return false;
  }

  ; // Noncompliant
}
