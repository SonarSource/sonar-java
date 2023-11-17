package checks.tests.AssertionsInTestsCheck;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class HamcrestTest {

  @Test
  public void noncompliant1() { // Noncompliant
  }

  @Test
  public void hamcrest_assertThat() {
    MatcherAssert.assertThat(1, org.hamcrest.CoreMatchers.is(1));
  }

  @Test
  public void hamcrest_assertThat_with_message() {
    MatcherAssert.assertThat("message", 1, org.hamcrest.CoreMatchers.is(1));
  }

  @Test
  public void hamcrest_assertThat_Object() {
    Object actualValue = "test";
    String requiredValue = "test";
    MatcherAssert.assertThat(actualValue, is(equalTo(requiredValue))); // correct resolution of assertThat method
  }
}
