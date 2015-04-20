class A {
  void foo() {
    org.junit.Assert.assertTrue(true); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.Assert.assertTrue("message", true); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.Assert.assertTrue(1 > 2);
    org.junit.Assert.assertFalse(false); // Noncompliant
    org.junit.Assert.assertFalse("message", false); // Noncompliant
    junit.framework.Assert.assertTrue(true); // Noncompliant
    junit.framework.Assert.assertTrue("message", true); // Noncompliant
    junit.framework.Assert.assertTrue(1 > 2);
    junit.framework.Assert.assertFalse(true); // Noncompliant {{Remove or correct this assertion.}}
  }
}