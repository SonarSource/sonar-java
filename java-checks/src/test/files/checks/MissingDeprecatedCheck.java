class Foo {

  @Deprecated          // Non-Compliant
  public int foo;

  public void foo1() { // Compliant
  }

  @Deprecated          // Non-Compliant
  public void foo2() {
  }

  /**
   * @deprecated
   */
  public void foo3() { // Non-Compliant

  }

  /**
   * @deprecated
   */
  @Ignore
  @Deprecated
  public void foo4() { // Compliant
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

    @Deprecated        // Compliant - cannot have javadoc
    int local1 = 0;

  }

}

interface Bar {

  @Deprecated          // Non-Compliant
  int foo();

}

/**
* @deprecated
*/
class Qix  {

  /**
   * @deprecated
   */
  public void foo() {} // Compliant class is deprecated

  public void foo1() {} // Compliant

  @Deprecated
  public void foo1() {} // Compliant class is deprecated
}

@Deprecated
interface Plop {
  /**
   * @deprecated
   */
  public void foo(); // Compliant interface is deprecated

}
interface mockito {
  /**
   * ...
   *
   * @deprecated Use {@link EasyMock#createMockBuilder(Class)} instead
   */
  @Deprecated
  <T> T createMock(Class<T> toMock, Method... mockedMethods);
}