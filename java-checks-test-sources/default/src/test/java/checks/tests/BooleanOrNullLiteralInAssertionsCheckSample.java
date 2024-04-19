package checks.tests;

import static org.hamcrest.core.IsNot.not;

class BooleanOrNullLiteralInAssertionsCheckSample {
  void booleans() {
    org.junit.Assert.assertTrue( // Noncompliant [[sc=22;ec=32;secondary=8]] {{Remove or correct this assertion.}}
      true
    );
    org.junit.Assert.assertEquals( // Noncompliant [[sc=22;ec=34;secondary=11,12]] {{Remove or correct this assertion.}}
      true,
      true
    );
    org.junit.Assert.assertTrue("message", true); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.Assert.assertTrue(1 > 2);
    org.junit.Assert.assertFalse(false); // Noncompliant
    org.junit.Assert.assertFalse("message", false); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTrue(true); // Noncompliant [[sc=38;ec=48]] {{Remove or correct this assertion.}}
    org.junit.jupiter.api.Assertions.assertFalse(true); // Noncompliant
    org.junit.jupiter.api.Assertions.assertTrue(getBool());
    org.junit.jupiter.api.Assertions.assertFalse(getBool());
    org.junit.jupiter.api.Assertions.assertEquals(true, getBool()); // Noncompliant [[sc=38;ec=50]] {{Use assertTrue instead.}}
    org.junit.jupiter.api.Assertions.assertSame(getBool(), false); // Noncompliant [[sc=38;ec=48]] {{Use assertFalse instead.}}
    org.junit.jupiter.api.Assertions.assertNotEquals(true, getBool()); // Noncompliant {{Use assertFalse instead.}}
    org.junit.jupiter.api.Assertions.assertNotEquals(false, getBool()); // Noncompliant {{Use assertTrue instead.}}

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
    org.junit.Assert.assertThat(true, null); // Noncompliant
    org.junit.Assert.assertThat("", not(false)); // Compliant
    junit.framework.TestCase.assertTrue(true); // Noncompliant
  }

  boolean getBool() {
    return true;
  }

  void nulls() {
    org.junit.Assert.assertNull(null); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.Assert.assertEquals(null, null); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.Assert.assertEquals(null, getObject()); // Noncompliant {{Use assertNull instead.}}
    org.junit.Assert.assertEquals(getObject(), null); // Noncompliant {{Use assertNull instead.}}
    org.junit.Assert.assertNotEquals(null, getObject()); // Noncompliant {{Use assertNotNull instead.}}
    org.junit.Assert.assertNotEquals(getObject(), null); // Noncompliant {{Use assertNotNull instead.}}
    org.junit.Assert.assertSame(null, null); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.Assert.assertSame(null, getObject()); // Noncompliant {{Use assertNull instead.}}
    org.junit.Assert.assertSame(getObject(), null); // Noncompliant {{Use assertNull instead.}}
    org.junit.Assert.assertNotSame(null, getObject()); // Noncompliant {{Use assertNotNull instead.}}
    org.junit.Assert.assertNotSame(getObject(), null); // Noncompliant {{Use assertNotNull instead.}}
    org.junit.Assert.assertNull(getObject()); // Compliant
    org.junit.Assert.assertNotNull(getObject()); // Compliant
    org.junit.Assert.assertEquals("message", getObject(), getObject()); // Compliant

    org.junit.jupiter.api.Assertions.assertNull(null); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.jupiter.api.Assertions.assertEquals((Object) null, null); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.jupiter.api.Assertions.assertEquals(null, getObject()); // Noncompliant {{Use assertNull instead.}}
    org.junit.jupiter.api.Assertions.assertEquals(getObject(), null); // Noncompliant {{Use assertNull instead.}}
    org.junit.jupiter.api.Assertions.assertNotEquals(null, getObject()); // Noncompliant {{Use assertNotNull instead.}}
    org.junit.jupiter.api.Assertions.assertNotEquals(getObject(), null); // Noncompliant {{Use assertNotNull instead.}}
    org.junit.jupiter.api.Assertions.assertSame(null, null); // Noncompliant {{Remove or correct this assertion.}}
    org.junit.jupiter.api.Assertions.assertSame(null, getObject()); // Noncompliant {{Use assertNull instead.}}
    org.junit.jupiter.api.Assertions.assertSame(getObject(), null); // Noncompliant {{Use assertNull instead.}}
    org.junit.jupiter.api.Assertions.assertNotSame(null, getObject()); // Noncompliant {{Use assertNotNull instead.}}
    org.junit.jupiter.api.Assertions.assertNotSame(getObject(), null); // Noncompliant {{Use assertNotNull instead.}}
    org.junit.jupiter.api.Assertions.assertNull(getObject()); // Compliant
    org.junit.jupiter.api.Assertions.assertNotNull(getObject()); // Compliant
    org.junit.jupiter.api.Assertions.assertEquals(getObject(), getObject(), "message"); // Compliant


    junit.framework.Assert.assertNull(null); // Noncompliant {{Remove or correct this assertion.}}
    junit.framework.Assert.assertEquals(null, null); // Noncompliant {{Remove or correct this assertion.}}
    junit.framework.Assert.assertEquals(null, getObject()); // Noncompliant {{Use assertNull instead.}}
    junit.framework.Assert.assertEquals(getObject(), null); // Noncompliant {{Use assertNull instead.}}
    junit.framework.Assert.assertSame(null, null); // Noncompliant {{Remove or correct this assertion.}}
    junit.framework.Assert.assertSame(null, getObject()); // Noncompliant {{Use assertNull instead.}}
    junit.framework.Assert.assertSame(getObject(), null); // Noncompliant {{Use assertNull instead.}}
    junit.framework.Assert.assertNotSame(null, getObject()); // Noncompliant {{Use assertNotNull instead.}}
    junit.framework.Assert.assertNotSame(getObject(), null); // Noncompliant {{Use assertNotNull instead.}}
    junit.framework.Assert.assertNull(getObject()); // Compliant
    junit.framework.Assert.assertNotNull(getObject()); // Compliant
    junit.framework.Assert.assertEquals("message", getObject(), getObject()); // Compliant


    org.fest.assertions.Assertions.assertThat((Object) null).isNull(); // Noncompliant {{Remove or correct this assertion.}}
    org.fest.assertions.Assertions.assertThat((Object) null).isEqualTo(null); // Noncompliant {{Remove or correct this assertion.}}
    org.fest.assertions.Assertions.assertThat((Object) null).isEqualTo(getObject()); // Noncompliant {{Use isNull instead.}}
    org.fest.assertions.Assertions.assertThat(getObject()).isNotEqualTo(null); // Noncompliant {{Use isNotNull instead.}}
    org.fest.assertions.Assertions.assertThat((Object) null).isNotEqualTo(getObject()); // Noncompliant {{Use isNotNull instead.}}
    org.fest.assertions.Assertions.assertThat(getObject()).isEqualTo(null); // Noncompliant {{Use isNull instead.}}
    org.fest.assertions.Assertions.assertThat((Object) null).isSameAs(null); // Noncompliant {{Remove or correct this assertion.}}
    org.fest.assertions.Assertions.assertThat((Object) null).as("description").isSameAs(getObject()); // Noncompliant {{Use isNull instead.}}
    org.fest.assertions.Assertions.assertThat(getObject()).isSameAs(null); // Noncompliant {{Use isNull instead.}}
    org.fest.assertions.Assertions.assertThat((Object) null).isNotSameAs(getObject()); // Noncompliant {{Use isNotNull instead.}}
    org.fest.assertions.Assertions.assertThat(getObject()).isNotSameAs(null); // Noncompliant {{Use isNotNull instead.}}
    org.fest.assertions.Assertions.assertThat(getObject()).isNull(); // Compliant
    org.fest.assertions.Assertions.assertThat(getObject()).isNotNull(); // Compliant
    org.fest.assertions.Assertions.assertThat(getObject()).isEqualTo(getObject()); // Compliant

    org.fest.assertions.ObjectAssert objectAssert = org.fest.assertions.Assertions.assertThat(getObject());
    objectAssert.isEqualTo(null); // Noncompliant {{Use isNull instead.}}
    objectAssert.isEqualTo(getObject()); // Compliant

    // Make sure the code for finding the actual value doesn't explode when calling is* methods without an
    // explicit receiver
    new org.fest.assertions.ObjectAssert(getObject()) {
      public void foo() {
        isEqualTo(null); // Noncompliant {{Use isNull instead.}}
        isEqualTo(getObject()); // Compliant
      }
    };
  }

  Object getObject() {
    return null;
  }
}
