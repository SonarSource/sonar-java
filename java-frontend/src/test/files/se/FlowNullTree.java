abstract class A {

  void test(Object o, boolean b2) {
    try {
      try {
        f(); // flow@f1,f4 {{...}}
      } finally {
        if (b2) { // flow@f1,f4 {{...}}
          o = null; // flow@f1,f2,f3,f4 {{..}}
          g(); // flow@f1,f3 {{Exception is thrown here.}}
          o = new Object();
        }
      }
      h();
    } finally {
      o.toString(); // Noncompliant [[flows=f1,f2,f3,f4]] flow@f1,f2,f3,f4
    }
  }

  abstract void f();
  abstract void g();
  abstract void h();
}

