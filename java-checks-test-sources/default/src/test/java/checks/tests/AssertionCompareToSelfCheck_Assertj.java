package checks.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

public class AssertionCompareToSelfCheck_Assertj {

  @Test
  void test_object() {
    Object actual = new Object();
    Object expected = new Object();
    assertThat(actual).isEqualTo(expected); // Compliant
    assertThat(
      actual
      ).isEqualTo(actual); // Noncompliant [[sc=19;ec=25;secondary=22]] {{Replace this assertion to not have the same actual and expected expression.}}
    assertThatObject(actual).as("message").isEqualTo(actual);   // Noncompliant
    assertThat(actual).describedAs("message").isEqualTo(actual);   // Noncompliant
    assertThat(actual).withFailMessage("message", 1, 2).isEqualTo(actual);   // Noncompliant
    assertThat(actual).overridingErrorMessage("message", 1, 2).isEqualTo(actual);   // Noncompliant
    assertThat(getRandomObjects()).isEqualTo(getRandomObjects()); // Compliant, actual and expected could be different
    assertThat(getRandomObjects().length).isEqualTo(getRandomObjects().length); // Compliant
    assertThat(new Random()).isEqualTo(new Random()); // Compliant
    assertThatObject(null).isEqualTo(null); // Noncompliant
    assertThat(actual).extracting("name").isEqualTo(actual); // Compliant, contain's "extracting"

    ObjectAssert<Object> assertion = assertThat(actual);
    assertion.isEqualTo(actual);   // false-negative, only support chained methods
  }

  @Test
  void testEquals() {
    Object object = new Object();
    assertThat(object).isEqualTo(object); // Compliant
  }

  @Test
  void test_hashCode() {
    Object object = new Object();
    assertThat(object).hasSameHashCodeAs(object); // Compliant
  }

  @Test
  void test_object_methods() {
    Object object = new Object();
    assertThat(object).isEqualTo(object); // Compliant
    assertThat(object).hasSameHashCodeAs(object); // Compliant
  }

  @Test
  void test_string() {
    String actual = "foo";
    String expected = "foo";
    assertThat(actual).contains(expected); // Compliant
    assertThat(actual).contains(actual);   // Noncompliant
    assertThat(actual).containsIgnoringCase(actual);   // Noncompliant
    assertThat(actual).doesNotContain(actual); // Noncompliant
    assertThat(actual).containsSequence(actual); // Noncompliant
    assertThat(actual).containsSubsequence(actual); // Noncompliant
    assertThat(actual).endsWith(actual); // Noncompliant
    assertThat(actual).hasSameClassAs(actual); // Noncompliant
    assertThat(actual).hasSameHashCodeAs(actual); // Noncompliant
    assertThat(actual).hasSameSizeAs(actual); // Noncompliant
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
    assertThat(actual).contains(actual);   // Noncompliant
    assertThat(actual).containsAll(actual);  // Noncompliant
    assertThat(actual).containsAnyOf(actual);  // Noncompliant
    assertThat(actual).containsOnly(actual);  // Noncompliant
    assertThat(actual).containsOnlyElementsOf(actual);  // Noncompliant
    assertThat(actual).hasSameElementsAs(actual);  // Noncompliant
    assertThat(actual).hasSameHashCodeAs(actual);  // Noncompliant
    assertThat(actual).hasSameSizeAs(actual);  // Noncompliant
    assertThat(actual).isEqualTo(actual);  // Noncompliant
    assertThat(actual).isSameAs(actual);  // Noncompliant

    assertThat(actual).contains(); // Compliant
    assertThat(actual).containsAnyOf();  // Compliant
    assertThat(actual).containsOnly();  // Compliant
  }

  @Test
  void test_map() {
    Map<Object, Object> actual = new HashMap<>();
    Map<Object, Object> expected = new HashMap<>();
    assertThat(actual).containsAllEntriesOf(expected); // Compliant
    assertThat(actual).containsAllEntriesOf(actual);   // Noncompliant
    assertThat(actual).containsExactlyInAnyOrderEntriesOf(actual); // Noncompliant
  }

  Object[] getRandomObjects() {
    return new Object[0];
  }

  // coverage
  Object fieldA = new Object();
  Object fieldB = assertThat(fieldA).isEqualTo(fieldA); // Noncompliant

}
