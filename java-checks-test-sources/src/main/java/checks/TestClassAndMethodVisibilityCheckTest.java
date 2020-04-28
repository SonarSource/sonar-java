package checks;

import org.junit.jupiter.api.Test;

class TestClassAndMethodVisibilityCheckTest {

  @Test
  public void testPublic() {} // Noncompliant [[sc=3;ec=9]] {{Remove this access modifier}}

  @Test
  protected void testProtected() {} // Noncompliant

  @Test
  private void testPrivate() {} // Noncompliant

  @Test
  void testDefault() {} // compliant

  public void notATest() {} // compliant

  void noTestHereEither() {} // compliant

  @org.junit.Test
  public void testWithJUnit4Annotation() {} // Compliant

  @Test
  static final void staticFinalTestMethod() {} // Compliant

  public static class WithoutTest { // Compliant

  }

  public static class PublicWithOneTest { // Noncompliant [[sc=3;ec=9]] {{Remove this access modifier}}
    @Test
    void test() {}
  }

  static class WithOneTest { // Compliant
    @Test
    void test() {}
  }
}
