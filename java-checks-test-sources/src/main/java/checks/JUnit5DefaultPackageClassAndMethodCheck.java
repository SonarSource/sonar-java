package checks;

import org.junit.jupiter.api.Test;

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
}
