import java.lang.Deprecated;

class A extends junit.framework.TestCase {
  void testFoo() {
  }
}
class B extends junit.framework.TestCase { // Noncompliant [[sc=7;ec=8]] {{Add some tests to this class.}}
  void foo() {
  }
}

class ATest { // Noncompliant {{Add some tests to this class.}}
  @Deprecated
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

public abstract class AbstractIntegrationTest { //designed for extension should not raise issue.

}

class TestNGTest {
  @org.testng.annotations.Test
  void foo() {
  }
}

@org.testng.annotations.Test
public class FooTest {
  public void test1() {
  }

  public void test2() {
  }
}

@org.testng.annotations.Test
public class TestNGClassTest { // Noncompliant
  public int field;
  private void test1() { }
  public static void foo() {}
}
@org.testng.annotations.Test(groups ="integration")
public abstract class AbstractIntegrationTest2{
}
