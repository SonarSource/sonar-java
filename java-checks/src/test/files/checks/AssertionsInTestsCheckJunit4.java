import javax.annotation.Nullable;
import org.junit.Test;

public class AssertionsInTestsCheckTest {

  private static int staticMethod() {}

  private static int VAL = staticMethod();

  @Test
  public void compliant1() {
    org.fest.assertions.Fail.fail("foo");
  }

  @Test
  public void compliant2() {
    org.junit.Assert.assertEquals(true, true);
  }

  @Test
  public void compliant3() {
    org.junit.Assert.assertTrue(true);
    org.junit.Assert.assertTrue(true); // Coverage
  }

  @Test
  public void compliant4() {
    org.junit.Assert.assertThat("aaa", org.junit.matchers.JUnitMatchers.containsString("a"));
  }

  @Test
  public void compliant5() {
    org.fest.assertions.Assertions.assertThat(true).isEqualTo(true);
  }

  @Test
  public void nonCompliant1() { // Noncompliant {{Add at least one assertion to this test case.}}
  }

  @Test
  public void nonCompliant2() {  // Noncompliant {{Add at least one assertion to this test case.}}
    org.fest.assertions.Assertions.assertThat(true);  // Fest assertion stub with no checks
    org.fest.assertions.Assertions.assertThat(true).as("foo");  // Fest assertion stub with no checks
    org.fest.assertions.Assertions.assertThat(true).describedAs("foo");  // Fest assertion stub with no checks
    org.fest.assertions.Assertions.assertThat(true).overridingErrorMessage("foo");  // Fest assertion stub with no checks
  }

  @Nullable
  public Test notAtest() {
    compliant1();
  }

}
