import org.fest.assertions.GenericAssert;

import java.lang.Integer;

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
    org.fest.assertions.Fail.fail("foo",  null);
    org.fest.assertions.Fail.failure("foo");
  }
  
  class MyCustomGenericAssert extends GenericAssert<String, String> {

    protected MyCustomGenericAssert(Class<String> selfType, String actual) {
      super(selfType, actual); // Compliant
    }
  }
}
