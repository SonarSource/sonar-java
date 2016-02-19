class Foo {

  @Deprecated
  public int foo; // Noncompliant [[sc=14;ec=17]] {{Add the missing @deprecated Javadoc tag.}}

  public void foo1() {
  }

  @Deprecated
  public void foo2() { // Noncompliant
  }

  /**
   * @deprecated
   */
  public void foo3() { // Noncompliant {{Add the missing @Deprecated annotation.}}

  }

  /**
   * @deprecated
   */
  @Ignore
  @Deprecated
  public void foo4() {
  }

  @Deprecated
  /**
   * @deprecated
   */
  public void foo5() { // Noncompliant
  }

  /*
   * @deprecated
   */
  @Deprecated
  public int foo7() { // Noncompliant
  }

  /**
   *
   */
  @Deprecated
  public void foo8() { // Noncompliant
  }

  @java.lang.Deprecated
  public void foo9() {

    @Deprecated
    int local1 = 0;

  }

}

interface Bar {

  @Deprecated
  int foo(); // Noncompliant

}

/**
* @deprecated
*/
class Qix  { // Noncompliant

  /**
   * @deprecated
   */
  public void foo() {}

  public void foo1() {}

  @Deprecated
  public void foo1() {}
}

@Deprecated
interface Plop { // Noncompliant
  /**
   * @deprecated
   */
  public void foo();

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
