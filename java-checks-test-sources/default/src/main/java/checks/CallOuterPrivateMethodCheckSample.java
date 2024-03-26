package checks;

class CallOuterPrivateMethodCheckSample {

  public void foo() {}
  private void bar() {}
  private void qix() { // Noncompliant [[sc=16;ec=19]] {{Move this method into "Inner".}}
    bar();
  }
  private void baz(){} // Noncompliant [[sc=16;ec=19]] {{Move this method into "Inner".}}
  private void bax(){} // Noncompliant [[sc=16;ec=19]] {{Move this method into the anonymous class declared at line 59.}}
  private static void bay(){} // Compliant, can't move static method into non-static inner class
  private static void baw() {} // Noncompliant [[sc=23;ec=26]] {{Move this method into "StaticInner".}}
  private void calledOnAnotherInstance() {} // Compliant, called on another instance, cannot move it inside inner class
  private void calledOnAnotherInstance2() {} // Compliant, called on another instance, cannot move it inside inner class
  private void calledOnCurrentInstance() {} // Noncompliant

  class Inner {

    Inner() {
      // For coverage
      super();
    }

    void plop() {
      bar();
      qix();
      foo();
      baz();
      baz();
      baz();
      baz();
      bay();
      innerFun();
    }
    private void innerFun() {}

    void callingOnInstance() {
      CallOuterPrivateMethodCheckSample o = new CallOuterPrivateMethodCheckSample();
      o.calledOnAnotherInstance2();
    }

    void callingCurrentInstance() {
      CallOuterPrivateMethodCheckSample.this.calledOnCurrentInstance();
    }
  }

  static class StaticInner {
    void plop() {
      baw();
    }

    void callingOnInstance() {
      CallOuterPrivateMethodCheckSample o = new CallOuterPrivateMethodCheckSample();
      o.calledOnAnotherInstance();
    }
  }

  Object foo = new Object() {
    void plop() {
      bax();
    }
  };

}

class Parent {

  interface G1<T> {  }
  interface G2<K, V> {  }

  private static <K, V> G2<K, V> m(G2<K, V> o) { // Noncompliant
    return null;
  }

  static class Inner {
    Inner(G2<String, Integer> p1, G2<String, G1<Double>> p2) {
      m(p1);
      m(p2);
    }
  }
}
