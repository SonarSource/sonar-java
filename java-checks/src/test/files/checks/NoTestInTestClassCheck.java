import java.lang.Deprecated;
import org.junit.experimental.runners.Enclosed;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;
import org.junit.runner.Suite;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import com.googlecode.zohhak.api.TestWith;
import com.googlecode.zohhak.api.runners.ZohhakRunner;


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

@org.junit.runner.RunWith(cucumber.api.junit.Cucumber.class)
public class MyCucumberTest { // should not raise an issue
}
@RunWith(Cucumber.class)
public class MyCucumber2Test { // no issue
}

@RunWith(MyRunner.class)
public class MyCucumber3Test { // Noncompliant - not recognized
}
@RunWith(getRunner())
public class MyCucumber4Test { // Noncompliant - does not compile, not a class literal
  public Class<? extends Runner> getRunner() {
    return null;
  }
}
@RunWith(value1= MyRunner.class, value2= YourRunner.class)
public class MyCucumber5Test { // Noncompliant - does not compile, not a class literal
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

@RunWith(Suite.class)
@Suite.SuiteClasses(value = { S2187Test.Test1.class, S2187Test.Test2.class })
public class S2187Test {

  public static class Test1 {

    @Test
    public void test() {
      Assert.assertTrue(true);
    }
  }

  public static class Test2 {

    @Test
    public void test() {
      Assert.assertTrue(true);
    }
  }

}

@RunWith(Theories.class)
public class MyTheorieClassTest {
  @Theory
  public void test_method() {

  }
}

public class Junit5MetaAnnotationTest {

  @Test
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Bar {

  }

  @Bar
  public void test() {

  }

}

public class JUnit5InheritedDefaultMethodsTest implements TestA { // Compliant

}

interface TestA {

  @Test
  default void method1() {}
}

public class CrazyHierarchyTest extends AbstractCrazyHierarchyTest { } // Compliant, contains test from TestA interface

abstract class AbstractCrazyHierarchyTest implements TestB { }

interface TestB extends TestA { }

class MyUnitTest { // Compliant
  @org.junit.jupiter.params.ParameterizedTest
  void foo() {
    assertThat(plop);
  }
}

class CustomAnnotationTest {
  @CustomAnnotation
  void foo() {}
}

@org.junit.platform.commons.annotation.Testable
@interface CustomAnnotation {}

class NestedTest { // Compliant
  @Nested
  class NestedClass {
    @Test
    public void foo() {
      Assert.assertTrue(true);
    }
  }
}

class NoTestsInNestedTest { // Noncompliant {{Add some tests to this class.}}
  @Nested
  class NestedClass {
    public void foo() {
      Assert.assertTrue(true);
    }
  }
}

class SomeTest implements SomeInterface { }// Noncompliant {{Add some tests to this class.}}

interface SomeInterface {
  class Foo implements SomeInterface { }
}


@RunWith(ZohhakRunner.class)
public class MyZohhakTest { // Noncompliant
}

@RunWith(ZohhakRunner.class)
public class MyZohhak2Test { // Compliant, Zohhak uses @TestWith
  @TestWith({
    "1, 2",
    "3, 4"
  })
  public void testFoo1(int p1, int p2) {
  }
}

@RunWith(ZohhakRunner.class)
public class MyZohhak3Test { // Compliant, Zohhak uses @TestWith
  @TestWith(value=" 7 = 7 > 5 => true", separator="=>")
  public void testFoo3(String string, boolean bool) {
  }
}

@RunWith(ZohhakRunner.class)
public class MyZohhak4Test { // Compliant
  @Test
  public void testFoo4() {
  }
}
