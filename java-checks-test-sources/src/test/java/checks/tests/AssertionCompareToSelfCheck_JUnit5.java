package checks.tests;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

public class AssertionCompareToSelfCheck_JUnit5 {

  @Test
  void test_object() {
    Object actual = new Object();
    Object expected = new Object();
    assertEquals(expected, actual); // Compliant
    assertEquals(expected, actual, "message"); // Compliant
    assertEquals(actual, actual); // Noncompliant [[sc=18;ec=24]] {{Replace this assertion to not have the same actual and expected expression.}}
    assertEquals(actual, actual, "message"); // Noncompliant
    assertEquals(actual, actual, () -> "message"); // Noncompliant
  }

  @Test
  void test_array() {
    Object[] actual = new Object[0];
    Object[] expected = new Object[0];
    assertArrayEquals(expected, actual); // Compliant
    assertArrayEquals(expected, actual, "message"); // Compliant
    assertArrayEquals(actual, actual); // Noncompliant
    assertArrayEquals(actual, actual, "message"); // Noncompliant

    assertArrayEquals((new Object[]{new Object()}), (new Object[]{new Object()})); // Compliant
    assertArrayEquals(new Object[]{1}, new Object[]{1}); // Noncompliant
    assertArrayEquals(new Object[]{1, new Object()}, new Object[]{1, new Object()}); // Compliant
    assertArrayEquals(new Object[4], new Object[4]); // Noncompliant
    assertArrayEquals(new Object[]{}, new Object[]{}); // Noncompliant
  }

  @Test
  void test_list() {
    List<String> actual = Collections.emptyList();
    List<String> expected = Collections.emptyList();
    assertIterableEquals(expected, actual); // Compliant
    assertIterableEquals(expected, actual, "message"); // Compliant
    assertIterableEquals(actual, actual); // Noncompliant
    assertLinesMatch(actual, actual); // Noncompliant
  }

}
