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

abstract class E {
  void foo1(LocalA param) {
    doSomething(param.getValue());
    doSomethingElse(param.getOtherValue());
  }

  // Compliant - foo2() looks similar but types of LocalB and LocalA are different. Only their method names are the same
  void foo2(LocalB param) {
    doSomething(param.getValue());
    doSomethingElse(param.getOtherValue());
  }

  void qix1(Object[] items) {
    for (Object item : items) {
      doSomething(item);
    }
  }

  // Compliant - can not be simply swap to call to other method, or common method
  void qix2(java.util.List<Object> items) {
    for (Object item : items) {
      doSomething(item);
    }
  }

  void bar1(E a) {
    doSomething(a);
    doSomethingElse(a);
  }

  // Compliant - We can not guarantee equivalence  because of UnknownType in bar2
  void bar2(UnknownType a) {
    doSomething(a);
    doSomethingElse(a);
  }

  void gul1(UnknownType b) {
    doSomething(b);
    doSomethingElse(b);
  }

  // Compliant - We can not guarantee equivalence because of UnknownType in gul1
  void gul2(E b) {
    doSomething(b);
    doSomethingElse(b);
  }

  void bam1(LocalA o1, LocalB o2) {
    doSomething(o1);
    doSomethingElse(o2);
  }

  LocalB o2;
  void bam2(LocalA o1) { // Noncompliant - parameters are not considered
    doSomething(o1);
    doSomethingElse(o2);
  }

  abstract void doSomething(Object o);
  abstract void doSomethingElse(Object o);

  abstract static class LocalA {
    Object getValue();
    Object getOtherValue();
  }

  abstract static class LocalB {
    Object getValue();
    Object getOtherValue();
  }
}
