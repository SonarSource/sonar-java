/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.se.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.utils.SETestUtils;

class CustomUnclosedResourcesCheckTest {

  @Test
  void constructorClosed() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.constructor = "org.sonar.custom.GenericResource(java.lang.String)";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource(java.lang.String)";
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/customresources/ConstructorClosed.java")
      .withCheck(check)
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void constructorClosedAny() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.constructor = "org.sonar.custom.GenericResource";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource";
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/customresources/ConstructorClosedAny.java")
      .withCheck(check)
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void factoryClosed() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.factoryMethod = "org.sonar.custom.ResourceFactory#createResource(java.lang.String)";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource(java.lang.String)";
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/customresources/FactoryClosed.java")
      .withCheck(check)
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void factoryClosedAny() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.factoryMethod = "org.sonar.custom.ResourceFactory#createResource";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource";
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/customresources/FactoryClosedAny.java")
      .withCheck(check)
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void openedClosed() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.openingMethod = "org.sonar.custom.GenericResource#open(java.lang.String)";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource(java.lang.String)";
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/customresources/OpenedClosed.java")
      .withCheck(check)
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void openedClosedAny() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.openingMethod = "org.sonar.custom.GenericResource#open";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource";
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/customresources/OpenedClosedAny.java")
      .withCheck(check)
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  /**
   * {@link CustomUnclosedResourcesCheck.CustomResourceConstraint} class needs to be different, otherwise issued will be duplicated. See SONARJAVA-1624
   */
  @Test
  void check_status_is_different_instance() {
    CustomUnclosedResourcesCheck check1 = new CustomUnclosedResourcesCheck();
    check1.constructor = "org.sonar.custom.GenericResource(java.lang.String)";
    check1.closingMethod = "org.sonar.custom.GenericResource#closeResource(java.lang.String)";
    CustomUnclosedResourcesCheck check2 = new CustomUnclosedResourcesCheck();
    check2.constructor = "org.sonar.custom.GenericResource(java.lang.String)";
    check2.closingMethod = "org.sonar.custom.GenericResource#closeResource(java.lang.String)";
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/customresources/ConstructorClosed.java")
      .withChecks(check1, check2)
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

}
