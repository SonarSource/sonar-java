package checks;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;; // Noncompliant [[sc=29;ec=30;quickfixes=qf1]] {{Remove this empty statement.}}
  // fix@qf1 {{Remove this empty statement}}
  // edit@qf1 [[sc=29;ec=30]] {{}}

; // Noncompliant [[sc=1;ec=2;quickfixes=qf2]] {{Remove this empty statement.}}
// fix@qf2 {{Remove this empty statement}}
// edit@qf2 [[sl=-4;sc=30;el=+0;ec=2]] {{}}

abstract class EmptyStatementUsageCheck {
  int a; // Compliant
  int b;; // Noncompliant {{Remove this empty statement.}}
  ; // Noncompliant {{Remove this empty statement.}}
  
  public EmptyStatementUsageCheck() {
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
      ; // Noncompliant [[sc=7;ec=8;quickfixes=qf3]] {{Remove this empty statement.}}
        // fix@qf3 {{Remove this empty statement}}
        // edit@qf3 [[sl=-1;sc=15;el=+0;ec=8]] {{}}
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

  void somethingAfter() {
    ;tul(); // Noncompliant [[sc=5;ec=6;quickfixes=qf4]] {{Remove this empty statement.}}
    // fix@qf4 {{Remove this empty statement}}
    // edit@qf4 [[sc=5;ec=6]] {{}}
  }
}

class EmptyStatementUsageCheckBar {
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
// Noncompliant@+3 [[sc=1;ec=2;quickfixes=qf_last_statement]]
// fix@qf_last_statement {{Remove this empty statement}}
// edit@qf_last_statement [[sl=-4;sc=2;el=+0;ec=2]] {{}}
;
