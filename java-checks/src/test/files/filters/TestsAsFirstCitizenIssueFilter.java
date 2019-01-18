/**
 * Executed rules:
 * - RawExceptionCheck (S1228)
 * - BadMethodNameCheck (S112)
 */
abstract class A {

  void method1() throws Exception { // NoIssue
    foo();
  }

  void method2()
    throws Exception { // NoIssue
    foo();
  }

  abstract void foo() throws java.io.IOException;

  @org.junit.Test
  public void this_is_a_test_method() { // NoIssue
  }

  @org.foo.Test
  public void this_could_be_a_test_method() { // NoIssue
  }

  @org.foo.bar
  public void this_is_not_a_test_method() { // WithIssue
  }
}
