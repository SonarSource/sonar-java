/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks.verifier;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.fest.assertions.Fail;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceCode;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;


/**
 * It is possible to specify the absolute line number on which the issue should appear by appending {@literal "@<line>"} to "Noncompliant".
 * But usually better to use line number relative to the current, this is possible to do by prefixing the number with either '+' or '-'.
 * For example:
 * <pre>
 *   // Noncompliant@+1 {{do not import "java.util.List"}}
 *   import java.util.List;
 * </pre>
 */
public class JavaCheckVerifier extends SubscriptionVisitor {

  /**
   * Default location of the jars/zips to be taken into account when performing the analysis.
   */
  private static final String DEFAULT_TEST_JARS_DIRECTORY = "target/test-jars";
  private static final String TRIGGER = "// Noncompliant";
  private ArrayListMultimap<Integer, String> expected = ArrayListMultimap.create();
  private String testJarsDirectory;
  private boolean expectNoIssues = false;
  private String expectFileIssue;
  private Integer expectFileIssueOnline;

  private JavaCheckVerifier() {
    this.testJarsDirectory = DEFAULT_TEST_JARS_DIRECTORY;
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
   */
  public static void verify(String filename, JavaFileScanner check) {
    scanFile(filename, check, new JavaCheckVerifier());
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check, 
   * but using having the classpath extended with a collection of files (classes/jar/zip).
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   * @param classpath The files to be used as classpath
   */
  public static void verify(String filename, JavaFileScanner check, Collection<File> classpath) {
    scanFile(filename, check, new JavaCheckVerifier(), classpath);
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check, 
   * using jars/zips files from the given directory to extends the classpath.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   * @param testJarsDirectory The directory containing jars and/or zip defining the classpath to be used
   */
  public static void verify(String filename, JavaFileScanner check, String testJarsDirectory) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.testJarsDirectory = testJarsDirectory;
    scanFile(filename, check, javaCheckVerifier);
  }

  /**
   * Verifies that the provided file will not raise any issue when analyzed with the given check.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   */
  public static void verifyNoIssue(String filename, JavaFileScanner check) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.expectNoIssues = true;
    scanFile(filename, check, javaCheckVerifier);
  }

  /**
   * Verifies that the provided file will only raise an issue on the file, with the given message, when analyzed using the given check.
   *
   * @param filename The file to be analyzed
   * @param message The message expected to be raised on the file
   * @param check The check to be used for the analysis
   */
  public static void verifyIssueOnFile(String filename, String message, JavaFileScanner check) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.expectFileIssue = message;
    javaCheckVerifier.expectFileIssueOnline = null;
    scanFile(filename, check, javaCheckVerifier);
  }

  private static void scanFile(String filename, JavaFileScanner check, JavaCheckVerifier javaCheckVerifier) {
    Collection<File> classpath = Lists.newLinkedList();
    File testJars = new File(javaCheckVerifier.testJarsDirectory);
    if (testJars.exists()) {
      classpath = FileUtils.listFiles(testJars, new String[]{"jar", "zip"}, true);
    } else if (!DEFAULT_TEST_JARS_DIRECTORY.equals(javaCheckVerifier.testJarsDirectory)) {
      throw Fail.fail("The directory to be used to extend class path does not exists (" + testJars.getAbsolutePath() + ").");
    }
    classpath.add(new File("target/test-classes"));
    JavaAstScanner.scanSingleFile(new File(filename), new VisitorsBridge(Lists.newArrayList(check, javaCheckVerifier), Lists.newArrayList(classpath), null));
  }

  private static void scanFile(String filename, JavaFileScanner check, JavaCheckVerifier javaCheckVerifier, Collection<File> classpath) {
    JavaAstScanner.scanSingleFile(new File(filename), new VisitorsBridge(Lists.newArrayList(check, javaCheckVerifier), Lists.newArrayList(classpath), null));
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TRIVIA);
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    collectExpectedIssues(syntaxTrivia.comment(), syntaxTrivia.startLine());
  }

  private void collectExpectedIssues(String comment, int line) {
    if (comment.startsWith(TRIGGER)) {
      String cleanedComment = StringUtils.remove(comment, TRIGGER);

      String expectedMessage = StringUtils.substringBetween(cleanedComment, "{{", "}}");
      int expectedLine = line;

      cleanedComment = StringUtils.stripEnd(StringUtils.remove(cleanedComment, "{{" + expectedMessage + "}}"), " \t");
      if (StringUtils.startsWith(cleanedComment, "@")) {
        final int lineAdjustment;
        final char firstChar = cleanedComment.charAt(1);
        final int endIndex = cleanedComment.indexOf(' ');
        if (endIndex == -1) {
          lineAdjustment = Integer.parseInt(cleanedComment.substring(2));
          cleanedComment = "";
        } else {
          lineAdjustment = Integer.parseInt(cleanedComment.substring(2, endIndex));
          cleanedComment = cleanedComment.substring(endIndex + 1).trim();
        }
        if (firstChar == '+') {
          expectedLine += lineAdjustment;
        } else if (firstChar == '-') {
          expectedLine -= lineAdjustment;
        } else {
          throw Fail.fail("Use only '@+N' or '@-N' to shifts messages.");
        }
      }
      cleanedComment = StringUtils.trim(cleanedComment);
      int times = StringUtils.isEmpty(cleanedComment) ? 1 : Integer.parseInt(cleanedComment);
      for (int i = 0; i < times; i++) {
        expected.put(expectedLine, expectedMessage);
      }
    }
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    expected.clear();
    super.scanFile(context);
    VisitorsBridge.DefaultJavaFileScannerContext djfsc = (VisitorsBridge.DefaultJavaFileScannerContext) context;
    // leave file.
    checkIssues(djfsc.sourceFile);
    expected.clear();
  }

  private void checkIssues(SourceCode sourceCode) {
    if (expectNoIssues) {
      assertNoIssues(sourceCode);
    } else if (StringUtils.isNotEmpty(expectFileIssue)) {
      assertSingleIssue(sourceCode);
    } else {
      assertMultipleIssue(sourceCode);
    }
  }

  private void assertMultipleIssue(SourceCode sourceCode) throws AssertionError {
    Preconditions.checkState(sourceCode.hasCheckMessages(), "At least one issue expected");
    List<Integer> unexpectedLines = Lists.newLinkedList();
    for (CheckMessage checkMessage : sourceCode.getCheckMessages()) {
      int line = checkMessage.getLine();
      if (!expected.containsKey(line)) {
        unexpectedLines.add(line);
      } else {
        List<String> list = expected.get(line);
        String expectedMessage = list.remove(list.size() - 1);
        if (expectedMessage != null) {
          assertThat(checkMessage.getText(Locale.US)).isEqualTo(expectedMessage);
        }
      }
    }
    if (!expected.isEmpty() || !unexpectedLines.isEmpty()) {
      Collections.sort(unexpectedLines);
      String expectedMsg = !expected.isEmpty() ? ("Expected " + expected) : "";
      String unexpectedMsg = !unexpectedLines.isEmpty() ? ((expectedMsg.isEmpty() ? "" : ", ") + "Unexpected at " + unexpectedLines) : "";
      throw Fail.fail(expectedMsg + unexpectedMsg);
    }
  }

  private void assertSingleIssue(SourceCode sourceCode) {
    Set<CheckMessage> checkMessages = sourceCode.getCheckMessages();
    Preconditions.checkState(checkMessages.size() == 1, "A single issue is expected with line " + expectFileIssueOnline);
    CheckMessage checkMessage = Iterables.getFirst(checkMessages, null);
    assertThat(checkMessage.getLine()).isEqualTo(expectFileIssueOnline);
    assertThat(checkMessage.getText(Locale.US)).isEqualTo(expectFileIssue);
  }

  private void assertNoIssues(SourceCode sourceCode) {
    assertThat(sourceCode.getCheckMessages()).overridingErrorMessage("No issues expected but got: " + sourceCode.getCheckMessages()).isEmpty();
    // make sure we do not copy&paste verifyNoIssue call when we intend to call verify
    assertThat(expected.isEmpty()).overridingErrorMessage("The file should not declare noncompliants when no issues are expected").isTrue();
  }

}
