/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks.xml.maven;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DisallowedDependenciesCheckTest {

  private DisallowedDependenciesCheck check;

  @BeforeEach
  public void setup() {
    check = new DisallowedDependenciesCheck();
  }

  @Test
  void without_version() {
    check.dependencyName = "*:log4j";
    SonarXmlCheckVerifier.verifyIssues("noVersion/pom.xml", check);
  }

  @Test
  void with_simple_version() {
    check.dependencyName = "*:log4j";
    check.version = "1.2.*";
    SonarXmlCheckVerifier.verifyIssues("regexVersion/pom.xml", check);
  }

  @Test
  void with_range_version() {
    check.dependencyName = "*:log4j";
    check.version = "1.1.0-1.2.15";
    SonarXmlCheckVerifier.verifyIssues("rangeVersion/pom.xml", check);
  }

  @Test
  void should_fail_with_invalid_name_provided() {
    check.dependencyName = "org.sonar";
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> SonarXmlCheckVerifier.verifyIssues("noVersion/pom.xml", check));
    assertThat(e.getMessage()).isEqualTo("[S3417] Unable to build matchers from provided dependency name: org.sonar");
  }

  @Test
  void should_fail_with_invalid_version_provided() {
    check.dependencyName = "org.sonar.*:*";
    check.version = "version-0";
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> SonarXmlCheckVerifier.verifyIssues("noVersion/pom.xml", check));
    assertThat(e.getMessage()).isEqualTo("[S3417] Unable to build matchers from provided dependency name: org.sonar.*:*");
  }
}
