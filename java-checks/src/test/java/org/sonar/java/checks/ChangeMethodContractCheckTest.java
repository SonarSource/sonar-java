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
package org.sonar.java.checks;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.FilesUtils;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class ChangeMethodContractCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/S2638_ChangeMethodContractCheck/noPackageInfo/ChangeMethodContractCheck.java"))
      .withCheck(new ChangeMethodContractCheck())
      .verifyIssues();
  }

  @Test
  void test_package_level_annotations() {
    List<File> classPath = FilesUtils.getClassPath(FilesUtils.DEFAULT_TEST_JARS_DIRECTORY);
    // Add package-info to the classPath
    classPath.add(new File("../java-checks-test-sources/target/classes/"));
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/S2638_ChangeMethodContractCheck/nonNullApi/ChangeMethodContractCheck.java"))
      .withClassPath(classPath)
      .withCheck(new ChangeMethodContractCheck())
      .verifyIssues();
  }

  @Test
  void non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/ChangeMethodContractCheck.java"))
      .withCheck(new ChangeMethodContractCheck())
      .verifyNoIssues();
  }

}
