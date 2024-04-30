package checks.tests;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AssertionTypesCheck_JUnit5 {

  @Test
  void test_junit() {
    assertNotNull(bytePrimitive()); // Noncompliant {{Change the assertion arguments to not compare a primitive value with null.}}
//                ^^^^^^^^^^^^^^^
    assertNotNull(shortPrimitive(), "msg"); // Noncompliant
    assertNotNull(intPrimitive(), () -> "msg"); // Noncompliant
    assertNotNull(charPrimitive()); // Noncompliant
    assertNotNull(getString(), "msg"); // Compliant

    assertNotNull(null);

    assertNull(longPrimitive()); // Noncompliant
    assertNull(floatPrimitive(), "msg"); // Noncompliant
    assertNull(doublePrimitive(), () -> "msg"); // Noncompliant

    assertEquals(
      null, // Noncompliant {{Change the assertion arguments to not compare dissimilar types.}}
//    ^^^^
      doublePrimitive());
//    ^^^^^^^^^^^^^^^^^<
    assertEquals(null, booleanPrimitive(), "msg"); // Noncompliant

    assertNotEquals(null, doublePrimitive()); // Noncompliant
    assertNotEquals(null, booleanPrimitive(), "msg"); // Noncompliant

    assertNotEquals(42, intArray()); // Noncompliant
    assertNotEquals(new int[] {42}, intPrimitive()); // Noncompliant
    assertNotEquals(null, intArray()); // Compliant, arrays can be null
    assertNotEquals(new int[] {42}, null); // Compliant

    Object o = new A();
    A a = new A();
    A a2 = new A();
    B b = new B();
    X x = new X();
    Y y = new Y();
    I1 i1 = new B();
    I2 i2 = new Y();

    assertNotEquals(new int[] {42}, o); // Compliant
    assertNotEquals(new int[] {42}, a); // Noncompliant

    assertNotEquals(new A[] {}, new A[] {});     // Compliant
    assertNotEquals(new A[] {}, new B[] {});     // Compliant
    assertNotEquals(new A[] {}, new X[] {}); // Noncompliant
    assertNotEquals(new A[][] {}, new A[] {}); // Noncompliant
    assertNotEquals(new A[][] {}, new B[][] {}); // Compliant
    assertNotEquals(new A[][] {}, new X[][] {}); // Noncompliant

    assertNotEquals(new int[] {}, new int[] {}); // Compliant
    assertNotEquals(new A[] {}, new int[] {}); // Noncompliant
    assertNotEquals(new long[] {}, new int[] {}); // Noncompliant

    assertNotEquals(a, a2);         // Compliant
    assertNotEquals(b, new B(){});  // Compliant
    assertNotEquals(b, i1);         // Compliant
    assertNotEquals(i1, b);         // Compliant
    assertNotEquals(b, new I1(){}); // Noncompliant
    assertNotEquals(b, i2); // Noncompliant
    assertNotEquals(a, b);          // Compliant
    assertNotEquals(b, a);          // Compliant
    assertNotEquals(b, o);          // Compliant

    assertNotEquals(a, x); // Noncompliant
    assertEquals(a, x); // Noncompliant

    // Here we are not sure, but it seems valuable to raise an issue that
    // could be a false-positive because the negative assertion is useless by
    // always passing if types are dissimilar
    assertNotEquals(a, i1); // Noncompliant
    // example of false-positive:
    assertNotEquals((A) b, (I1) new B()); // Noncompliant

    // Here we are not sure, but it seems NOT valuable to raise an issue that
    // could be a false-positive because the positive assertion is helpful and
    // always fails if types are dissimilar
    assertEquals(a, i1);         // Compliant
    // example of test that can pass:
    assertEquals((A) b, (I1) b); // Compliant
    // And in case of final classes, the inheritance is known and final,
    // we can raise issues without having false-positives
    assertEquals(y, i1); // Noncompliant

    assertNotEquals(a, i2); // Noncompliant
    assertEquals(a, i2);            // Compliant
    assertEquals(a, x); // Noncompliant

    assertNotEquals(i1, i1);         // Compliant
    assertEquals(i1, i1);            // Compliant

    assertNotEquals(i1, i2); // Noncompliant
    assertEquals(i1, i2);            // Compliant

    assertNotEquals(y, i1); // Noncompliant

    assertNotEquals(42, Integer.valueOf(42)); // Compliant
    assertNotEquals(42L, Long.valueOf(42L)); // Compliant

    assertEquals(true, booleanPrimitive());       // Compliant
    assertEquals(Boolean.TRUE, booleanPrimitive());        // Compliant
    assertNotEquals(Boolean.FALSE, booleanPrimitive());    // Compliant
    assertNotEquals(false, booleanPrimitive()); // Compliant
    assertNotEquals(5, booleanPrimitive()); // Noncompliant
    assertNotEquals(5.0, intPrimitive());       // Compliant
    assertEquals(5, floatPrimitive());            // Compliant
    assertNotEquals(5L, intPrimitive());        // Compliant
    assertEquals(5, longPrimitive());             // Compliant
    assertNotEquals(5.0d, floatPrimitive());    // Compliant
    assertEquals(5.0f, doublePrimitive());        // Compliant
    assertEquals('a', charPrimitive());           // Compliant
    assertNotEquals(97, charPrimitive());       // Compliant
    assertEquals('a', intPrimitive());            // Compliant
    assertNotEquals("a", charPrimitive()); // Noncompliant
    assertNotEquals('a', getString()); // Noncompliant

    assertEquals(Long.valueOf(5), Long.valueOf(5));       // Compliant
    assertNotEquals(Integer.valueOf(5), Long.valueOf(5)); // Noncompliant
    assertNotEquals(Long.valueOf(5), Integer.valueOf(5)); // Noncompliant
  }

  @Test
  void test_date_and_time() {
    // JUnit does not support date and time comparison as AssertJ
    assertEquals(new Date(0), "1970-01-01T01:00:00"); // Noncompliant
    assertEquals(LocalDate.parse("1970-01-01"), "1970-01-01"); // Noncompliant
    assertEquals(LocalDateTime.parse("2007-12-03T10:15:30"), "2007-12-03T10:15:30"); // Noncompliant
    assertEquals(ZonedDateTime.parse("2007-12-03T10:15:30+01:00"), "2007-12-03T10:15:30+01:00"); // Noncompliant
    assertEquals(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"), "2007-12-03T10:15:30+01:00"); // Noncompliant
    assertEquals(OffsetTime.parse("10:15:30+01:00"), "10:15:30+01:00"); // Noncompliant
    assertEquals(LocalTime.parse("10:15"), "10:15"); // Noncompliant
    assertEquals(Instant.parse("2007-12-03T10:15:30.00Z"), "2007-12-03T10:15:30.00Z"); // Noncompliant
  }

  @Test
  void test_assertj_big_integer_and_decimal() {
    // JUnit does not support BigDecimal and BigInteger comparison with string, unlike AssertJ.
    assertEquals(getBigDecimal(), "123"); // Noncompliant
    assertEquals(getBigDecimal(), BigDecimal.valueOf(123)); // Compliant

    assertEquals(getBigInteger(), "123"); // Noncompliant
    assertEquals(getBigInteger(), BigInteger.valueOf(123)); // Compliant
  }

  void test_equals_method() {
    A a = new A();
    assertNotEquals(a, "foo"); // Compliant, because the name of the test is related to "equals"
                               // so it's legitimate to compare an object with dissimilar types
  }

  interface I1 {
  }

  interface I2 {
  }

  static class A {
  }

  static class B extends A implements I1 {
  }

  static class X {
  }

  static final class Y extends X implements I2 {
  }

  byte bytePrimitive() {
    return 1;
  }

  short shortPrimitive() {
    return 1;
  }

  int intPrimitive() {
    return 1;
  }

  long longPrimitive() {
    return 1;
  }

  float floatPrimitive() {
    return 1;
  }

  double doublePrimitive() {
    return 1;
  }

  boolean booleanPrimitive() {
    return true;
  }

  char charPrimitive() {
    return 'a';
  }

  String getString() {
    return "a";
  }

  BigDecimal getBigDecimal() {
    return BigDecimal.valueOf(123);
  }

  BigInteger getBigInteger() {
    return BigInteger.valueOf(123);
  }

  Character getCharacter() {
    return 'a';
  }

  int[] intArray() {
    return new int[0];
  }

}
