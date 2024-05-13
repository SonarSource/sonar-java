package checks.tests;

class TestCaseA extends junit.framework.TestCase {
  void testFoo() {
  }
}

class TestCaseB extends junit.framework.TestCase {
  void foo() {
  }
}

class ATest { // Noncompliant {{Add some tests to this class.}}
//    ^^^^^
  ATest() {}
  void foo() {
    new AnonymousClass() {
      void testfoo(){
      }
    };
  }
}

class BTest {
  @org.junit.Test
  void foo()  {
    class MyInnerTest { }
  }
}

enum MyTest {}

class AnonymousClass extends junit.framework.TestCase{
  void testfoo(){}
}

abstract class AbstractIntegrationTest { //designed for extension should not raise issue.

}
