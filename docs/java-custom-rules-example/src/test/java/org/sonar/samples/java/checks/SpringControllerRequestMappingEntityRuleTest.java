/*
 * Copyright (C) 2012-2024 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.samples.java.utils.FilesUtils;

class SpringControllerRequestMappingEntityRuleTest {

  @Test
  void check() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/SpringControllerRequestMappingEntityRule.java")
      .withCheck(new SpringControllerRequestMappingEntityRule())
      .withClassPath(FilesUtils.getClassPath("target/test-jars"))
      .verifyIssues();
  }

}
