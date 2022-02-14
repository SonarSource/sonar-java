package checks;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AssertionTypesCheck_JUnit4 {

  @Test
  void test_junit() {
    assertNotNull(intPrimitive()); // Noncompliant

    assertNotNull("message", unknown());
    assertNotNull(unknown());
    assertEquals(null, unknown());
    assertEquals("msg", null, unknown());
    assertNotEquals(null, unknown());

    Object o = new A();
    A a = new A();
    A a2 = new A();
    B b = new B();
    X x = new X();
    Y y = new Y();
    I1 i1 = new B();
    I2 i2 = new Y();

    assertNotEquals(new int[] {42}, o);
    assertNotEquals(new int[] {42}, a); // Noncompliant

    assertNotEquals(new A[] {}, new A[] {});
    assertNotEquals(new A[] {}, new B[] {});
    assertNotEquals(new A[] {}, new X[] {}); // Noncompliant
    assertNotEquals(new X[] {}, new A[] {}); // Noncompliant
    assertNotEquals(new A[][] {}, new A[] {}); // Noncompliant
    assertNotEquals(new A[][] {}, new B[][] {});
    assertNotEquals(new A[][] {}, new X[][] {}); // Noncompliant

    assertNotEquals(new int[] {}, new int[] {});
    assertNotEquals(new A[] {}, new int[] {});    // Noncompliant
    assertNotEquals(new long[] {}, new int[] {}); // Noncompliant

    assertNotEquals(a, a2);
    assertNotEquals(b, new B() {});
    assertNotEquals(b, i1);
    assertNotEquals(i1, b);
    assertNotEquals(b, new I1() {}); // Noncompliant
    assertNotEquals(b, i2);
    assertNotEquals(a, b);
    assertNotEquals(b, a);
    assertNotEquals(b, o);
    assertNotEquals(a, x); // Noncompliant
    assertEquals(a, x); // Noncompliant
    assertNotEquals(a, i1);
    assertEquals(a, i1);
    assertNotEquals(a, i2);
    assertEquals(a, i2);
    assertEquals(a, x); // Noncompliant
    assertNotEquals(i1, i1);
    assertEquals(i1, i1);
    assertNotEquals(i1, i2);
    assertEquals(i1, i2);
    assertNotEquals(y, i1);
    assertEquals(y, i1);
  }

  static class A {
  }

  static class B extends A implements I1 {
  }

  static final class Y extends X implements I2 {
  }

  int intPrimitive() {
    return 1;
  }

}
