package org.sonar.java.classpath;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DependencyVersionImplTest {

  @Test
  void versionComparisons() {
    DependencyVersionImpl dependencyVersion = new DependencyVersionImpl("org.example", "example-artifact",
      Version.parse("3.2.6-rc1").get());

    assertTrue(dependencyVersion.isGreaterThanOrEqualTo("3.2.5"));
    assertTrue(dependencyVersion.isGreaterThanOrEqualTo("2.9.4-rc2"));
    assertFalse(dependencyVersion.isGreaterThanOrEqualTo("3.2.11"));

    assertTrue(dependencyVersion.isLowerThan("3.3.0"));
    assertFalse(dependencyVersion.isLowerThan("3.2.6-rc1"));

    assertTrue(dependencyVersion.isGreaterThan("3.0.77"));
    assertFalse(dependencyVersion.isGreaterThan("3.2.6-rc1"));

    assertTrue(dependencyVersion.isLowerThanOrEqualTo("3.2.6-rc1"));
    assertFalse(dependencyVersion.isLowerThanOrEqualTo("3.2.5"));
  }
}
