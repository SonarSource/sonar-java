abstract class A {
  private final static String CODE = "bounteous";

  String getCode() { return CODE; }
  String getName() { return CODE; } // Noncompliant [[sc=10;ec=17;secondary=4]] {{Update this method so that its implementation is not identical to "getCode" on line 4.}}
  String getName() { return CODE; } // Noncompliant [[sc=10;ec=17;secondary=4]] {{Update this method so that its implementation is not identical to "getCode" on line 4.}}

  void greek1() {
    Object x = null;
    try {
      x = new Object();
      alpha();
      x = null;
    } catch (Exception e) {
      if (x == null) {
        beta();
      }
    }
  }

  void greek2() { // Noncompliant [[sc=8;ec=14;secondary=8]] {{Update this method so that its implementation is not identical to "greek1" on line 8.}}
    Object x = null;
    try {
      x = new Object();
      alpha();
      x = null;
    } catch (Exception e) {
      if (x == null) {
        beta();
      }
    }
  }

  abstract int alpha() throws Exception;
  abstract void beta();
  abstract void doSomething(Object... objects);

  String kar1() {return "hello"; }
  String kar2() {return "hello"; } // Noncompliant

  int bfg1() { return 42; };
  int bfg2() { return 42; }; // Noncompliant

  void xms1() { alpha(); }
  void xms2() { alpha(); } // Compliant

  int dnf1() { return alpha(); }
  int dnf2() { return alpha(); } // Compliant

  void xox1() { this.alpha(); }
  void xox2() { this.alpha(); } // Noncompliant

  void xax1(A a) { a.alpha(); }
  void xax2(A a) { a.alpha(); } // Noncompliant

  void xmx1(int p) { doSomething(p); }
  void xmx2(int p) { doSomething(p); } // Noncompliant

  int x;
  void setX(int value) { this.x = value; }
  void setY(int value) { this.x = value; } // Noncompliant

  void qix1() { }
  void qix2() { } // Compliant

  void foo1() { return; }
  void foo2() { return; } // Compliant

  boolean lol1() { return true; }
  boolean lol2() { return true; } // Compliant

  boolean gul1() { return null; }
  boolean gul2() { return null; } // Compliant

  String bul1() {return ""; }
  String bul2() {return ""; } // Compliant

  A kil1() { return this; }
  A kil2() { return this; } // Compliant

  int zik1() { return -1; }
  int zik2() { return -1; } // Compliant

  void bar1() { throw new UnsupportedOperationException(); }
  void bar2() { throw new UnsupportedOperationException(); } // Compliant

  void getValue(String item) { doStuff((Object) item); }
  void getValue(Long item) { doStuff((Object) item); } // Compliant - overload
  void doStuff(Object o) { }
}

class B {
  private final static String CODE = "bounteous";

  String getCode() { return CODE; }
  String getName() { return getCode(); } // Compliant
}

class C {
  // single method in class
  void foo() { }
}
