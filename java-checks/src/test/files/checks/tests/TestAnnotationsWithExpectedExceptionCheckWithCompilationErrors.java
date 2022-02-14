package checks;

public class TestAnnotationWithExpectedExceptionWithCompilationErrors {
  @org.junit.Test("foo") // Cover the case where @Test has an unnamed argument
  public void test() {
    org.junit.Assert.assertTrue(true);
  }
}
