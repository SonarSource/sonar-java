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
package org.sonar.java.checks.unused;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;

import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class UnusedPrivateFieldCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/unused/UnusedPrivateFieldCheck.java")
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyIssues();
  }

  @Test
  void testQuickFixes() {
    InternalCheckVerifier.newInstance()
      .onFile(testSourcesPath("checks/unused/UnusedPrivateFieldCheckWithQuickFixes.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .withQuickFixes(quickFixes())
      .verifyIssues();
  }

  @Test
  void testNative() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/unused/UnusedPrivateFieldCheckWithNative.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyNoIssues();
  }

  private static Map<AnalyzerMessage.TextSpan, JavaQuickFix> quickFixes() {
    Map<AnalyzerMessage.TextSpan, JavaQuickFix> quickFixes = new HashMap<>();

    AnalyzerMessage.TextSpan aField = JavaTextEdit
      .textSpan(4, 15, 4, 16);
    JavaQuickFix aFieldQuickFix = JavaQuickFix
      .newQuickFix("Remove \"a\".")
      .addTextEdit(JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(4, 15, 4, 17)))
      .build();
    quickFixes.put(aField, aFieldQuickFix);

    AnalyzerMessage.TextSpan field = JavaTextEdit
      .textSpan(10, 18, 10, 23);
    JavaQuickFix fieldQuickFix = JavaQuickFix
      .newQuickFix("Remove \"field\".")
      .addTextEdit(JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(10, 3, 10, 24)))
      .build();
    quickFixes.put(field, fieldQuickFix);

    AnalyzerMessage.TextSpan javadocField = JavaTextEdit
      .textSpan(15, 24, 15, 36);
    JavaQuickFix javadocFieldQuickFix = JavaQuickFix
      .newQuickFix("Remove \"javadocField\".")
      .addTextEdit(JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(12, 3, 15, 44)))
      .build();
    quickFixes.put(javadocField, javadocFieldQuickFix);

    return quickFixes;
  }
}
