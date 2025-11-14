/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.spring;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPathInModule;
import static org.sonar.java.test.classpath.TestClasspathUtils.SPRING_WEB_40_MODULE;

class SpringComposedRequestMappingCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/spring/SpringComposedRequestMappingCheck.java")
      .withCheck(new SpringComposedRequestMappingCheck())
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/spring/SpringComposedRequestMappingCheck.java")
      .withCheck(new SpringComposedRequestMappingCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void test_spring_web_4_0() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPathInModule(SPRING_WEB_40_MODULE, "checks/SpringComposedRequestMappingCheckSample.java"))
      .withCheck(new SpringComposedRequestMappingCheck())
      .withClassPath(SPRING_WEB_40_MODULE.getClassPath())
      .verifyNoIssues();
  }

}
