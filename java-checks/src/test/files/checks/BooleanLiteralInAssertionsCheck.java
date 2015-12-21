import static org.hamcrest.core.IsNot.not;

class A {
  void foo() {
    org.junit.Assert.assertTrue(true); // Noncompliant [[sc=33;ec=37]] {{Remove or correct this assertion.}}
    org.junit.Assert.assertTrue("message", true); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.Assert.assertTrue(1 > 2);
    org.junit.Assert.assertFalse(false); // Noncompliant
    org.junit.Assert.assertFalse("message", false); // Noncompliant
    junit.framework.Assert.assertTrue(true); // Noncompliant
    junit.framework.Assert.assertTrue("message", true); // Noncompliant
    junit.framework.Assert.assertNull("message", true); // Noncompliant
    junit.framework.Assert.assertNotNull(true); // Noncompliant
    junit.framework.Assert.assertTrue(1 > 2);
    junit.framework.Assert.assertFalse(true); // Noncompliant {{Remove or correct this assertion.}}
    org.fest.assertions.Assertions.assertThat(true).isTrue(); // Noncompliant {{Remove or correct this assertion.}}
    org.fest.assertions.Assertions.assertThat(1 > 2).isTrue();
    org.fest.assertions.Assertions.assertThat("foo").isNotNull();
    org.junit.Assert.assertTrue(true); // Noncompliant
    org.junit.Assert.assertEquals(true, true); // Noncompliant
    org.junit.Assert.assertThat(true, null); // Noncompliant
    org.junit.Assert.assertThat("", not(false)); // Compliant
    new junit.framework.TestCase().assertTrue(true); // Noncompliant
  }

}
