import static org.hamcrest.core.IsNot.not;

class A {
  void foo() {
    org.junit.Assert.assertTrue(true); // Noncompliant [[sc=33;ec=37]] {{Remove or correct this assertion.}}
    org.junit.Assert.assertTrue("message", true); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.Assert.assertTrue(1 > 2);
    org.junit.Assert.assertFalse(false); // Noncompliant
    org.junit.Assert.assertFalse("message", false); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTrue(true); // Noncompliant [[sc=49;ec=53]] {{Remove or correct this assertion.}}
    org.junit.jupiter.api.Assertions.assertFalse(true); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTrue(getBool());
    org.junit.jupiter.api.Assertions.assertFalse(getBool());
    org.junit.jupiter.api.Assertions.assertEquals(true, getBool()); // Noncompliant [[sc=51;ec=55]]
    org.junit.jupiter.api.Assertions.assertSame(getBool(), false); // Noncompliant [[sc=60;ec=65]]

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

  boolean getBool() {
    return true;
  }

}
