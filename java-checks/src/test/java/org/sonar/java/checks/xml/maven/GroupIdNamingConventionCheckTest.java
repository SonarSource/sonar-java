/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.checks.verifier.PomCheckVerifier;

public class GroupIdNamingConventionCheckTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_default() {
    GroupIdNamingConventionCheck check = new GroupIdNamingConventionCheck();
    PomCheckVerifier.verify("src/test/files/checks/xml/maven/GroupIdNamingConventionCheckDefaultNOK.xml", check);
    PomCheckVerifier.verifyNoIssue("src/test/files/checks/xml/maven/GroupIdNamingConventionCheckDefaultOK.xml", check);
    PomCheckVerifier.verifyNoIssue("src/test/files/checks/xml/maven/GroupIdNamingConventionCheckNoGroupId.xml", check);
  }

  @Test
  public void test_custom() {
    GroupIdNamingConventionCheck check = new GroupIdNamingConventionCheck();
    check.regex = "[a-z][a-z-0-9]*";
    PomCheckVerifier.verify("src/test/files/checks/xml/maven/GroupIdNamingConventionCheckCustomNOK.xml", check);
    PomCheckVerifier.verifyNoIssue("src/test/files/checks/xml/maven/GroupIdNamingConventionCheckCustomOK.xml", check);
    PomCheckVerifier.verifyNoIssue("src/test/files/checks/xml/maven/GroupIdNamingConventionCheckNoGroupId.xml", check);
  }

  @Test
  public void invalid_regex() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("[S3419] Unable to compile the regular expression: *");

    GroupIdNamingConventionCheck check = new GroupIdNamingConventionCheck();
    check.regex = "*";
    PomCheckVerifier.verifyNoIssue("src/test/files/checks/xml/maven/GroupIdNamingConventionCheckDefaultOK.xml", check);
  }
}
