/**
 * Executed rules:
 * - RawExceptionCheck (S00112)
 * - BadMethodNameCheck (S100)
 * - HardcodedURICheck (S1075)
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

  String s = "http://www.mywebsite.com"; // NoIssue
  int v = 42;
}
