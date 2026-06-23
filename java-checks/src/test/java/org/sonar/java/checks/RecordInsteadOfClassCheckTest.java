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

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class RecordInsteadOfClassCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/RecordInsteadOfClassCheckSample.java"))
      .withCheck(new RecordInsteadOfClassCheck())
      .withJavaVersion(16)
      .verifyIssues();
  }

  @Test
  void test_framework_annotation_prefix_scope() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/RecordInsteadOfClassCheckPackagePrefixSample.java")
      .withCheck(new RecordInsteadOfClassCheck())
      .withClassPath(List.of(
        new File("target/test-classes"),
        new File(System.getProperty("user.home") + "/.m2/repository/org/springframework/data/spring-data-mongodb/3.3.5/spring-data-mongodb-3.3.5.jar"),
        new File(System.getProperty("user.home") + "/.m2/repository/org/springframework/data/spring-data-elasticsearch/3.0.8.RELEASE/spring-data-elasticsearch-3.0.8.RELEASE.jar")))
      .withJavaVersion(16)
      .verifyIssues();
  }

  @Test
  void test_incomplete_semantic() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/RecordInsteadOfClassCheckSample.java"))
      .withCheck(new RecordInsteadOfClassCheck())
      .withJavaVersion(16)
      .verifyIssues();
  }
}
