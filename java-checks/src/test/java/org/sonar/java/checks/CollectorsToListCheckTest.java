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
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class CollectorsToListCheckTest {
  @Test
  void test() {
    createCheckVerifier()
      .withJavaVersion(16)
      .verifyIssues();
  }

  @Test
  void test_no_issues_for_older_versions() {
    createCheckVerifier()
      .withJavaVersion(15)
      .verifyNoIssues();
  }

  @Test
  void test_no_issues_for_unknown_versions() {
    createCheckVerifier()
      .verifyNoIssues();
  }

  private static CheckVerifier createCheckVerifier() {
    return CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CollectorsToListCheckSample.java"))
      .withCheck(new CollectorsToListCheck());
  }
}
