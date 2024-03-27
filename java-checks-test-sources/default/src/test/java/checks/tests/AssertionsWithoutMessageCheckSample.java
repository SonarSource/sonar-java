package checks.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.description.TextDescription;
import org.fest.assertions.BasicDescription;
import org.fest.assertions.GenericAssert;
import org.junit.jupiter.api.function.Executable;

import static checks.tests.MyAbstractIsEqualTo.isEqualTo;

class AssertionsWithoutMessageCheckSample {
  void foo() {
    org.assertj.core.api.Assertions.assertThat("").usingComparator(null).as("a").isEqualTo(222); // Compliant
    org.junit.Assert.assertTrue(true); // Noncompliant [[sc=22;ec=32]] {{Add a message to this assertion.}}
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


    org.fest.assertions.Assertions.assertThat(true).isTrue();// Noncompliant {{Add a message to this assertion chain before the predicate method.}}
    org.fest.assertions.Assertions.assertThat(true).as("verifying the truth").isTrue();
    org.fest.assertions.Assertions.assertThat(true).as(new BasicDescription("description")).isTrue();
    org.fest.assertions.Assertions.assertThat(true).describedAs("verifying the truth").isTrue(); // compliant - describedAs is an alias for as
    org.fest.assertions.Assertions.assertThat(true).describedAs(new BasicDescription("description"));
    org.fest.assertions.Assertions.assertThat(true).overridingErrorMessage("error message").isTrue();
    org.fest.assertions.Assertions.assertThat("").as("Message").isEqualTo("");
    org.fest.assertions.Assertions.assertThat("").isEqualTo("").as("Message"); // Noncompliant

    org.assertj.core.api.Assertions.assertThat(true).isTrue(); // Noncompliant
    org.assertj.core.api.Assertions.assertThatObject(true).isEqualTo(""); // Noncompliant
    org.assertj.core.api.Assertions.assertThat(true).as("verifying the truth").isTrue();
    org.assertj.core.api.Assertions.assertThat(true).as("verifying the truth", new Object()).isTrue();
    org.assertj.core.api.Assertions.assertThat(true).as(new TextDescription("verifying the truth")).isTrue();
    org.assertj.core.api.Assertions.assertThat(true).describedAs("verifying the truth").isTrue(); // compliant - describedAs is an alias for as
    org.assertj.core.api.Assertions.assertThat(true).describedAs("verifying the truth", new Object()).isTrue();
    org.assertj.core.api.Assertions.assertThat(true).describedAs(new TextDescription("verifying the truth")).isTrue();
    org.assertj.core.api.Assertions.assertThat(true).withFailMessage("fail message").isTrue();
    org.assertj.core.api.Assertions.assertThat(true).withFailMessage("fail message", new Object()).isTrue();
    org.assertj.core.api.Assertions.assertThat(true).overridingErrorMessage("fail message").isTrue();
    org.assertj.core.api.Assertions.assertThat(true).overridingErrorMessage("fail message", new Object()).isTrue();
    org.assertj.core.api.Assertions.assertThat("").as("Message").isEqualTo("");
    org.assertj.core.api.Assertions.assertThat("").isEqualTo("").as("Message"); // Noncompliant [[sc=52;ec=61]] {{Add a message to this assertion chain before the predicate method.}}
    org.assertj.core.api.Assertions.assertThat("").matches("x").matches("y"); // Noncompliant [[sc=52;ec=59]]
    org.assertj.core.api.AssertionsForClassTypes.assertThat("").isEqualTo(""); // Noncompliant

    org.assertj.core.api.Assertions.assertThat("").usingComparator(null).as("a").isEqualTo(222); // Compliant
    org.assertj.core.api.Assertions.assertThat("").as("message").usingComparator(null).isEqualTo(222); // Compliant
    org.assertj.core.api.Assertions.assertThat("").isEqualTo("1").usingComparator(null).isEqualTo("2"); // Noncompliant [[sc=52;ec=61]]
    org.assertj.core.api.Assertions.assertThat("").usingComparator(null).isEqualTo(222); // Noncompliant
    org.assertj.core.api.Assertions.assertThat(new Object()).as("message").extracting("field").isEqualTo(222); // Compliant
    org.assertj.core.api.Assertions.assertThat(new Object()).extracting("field").isEqualTo(222); // Noncompliant
    org.assertj.core.api.Assertions.assertThat(new ArrayList<>()).as("message").filteredOn("s", "e").isEqualTo(222); // Compliant
    org.assertj.core.api.Assertions.assertThat(new ArrayList<>()).filteredOn("s", "e").isEqualTo(222); // Noncompliant

    AbstractStringAssert variableAssert = org.assertj.core.api.Assertions.assertThat("").as("message");
    variableAssert.isEqualTo("");  // Compliant
    AbstractStringAssert variableAssertWithoutMessage = org.assertj.core.api.Assertions.assertThat("");
    variableAssertWithoutMessage.isEqualTo("");  // FN, we can not be sure that the assertion provide a message

    // Compliant, not used as expected (for coverage)
    isEqualTo();
    MyAbstractIsEqualTo.isEqualTo();

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
    org.junit.jupiter.api.Assertions.assertAll((Executable) null);
    org.junit.jupiter.api.Assertions.assertAll((Collection<Executable>) null);
    org.junit.jupiter.api.Assertions.assertLinesMatch((List<String>) null, null);
    org.junit.jupiter.api.Assertions.fail(() -> "message");
    org.junit.jupiter.api.Assertions.fail("message", new java.lang.RuntimeException());

    org.junit.jupiter.api.Assertions.assertFalse(false); // Noncompliant [[sc=38;ec=49]] {{Add a message to this assertion.}}
    org.junit.jupiter.api.Assertions.assertFalse(false, "message");
    org.junit.jupiter.api.Assertions.assertTrue(false); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTrue(false, () -> "message");
    org.junit.jupiter.api.Assertions.assertNull(null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertNull(null, "message");
    org.junit.jupiter.api.Assertions.assertNotNull(null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertNotNull(null, "message");
    org.junit.jupiter.api.Assertions.fail(); // Noncompliant
    org.junit.jupiter.api.Assertions.fail(new java.lang.RuntimeException()); // Noncompliant

    org.junit.jupiter.api.Assertions.assertIterableEquals(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertIterableEquals(null, null, "message");
    org.junit.jupiter.api.Assertions.assertIterableEquals(null, null, () -> "messageSupplier");
    org.junit.jupiter.api.Assertions.assertNotEquals((Object) null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertNotEquals((Object) null, null, "message");
    org.junit.jupiter.api.Assertions.assertNotSame(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertNotSame(null, null, "message");

    org.junit.jupiter.api.Assertions.assertSame(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertSame(null, null, "message");

    org.junit.jupiter.api.Assertions.assertThrows(null, null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertThrows(null, null, () -> "message");

    org.junit.jupiter.api.Assertions.assertTimeout(null, (Executable) null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTimeout(null, (Executable) null, "message");

    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(null, (Executable) null); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(null, (Executable) null, () -> "message");


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

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> new AssertionsWithoutMessageCheckSample()); // Noncompliant
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(AssertionsWithoutMessageCheckSample::new);  // Noncompliant
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> new AssertionsWithoutMessageCheckSample(), "message");
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(AssertionsWithoutMessageCheckSample::new, "message");
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(AssertionsWithoutMessageCheckSample::new, () -> "message");

    java.util.List<String> list = new java.util.ArrayList<String>();
    org.junit.jupiter.api.Assertions.assertIterableEquals(list, list); // Noncompliant
    org.junit.jupiter.api.Assertions.assertIterableEquals(list, list, "message");
    org.junit.jupiter.api.Assertions.assertIterableEquals(list, list, () -> "message");

    org.junit.jupiter.api.Assertions.assertTimeout(java.time.Duration.ofSeconds(3), AssertionsWithoutMessageCheckSample::new); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTimeout(java.time.Duration.ofSeconds(3), AssertionsWithoutMessageCheckSample::new, "message");
    org.junit.jupiter.api.Assertions.assertTimeout(java.time.Duration.ofSeconds(3), AssertionsWithoutMessageCheckSample::new, () -> "message");

    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), AssertionsWithoutMessageCheckSample::new); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), AssertionsWithoutMessageCheckSample::new, "message");
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), AssertionsWithoutMessageCheckSample::new, () -> "message");
  }

  class MyCustomGenericAssert extends GenericAssert<String, String> {

    protected MyCustomGenericAssert(Class<String> selfType, String actual) {
      super(selfType, actual); // Compliant
    }
  }
}

class MyAbstractIsEqualTo extends AbstractObjectAssert {

  public MyAbstractIsEqualTo(Object o, Class selfType) {
    super(o, selfType);
  }

  public static void isEqualTo() {
  }
}
