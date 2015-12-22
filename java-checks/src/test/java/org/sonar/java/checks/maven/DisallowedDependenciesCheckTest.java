/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks.maven;

import org.junit.Before;
import org.junit.Test;
import org.sonar.java.checks.verifier.MavenCheckVerifier;

public class DisallowedDependenciesCheckTest {

  private DisallowedDependenciesCheck check;

  @Before
  public void setup() {
    check = new DisallowedDependenciesCheck();
  }

  @Test
  public void simple_dependency_without_version() {
    check.setDependencyName("*:log4j");
    MavenCheckVerifier.verify("src/test/files/checks/maven/disallowedDependenciesCheck/simpleDependencyNoVersion-pom.xml", check);
  }

  @Test
  public void simple_dependency_with_simple_version() {
    check.setDependencyName("*:log4j:1.2.*");
    MavenCheckVerifier.verify("src/test/files/checks/maven/disallowedDependenciesCheck/simpleDependencyProvidedVersion-pom.xml", check);
  }

  @Test
  public void simple_dependency_with_range_version() {
    check.setDependencyName("*:log4j:1.1.0-1.2.15");
    MavenCheckVerifier.verify("src/test/files/checks/maven/disallowedDependenciesCheck/simpleDependencyRangeVersion-pom.xml", check);
  }

  @Test
  public void multiple_dependency_without_version() {
    check.setDependencyName("org.sonar.*:*,*:\\blog4j-core\\b");
    MavenCheckVerifier.verify("src/test/files/checks/maven/disallowedDependenciesCheck/multipleDependencyNoVersion-pom.xml", check);
  }

  @Test
  public void multiple_dependency_with_simple_version() {
    check.setDependencyName("org.sonar.*:*:1.2.*,*:log4j:1.2.*");
    MavenCheckVerifier.verify("src/test/files/checks/maven/disallowedDependenciesCheck/multipleDependencyProvidedVersion-pom.xml", check);
  }

  @Test
  public void multiple_dependency_with_range_version() {
    check.setDependencyName("org.sonar.*:*,*:log4j:1.2-1.3");
    MavenCheckVerifier.verify("src/test/files/checks/maven/disallowedDependenciesCheck/multipleDependencyRangeVersion-pom.xml", check);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_with_invalid_name_provided() {
    check.setDependencyName("org.sonar");
    MavenCheckVerifier.verify("src/test/files/checks/maven/disallowedDependenciesCheck/simpleDependencyNoVersion-pom.xml", check);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_with_invalid_version_provided() {
    check.setDependencyName("org.sonar.*:*:version-0");
    MavenCheckVerifier.verify("src/test/files/checks/maven/disallowedDependenciesCheck/simpleDependencyNoVersion-pom.xml", check);
  }

}
