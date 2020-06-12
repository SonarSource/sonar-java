package checks.tests;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AssertionCompareToSelfCheck_JUnit4 {

  @Test
  void test_object() {
    Object actual = new Object();
    Object expected = new Object();
    assertEquals(expected, actual); // Compliant
    assertEquals("message", expected, actual); // Compliant
    assertEquals(actual, actual); // Noncompliant [[sc=18;ec=24]] {{Replace this assertion to not have the same actual and expected expression.}}
    assertEquals("message", actual, actual); // Noncompliant
  }

  @Test
  void test_array() {
    Object[] actual = new Object[0];
    Object[] expected = new Object[0];
    assertArrayEquals(expected, actual); // Compliant
    assertArrayEquals("message", expected, actual); // Compliant
    assertArrayEquals(actual, actual); // Noncompliant
    assertArrayEquals("message", actual, actual); // Noncompliant

    assertEquals(actual.length, actual.length); // Noncompliant

    // When there's a method invocation in the actual and expected expression,
    // we are not sure if the result will be the same, we don't try to identify pure functions
    assertEquals(actual.hashCode(), actual.hashCode()); // false-negative, limitation
  }

}
