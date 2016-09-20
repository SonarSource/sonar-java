import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class AssertionsInTestsCheckHamcrest {

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

}
