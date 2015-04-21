class A {
  void foo() {
    org.junit.Assert.assertTrue(true); // Noncompliant {{Add a message to this assertion.}}
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


    org.fest.assertions.Assertions.assertThat(true).isTrue();// Noncompliant {{Add a message to this assertion.}}
    org.fest.assertions.Assertions.assertThat(true).as("verifying the truth").isTrue();
    org.fest.assertions.Assertions.Assertions.assertThat("").isEqualTo("").as("");

  }
}