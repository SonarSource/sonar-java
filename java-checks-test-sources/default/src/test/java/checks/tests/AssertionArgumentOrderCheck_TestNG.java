package checks.tests;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class AssertionArgumentOrderCheck_TestNG {

  @Test
  void test() {
    assertSame(actualString(), "abc"); // Compliant
    assertSame(actualString(), "abc", "message"); // Compliant
    assertSame(
      "abc", // Noncompliant {{Swap these 2 arguments so they are in the correct order: actual value, expected value.}}
//    ^^^^^
      actualString());
//    ^^^^^^^^^^^^^^<
    assertSame("abc", actualString(), "message"); // Noncompliant

    assertNotSame(actualString(), "abc"); // Compliant
    assertNotSame("abc", actualString()); // Noncompliant

    assertEquals(actualInt(), 42); // Compliant
    assertEquals(actualInt(), 42, "message"); // Compliant
    assertEquals(42, actualInt()); // Noncompliant
    assertEquals(42, actualInt(), "message"); // Noncompliant

    assertNotEquals(actualBoolean(), true); // Compliant
    assertNotEquals(true, actualBoolean()); // Noncompliant

    assertEqualsNoOrder(actualArray(), new Object[] {""}); // Compliant
    assertEqualsNoOrder(new Object[] {""}, actualArray()); // false-negative, limitation of the rule regarding arrays of literals

    assertTrue(actualBoolean(), "message");
  }

  int actualInt() {
    return 0;
  }

  String actualString() {
    return "";
  }

  boolean actualBoolean() {
    return true;
  }

  Object[] actualArray() {
    return new Object[0];
  }

}
