package checks.tests;

import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnit5DefaultPackageClassAndMethodCheckSample { // Noncompliant {{Remove redundant visibility modifiers from this test class and its methods.}} [[quickfixes=qf1]]
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  // fix@qf1 {{Remove all redundant visibility modifiers}}
  // edit@qf1 [[sl=+12;sc=3;el=+12;ec=10]] {{}}
  // edit@qf1 [[sl=+16;sc=3;el=+16;ec=13]] {{}}
  // edit@qf1 [[sl=+20;sc=3;el=+20;ec=10]] {{}}
  // edit@qf1 [[sl=+24;sc=3;el=+24;ec=10]] {{}}
  // edit@qf1 [[sl=+28;sc=3;el=+28;ec=10]] {{}}
  // edit@qf1 [[sl=+32;sc=3;el=+32;ec=10]] {{}}
  // edit@qf1 [[sl=+36;sc=3;el=+36;ec=13]] {{}}

  @BeforeAll
  public static void beforeAll() {}
//^^^^^^<

  @AfterAll
  protected static void afterAll() {}
//^^^^^^^^^<

  @BeforeEach
  public void beforeEach() {}
//^^^^^^<

  @AfterEach
  public void afterEach() {}
//^^^^^^<

  @Test
  public void testPublic() {}
//^^^^^^<

  @Test
  public static void testPublicStatic() {}
//^^^^^^<

  @Test
  protected void testProtected() {}
//^^^^^^^^^<

  @Test
  private void testPrivate() {} // Compliant, bug raises by S5810

  @Test
  static void testStatic() {} // Compliant, bug raises by S5810

  @Test
  int testReturnValue() { return 0; } // Compliant, bug raises by S5810

  @Test
  void testDefault() {} // Compliant

  public void notATest() {} // Compliant

  void noTestHereEither() {} // Compliant

  @org.junit.Test
  public void testWithJUnit4Annotation() {} // Compliant, JUnit4 not JUnit5

  @Test
  static final void staticFinalTestMethod() {} // Compliant

  public static class WithoutTest { // Compliant

  }

  protected static class PublicWithOneTest { // Noncompliant {{Remove this 'protected' modifier.}} [[quickfixes=qf2]]
//^^^^^^^^^
  // fix@qf2 {{Remove this 'protected' modifier.}}
  // edit@qf2 [[sc=3;ec=13]] {{}}
    @Test
    void test() {}
  }

  public static class WithClassAndMethodModifiers { // Noncompliant {{Remove redundant visibility modifiers from this test class and its methods.}} [[quickfixes=qf3]]
  //                  ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf3 {{Remove all redundant visibility modifiers}}
    // edit@qf3 [[sl=+6;sc=5;el=+6;ec=12]] {{}}
    // edit@qf3 [[sc=3;ec=10]] {{}}
    @Test
    public void test() {}
  }

  private static class PrivateWithOneTest { // Compliant, bug handled by S5810
    @Test
    void test() {}
  }

  static class WithOneTest { // Compliant
    @Test
    void test() {}
  }

  public static class TestClassWithPublicStaticMethod { // Compliant
    @Test
    void test() {}

    public static Object foo() { // in order to have this method visible from outside, class must be public
      return null;
    }
  }

  public static class TestClassWithPublicStaticField { // Compliant

    public static String MY_CONSTANT = "yolo"; // in order to have this field visible from outside, class must be public

    @Test
    void test() {}
  }

  public abstract class AbstractTest {

    @BeforeAll
    public void beforeAll() {} // Compliant, the rule does not report on abstract classes

    @BeforeEach
    public void beforeEach() {} // Compliant, the rule does not report on abstract classes

    @Test
    protected void test_inherited() {
      assertEquals(42, 21 * 2);
    }

    protected abstract void test_abstract();

    @Test
    public void test_to_override() {
      assertEquals(12, 23 - 11);
    }
  }

  interface InterfaceTest {
    void test_to_implement();

    @Test
    default void default_test() {
      assertEquals(49, 7 * 7);
    }
  }


  class ChildTest extends AbstractTest implements InterfaceTest {

    @BeforeAll
    @Override
    public void beforeAll() {} // Compliant, the rule does not report because it overrides a method from a superclass

    @BeforeEach
    @Override
    public void beforeEach() {} // Compliant, the rule does not report because it overrides a method from a superclass

    @Test
    @Override
    public void test_to_implement() {
      assertEquals("aaa", "AAA".toLowerCase(Locale.ROOT));
    }

    @Test
    @Override
    protected void test_abstract() {
      assertTrue("   ".trim().isEmpty());
    }

    @Override
    @Test
    public void test_to_override() {
      super.test_to_override();
      assertEquals(23, 12 + 11);
    }
  }

  Object anonymousClassWithTest = new Object() {
    @Test
    public void test() {} // Compliant, anonymous classes are ignored
  };
}
