/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.se;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.fest.assertions.Fail;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
public class JavaCheckVerifier extends SubscriptionVisitor {

  /**
   * Default location of the jars/zips to be taken into account when performing the analysis.
   */
  private static final String DEFAULT_TEST_JARS_DIRECTORY = "target/test-jars";
  private static final String TRIGGER = "// Noncompliant";
  private final ArrayListMultimap<Integer, Map<IssueAttribute, String>> expected = ArrayListMultimap.create();
  private String testJarsDirectory;
  private boolean expectNoIssues = false;
  private String expectFileIssue;
  private Integer expectFileIssueOnline;

  private static final Map<String, IssueAttribute> ATTRIBUTE_MAP = ImmutableMap.<String, IssueAttribute>builder()
    .put("message", IssueAttribute.MESSAGE)
    .put("effortToFix", IssueAttribute.EFFORT_TO_FIX)
    .put("sc", IssueAttribute.START_COLUMN)
    .put("startColumn", IssueAttribute.START_COLUMN)
    .put("el", IssueAttribute.END_LINE)
    .put("endLine", IssueAttribute.END_LINE)
    .put("ec", IssueAttribute.END_COLUMN)
    .put("endColumn", IssueAttribute.END_COLUMN)
    .put("secondary", IssueAttribute.SECONDARY_LOCATIONS)
    .build();

  enum IssueAttribute {
    MESSAGE,
    START_COLUMN,
    END_COLUMN,
    END_LINE,
    EFFORT_TO_FIX,
    SECONDARY_LOCATIONS
  }

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
      Fail.fail("The directory to be used to extend class path does not exists (" + testJars.getAbsolutePath() + ").");
    }
    classpath.add(new File("target/test-classes"));
    scanFile(filename, check, javaCheckVerifier, classpath);
  }

  private static void scanFile(String filename, JavaFileScanner check, JavaCheckVerifier javaCheckVerifier, Collection<File> classpath) {
    VisitorsBridge visitorsBridge = new VisitorsBridge(Lists.newArrayList(check, javaCheckVerifier), Lists.newArrayList(classpath), null);
    JavaAstScanner.scanSingleFileForTests(new File(filename), visitorsBridge);
    VisitorsBridge.TestJavaFileScannerContext testJavaFileScannerContext = visitorsBridge.lastCreatedTestContext();
    checkIssues(testJavaFileScannerContext.getIssues(), javaCheckVerifier);
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

      EnumMap<IssueAttribute, String> attr = new EnumMap<>(IssueAttribute.class);
      String expectedMessage = StringUtils.substringBetween(cleanedComment, "{{", "}}");
      if (StringUtils.isNotEmpty(expectedMessage)) {
        attr.put(IssueAttribute.MESSAGE, expectedMessage);
      }
      int expectedLine = line;
      String attributesSubstr = extractAttributes(comment, attr);

      cleanedComment = StringUtils.stripEnd(StringUtils.remove(StringUtils.remove(cleanedComment, "[[" + attributesSubstr + "]]"), "{{" + expectedMessage + "}}"), " \t");
      if (StringUtils.startsWith(cleanedComment, "@")) {
        final int lineAdjustment;
        final char firstChar = cleanedComment.charAt(1);
        final int endIndex = cleanedComment.indexOf(' ');
        if (endIndex == -1) {
          lineAdjustment = Integer.parseInt(cleanedComment.substring(2));
        } else {
          lineAdjustment = Integer.parseInt(cleanedComment.substring(2, endIndex));
        }
        if (firstChar == '+') {
          expectedLine += lineAdjustment;
        } else if (firstChar == '-') {
          expectedLine -= lineAdjustment;
        } else {
          Fail.fail("Use only '@+N' or '@-N' to shifts messages.");
        }
      }
      updateEndLine(expectedLine, attr);
      expected.put(expectedLine, attr);
    }
  }

  private static void updateEndLine(int expectedLine, EnumMap<IssueAttribute, String> attr) {
    if (attr.containsKey(IssueAttribute.END_LINE)) {
      String endLineStr = attr.get(IssueAttribute.END_LINE);
      if (endLineStr.startsWith("+")) {
        int endLine = Integer.parseInt(endLineStr);
        attr.put(IssueAttribute.END_LINE, Integer.toString(expectedLine + endLine));
      } else {
        Fail.fail("endLine attribute should be relative to the line and must be +N with N integer");
      }
    }
  }

  private static String extractAttributes(String comment, Map<IssueAttribute, String> attr) {
    String attributesSubstr = StringUtils.substringBetween(comment, "[[", "]]");
    if (!StringUtils.isEmpty(attributesSubstr)) {
      Iterable<String> attributes = Splitter.on(";").split(attributesSubstr);
      for (String attribute : attributes) {
        String[] split = StringUtils.split(attribute, '=');
        if (split.length == 2 && ATTRIBUTE_MAP.containsKey(split[0])) {
          attr.put(ATTRIBUTE_MAP.get(split[0]), split[1]);
        } else {
          Fail.fail("// Noncompliant attributes not valid: " + attributesSubstr);
        }
      }
    }
    return attributesSubstr;
  }

  private static void checkIssues(Set<AnalyzerMessage> issues, JavaCheckVerifier javaCheckVerifier) {
    if (javaCheckVerifier.expectNoIssues) {
      assertNoIssues(javaCheckVerifier.expected, issues);
    } else if (StringUtils.isNotEmpty(javaCheckVerifier.expectFileIssue)) {
      assertSingleIssue(javaCheckVerifier.expectFileIssueOnline, javaCheckVerifier.expectFileIssue, issues);
    } else {
      assertMultipleIssue(javaCheckVerifier.expected, issues);
    }
  }

  private static void assertMultipleIssue(Multimap<Integer, Map<IssueAttribute, String>> expected, Set<AnalyzerMessage> issues) throws AssertionError {
    Preconditions.checkState(!issues.isEmpty(), "At least one issue expected");
    List<Integer> unexpectedLines = Lists.newLinkedList();
    for (AnalyzerMessage issue : issues) {
      validateIssue(expected, unexpectedLines, issue);
    }
    if (!expected.isEmpty() || !unexpectedLines.isEmpty()) {
      Collections.sort(unexpectedLines);
      String expectedMsg = !expected.isEmpty() ? ("Expected " + expected) : "";
      String unexpectedMsg = !unexpectedLines.isEmpty() ? ((expectedMsg.isEmpty() ? "" : ", ") + "Unexpected at " + unexpectedLines) : "";
      Fail.fail(expectedMsg + unexpectedMsg);
    }
  }

  private static void validateIssue(Multimap<Integer, Map<IssueAttribute, String>> expected, List<Integer> unexpectedLines, AnalyzerMessage issue) {
    int line = issue.getLine();
    if (expected.containsKey(line)) {
      Map<IssueAttribute, String> attrs = Iterables.getLast(expected.get(line));
      assertEquals(issue, attrs, IssueAttribute.MESSAGE);
      Double cost = issue.getCost();
      if (cost != null) {
        assertEquals(Integer.toString(cost.intValue()), attrs, IssueAttribute.EFFORT_TO_FIX);
      }
      validateAnalyzerMessage(attrs, issue);
      expected.remove(line, attrs);
    } else {
      unexpectedLines.add(line);
    }
  }

  private static void validateAnalyzerMessage(Map<IssueAttribute, String> attrs, AnalyzerMessage analyzerMessage) {
    Double effortToFix = analyzerMessage.getCost();
    if (effortToFix != null) {
      assertEquals(Integer.toString(effortToFix.intValue()), attrs, IssueAttribute.EFFORT_TO_FIX);
    }
    AnalyzerMessage.TextSpan textSpan = analyzerMessage.primaryLocation();
    assertEquals(normalizeColumn(textSpan.startCharacter), attrs, IssueAttribute.START_COLUMN);
    assertEquals(Integer.toString(textSpan.endLine), attrs, IssueAttribute.END_LINE);
    assertEquals(normalizeColumn(textSpan.endCharacter), attrs, IssueAttribute.END_COLUMN);
    if (attrs.containsKey(IssueAttribute.SECONDARY_LOCATIONS)) {
      List<AnalyzerMessage> secondaryLocations = analyzerMessage.secondaryLocations;
      Multiset<String> actualLines = HashMultiset.create();
      for (AnalyzerMessage secondaryLocation : secondaryLocations) {
        actualLines.add(Integer.toString(secondaryLocation.getLine()));
      }
      List<String> expected = Lists.newArrayList(Splitter.on(",").omitEmptyStrings().trimResults().split(attrs.get(IssueAttribute.SECONDARY_LOCATIONS)));
      List<String> unexpected = new ArrayList<>();
      for (String actualLine : actualLines) {
        if (expected.contains(actualLine)) {
          expected.remove(actualLine);
        } else {
          unexpected.add(actualLine);
        }
      }
      if (!expected.isEmpty() || !unexpected.isEmpty()) {
        Fail.fail("Secondary locations: expected: " + expected + " unexpected:" + unexpected);
      }
    }
  }

  private static String normalizeColumn(int startCharacter) {
    return Integer.toString(startCharacter + 1);
  }

  private static void assertEquals(String value, Map<IssueAttribute, String> attributes, IssueAttribute attribute) {
    if (attributes.containsKey(attribute)) {
      assertThat(value).as("attribute mismatch for " + attribute + ": " + attributes).isEqualTo(attributes.get(attribute));
    }
  }

  private static void assertEquals(AnalyzerMessage issue, Map<IssueAttribute, String> attributes, IssueAttribute attribute) {
    if (attributes.containsKey(attribute)) {
      assertThat(issue.getMessage()).as("line " + issue.getLine() + " attribute mismatch for " + attribute + ": " + attributes).isEqualTo(attributes.get(attribute));
    }
  }

  private static void assertSingleIssue(Integer expectFileIssueOnline, String expectFileIssue, Set<AnalyzerMessage> issues) {
    Preconditions.checkState(issues.size() == 1, "A single issue is expected with line " + expectFileIssueOnline);
    AnalyzerMessage issue = Iterables.getFirst(issues, null);
    assertThat(issue.getLine()).isEqualTo(expectFileIssueOnline);
    assertThat(issue.getMessage()).isEqualTo(expectFileIssue);
  }

  private static void assertNoIssues(Multimap<Integer, Map<IssueAttribute, String>> expected, Set<AnalyzerMessage> issues) {
    assertThat(issues).overridingErrorMessage("No issues expected but got: " + issues).isEmpty();
    // make sure we do not copy&paste verifyNoIssue call when we intend to call verify
    assertThat(expected.isEmpty()).overridingErrorMessage("The file should not declare noncompliants when no issues are expected").isTrue();
  }

}
