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
package org.sonar.java.checks.spring;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class SpringBeansShouldBeAccessibleCheckTest {

  private static final String BASE_PATH = "checks/spring/s4605/";

  @Test
  void testComponentScan() {
    final String testFolder = BASE_PATH + "componentScan/";
    List<String> files = Arrays.asList(
      testSourcesPath("SpringBootAppInDefaultPackage.java"),
      testSourcesPath(testFolder + "packageA/ComponentA.java"),
      testSourcesPath(testFolder + "packageB/ComponentB.java"),
      testSourcesPath(testFolder + "packageC/ComponentC.java"),
      testSourcesPath(testFolder + "packageX/ComponentX.java"),
      testSourcesPath(testFolder + "packageY/ComponentY.java"),
      testSourcesPath(testFolder + "packageZ/ComponentZ.java"),
      testSourcesPath(testFolder + "packageFP/ComponentFP.java"),
      testSourcesPath(testFolder + "ComponentScan.java"));

    JavaCheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyIssues();
    JavaCheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void testSpringBootApplication() {
    final String testFolder = BASE_PATH + "springBootApplication/";
    List<String> files = Arrays.asList(
      testSourcesPath(testFolder + "Ko/Ko.java"),
      testSourcesPath(testFolder + "app/Ok/Ok.java"),
      testSourcesPath(testFolder + "app/SpringBootApp.java"),
      testSourcesPath(testFolder + "secondApp/AnotherOk.java"),
      testSourcesPath(testFolder + "secondApp/AnotherSpringBootApp.java"));

    JavaCheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyIssues();
    JavaCheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

}
