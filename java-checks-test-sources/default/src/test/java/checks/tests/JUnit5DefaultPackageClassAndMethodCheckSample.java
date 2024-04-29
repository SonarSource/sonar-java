package checks.tests;

import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnit5DefaultPackageClassAndMethodCheckSample {

  @Test
  public void testPublic() {} // Noncompliant [[quickfixes=qf1]] {{Remove this 'public' modifier.}}
//^^^^^^
  // fix@qf1 {{Remove "public" modifier}}
  // edit@qf1 [[sc=3;ec=10]] {{}}

  @Test
  public static void testPublicStatic() {} // Noncompliant [[quickfixes=qf2]] {{Remove this 'public' modifier.}}
//^^^^^^
  // fix@qf2 {{Remove "public" modifier}}
  // edit@qf2 [[sc=3;ec=10]] {{}}

  @Test
  protected void testProtected() {} // Noncompliant [[quickfixes=qf3]]
//^^^^^^^^^
  // fix@qf3 {{Remove "protected" modifier}}
  // edit@qf3 [[sc=3;ec=13]] {{}}

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

  protected static class PublicWithOneTest { // Noncompliant [[quickfixes=qf4]] {{Remove this 'protected' modifier.}}
//^^^^^^^^^
  // fix@qf4 {{Remove "protected" modifier}}
  // edit@qf4 [[sc=3;ec=13]] {{}}
    @Test
    void test() {}
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
}
