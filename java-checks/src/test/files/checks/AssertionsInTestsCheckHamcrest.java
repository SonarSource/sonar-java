import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.Rule;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertionsInTestsCheckTest {

  @Test
  public void noncompliant1() { // Noncompliant
  }

  @Test
  public void fakeTest() {
    org.hamcrest.MatcherAssert.assertThat(1, org.hamcrest.CoreMatchers.is(1));
  }

  @Test
  public void fakeTest2() {
    org.hamcrest.MatcherAssert.assertThat("message", 1, org.hamcrest.CoreMatchers.is(1));
  }

}
