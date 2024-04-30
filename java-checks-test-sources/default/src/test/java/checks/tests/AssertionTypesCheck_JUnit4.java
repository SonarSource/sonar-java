package checks.tests;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AssertionTypesCheck_JUnit4 {

  @Test
  void test_junit() {
    assertNotNull("message", bytePrimitive()); // Noncompliant {{Change the assertion arguments to not compare a primitive value with null.}}
//                           ^^^^^^^^^^^^^^^
    assertNotNull(shortPrimitive()); // Noncompliant
    assertNotNull(intPrimitive()); // Noncompliant
    assertNotNull(charPrimitive()); // Noncompliant
    assertNotNull(getString());        // Compliant
    assertNotNull("msg", getString()); // Compliant

    assertNotNull("msg", null); // Compliant

    assertNull(longPrimitive()); // Noncompliant
    assertNull("msg", floatPrimitive()); // Noncompliant
    assertNull("msg", doublePrimitive()); // Noncompliant

    assertEquals(
      null, // Noncompliant {{Change the assertion arguments to not compare dissimilar types.}}
//    ^^^^
      doublePrimitive());
//    ^^^^^^^^^^^^^^^^^<
    assertEquals(
      doublePrimitive(), // Noncompliant
      null);
    assertEquals(null, null);
    assertEquals("msg", null, booleanPrimitive()); // Noncompliant

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

    // Here we are not sure, but it seems NOT valuable to raise an issue that
    // could be a false-positive because the positive assertion is helpful and
    // always fails if types are dissimilar
    assertEquals(a, i1);             // Compliant
    // And in case of final classes, the inheritance is known and final,
    // we can raise issues without having false-positives
    assertEquals(y, i1); // Noncompliant

    assertNotEquals(a, i2); // Noncompliant
    assertEquals(a, i2);             // Compliant
    assertEquals(a, x); // Noncompliant

    assertNotEquals(i1, i1);         // Compliant
    assertEquals(i1, i1);            // Compliant

    assertNotEquals(i1, i2); // Noncompliant
    assertEquals(i1, i2);            // Compliant

    assertNotEquals(y, i1); // Noncompliant

    assertEquals(true, booleanPrimitive());              // Compliant
    assertEquals(Boolean.TRUE, booleanPrimitive());      // Compliant
    assertNotEquals(Boolean.FALSE, booleanPrimitive());  // Compliant
    assertNotEquals(false, booleanPrimitive());          // Compliant
    assertNotEquals(5, booleanPrimitive()); // Noncompliant
    assertNotEquals(5.0, intPrimitive()); // Noncompliant
    assertEquals(5, floatPrimitive());                   // Compliant
    assertNotEquals(5L, intPrimitive());                 // Compliant
    assertEquals(5, longPrimitive());                    // Compliant
    assertNotEquals(5.0d, floatPrimitive()); // Noncompliant
    assertEquals(5.0f, doublePrimitive(), 0.01);         // Compliant
    assertEquals('a', charPrimitive());                  // Compliant
    assertNotEquals(97, charPrimitive());                // Compliant
    assertEquals('a', intPrimitive());                   // Compliant
    assertNotEquals("a", charPrimitive()); // Noncompliant
    assertNotEquals('a', getString()); // Noncompliant

    assertEquals(Long.valueOf(5), Long.valueOf(5));       // Compliant
    assertNotEquals(Integer.valueOf(5), Long.valueOf(5)); // Noncompliant
    assertNotEquals(Long.valueOf(5), Integer.valueOf(5)); // Noncompliant
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

  Character getCharacter() {
    return 'a';
  }

  int[] intArray() {
    return new int[0];
  }

}
