/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;
import org.sonar.plugins.java.api.JavaFileScanner;

/**
 * This interface defines how to use checks (rules) verifiers. It's goal is to provide all the required information
 * to the analyzer to verify checks' expected behavior.
 * <p>
 * The starting point to define a verifier is {@link #newVerifier()}. Then, configuration can be specified.
 * <p>
 * It is required to provide to the verifier at least the following:
 * <ul>
 *   <li>A rule, by calling {@link #withCheck(JavaFileScanner)}, or {@link #withChecks(JavaFileScanner...)}</li>
 *   <li>A test file, by calling {@link #onFile(String)}, {@link #onFiles(String...)}, or {@link #onFiles(Collection)}</li>
 * </ul>
 * Methods starting with "verify..." (e.g {@link #verifyIssues()} ) are the methods which effectively validate the rule.
 * It is required to call any of them at the end of the verifier's configuration in order to trigger the verification.
 * <strong>Nothing will happen if one of these method is not called.</strong>
 * <p>
 * In the test file(s), lines on which it is expected to have issues being raised have to be flagged with a comment
 * prefixed by the "Noncompliant" string, followed by some optional details/specificity of the expected issue.
 * <p>
 * It is possible to specify the absolute line number on which the issue should appear by appending {@literal "@<line>"} to "Noncompliant".
 * But it is usually better to use line number relative to the current, this is possible to do by prefixing the number with either '+' or '-'.
 * <p>
 * For example, the following comment says that an issue is going to be raised on the next line (@+1), with the given message:
 * <pre>
 *   // Noncompliant@+1 {{do not import "java.util.List"}}
 *   import java.util.List;
 * </pre>
 * Full syntax:
 * <pre>
 *   // Noncompliant@+1 [[startColumn=1;endLine=+1;endColumn=2;effortToFix=4;secondary=3,4]] {{issue message}}
 * </pre>
 * Some attributes can also be written using a simplified form, for instance:
 * <pre>
 *   // Noncompliant [[sc=14;ec=42]] {{issue message}}
 * </pre>
 * Finally, note that attributes between {@literal [[...]]} are all optional:
 * <ul>
 *   <li>startColumn (sc): column where the highlight starts</li>
 *   <li>endLine (el): relative endLine where the highlight ends (i.e. +1), same line if omitted</li>
 *   <li>endColumn (ec): column where the highlight ends</li>
 *   <li>effortToFix: the cost to fix as integer</li>
 *   <li>secondary: a comma separated list of integers identifying the lines of secondary locations if any</li>
 * </ul>
 */
public interface CheckVerifier {

  /**
   * Entry point of check verification. Will return a new instance of verifier to be configured.
   *
   * @return the newly instantiated verifier
   */
  static CheckVerifier newVerifier() {
    return InternalCheckVerifier.newInstance();
  }

  /**
   * Defines the check to be verified against at least one test file.
   *
   * @param check the rule to be verified
   *
   * @return the verifier configured to use the check provided as argument
   */
  CheckVerifier withCheck(JavaFileScanner check);

  /**
   * Defines the check(s) to be verified against at least one test file.
   *
   * @param checks the rules to be verified
   *
   * @return the verifier configured to use the checks provided as argument
   */
  CheckVerifier withChecks(JavaFileScanner... checks);

  /**
   * Defines the classpath to be used for the verification. Usually used when the code of the
   * test files requires the knowledge of a particular set of libraries or java compiled classes.
   *
   * @param classpath a collection of file which defines the classes/jars/zips which contains 
   * the bytecode to be used as classpath when executing the rule
   *
   * @return the verifier configured to use the files provided as argument as classpath
   */
  CheckVerifier withClassPath(Collection<File> classpath);

  /**
   * Defines the java version syntax to be used for the verification. Usually used when the code of the
   * test files target explicitly a given version (eg. java 7), where a particular syntax/API has been introduced.
   *
   * @param javaVersionAsInt defines the java language syntax version to be considered during verification, provided as an integer.
   * For instance, for Java 1.7, use '7'. For Java 12, simply '12'.
   *
   * @return the verifier configured to consider the provided test file(s) as following the syntax of the given java version
   */
  CheckVerifier withJavaVersion(int javaVersionAsInt);

  /**
   * Defines the whether the current file is analyzer in an android context.
   *
   * @return the verifier currently configured
   */
  CheckVerifier withinAndroidContext(boolean inAndroidContext);

  /**
   * Defines the filename to be verified with the given rule(s). This file should contain all the "Noncompliant"
   * comments defining the expected issues.
   *
   * @param filename the file to be analyzed
   *
   * @return the verifier configured to consider the provided test file as source for the rule(s)
   */
  CheckVerifier onFile(String filename);

  /**
   * Defines the filenames to be verified with the given rule(s). These files should all contain "Noncompliant"
   * comments defining the expected issues.
   *
   * @param filenames the files to be analyzed
   *
   * @return the verifier configured to consider the provided test file(s) as source for the rule(s)
   */
  CheckVerifier onFiles(String... filenames);

  /**
   * Defines a collection of filenames to be verified with the given rule(s). These files should all
   * contain "Noncompliant" comments defining the expected issues.
   *
   * @param filenames a collection of files to be analyzed
   *
   * @return the verifier configured to consider the provided test file(s) as source for the rule(s)
   */
  CheckVerifier onFiles(Collection<String> filenames);

  /**
   * Adds a collection of files with an expected status to be verified by the given rule(s).
   * If a file by the same filename is already listed to be analyzed, an exception is thrown.
   * @param status The status of the files to be analyzed
   * @param filenames a collection of files to be analyzed
   * @return the verifier configured
   * @throws IllegalArgumentException if a file by the same filename had already been added
   */
  CheckVerifier addFiles(InputFile.Status status, String... filenames);

  /**
   * Adds a collection of files with an expected status.
   * If a file by the same filename is already listed to be analyzed, an exception is thrown.
   * @param status The status of the files to be analyzed
   * @param filenames a collection of files to be analyzed
   * @return the verifier configured
   * @throws IllegalArgumentException if a file by the same filename had already been added
   */
  CheckVerifier addFiles(InputFile.Status status, Collection<String> filenames);

  /**
   * Tells the verifier that no bytecode will be provided. This method is usually used in combination with
   * {@link #verifyNoIssues()}, to assert the fact that if no bytecode is provided, the rule will not raise
   * any issues.
   *
   * @return the verifier configured to consider that no bytecode will be provided for analysis
   */
  CheckVerifier withoutSemantic();

  /**
   * Tells the verifier to feed the check with cached information in its preScan phase.
   * @param readCache A source of information from previous analyses
   * @param writeCache A place to dump information at the end of the analysis
   * @return the verifier configured with the caches to use.
   */
  CheckVerifier withCache(@Nullable ReadCache readCache, @Nullable WriteCache writeCache);

  /**
   * Verifies that all the expected issues are correctly raised by the rule(s),
   * at their expected positions and attributes.
   */
  void verifyIssues();

  /**
   * Verifies that an issue (only one) is raised directly on the file, and not
   * within the content of the file.
   *
   * @param expectedIssueMessage the message to be expected with the issue.
   */
  void verifyIssueOnFile(String expectedIssueMessage);

  /**
   * Verifies that an issue (only one) is raised directly on the project which would include this file,
   * and not within the content of the file.
   *
   * @param expectedIssueMessage
   */
  void verifyIssueOnProject(String expectedIssueMessage);

  /**
   * Verifies that no issues are raised by the rule(s) on the given file(s).
   */
  void verifyNoIssues();
}
