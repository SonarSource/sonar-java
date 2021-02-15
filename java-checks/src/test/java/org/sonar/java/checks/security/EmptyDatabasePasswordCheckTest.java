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
package org.sonar.java.checks.security;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.java.testing.FilesUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;

import static org.sonar.java.CheckTestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.CheckTestUtils.testSourcesPath;

class EmptyDatabasePasswordCheckTest {

  @Test
  void test() throws Exception {
    List<File> classPath = FilesUtils.getClassPath(FilesUtils.DEFAULT_TEST_JARS_DIRECTORY);
    classPath.add(new File("../java-checks-test-sources/target/classes"));
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/security/EmptyDatabasePasswordCheck.java"))
      .withClassPath(classPath)
      .withCheck(new EmptyDatabasePasswordCheck())
      .verifyIssues();
  }
}
