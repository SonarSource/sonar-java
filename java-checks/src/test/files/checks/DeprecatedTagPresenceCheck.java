class Foo {

  @Deprecated          // Non-Compliant
  public int foo;

  public void foo1() { // Compliant
  }

  @Deprecated
  public void foo2() { // Non-Compliant
  }

  /**
   * @deprecated
   */
  public void foo3() { // Non-Compliant

  }

  /**
   * @deprecated
   */
  @Ignore              // Non-Compliant
  @Deprecated
  public void foo4() {
  }

  @Deprecated          // Non-Compliant
  /**
   * @deprecated
   */
  public void foo5() {
  }

  /*
   * @deprecated
   */
  @Deprecated          // Non-Compliant
  public int foo7() {
  }

  /**
   *
   */
  @Deprecated          // Non-Compliant
  public void foo8() {
  }

  @java.lang.Deprecated // Compliant - no one does this
  public void foo9() {

    @Deprecated        // Non-Compliant
    int local1 = 0;

  }

}

interface Bar {

  @Deprecated          // Non-Compliant
  int foo();

}
