package checks;

import org.junit.jupiter.api.Test;

class TestClassAndMethodVisibilityCheckTest {

  @Test
  public void testPublic() {} // Noncompliant [[sc=3;ec=9]] {{Remove this access modifier}}

  @Test
  protected void testProtected() {} // Noncompliant [[sc=3;ec=12]] {{Remove this access modifier}}

  @Test
  private void testPrivate() {} // Noncompliant [[sc=3;ec=10]] {{Remove this access modifier}}

  @Test
  void testDefault() {} // compliant

  public void noTestHere() {} // compliant - this rule only applies to test methods

  void noTestHereEither() {} // compliant
}
