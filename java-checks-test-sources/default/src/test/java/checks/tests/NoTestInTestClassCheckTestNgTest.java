package checks.tests;

import org.testng.annotations.Test;

class TestNGTest {
  @Test
  void foo() {
  }
}

@Test
class TestNgFooTest {
  public void test1() {
  }

  public void test2() {
  }
}

@Test
class TestNGClassTest { // Noncompliant
  public int field;
  private void test1() { }
  public static void foo() {}
}

@Test
class TestNGClassTestUseAnnotation {

  @Test
  void myMethod(){

  }
}

@Test(groups ="integration")
abstract class AbstractIntegrationTest2{
}
