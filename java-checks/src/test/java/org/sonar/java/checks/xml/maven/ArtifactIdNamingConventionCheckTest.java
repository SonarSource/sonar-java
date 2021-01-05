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

import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactIdNamingConventionCheckTest {

  @Test
  void test_default() {
    ArtifactIdNamingConventionCheck check = new ArtifactIdNamingConventionCheck();
    SonarXmlCheckVerifier.verifyIssues("defaultNOK/pom.xml", check);
    SonarXmlCheckVerifier.verifyIssues("emptyArtifactId/pom.xml", check);
    SonarXmlCheckVerifier.verifyNoIssue("defaultOK/pom.xml", check);
    SonarXmlCheckVerifier.verifyNoIssue("noArtifactId/pom.xml", check);
  }

  @Test
  void test_custom() {
    ArtifactIdNamingConventionCheck check = new ArtifactIdNamingConventionCheck();
    check.regex = "[a-z]+";
    SonarXmlCheckVerifier.verifyIssues("customNOK/pom.xml", check);
    SonarXmlCheckVerifier.verifyIssues("emptyArtifactId/pom.xml", check);
    SonarXmlCheckVerifier.verifyNoIssue("customOK/pom.xml", check);
    SonarXmlCheckVerifier.verifyNoIssue("noArtifactId/pom.xml", check);
  }

  @Test
  void invalid_regex() {
    ArtifactIdNamingConventionCheck check = new ArtifactIdNamingConventionCheck();
    check.regex = "*";

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> SonarXmlCheckVerifier.verifyNoIssue("defaultOK/pom.xml", check));
    assertThat(e.getMessage()).isEqualTo("[S3420] Unable to compile the regular expression: *");
  }

  @Test
  void not_a_pom() {
    ArtifactIdNamingConventionCheck check = new ArtifactIdNamingConventionCheck();
    SonarXmlCheckVerifier.verifyNoIssue("../irrelevant.xml", check);
  }

}
