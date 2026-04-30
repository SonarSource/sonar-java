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

class ArchiveEntryPathTraversalCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/ArchiveEntryPathTraversalCheckSample.java"))
      .withCheck(new ArchiveEntryPathTraversalCheck())
      .verifyIssues();
  }

  @Test
  void cve_2022_4494() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CVE_2022_4494_ArchiveEntryPathTraversal.java"))
      .withCheck(new ArchiveEntryPathTraversalCheck())
      .verifyIssues();
  }

  @Test
  void cve_2022_39367() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CVE_2022_39367_ArchiveEntryPathTraversal.java"))
      .withCheck(new ArchiveEntryPathTraversalCheck())
      .verifyIssues();
  }

  @Test
  void cve_2022_31194() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CVE_2022_31194_RequestPathTraversal.java"))
      .withCheck(new ArchiveEntryPathTraversalCheck())
      .verifyIssues();
  }

  @Test
  void cve_2022_29253() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CVE_2022_29253_ClassLoaderPathTraversal.java"))
      .withCheck(new ArchiveEntryPathTraversalCheck())
      .verifyIssues();
  }
}
