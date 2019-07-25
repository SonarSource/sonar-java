import org.fest.assertions.GenericAssert;

class A {
  void foo() {
    org.junit.Assert.assertTrue(true); // Noncompliant [[sc=5;ec=38]] {{Add a message to this assertion.}}
    org.junit.Assert.assertTrue("message", true);
    org.junit.Assert.assertTrue(1 > 2); // Noncompliant {{Add a message to this assertion.}}
    org.junit.Assert.assertFalse(false); // Noncompliant
    org.junit.Assert.assertFalse("message", false);
    org.junit.Assert.assertEquals("message", "foo", "bar");
    org.junit.Assert.assertEquals("foo", "bar"); // Noncompliant
    junit.framework.Assert.assertTrue(true); // Noncompliant
    junit.framework.Assert.assertTrue("message", true);
    junit.framework.Assert.assertEquals("message", "foo", "bar");
    junit.framework.Assert.assertEquals("message", "foo", "bar");
    junit.framework.Assert.assertEquals("foo", "bar"); // Noncompliant
    junit.framework.Assert.assertNotNull("foo", "bar");
    junit.framework.Assert.assertNotNull("foo"); // Noncompliant


    org.fest.assertions.Assertions.assertThat(true).isTrue();// Noncompliant {{Add a message to this assertion.}}
    org.fest.assertions.Assertions.assertThat(true).as("verifying the truth").isTrue();
    org.fest.assertions.Assertions.Assertions.assertThat("").isEqualTo("").as("");

    org.junit.Assert.assertThat("foo", null); // Noncompliant {{Add a message to this assertion.}}
    org.junit.Assert.assertThat("foo", "bar", null);
    org.junit.Assert.assertThat("foo", new Integer(1), null);

    junit.framework.Assert.assertNotSame("foo", "bar"); // Noncompliant
    junit.framework.Assert.assertNotSame("foo", "foo", "bar");
    junit.framework.Assert.assertSame("foo", "bar"); // Noncompliant
    junit.framework.Assert.assertSame("foo", "foo", "bar");


    org.junit.Assert.fail(); // Noncompliant
    org.junit.Assert.fail("Foo");
    junit.framework.Assert.fail(); // Noncompliant
    junit.framework.Assert.fail("Foo");
    org.fest.assertions.Fail.fail(); // Noncompliant
    org.fest.assertions.Fail.fail("foo");
    org.fest.assertions.Fail.fail("foo", null);
    org.fest.assertions.Fail.failure("foo");
  }

  void junit5() {
    org.junit.jupiter.api.Assertions.assertAll(null);
    org.junit.jupiter.api.Assertions.assertLinesMatch(null, null);
    org.junit.jupiter.api.Assertions.fail(() -> "message");
    org.junit.jupiter.api.Assertions.fail(new java.lang.RuntimeException());

    org.junit.jupiter.api.Assertions.assertFalse(false); // Noncompliant [[sc=5;ec=56]] {{Add a message to this assertion.}}
    org.junit.jupiter.api.Assertions.assertFalse(false, "message");
    org.junit.jupiter.api.Assertions.assertTrue(false); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTrue(false, () -> "message");
    org.junit.jupiter.api.Assertions.assertNull(null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertNull(null, "message");
    org.junit.jupiter.api.Assertions.assertNotNull(null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertNotNull(null, "message");

    org.junit.jupiter.api.Assertions.assertIterableEquals(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertIterableEquals(null, null, "message");
    org.junit.jupiter.api.Assertions.assertIterableEquals(null, null, () -> "messageSupplier");
    org.junit.jupiter.api.Assertions.assertNotEquals(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertNotEquals(null, null, "message");
    org.junit.jupiter.api.Assertions.assertNotSame(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertNotSame(null, null, "message");

    org.junit.jupiter.api.Assertions.assertSame(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertSame(null, null, "message");

    org.junit.jupiter.api.Assertions.assertThrows(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertThrows(null, null, () -> "message");

    org.junit.jupiter.api.Assertions.assertTimeout(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTimeout(null, null, "message");

    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(null, null, () -> "message");


    org.junit.jupiter.api.Assertions.assertArrayEquals(new boolean[0], new boolean[0]); // Noncompliant
    org.junit.jupiter.api.Assertions.assertArrayEquals(new boolean[0], new boolean[0], "message");
    org.junit.jupiter.api.Assertions.assertArrayEquals(new float[0], new float[0]); // Noncompliant
    org.junit.jupiter.api.Assertions.assertArrayEquals(new float[0], new float[0], 1f); // Noncompliant
    org.junit.jupiter.api.Assertions.assertArrayEquals(new float[0], new float[0], 1f, "message");
    org.junit.jupiter.api.Assertions.assertArrayEquals(new float[0], new float[0], "message");

    byte b = 0;
    org.junit.jupiter.api.Assertions.assertEquals(b, b); // Noncompliant
    org.junit.jupiter.api.Assertions.assertEquals(b, b, () -> "messageSupplier");
    org.junit.jupiter.api.Assertions.assertEquals(1.0, 2.0); // Noncompliant
    org.junit.jupiter.api.Assertions.assertEquals(1.0, 2.0, 0.1); // Noncompliant
    org.junit.jupiter.api.Assertions.assertEquals(1.0, 2.0, () -> "messageSupplier");
    org.junit.jupiter.api.Assertions.assertEquals(1.0, 2.0, 1.0, () -> "messageSupplier");

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> new A()); // Noncompliant
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(A::new);  // Noncompliant
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> new A(), "message");
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(A::new, "message");
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(A::new, () -> "message");

    java.util.List<String> list = new java.util.ArrayList<String>();
    org.junit.jupiter.api.Assertions.assertIterableEquals(list, list); // Noncompliant
    org.junit.jupiter.api.Assertions.assertIterableEquals(list, list, "message");
    org.junit.jupiter.api.Assertions.assertIterableEquals(list, list, () -> "message");

    org.junit.jupiter.api.Assertions.assertTimeout(java.time.Duration.ofSeconds(3), A::new); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTimeout(java.time.Duration.ofSeconds(3), A::new, "message");
    org.junit.jupiter.api.Assertions.assertTimeout(java.time.Duration.ofSeconds(3), A::new, () -> "message");

    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), A::new); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), A::new, "message");
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), A::new, () -> "message");
  }

  class MyCustomGenericAssert extends GenericAssert<String, String> {

    protected MyCustomGenericAssert(Class<String> selfType, String actual) {
      super(selfType, actual); // Compliant
    }
  }
}
