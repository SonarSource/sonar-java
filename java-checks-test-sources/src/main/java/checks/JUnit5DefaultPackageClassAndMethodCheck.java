package checks;

import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnit5DefaultPackageClassAndMethodCheck {

  @Test
  public void testPublic() {} // Noncompliant [[sc=3;ec=9]] {{Remove this 'public' modifier.}}

  @Test
  public static void testPublicStatic() {} // Noncompliant {{Remove this 'public' modifier.}}

  @Test
  protected void testProtected() {} // Noncompliant

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

  protected static class PublicWithOneTest { // Noncompliant [[sc=3;ec=12]] {{Remove this 'protected' modifier.}}
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

  public static class TestClassWiuthStaticMethod { // Compliant
    @Test
    void test() {}

    public static Object foo() { // in order to have this method visible from outside, class must be public
      return null;
    }
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
