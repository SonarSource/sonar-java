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

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class UselessPackageInfoCheckTest {

  @Test
  void withNoOtherFile() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFiles/package-info.java"))
      .withCheck(new UselessPackageInfoCheck())
      .verifyIssueOnFile("Remove this package.");
  }

  @Test
  void withOtherFile() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/UselessPackageInfoCheck/package-info.java"))
      .withCheck(new UselessPackageInfoCheck())
      .verifyNoIssues();
  }

  @Test
  void notAPackageInfo() {
    CheckVerifier.newVerifier()
      .onFiles(
        testSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld1.java"),
        testSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld2.java"))
      .withCheck(new UselessPackageInfoCheck())
      .verifyNoIssues();
  }

  @Test
  void notAPackageInfoOnSingleFile() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld1.java"))
      .withCheck(new UselessPackageInfoCheck())
      .verifyNoIssues();
  }

  @Test
  void defaultPackage() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("DefaultPackage.java"))
      .withCheck(new UselessPackageInfoCheck())
      .verifyNoIssues();
  }

}
