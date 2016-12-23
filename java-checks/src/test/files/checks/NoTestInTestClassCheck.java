import java.lang.Deprecated;
import org.junit.experimental.runners.Enclosed;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;
import static org.assertj.core.api.Assertions.assertThat;

class A extends junit.framework.TestCase {
  void testFoo() {
  }
}

public class JUnit3Test extends junit.framework.TestCase {
  public void testNothing() {
    assertTrue(true);
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

class BaseTest {
  @Test
  public void test() {
  }
}

class InterTest extends BaseTest {
}

class ImplTest extends BaseTest {
}

class OtherTest extends BaseTest {
  @Test
  public void test2() {
  }
}
@org.junit.runner.RunWith(org.junit.experimental.runners.Enclosed.class)
public class MyNewTest { // should not raise an issue
}
@RunWith(Enclosed.class)
public class MyNew2Test { // no issue
}
@org.junit.runner.RunWith(cucumber.api.junit.Cucumber.class)
public class MyCucumberTest { // should not raise an issue
}
@RunWith(Cucumber.class)
public class MyCucumber2Test { // no issue
}

public class CTest {
  @org.junit.jupiter.api.Test // no issue, junit5 annotation
  public void testFoo() {
    assertThat(new A().foo(null)).isEqualTo(0);
  }
}
public class DTest { // Noncompliant {{Add some tests to this class.}}
  public void testFoo() {
    assertThat(new A().foo(null)).isEqualTo(0);
  }
}
