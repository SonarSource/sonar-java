/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.classpath;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest {

  @Test
  void testCompareTo() {
    VersionImpl version = VersionImpl.parse("3.2.6-rc1");

    assertTrue(version.isGreaterThanOrEqualTo("3.2"));
    assertTrue(version.isGreaterThanOrEqualTo("3.2.5"));
    assertTrue(version.isGreaterThanOrEqualTo("2.9.4-rc2"));
    assertFalse(version.isGreaterThanOrEqualTo("3.2.11"));

    assertTrue(version.isLowerThan("4.0.0.RELEASE"));
    assertTrue(version.isLowerThan("3.3.0"));
    assertFalse(version.isLowerThan("3.2.6-rc1"));

    assertTrue(version.isGreaterThan("3.0.77"));
    assertFalse(version.isGreaterThan("3.2"));
    assertFalse(version.isGreaterThan("3.2.6-rc1"));

    assertTrue(version.isLowerThanOrEqualTo("3.2.6-rc1"));
    assertFalse(version.isLowerThanOrEqualTo("3.2.5"));
  }

  @Test
  void testCompareTo_withNoPatchNumber() {
    VersionImpl version = VersionImpl.parse("3.2");

    assertTrue(version.isGreaterThanOrEqualTo("3.2"));
    assertTrue(version.isGreaterThanOrEqualTo("3.2.5"));
    assertTrue(version.isGreaterThanOrEqualTo("2.9.4-rc2"));
    assertTrue(version.isGreaterThanOrEqualTo("3.2.11"));
    assertFalse(version.isGreaterThanOrEqualTo("3.3.11"));
    assertFalse(version.isGreaterThanOrEqualTo("4.0"));

    assertTrue(version.isLowerThan("4.0.0.RELEASE"));
    assertTrue(version.isLowerThan("3.3.0"));
    assertFalse(version.isLowerThan("3.2.6-rc1"));

    assertTrue(version.isGreaterThan("3.0.77"));
    assertFalse(version.isGreaterThan("3.2"));
    assertFalse(version.isGreaterThan("3.2.6-rc1"));

    assertTrue(version.isLowerThanOrEqualTo("3.2.5"));
    assertFalse(version.isLowerThanOrEqualTo("3.1"));
  }

  @Test
  void testParse() {
    assertThrows(IllegalArgumentException.class, () -> VersionImpl.parse("foo"));
  }

  @Test
  void testToString() {
    assertEquals("5.4.3-rc1", VersionImpl.parse("5.4.3-rc1").toString());
    assertEquals("5.43-rc1", VersionImpl.parse("5.43-rc1").toString());
    assertEquals("5.431", VersionImpl.parse("5.431").toString());
  }

  @Test
  void testEqualsAndHashCode() {
    VersionImpl version1 = VersionImpl.parse("1.2");
    VersionImpl version2 = VersionImpl.parse("1.2");
    VersionImpl version3 = VersionImpl.parse("1.2.3");

    assertEquals(version1, version2);
    assertEquals(version1.hashCode(), version2.hashCode());
    assertNotEquals(version1, version3);
    assertNotEquals(version3, version1);
    assertNotEquals(version1.hashCode(), version3.hashCode());
    assertFalse(version1.equals("foo"));
    assertFalse(version1.equals(null));

    VersionImpl version23 = VersionImpl.parse("2.3");
    VersionImpl version24 = VersionImpl.parse("2.4");
    assertNotEquals(version1, version23);
    assertNotEquals(version23, version24);
  }
}
