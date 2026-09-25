/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

class CollectionsSortCheckTest {

  @Test
  void test() {
    createCheckVerifier()
      .withJavaVersion(8)
      .verifyIssues();
  }

  @Test
  void test_no_issues_for_older_versions() {
    createCheckVerifier()
      .withJavaVersion(7)
      .verifyNoIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CollectionsSortCheckSample.java"))
      .withCheck(new CollectionsSortCheck())
      .withJavaVersion(8)
      .withoutSemantic()
      .verifyIssues();
  }

  private static CheckVerifier createCheckVerifier() {
    return CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CollectionsSortCheckSample.java"))
      .withCheck(new CollectionsSortCheck());
  }
}
