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
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

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

    CheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyIssues();
    CheckVerifier.newVerifier()
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
      testSourcesPath(testFolder + "app/SpringBootApp1.java"),
      testSourcesPath(testFolder + "secondApp/AnotherOk.java"),
      testSourcesPath(testFolder + "secondApp/SpringBootApp2.java"));

    CheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void testSpringBootApplicationWithAnnotation() {
    final String testFolderThirdApp = BASE_PATH + "springBootApplication/thirdApp/";
    List<String> thirdAppTestFiles = Arrays.asList(
      testSourcesPath(testFolderThirdApp + "SpringBootApp3.java"),
      testSourcesPath(testFolderThirdApp + "domain/SomeClass.java"),
      testSourcesPath(testFolderThirdApp + "controller/Controller.java"));

    CheckVerifier.newVerifier()
      .onFiles(thirdAppTestFiles)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyIssues();

    final String testFolderFourthApp = BASE_PATH + "springBootApplication/fourthApp/";
    List<String> fourthAppTestFiles = Arrays.asList(
      testSourcesPath(testFolderFourthApp + "SpringBootApp4.java"),
      nonCompilingTestSourcesPath(testFolderFourthApp + "SpringBootApp4b.java"),
      testSourcesPath(testFolderFourthApp + "domain/SomeClass.java"),
      testSourcesPath(testFolderFourthApp + "utility/SomeUtilityClass.java"),
      testSourcesPath(testFolderFourthApp + "controller/Controller.java"));

    CheckVerifier.newVerifier()
      .onFiles(fourthAppTestFiles)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyIssues();
  }


}
