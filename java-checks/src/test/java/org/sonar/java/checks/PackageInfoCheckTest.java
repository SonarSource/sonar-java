/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class PackageInfoCheckTest {

  @Test
  void no_package_info() {
    final String expectedPackage = "checks.packageInfo.nopackageinfo";

    PackageInfoCheck check = new PackageInfoCheck();
    String expectedMessage = "Add a 'package-info.java' file to document the '" + expectedPackage + "' package";

    CheckVerifier.newVerifier()
      .onFiles(
        testSourcesPath("DefaultPackage.java"),
        testSourcesPath("checks/packageInfo/HelloWorld.java"),
        testSourcesPath("checks/packageInfo/package-info.java"),
        testSourcesPath("checks/packageInfo/nopackageinfo/HelloWorld.java"),
        testSourcesPath("checks/packageInfo/nopackageinfo/nopackageinfo.java"))
      .withCheck(check)
      .verifyIssueOnProject(expectedMessage);

    Set<String> set = check.missingPackageWithoutPackageFile;
    assertThat(set).hasSize(1);
    assertThat(set.iterator().next()).isEqualTo(expectedPackage);
  }

}
