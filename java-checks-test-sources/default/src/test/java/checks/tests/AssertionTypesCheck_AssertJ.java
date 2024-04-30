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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

public class AssertionTypesCheck_AssertJ {

  @Test
  void test_assertj() {
    assertThat(bytePrimitive())
      .isNotNull(); // Noncompliant {{Change the assertion arguments to not compare a primitive value with null.}}
//     ^^^^^^^^^
    assertThat(shortPrimitive())
      .as("msg")
      .isNotNull() // Noncompliant
      .isNull(); // Noncompliant
    assertThatObject(intPrimitive()).isNull(); // Noncompliant
    assertThat(charPrimitive()).isNotNull(); // Noncompliant
    assertThat(getString()).isNotNull();          // Compliant
    assertThat(longPrimitive()).isNotNull(); // Noncompliant
    assertThat(floatPrimitive()).isNotNull(); // Noncompliant
    assertThat(doublePrimitive()).isNotNull(); // Noncompliant

    assertThat(doublePrimitive())
//             ^^^^^^^^^^^^^^^^^>
      .withFailMessage("msg", 42)
      .isNotEqualTo(null); // Noncompliant {{Change the assertion arguments to not compare dissimilar types.}}
//                  ^^^^

    assertThat(booleanPrimitive())
      .describedAs("msg")
      .isNotEqualTo("foo") // Noncompliant
      .isNotSameAs(null) // Noncompliant
      .isSameAs("bar"); // Noncompliant

    assertThat(doublePrimitive())
      .overridingErrorMessage("msg", 42)
      .isNotEqualTo(null); // Noncompliant
    assertThat(booleanPrimitive()).isNotEqualTo(null); // Noncompliant

    assertThat(intArray()).isNotEqualTo(42); // Noncompliant
    assertThat(intArray()).isNotEqualTo(new int[] {42}); // Compliant
    assertThat(intArray()).isNotEqualTo(null);             // Compliant

    assertThat(intPrimitive()).isNotEqualTo(42);             // Compliant
    assertThat(intPrimitive()).isNotEqualTo(new int[] {42}); // Noncompliant

    Object o = new A();
    A a = new A();
    A a2 = new A();
    B b = new B();
    X x = new X();
    Y y = new Y();
    I1 i1 = new B();
    I2 i2 = new Y();

    assertThat(o).isNotEqualTo(new int[] {42}); // Compliant
    assertThat(a).isNotEqualTo(new int[] {42}); // Noncompliant
    assertThat(a).isNotEqualTo(new A[] {a}); // Noncompliant
    assertThat(new int[] {42}).isNotEqualTo(a); // Noncompliant
    assertThat(new A[] {a}).isNotEqualTo(a); // Noncompliant

    assertThat(new A[] {}).isNotEqualTo(new A[] {}); // Compliant
    assertThat(new B[] {}).isNotEqualTo(new A[] {}); // Compliant
    assertThat(new X[] {}).isNotEqualTo(new A[] {}); // Noncompliant
    assertThat(new A[] {}).isNotEqualTo(new A[][] {}); // Noncompliant
    assertThat(new B[][] {}).isNotEqualTo(new A[][] {}); // Compliant
    assertThat(new X[][] {}).isNotEqualTo(new A[][] {}); // Noncompliant
    assertThat(new A[][][] {}).isNotEqualTo(new A[][] {}); // Noncompliant
    assertThat(new int[][] {}).isNotEqualTo(new int[][] {}); // Compliant
    assertThat(new boolean[][] {}).isNotEqualTo(new int[][] {}); // Noncompliant
    assertThat(new A[][] {}).isNotEqualTo(new int[][] {}); // Noncompliant
    assertThat(new int[][] {}).isNotEqualTo(new A[][] {}); // Noncompliant

    assertThat(new int[] {}).isNotEqualTo(new int[] {}); // Compliant
    assertThat(new int[] {}).isNotEqualTo(new A[] {}); // Noncompliant
    assertThat(new int[] {}).isNotEqualTo(new long[] {}); // Noncompliant

    assertThat(a2).isNotEqualTo(a); // Compliant
    assertThat(new B() {}).isNotEqualTo(b); // Compliant
    assertThat(i1).isNotEqualTo(b); // Compliant
    assertThat(b).isNotEqualTo(i1);// Compliant
    assertThat(new I1() {}).isNotEqualTo(b); // Noncompliant
    assertThat(i2).isNotEqualTo(b); // Noncompliant
    assertThat(b).isNotEqualTo(a); // Compliant
    assertThat(a).isNotEqualTo(b); // Compliant
    assertThat(o).isNotEqualTo(b); // Compliant

    org.assertj.core.api.AssertionsForInterfaceTypes.
      assertThat(x).isNotEqualTo(a); // Noncompliant
    org.assertj.core.api.AssertionsForClassTypes.
      assertThat(x).isEqualTo(a); // Noncompliant

    // Here we are not sure, but it seems valuable to raise an issue that
    // could be a false-positive because the negative assertion is useless by
    // always passing if types are dissimilar
    assertThat(i1).isNotEqualTo(a); // Noncompliant

    // Here we are not sure, but it seems NOT valuable to raise an issue that
    // could be a false-positive because the positive assertion is helpful and
    // always fails if types are dissimilar
    assertThat(i1).isEqualTo(a); // Compliant
    // And in case of final classes, the inheritance is known and final,
    // we can raise issues without having false-positives
    assertThat(i1).isEqualTo(y); // Noncompliant

    assertThat(i2).isNotEqualTo(a); // Noncompliant
    assertThat(i2).isEqualTo(a);      // Compliant
    assertThat(x).isEqualTo(a); // Noncompliant

    assertThat(i1).isNotEqualTo(i1);   // Compliant
    assertThat(i1).isEqualTo(i1);      // Compliant

    assertThat(i2).isNotEqualTo(i1); // Noncompliant
    assertThat(i2).isEqualTo(i1);      // Compliant

    assertThat(i1).isNotEqualTo(y); // Noncompliant

    assertThat(a)
      .extracting("hashCode")
      .isNotEqualTo(42); // Compliant

    int[][] arrayOfArray = new int[0][];
    // assertThat(arrayOfArray).contains(arrayOfArray); // false-negative - removed in latest versions of assertj
    // assertThat(arrayOfArray).doesNotContain(arrayOfArray); // false-negative - removed in latest versions of assertj
    assertThat(arrayOfArray).isIn(arrayOfArray);           // false-negative
  }

  void test_equals_method() {
    A a = new A();
    assertThat(a).isNotEqualTo("foo"); // Compliant, because the name of the test is related to "equals"
                                       // so it's legitimate to compare an object with dissimilar types
  }

  @Test
  void test_assertj_primitives() {
    assertThat(booleanPrimitive()).isEqualTo(true);         // Compliant
    assertThat(booleanPrimitive()).isEqualTo(Boolean.TRUE); // Compliant
    assertThat(booleanPrimitive()).isNotEqualTo(Boolean.FALSE); // Compliant
    assertThat(booleanPrimitive()).isNotEqualTo(false);     // Compliant
    assertThat(booleanPrimitive()).isNotEqualTo(5); // Noncompliant
    assertThat(intPrimitive()).isNotEqualTo(5.0); // Noncompliant
    assertThat(floatPrimitive()).isEqualTo(5);              // Compliant
    assertThat(intPrimitive()).isNotEqualTo(5L); // Noncompliant
    assertThat(longPrimitive()).isEqualTo(5);               // Compliant
    assertThat(floatPrimitive()).isNotEqualTo(5.0d); // Noncompliant
    assertThat(doublePrimitive()).isEqualTo(5.0f);          // Compliant
    assertThat(charPrimitive()).isEqualTo('a');             // Compliant
    assertThat(charPrimitive()).isNotEqualTo(97); // Noncompliant
    assertThat(intPrimitive()).isEqualTo('a');              // Compliant
    assertThat(charPrimitive()).isNotEqualTo("a"); // Noncompliant
    assertThat(getString()).isNotEqualTo('a'); // Noncompliant

    assertThat(Long.valueOf(5)).isEqualTo(Long.valueOf(5));       // Compliant
    assertThat(Long.valueOf(5)).isNotEqualTo(Integer.valueOf(5)); // Noncompliant
    assertThat(Integer.valueOf(5)).isNotEqualTo(Long.valueOf(5)); // Noncompliant
  }

  @Test
  void test_assertj_date_and_time() {
    // AssertJ supports date and time comparison with string.
    assertThat(getDate()).isEqualTo("1970-01-01T01:00:00"); // Compliant
    assertThat(getDate()).isNotEqualTo("2020-01-01T01:00:00"); // Compliant

    assertThat(new Date(0)).isEqualTo("1970-01-01T01:00:00"); // Compliant
    assertThat(LocalDate.parse("1970-01-01")).isEqualTo("1970-01-01"); // Compliant
    assertThat(LocalDateTime.parse("2007-12-03T10:15:30")).isEqualTo("2007-12-03T10:15:30"); // Compliant
    assertThat(ZonedDateTime.parse("2007-12-03T10:15:30+01:00")).isEqualTo("2007-12-03T10:15:30+01:00"); // Compliant
    assertThat(OffsetDateTime.parse("2007-12-03T10:15:30+01:00")).isEqualTo("2007-12-03T10:15:30+01:00"); // Compliant
    assertThat(OffsetTime.parse("10:15:30+01:00")).isEqualTo("10:15:30+01:00"); // Compliant
    assertThat(LocalTime.parse("10:15")).isEqualTo("10:15"); // Compliant
    assertThat(Instant.parse("2007-12-03T10:15:30.00Z")).isEqualTo("2007-12-03T10:15:30.00Z"); // Compliant

    assertThat(getDate().hashCode()).isEqualTo("1970-01-01T01:00:00"); // Noncompliant
    assertThat(OffsetDateTime.timeLineOrder()).isEqualTo("2007-12-03T10:15:30+01:00"); // Noncompliant
    assertThat("1970-01-01T01:00:00").isEqualTo(getDate()); // Noncompliant
    assertThat(getDate()).isSameAs("1970-01-01T01:00:00"); // Noncompliant
  }

  @Test
  void test_assertj_big_integer_and_decimal() {
    // AssertJ supports BigDecimal and BigInteger comparison with string.
    assertThat(BigDecimal.valueOf(123)).isEqualTo("123"); // Compliant
    assertThat(getBigDecimal()).isEqualTo("123"); // Compliant
    assertThat(getBigDecimal()).isEqualTo(BigDecimal.valueOf(123)); // Compliant
    assertThat(getBigDecimal().toString()).isEqualTo("123"); // Compliant
    assertThat(getBigDecimal().hashCode()).isEqualTo("123"); // Noncompliant

    assertThat(BigInteger.valueOf(123)).isEqualTo("123"); // Compliant
    assertThat(getBigInteger()).isEqualTo("123"); // Compliant
    assertThat(getBigInteger()).isEqualTo(BigInteger.valueOf(123)); // Compliant
    assertThat(getBigInteger().toString()).isEqualTo("123"); // Compliant
    assertThat(getBigInteger().hashCode()).isEqualTo("123"); // Noncompliant
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

  Date getDate() {
    return new Date(0);
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
