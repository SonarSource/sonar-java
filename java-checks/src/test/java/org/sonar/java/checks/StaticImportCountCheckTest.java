/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

public class StaticImportCountCheckTest {

  private static final String TEST_FILES_DIR = "src/test/files/checks/StaticImportCountCheck/";

  @Test
  public void static_imports_below_threshold_are_compliant() {
    JavaCheckVerifier.verifyNoIssue(TEST_FILES_DIR + "CompliantImports.java", new StaticImportCountCheck());
  }

  @Test
  public void cu_with_just_static_imports() {
    JavaCheckVerifier.verify(TEST_FILES_DIR + "StaticImportCountCheck.java", new StaticImportCountCheck());
  }

  @Test
  public void cu_with_normal_and_static_imports() {
    JavaCheckVerifier.verify(TEST_FILES_DIR + "MixedStandardAndStaticImports.java", new StaticImportCountCheck());
  }

  @Test
  public void cu_with_custom_threshold_compliant() {
    StaticImportCountCheck check = new StaticImportCountCheck();
    check.setThreshold(5);
    JavaCheckVerifier.verifyNoIssue(TEST_FILES_DIR + "MixedStandardAndStaticImportsCompliant.java", check);
  }

  @Test
  public void cu_with_custom_threshold_noncompliant() {
    StaticImportCountCheck check = new StaticImportCountCheck();
    check.setThreshold(3);
    JavaCheckVerifier.verify(TEST_FILES_DIR + "MixedStandardAndStaticImportsCustomThreshold.java", check);
  }

}
