package checks;

public class TestAnnotationWithExpectedExceptionCheck {
  @org.junit.Test(expected = ArrayIndexOutOfBoundsException.class) // Noncompliant [[sc=30;ec=66]] {{Exception testing via JUnit @Test annotation should be avoided.}}
  public void testException() {
    throwingMethod();
  }

  @org.junit.Test(expected = ArrayIndexOutOfBoundsException.class, timeout = 0) // Noncompliant [[sc=30;ec=66]]
  public void testException2() {
    throwingMethod();
  }

  @org.junit.Test(timeout = 0, expected = ArrayIndexOutOfBoundsException.class) // Noncompliant [[sc=43;ec=79]]
  public void testException3() {
    throwingMethod();
  }

  @org.junit.Test // Compliant
  public void testException4() {
    try {
      throwingMethod();
      org.junit.Assert.fail("Expected an exception");
    } catch (RuntimeException ignored) {}
  }

  @org.junit.Test(timeout = 0) // Compliant
  public void testWithTimeout() {}

  @Override // Cover the case that there's an annotation besides @Test
  public String toString() {
    return "foo";
  }

  private void throwingMethod() {
    throw new RuntimeException();
  }
}
