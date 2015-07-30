import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.Rule;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertionsInTestsCheckTest {

  @Rule
  public final JUnitSoftAssertions jsoftly = new JUnitSoftAssertions();

  @Test
  public void noncompliant1() { // Noncompliant
    assertThat("a").as("aaa");
  }
  @Test
  public void noncompliant2() { // Noncompliant
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(5).isLessThan(3);
    softly.assertThat(1).isGreaterThan(2);
  }
  @Test
  public void noncompliant3() { // Noncompliant
    jsoftly.assertThat(3);
  }

  @Test
  public void compliant1a() {
    org.assertj.core.api.Assertions.assertThat("a").hasSize(1);
  }
  @Test
  public void compliant1b() {
    assertThat("a").hasSize(1);
  }
  @Test
  public void compliant2() {
    org.assertj.core.api.Assertions.fail("a");
  }
  @Test
  public void compliant3() {
    org.assertj.core.api.Assertions.fail("a", new IllegalArgumentException());
  }
  @Test
  public void compliant4() {
    org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
  }
  @Test
  public void compliant5() {
    org.assertj.core.api.Fail.fail("failure");
  }
  @Test
  public void compliant6() {
    org.assertj.core.api.Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
  }
  @Test
  public void compliant7() {
    org.assertj.core.api.Fail.shouldHaveThrown(IllegalArgumentException.class);
  }
  @Test
  public void compliant8() {
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(5).isLessThan(3);
    softly.assertAll();
  }
  @Test
  public void compliant9() {
    jsoftly.assertThat(5).isLessThan(3);
  }
  @Test
  public void compliant10() {
    SoftAssertions softly = jsoftly;
    softly.assertThat(5).isLessThan(3);
  }

}
