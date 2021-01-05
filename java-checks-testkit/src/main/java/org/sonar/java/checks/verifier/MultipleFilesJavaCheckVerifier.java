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
package org.sonar.java.checks.verifier;

import java.util.List;
import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.JavaFileScanner;

/**
 * Please read the documentation of @see org.sonar.java.checks.verifier.JavaCheckVerifier
 *
 * The main difference is that this class run a check on multiple files and verifies the results at the end of analysis.
 *
 * @deprecated This class is deprecated, all its features can be achieved by using {@link JavaCheckVerifier#newVerifier()} instead.
 */
@Beta
@Deprecated
public final class MultipleFilesJavaCheckVerifier {

  private MultipleFilesJavaCheckVerifier() {
  }

  /**
   * Verifies that all the expected issues are raised after analyzing all the given files with the given check.
   *
   * <br /><br />
   *
   * By default, any jar or zip archive present in the folder defined by {@link JavaCheckVerifier#DEFAULT_TEST_JARS_DIRECTORY} will be used
   * to add extra classes to the classpath. If this folder is empty or does not exist, then the analysis will be based on the source of
   * the provided file.
   *
   * @param filesToScan The files to be analyzed
   * @param check The check to be used for the analysis
   */
  public static void verify(List<String> filesToScan, JavaFileScanner check) {
    JavaCheckVerifier.newVerifier()
      .onFiles(filesToScan)
      .withCheck(check)
      .verifyIssues();
  }

  /**
   * Verifies that no issues are raised after analyzing all the given files with the given check.
   *
   * @param filesToScan The files to be analyzed
   * @param check The check to be used for the analysis
   */
  public static void verifyNoIssue(List<String> filesToScan, JavaFileScanner check) {
    JavaCheckVerifier.newVerifier()
      .onFiles(filesToScan)
      .withCheck(check)
      .verifyNoIssues();
  }

  /**
   * Verifies that no issues are raised after analyzing all given files with the given check when semantic is not available.
   *
   * @param filesToScan The files to be analyzed
   * @param check The check to be used for the analysis
   */
  public static void verifyNoIssueWithoutSemantic(List<String> filesToScan, JavaFileScanner check) {
    JavaCheckVerifier.newVerifier()
      .onFiles(filesToScan)
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }
}
