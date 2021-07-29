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
    InternalCheckVerifier.newInstance()
      .onFile("src/test/files/checks/unused/UnusedPrivateFieldCheck.java")
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

    AnalyzerMessage.TextSpan simpleField = JavaTextEdit
      .textSpan(108, 18, 108, 24);
    JavaQuickFix simpleFieldQuickFix = JavaQuickFix
      .newQuickFix("Remove \"field1\".")
      .addTextEdit(JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(108, 3, 108, 25)))
      .build();
    quickFixes.put(simpleField, simpleFieldQuickFix);

    AnalyzerMessage.TextSpan fieldWithJavadoc = JavaTextEdit
      .textSpan(113, 24, 113, 42);
    JavaQuickFix fieldWithJavadocQuickFix = JavaQuickFix
      .newQuickFix("Remove \"mySuperUnusedField\".")
      .addTextEdit(JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(113 /* FIXME 110 */, 3, 113, 50)))
      .build();
    quickFixes.put(fieldWithJavadoc, fieldWithJavadocQuickFix);

    return quickFixes;
  }
}
