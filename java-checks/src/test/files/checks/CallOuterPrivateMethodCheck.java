class A {

  public void foo() {}
  private void bar() {}
  private void qix() { // Noncompliant [[sc=16;ec=19]] {{Move this method into "inner".}}
    bar();
  }
  private void baz(){} // Noncompliant [[sc=16;ec=19]] {{Move this method into "inner".}}

  class inner {
    void plop() {
      bar();
      qix();
      foo();
      baz();
      baz();
      baz();
      baz();
      innerFun();
    }
    private void innerFun() {}
  }

}