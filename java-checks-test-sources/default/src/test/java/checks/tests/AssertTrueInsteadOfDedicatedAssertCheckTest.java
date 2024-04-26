/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package checks.tests;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssertTrueInsteadOfDedicatedAssertCheckTest {

  Object foo = null;
  Object bar = null;

  boolean predicate() {
    return true;
  }

  {
    assertTrue(null == null); // Noncompliant
    Assertions.assertFalse(null == null); // Noncompliant
  }

  void assertTrue_JUnit4_orgJunitAssert() {
    Assert.assertTrue(null == foo); // Noncompliant {{Use assertNull instead.}}
//         ^^^^^^^^^^
//  ^^^<
    assertTrue(foo == null); // Noncompliant

    Assert.assertTrue(foo != null); // Noncompliant {{Use assertNotNull instead.}}
    assertTrue("null and foo should not be equal", null != foo); // Noncompliant

    Assert.assertTrue(foo == bar); // Noncompliant {{Use assertSame instead.}}
    assertTrue(bar != foo); // Noncompliant {{Use assertNotSame instead.}}

    Assert.assertTrue("This is a String", foo.equals(bar)); // Noncompliant {{Use assertEquals instead.}}
    assertTrue(!bar.equals(foo)); // Noncompliant {{Use assertNotEquals instead.}}

    assertTrue((foo = bar).equals(bar)); // Noncompliant {{Use assertEquals instead.}}

    Assert.assertTrue(foo == null || foo == null); // compliant - we only flag simple cases
    assertTrue(predicate()); // compliant
    assertTrue(foo.equals(bar) && bar != null); // compliant

  }

  void assertFalse_JUnit4_orgJunitAssert() {
    Assert.assertFalse(null == foo); // Noncompliant {{Use assertNotNull instead.}}
    assertFalse(foo == null); // Noncompliant

    Assert.assertFalse(foo != null); // Noncompliant {{Use assertNull instead.}}
    assertFalse("null and foo should not be equal", null != foo); // Noncompliant

    Assert.assertFalse(foo == bar); // Noncompliant {{Use assertNotSame instead.}}
    assertFalse(bar != foo); // Noncompliant {{Use assertSame instead.}}

    Assert.assertFalse("This is a String", foo.equals(bar)); // Noncompliant {{Use assertNotEquals instead.}}
    assertFalse(!bar.equals(foo)); // Noncompliant {{Use assertEquals instead.}}

    assertFalse((foo = bar).equals(bar)); // Noncompliant {{Use assertNotEquals instead.}}

    Assert.assertFalse(foo == null || foo == null); // compliant - we only flag simple cases
    assertFalse(predicate()); // compliant
    assertFalse(foo.equals(bar) && bar != null); // compliant
  }

  class MyTestCase extends TestCase {
    void assertTrueFalse_JUnit4_junitFrameworkTestCase() {
      super.assertTrue(null == foo); // Noncompliant
      assertTrue("message", foo == bar); // Noncompliant
      assertFalse(foo == null); // Noncompliant
      assertFalse(null != bar); // Noncompliant
      assertTrue(predicate()); // compliant
    }
  }

  void assertTrueFalse_JUnit4_junitFrameworkAssert() {
    junit.framework.Assert.assertTrue(foo != bar); // Noncompliant
    junit.framework.Assert.assertTrue("message", bar != null); // Noncompliant
    junit.framework.Assert.assertFalse(bar == foo); // Noncompliant
    junit.framework.Assert.assertFalse("message", null != foo); // Noncompliant
  }

  void assertTrueFalse_JUnit5() {
    Assertions.assertTrue(foo != bar); // Noncompliant
    Assertions.assertTrue(bar != null, "mhmm a massage!"); // Noncompliant
    Assertions.assertTrue(bar != null, () -> "a lazy massage :/"); // Noncompliant
    Assertions.assertFalse(bar == foo); // Noncompliant
    Assertions.assertFalse(null != foo, "message"); // Noncompliant
    Assertions.assertFalse(null != foo, () -> "message"); // Noncompliant
    Assertions.assertTrue(() -> foo == bar); // false-negative because BooleanSupplier is not supported by this rule
  }

  void testPrimitiveAndBoxedTypesSpecialCases() {
    assertFalse(2 == 3); // Noncompliant {{Use assertNotEquals instead.}}
    assertTrue(new Integer(5) == 6); // Noncompliant {{Use assertEquals instead.}}
    assertTrue(Boolean.valueOf(true) != false); // Noncompliant {{Use assertNotEquals instead.}}
    assertTrue(new Integer(5) == new Integer(5)); // Noncompliant {{Use assertSame instead.}}
  }
}
