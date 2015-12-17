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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.checks.verifier.MavenCheckVerifier;

public class ArtifactIdNamingConventionCheckTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_default() {
    ArtifactIdNamingConventionCheck check = new ArtifactIdNamingConventionCheck();
    MavenCheckVerifier.verify("src/test/files/checks/maven/ArtifactIdNamingConventionCheckDefaultNOK.xml", check);
    MavenCheckVerifier.verifyNoIssue("src/test/files/checks/maven/ArtifactIdNamingConventionCheckDefaultOK.xml", check);
    MavenCheckVerifier.verifyNoIssue("src/test/files/checks/maven/ArtifactIdNamingConventionCheckNoArtifactId.xml", check);
  }

  @Test
  public void test_custom() {
    ArtifactIdNamingConventionCheck check = new ArtifactIdNamingConventionCheck();
    check.regex = "[a-z]+";
    MavenCheckVerifier.verify("src/test/files/checks/maven/ArtifactIdNamingConventionCheckCustomNOK.xml", check);
    MavenCheckVerifier.verifyNoIssue("src/test/files/checks/maven/ArtifactIdNamingConventionCheckCustomOK.xml", check);
    MavenCheckVerifier.verifyNoIssue("src/test/files/checks/maven/ArtifactIdNamingConventionCheckNoArtifactId.xml", check);
  }

  @Test
  public void invalid_regex() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("[S3420] Unable to compile the regular expression: *");

    ArtifactIdNamingConventionCheck check = new ArtifactIdNamingConventionCheck();
    check.regex = "*";
    MavenCheckVerifier.verifyNoIssue("src/test/files/checks/maven/ArtifactIdNamingConventionCheckDefaultOK.xml", check);
  }

}
