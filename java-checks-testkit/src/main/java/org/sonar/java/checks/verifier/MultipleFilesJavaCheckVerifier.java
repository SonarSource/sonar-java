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
package org.sonar.java.checks.verifier;

import com.google.common.annotations.Beta;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaFileScanner;

/**
 * Please read the documentation of @see org.sonar.java.checks.verifier.JavaCheckVerifier
 *
 * The main difference is that this class run a check on multiple files and verifies the results at the end of analysis.
 */
@Beta
public class MultipleFilesJavaCheckVerifier extends CheckVerifier {

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
    verify(new MultipleFilesJavaCheckVerifier(), filesToScan, check, false, true);
  }

  /**
   * Verifies that no issues are raised after analyzing all the given files with the given check.
   *
   * @param filesToScan The files to be analyzed
   * @param check The check to be used for the analysis
   */
  public static void verifyNoIssue(List<String> filesToScan, JavaFileScanner check) {
    verify(new MultipleFilesJavaCheckVerifier(), filesToScan, check, true, true);
  }

  /**
   * Verifies that no issues are raised after analyzing all given files with the given check when semantic is not available.
   *
   * @param filesToScan The files to be analyzed
   * @param check The check to be used for the analysis
   */
  public static void verifyNoIssueWithoutSemantic(List<String> filesToScan, JavaFileScanner check) {
    MultipleFilesJavaCheckVerifier verifier = new MultipleFilesJavaCheckVerifier() {
      @Override
      public String getExpectedIssueTrigger() {
        return "// NOSEMANTIC_ISSUE";
      }
    };
    verify(verifier, filesToScan, check, true, false);
  }

  private static void verify(MultipleFilesJavaCheckVerifier verifier, List<String> filesToScan, JavaFileScanner check, boolean expectNoIssues, boolean withSemantic) {
    if (expectNoIssues) {
      verifier.expectNoIssues();
    }
    Set<AnalyzerMessage> issues = verifier.scanFiles(filesToScan, check, withSemantic);
    verifier.checkIssues(issues, expectNoIssues);
  }

  private Set<AnalyzerMessage> scanFiles(List<String> filesToScan, JavaFileScanner check, boolean withSemantic) {
    List<File> classPath = JavaCheckVerifier.getClassPath(JavaCheckVerifier.DEFAULT_TEST_JARS_DIRECTORY);
    VisitorsBridgeForTests visitorsBridge;
    if (withSemantic) {
      visitorsBridge = new VisitorsBridgeForTests(Arrays.asList(check, new JavaCheckVerifier.ExpectedIssueCollector(this)), classPath, null);
    } else {
      visitorsBridge = new VisitorsBridgeForTests(Arrays.asList(check, new JavaCheckVerifier.ExpectedIssueCollector(this)), null);
    }
    visitorsBridge.setJavaVersion(new JavaVersionImpl());
    JavaAstScanner astScanner = new JavaAstScanner(null);
    astScanner.setVisitorBridge(visitorsBridge);
    astScanner.scan(filesToScan.stream()
      .map(filename -> new TestInputFileBuilder("", filename).setCharset(StandardCharsets.UTF_8).build())
      .collect(Collectors.toList()));

    VisitorsBridgeForTests.TestJavaFileScannerContext testJavaFileScannerContext = visitorsBridge.lastCreatedTestContext();
    return testJavaFileScannerContext.getIssues();
  }

  @Override
  public String getExpectedIssueTrigger() {
    return "// " + ISSUE_MARKER;
  }

}
