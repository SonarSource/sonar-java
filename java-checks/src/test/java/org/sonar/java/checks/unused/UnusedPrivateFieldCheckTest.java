/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.unused;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class UnusedPrivateFieldCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldCheck.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/unused/UnusedPrivateFieldCheck.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyIssues();
  }

  @Test
  void test_native() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldCheckWithNative.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyNoIssues();
  }

  @Test
  void test_quick_fixes() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldCheckWithQuickfixes.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyIssues();
  }

  @Test
  void test_ignored_annotation() {
    UnusedPrivateFieldCheck check = new UnusedPrivateFieldCheck();
    check.ignoreAnnotations = "javax.inject.Inject,,javax.annotation.Nullable";
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldCheckWithIgnoredAnnotation.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_lombok_annotations() {
    InternalCheckVerifier.newInstance()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldLombok.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyIssues();
  }

}
