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

import java.io.File;
import java.util.Collection;
import org.sonar.java.annotations.Beta;
import org.sonar.java.testing.CheckVerifier;
import org.sonar.java.testing.FilesUtils;
import org.sonar.plugins.java.api.JavaFileScanner;

/**
 * It is possible to specify the absolute line number on which the issue should appear by appending {@literal "@<line>"} to "Noncompliant".
 * But usually better to use line number relative to the current, this is possible to do by prefixing the number with either '+' or '-'.
 * For example:
 * <pre>
 *   // Noncompliant@+1 {{do not import "java.util.List"}}
 *   import java.util.List;
 * </pre>
 * Full syntax:
 * <pre>
 *   // Noncompliant@+1 [[startColumn=1;endLine=+1;endColumn=2;effortToFix=4;secondary=3,4]] {{issue message}}
 * </pre>
 * Attributes between [[]] are optional:
 * <ul>
 *   <li>startColumn: column where the highlight starts</li>
 *   <li>endLine: relative endLine where the highlight ends (i.e. +1), same line if omitted</li>
 *   <li>endColumn: column where the highlight ends</li>
 *   <li>effortToFix: the cost to fix as integer</li>
 *   <li>secondary: a comma separated list of integers identifying the lines of secondary locations if any</li>
 * </ul>
 */
@Beta
public final class JavaCheckVerifier {

  private JavaCheckVerifier() {
  }

  public static CheckVerifier newVerifier() {
    return CheckVerifier.newVerifier();
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check.
   *
   * <br /><br />
   *
   * By default, any jar or zip archive present in the folder defined by {@link JavaCheckVerifier#DEFAULT_TEST_JARS_DIRECTORY} will be used
   * to add extra classes to the classpath. If this folder is empty or does not exist, then the analysis will be based on the source of
   * the provided file.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   *
   * @deprecated use {@link #newVerifier()} instead
   */
  @Deprecated
  public static void verify(String filename, JavaFileScanner check) {
    newVerifier()
      .onFile(filename)
      .withCheck(check)
      .verifyIssues();
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check and a given
   * java version used for the sources.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   * @param javaVersion The version to consider for the analysis (6 for java 1.6, 7 for 1.7, etc.)
   *
   * @deprecated use {@link #newVerifier()} instead
   */
  @Deprecated
  public static void verify(String filename, JavaFileScanner check, int javaVersion) {
    newVerifier()
      .onFile(filename)
      .withCheck(check)
      .withJavaVersion(javaVersion)
      .verifyIssues();
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check,
   * but using having the classpath extended with a collection of files (classes/jar/zip).
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   * @param classpath The files to be used as classpath
   *
   * @deprecated use {@link #newVerifier()} instead
   */
  @Deprecated
  public static void verify(String filename, JavaFileScanner check, Collection<File> classpath) {
    newVerifier()
      .onFile(filename)
      .withCheck(check)
      .withClassPath(classpath)
      .verifyIssues();
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check,
   * using jars/zips files from the given directory to extends the classpath.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   * @param testJarsDirectory The directory containing jars and/or zip defining the classpath to be used
   *
   * @deprecated use {@link #newVerifier()} instead
   */
  @Deprecated
  public static void verify(String filename, JavaFileScanner check, String testJarsDirectory) {
    newVerifier()
      .onFile(filename)
      .withCheck(check)
      .withClassPath(FilesUtils.getClassPath(testJarsDirectory))
      .verifyIssues();
  }

  /**
   * Verifies that the provided file will not raise any issue when analyzed with the given check.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   *
   * @deprecated use {@link #newVerifier()} instead
   */
  @Deprecated
  public static void verifyNoIssue(String filename, JavaFileScanner check) {
    newVerifier()
      .onFile(filename)
      .withCheck(check)
      .verifyNoIssues();
  }

  /**
  * @deprecated use {@link #newVerifier()} instead
  */
  @Deprecated
  public static void verifyNoIssueWithoutSemantic(String filename, JavaFileScanner check, int javaVersion) {
    newVerifier()
      .onFile(filename)
      .withCheck(check)
      .withJavaVersion(javaVersion)
      .withoutSemantic()
      .verifyNoIssues();
  }

  /**
  * @deprecated use {@link #newVerifier()} instead
  */
  @Deprecated
  public static void verifyNoIssueWithoutSemantic(String filename, JavaFileScanner check) {
    newVerifier()
      .onFile(filename)
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }

  /**
   * Verifies that the provided file will not raise any issue when analyzed with the given check.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   * @param javaVersion The version to consider for the analysis (6 for java 1.6, 7 for 1.7, etc.)
   *
   * @deprecated use {@link #newVerifier()} instead
   */
  @Deprecated
  public static void verifyNoIssue(String filename, JavaFileScanner check, int javaVersion) {
    newVerifier()
      .onFile(filename)
      .withCheck(check)
      .withJavaVersion(javaVersion)
      .verifyNoIssues();
  }

  /**
   * Verifies that the provided file will only raise an issue on the file, with the given message, when analyzed using the given check.
   *
   * @param filename The file to be analyzed
   * @param message The message expected to be raised on the file
   * @param check The check to be used for the analysis
   *
   * @deprecated use {@link #newVerifier()} instead
   */
  @Deprecated
  public static void verifyIssueOnFile(String filename, String message, JavaFileScanner check) {
    newVerifier()
      .onFile(filename)
      .withCheck(check)
      .verifyIssueOnFile(message);
  }

  /**
   * @deprecated use {@link #newVerifier()} instead
   */
  @Deprecated
  public static void verifyIssueOnProject(String filename, String message, JavaFileScanner check) {
    newVerifier()
      .onFile(filename)
      .withCheck(check)
      .verifyIssueOnProject(message);
  }
}
