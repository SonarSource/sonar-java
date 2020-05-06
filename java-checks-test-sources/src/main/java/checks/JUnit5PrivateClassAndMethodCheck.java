package checks;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JUnit5PrivateClassAndMethodCheck {

  @Test
  private void testPrivate() {} // Noncompliant [[sc=3;ec=10]] {{Remove this 'private' modifier.}}

  @Test
  void testDefault() {} // Compliant

  @Test
  protected void testProtected() {} // Compliant

  @Test
  public void testPublic() {} // Compliant

  private void notATest() {} // Compliant

  @org.junit.Test
  private void testWithJUnit4Annotation() {} // Compliant, this rule does not target JUnit4

  @Test
  static final void staticFinalTestMethod() {} // Compliant

  @Nested
  private class PrivateWithoutTest { // Compliant

  }

  @Nested
  private class PrivateWithOneTest { // Noncompliant [[sc=3;ec=10]] {{Remove this 'private' modifier.}}
    @Test
    void test() {}
  }

  @Nested
  public class PublicWithOneTest { // Compliant
    @Test
    void test() {}
  }

  @Nested
  class DefaultWithOneTest { // Compliant
    @Test
    void test() {}
  }

}
