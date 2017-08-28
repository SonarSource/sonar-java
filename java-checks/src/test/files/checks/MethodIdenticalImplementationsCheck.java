abstract class A {
  private final static String CODE = "bounteous";
  private String code;

  public String getCode() { return code; }
  public String getName() { return code; } // Noncompliant [[sc=17;ec=24;secondary=5]] {{Update this method so that its implementation is not identical to "getCode" on line 5.}}
  public String getWord() { return code; } // Noncompliant [[sc=17;ec=24;secondary=5]] {{Update this method so that its implementation is not identical to "getCode" on line 5.}}

  String getOtherCode() { return CODE; }
  String getOtherName() { return CODE; } // Compliant - not getters (not public)

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

  void greek2() { // Noncompliant [[sc=8;ec=14;secondary=12]] {{Update this method so that its implementation is not identical to "greek1" on line 12.}}
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


  void getValue(String item) {
    doSomething((Object) item);
    doSomething((Object) item);
  }
  void getValue(Long item) {  // Compliant - overload
    doSomething((Object) item);
    doSomething((Object) item);
  }
}

class B {
  private String code;

  String getCode() { return code; }
  String getName() { return getCode(); } // Compliant
}

class C {
  // single method in class
  void foo() { }
}
