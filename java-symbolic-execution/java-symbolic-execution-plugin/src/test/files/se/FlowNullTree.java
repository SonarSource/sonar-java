abstract class A {

  void test(Object o, boolean b2) {
    boolean b1 = true;
    try {
      try {
        f();
      } finally {
        if (b2) {
          o = null;
          g();
          o = new Object();
        }
      }
      h();
    } finally {
      // because of incorrect stack handling, if-condition tree will be considered as throwing exception
      // and because direct parent of b1 is empty block, it's syntax tree will be null, so no flow message should be added
      if (b1)  // Noncompliant  (we don't care about issue details in this test, just particular shape of CFG and program state which triggers corner case)
        o.toString(); // Noncompliant
    }
  }

  abstract void f();
  abstract void g();
  abstract void h();
}

