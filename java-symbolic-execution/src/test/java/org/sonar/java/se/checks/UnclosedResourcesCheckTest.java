/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.se.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.utils.SETestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class UnclosedResourcesCheckTest {

  @Test
  void test() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/UnclosedResourcesCheck.java")
      .withCheck(new UnclosedResourcesCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void jdbcTests() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/JdbcResourcesTestFile.java")
      .withCheck(new UnclosedResourcesCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void spring() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/SpringResource.java")
      .withCheck(new UnclosedResourcesCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void streams() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/StreamResource.java")
      .withCheck(new UnclosedResourcesCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void testWithExcludedTypes() {
    UnclosedResourcesCheck unclosedResourcesCheck = new UnclosedResourcesCheck();
    unclosedResourcesCheck.excludedTypes = "java.io.FileInputStream, java.sql.Statement";
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/ExcludedResourcesTestFile.java")
      .withCheck(unclosedResourcesCheck)
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void try_with_resources() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/UnclosedResourcesCheckARM.java")
      .withCheck(new UnclosedResourcesCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void test_value_as_string_for_open_resource_constraints() throws Exception {
    assertThat(UnclosedResourcesCheck.ResourceConstraint.OPEN.valueAsString()).isSameAs("open");
    assertThat(UnclosedResourcesCheck.ResourceConstraint.CLOSED.valueAsString()).isSameAs("closed");
  }

  @Test
  void test_streams() throws Exception {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/UnclosedResourcesCheckStreams.java")
      .withCheck(new UnclosedResourcesCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void skip_exception_messages() throws Exception {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/UnclosedResourcesCheckWithoutExceptionMessages.java")
      .withCheck(new UnclosedResourcesCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }
}
