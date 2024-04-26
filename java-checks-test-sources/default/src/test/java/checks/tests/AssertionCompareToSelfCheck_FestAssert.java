package checks.tests;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.fest.assertions.Assertions.assertThat;

public class AssertionCompareToSelfCheck_FestAssert {

  @Test
  void test_object() {
    Object actual = new Object();
    Object expected = new Object();
    assertThat(actual).isEqualTo(expected); // Compliant
    assertThat(actual).isEqualTo(actual); // Noncompliant {{Replace this assertion to not have the same actual and expected expression.}}
//                               ^^^^^^
    assertThat(actual).as("message").isEqualTo(actual); // Noncompliant
    assertThat(actual).describedAs("message").isEqualTo(actual); // Noncompliant
    assertThat(actual).overridingErrorMessage("message").isEqualTo(actual); // Noncompliant
    assertThat(getRandomObjects()).isEqualTo(getRandomObjects()); // Compliant, actual and expected could be different
    assertThat(getRandomObjects().length).isEqualTo(getRandomObjects().length); // Compliant, actual and expected could be different
  }

  @Test
  void test_string() {
    String actual = "foo";
    String expected = "foo";
    assertThat(actual).contains(expected); // Compliant
    assertThat(actual).contains(actual); // Noncompliant
    assertThat(actual).containsIgnoringCase(actual); // Noncompliant
    assertThat(actual).doesNotContain(actual); // Noncompliant
    assertThat(actual).endsWith(actual); // Noncompliant
    assertThat(actual).isEqualTo(actual); // Noncompliant
    assertThat(actual).isEqualToIgnoringCase(actual); // Noncompliant
    assertThat(actual).isSameAs(actual); // Noncompliant
    assertThat(actual).startsWith(actual); // Noncompliant
  }

  @Test
  void test_list() {
    List<Object> actual = new ArrayList<>();
    Object expected = new Object();
    assertThat(actual).contains(expected); // Compliant
    assertThat(actual).contains(actual); // Noncompliant
    assertThat(actual).containsExactly(actual); // Noncompliant
    assertThat(actual).containsOnly(actual); // Noncompliant
    assertThat(actual).isEqualTo(actual); // Noncompliant
    assertThat(actual).isSameAs(actual); // Noncompliant

    assertThat(actual).contains(); // Compliant
    assertThat(actual).containsExactly();  // Compliant
    assertThat(actual).containsOnly(); // Compliant
  }

  Object[] getRandomObjects() {
    return new Object[0];
  }
}
