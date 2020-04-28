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
package checks;

import org.junit.Assert;

import static org.junit.Assert.assertTrue;

public class AssertTrueInsteadOfDedicatedAssertCheckTest {

  {
    assertTrue(null == null); // Noncompliant
  }

  void someMethod() {
    Object foo = null;
    Object bar = null;

    Assert.assertTrue(null == foo); // Noncompliant [[sc=12;ec=22;secondary=36]] {{Use assertNull instead.}}
    assertTrue(foo == null); // Noncompliant

    Assert.assertTrue(foo != null); // Noncompliant {{Use assertNotNull instead.}}
    assertTrue("null and foo should not be equal", null != foo); // Noncompliant

    Assert.assertTrue(foo == bar); // Noncompliant {{Use assertSame instead.}}
    assertTrue(bar != foo); // Noncompliant

    Assert.assertTrue("This is a String", foo.equals(bar)); // Noncompliant {{Use assertEquals instead.}}
    assertTrue(!bar.equals(foo)); // Noncompliant {{Use assertNotEquals instead.}}

    assertTrue((foo = bar).equals(bar)); // Noncompliant {{Use assertEquals instead.}}

    Assert.assertTrue(foo == null || foo == null); // compliant - we only flag simple cases
    assertTrue(predicate()); // compliant
    assertTrue(foo.equals(bar) && bar != null); // compliant
  }

  boolean predicate() {
    return true;
  }
}
