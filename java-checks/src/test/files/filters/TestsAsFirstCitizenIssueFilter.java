abstract class A {

  void method1() throws Exception { // NoIssue
    foo();
  }

  void method2()
    throws Exception { // NoIssue
    foo();
  }

  abstract void foo() throws java.io.IOException;
}
