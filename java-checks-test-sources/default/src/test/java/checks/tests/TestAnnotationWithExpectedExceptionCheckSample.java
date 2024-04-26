package checks.tests;

public class TestAnnotationWithExpectedExceptionCheckSample {
  @org.junit.Test(expected = ArrayIndexOutOfBoundsException.class) // Compliant, no assertions used
  public void testException() {
    throwingMethod();
  }

  @org.junit.Test(expected = ArrayIndexOutOfBoundsException.class) // Noncompliant {{Move assertions into separate method or use assertThrows or try-catch instead.}}
//                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  public void testException2() {
    throwingMethod();
    org.junit.Assert.assertTrue(true);
//  ^^^<
    org.junit.Assert.assertTrue(true);
//  ^^^<
  }

  @org.junit.Test(expected = ArrayIndexOutOfBoundsException.class, timeout = 0) // Noncompliant
  public void testException3() {
    throwingMethod();
    org.junit.Assert.assertTrue(true);
  }

  @org.junit.Test(timeout = 0, expected = ArrayIndexOutOfBoundsException.class) // Noncompliant
  public void testException4() {
    throwingMethod();
    org.junit.Assert.assertTrue(true);
  }

  @org.junit.Test // Compliant
  public void testException5() {
    try {
      throwingMethod();
      org.junit.Assert.fail("Expected an exception");
    } catch (RuntimeException ignored) {}
  }

  @org.junit.Test(timeout = 0) // Compliant, no expected exception
  public void testWithTimeout() {
    org.junit.Assert.assertTrue(true);
  }

  @Override // Cover the case that there's an annotation besides @Test
  @org.junit.Test // Compliant, no expected exception
  public String toString() {
    org.junit.Assert.assertTrue(true);
    return "foo";
  }

  @Override // Cover the case that there's an annotation besides @Test and an expected exception is used
  @org.junit.Test(expected = ArrayIndexOutOfBoundsException.class) // Noncompliant
  public int hashCode() {
    org.junit.Assert.assertTrue(true);
    return 42;
  }

  private void throwingMethod() {
    throw new RuntimeException();
  }
}
