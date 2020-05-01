package checks;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExpectedExceptionCheck {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void assertions_before_and_after_expect() throws IOException {
    Assert.assertEquals(1, 1);
    expectedException.expect(IOException.class); // Noncompliant [[sc=23;ec=29;secondary=19,20]] {{Do not test exceptions via ExpectedException before other assertions.}}
    functionThrowingIOException();
    Assert.assertEquals(1, 1);
    Assert.assertNotEquals(2, 3);
  }

  @Test
  public void assertions_before_expect() throws IOException {
    Assert.assertEquals(1, 1);
    expectedException.expect(IOException.class); // Compliant
    functionThrowingIOException();
  }

  @Test
  public void no_other_assertions() throws IOException {
    expectedException.expect(IOException.class); // Compliant
    functionThrowingIOException();

    // coverage, ignore nested classes and lambdas
    Runnable r = () -> Assert.assertEquals(1, 1);
    class A { void f() { Assert.assertEquals(1, 1); } }
  }

  @Test
  public void fail_after_expect() throws IOException {
    expectedException.expect(IOException.class); // Compliant
    functionThrowingIOException();
    Assert.fail("Should not be executed");
  }

  void functionThrowingIOException() throws IOException {
    throw new IOException("(◣_◢)");
  }

  {
    // coverage
    expectedException.expect(IOException.class);
  }

}
