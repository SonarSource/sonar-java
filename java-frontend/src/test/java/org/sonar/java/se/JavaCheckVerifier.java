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
package org.sonar.java.se;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Fail;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaFileScanner;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.java.se.Expectations.IssueAttribute.EFFORT_TO_FIX;
import static org.sonar.java.se.Expectations.IssueAttribute.END_COLUMN;
import static org.sonar.java.se.Expectations.IssueAttribute.END_LINE;
import static org.sonar.java.se.Expectations.IssueAttribute.FLOWS;
import static org.sonar.java.se.Expectations.IssueAttribute.MESSAGE;
import static org.sonar.java.se.Expectations.IssueAttribute.SECONDARY_LOCATIONS;
import static org.sonar.java.se.Expectations.IssueAttribute.START_COLUMN;

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
public class JavaCheckVerifier {

  /**
   * Default location of the jars/zips to be taken into account when performing the analysis.
   */
  private static final String DEFAULT_TEST_JARS_DIRECTORY = "target/test-jars";
  private final String testJarsDirectory;
  private final Expectations expectations;

  public JavaCheckVerifier() {
    this.testJarsDirectory = DEFAULT_TEST_JARS_DIRECTORY;
    this.expectations = new Expectations();
  }

  private JavaCheckVerifier(Expectations expectations) {
    this(DEFAULT_TEST_JARS_DIRECTORY, expectations);
  }

  private JavaCheckVerifier(String testJarsDirectory, Expectations expectations) {
    this.testJarsDirectory = testJarsDirectory;
    this.expectations = expectations;
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
  public static void verify(String filename, JavaFileScanner... check) {
    new JavaCheckVerifier().scanFile(filename, check);
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
    new JavaCheckVerifier().scanFile(filename, new JavaFileScanner[] {check}, classpath);
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
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier(testJarsDirectory, new Expectations());
    javaCheckVerifier.scanFile(filename, new JavaFileScanner[] {check});
  }

  /**
   * Verifies that the provided file will not raise any issue when analyzed with the given check.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   */
  public static void verifyNoIssue(String filename, JavaFileScanner check) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier(new Expectations(true, null, null));
    javaCheckVerifier.scanFile(filename, new JavaFileScanner[] {check});
  }

  /**
   * Verifies that the provided file will only raise an issue on the file, with the given message, when analyzed using the given check.
   *
   * @param filename The file to be analyzed
   * @param message The message expected to be raised on the file
   * @param check The check to be used for the analysis
   */
  public static void verifyIssueOnFile(String filename, String message, JavaFileScanner check) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier(new Expectations(false, message, null));
    javaCheckVerifier.scanFile(filename, new JavaFileScanner[] {check});
  }

  public void scanFile(String filename, JavaFileScanner[] checks) {
    Collection<File> classpath = Lists.newLinkedList();
    File testJars = new File(testJarsDirectory);
    if (testJars.exists()) {
      classpath = FileUtils.listFiles(testJars, new String[] {"jar", "zip"}, true);
    } else if (!DEFAULT_TEST_JARS_DIRECTORY.equals(testJarsDirectory)) {
      fail("The directory to be used to extend class path does not exists (" + testJars.getAbsolutePath() + ").");
    }
    classpath.add(new File("target/test-classes"));
    scanFile(filename, checks, classpath);
  }

  private void scanFile(String filename, JavaFileScanner[] checks, Collection<File> classpath) {
    List<JavaFileScanner> visitors = new ArrayList<>(Arrays.asList(checks));
    visitors.add(expectations.parser());
    InputFile inputFile = TestUtils.inputFile(filename);
    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(visitors, Lists.newArrayList(classpath), sonarComponents());
    JavaAstScanner.scanSingleFileForTests(inputFile, visitorsBridge);
    VisitorsBridgeForTests.TestJavaFileScannerContext testJavaFileScannerContext = visitorsBridge.lastCreatedTestContext();
    checkIssues(testJavaFileScannerContext.getIssues());
  }

  private static SonarComponents sonarComponents() {
    SensorContextTester context = SensorContextTester.create(new File(""))
      .setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    context.setSettings(new MapSettings().setProperty("sonar.java.failOnException", true));
    SonarComponents sonarComponents = new SonarComponents(null, context.fileSystem(), null, null, null) {
      @Override
      public boolean reportAnalysisError(RecognitionException re, InputFile inputFile) {
        return false;
      }
    };
    sonarComponents.setSensorContext(context);
    return sonarComponents;
  }

  protected void checkIssues(Set<AnalyzerMessage> issues) {
    if (expectations.expectNoIssues) {
      assertNoIssues(expectations.issues, issues);
    } else if (StringUtils.isNotEmpty(expectations.expectFileIssue)) {
      assertSingleIssue(expectations.expectFileIssueOnLine, expectations.expectFileIssue, issues);
    } else {
      assertMultipleIssue(issues);
    }
  }

  private void assertMultipleIssue(Set<AnalyzerMessage> issues) throws AssertionError {
    if (issues.isEmpty()) {
      Fail.fail("At least one issue expected");
    }
    List<Integer> unexpectedLines = Lists.newLinkedList();
    Multimap<Integer, Expectations.Issue> expected = expectations.issues;

    for (AnalyzerMessage issue : issues) {
      validateIssue(expected, unexpectedLines, issue);
    }
    if (!expected.isEmpty() || !unexpectedLines.isEmpty()) {
      Collections.sort(unexpectedLines);
      List<Integer> expectedLines = expected.keys().stream().sorted().collect(Collectors.toList());
      String expectedMsg = !expectedLines.isEmpty() ? ("Expected at " + expectedLines) : "";
      String unexpectedMsg = !unexpectedLines.isEmpty() ? ((expectedMsg.isEmpty() ? "" : ", ") + "Unexpected at " + unexpectedLines) : "";
      fail(expectedMsg + unexpectedMsg);
    }
    assertSuperfluousFlows();
  }

  private void assertSuperfluousFlows() {
    Set<String> unseenFlowIds = expectations.unseenFlowIds();
    Map<String, String> unseenFlowWithLines = unseenFlowIds.stream()
        .collect(Collectors.toMap(Function.identity(), expectations::flowToLines));

    assertThat(unseenFlowWithLines).overridingErrorMessage("Following flow comments were observed, but not referenced by any issue: " + unseenFlowWithLines).isEmpty();
  }

  private void validateIssue(Multimap<Integer, Expectations.Issue> expected,
    List<Integer> unexpectedLines, AnalyzerMessage issue) {
    int line = issue.getLine();
    if (expected.containsKey(line)) {
      Expectations.Issue attrs = Iterables.getLast(expected.get(line));
      assertAttributeMatch(issue, attrs, MESSAGE);
      validateAnalyzerMessageAttributes(attrs, issue);
      expected.remove(line, attrs);
    } else {
      unexpectedLines.add(line);
    }
  }

  private void validateAnalyzerMessageAttributes(Expectations.Issue attrs, AnalyzerMessage analyzerMessage) {
    Double effortToFix = analyzerMessage.getCost();
    if (effortToFix != null) {
      assertAttributeMatch(effortToFix, attrs, EFFORT_TO_FIX);
    }
    validateLocation(attrs, analyzerMessage.primaryLocation());
    if (attrs.containsKey(SECONDARY_LOCATIONS)) {
      List<AnalyzerMessage> actual = analyzerMessage.flows.stream().map(l -> l.isEmpty() ? null : l.get(0)).filter(Objects::nonNull).collect(Collectors.toList());
      List<Integer> expected = (List<Integer>) attrs.get(SECONDARY_LOCATIONS);
      validateSecondaryLocations(actual, expected);
    }
    if (attrs.containsKey(FLOWS)) {
      validateFlows(analyzerMessage.flows, (List<String>) attrs.get(FLOWS));
    }
  }

  private static void validateLocation(Map<Expectations.IssueAttribute, Object> attrs, AnalyzerMessage.TextSpan textSpan) {
    Objects.requireNonNull(textSpan);
    assertAttributeMatch(normalizeColumn(textSpan.startCharacter), attrs, START_COLUMN);
    assertAttributeMatch(textSpan.endLine, attrs, END_LINE);
    assertAttributeMatch(normalizeColumn(textSpan.endCharacter), attrs, END_COLUMN);
  }

  private void validateFlows(List<List<AnalyzerMessage>> actual, List<String> expectedFlowIds) {
    Map<String, List<AnalyzerMessage>> foundFlows = new HashMap<>();
    List<List<AnalyzerMessage>> unexpectedFlows = new ArrayList<>();
    actual.forEach(f -> validateFlow(f, foundFlows, unexpectedFlows));
    expectedFlowIds.removeAll(foundFlows.keySet());

    assertExpectedAndMissingFlows(expectedFlowIds, unexpectedFlows);
    validateFoundFlows(foundFlows);
  }

  private void assertExpectedAndMissingFlows(List<String> expectedFlowIds, List<List<AnalyzerMessage>> unexpectedFlows) {
    if (expectedFlowIds.size() == 1 && expectedFlowIds.size() == unexpectedFlows.size()) {
      assertSoleFlowDiscrepancy(expectedFlowIds.get(0), unexpectedFlows.get(0));
    }

    String unexpectedMsg = unexpectedFlows.stream()
      .map(JavaCheckVerifier::flowToString)
      .collect(joining("\n"));

    String missingMsg = expectedFlowIds.stream().map(fid -> String.format("%s [%s]", fid, expectations.flowToLines(fid))).collect(joining(","));

    if (!unexpectedMsg.isEmpty() || !missingMsg.isEmpty()) {
      unexpectedMsg = unexpectedMsg.isEmpty() ? "" : String.format("Unexpected flows: %s. ", unexpectedMsg);
      missingMsg = missingMsg.isEmpty() ? "" : String.format("Missing flows: %s.", missingMsg);
      Fail.fail(unexpectedMsg + missingMsg);
    }
  }

  private void assertSoleFlowDiscrepancy(String expectedId, List<AnalyzerMessage> actualFlow) {
    SortedSet<Expectations.FlowComment> expected = expectations.flows.get(expectedId);
    List<Integer> expectedLines = expected.stream().map(f -> f.line).collect(Collectors.toList());
    List<Integer> actualLines = actualFlow.stream().map(AnalyzerMessage::getLine).collect(Collectors.toList());
    assertThat(actualLines).as("Flow " + expectedId + " has line differences").isEqualTo(expectedLines);
  }

  private void validateFlow(List<AnalyzerMessage> flow, Map<String, List<AnalyzerMessage>> foundFlows, List<List<AnalyzerMessage>> unexpectedFlows) {
    Optional<String> flowId = expectations.containFlow(flow);
    if (flowId.isPresent()) {
      foundFlows.put(flowId.get(), flow);
    } else {
      unexpectedFlows.add(flow);
    }
  }

  private void validateFoundFlows(Map<String, List<AnalyzerMessage>> foundFlows) {
    foundFlows.forEach((flowId, flow) -> validateFlowAttributes(flow, flowId));
  }

  private void validateFlowAttributes(List<AnalyzerMessage> actual, String flowId) {
    SortedSet<Expectations.FlowComment> expected = expectations.flows.get(flowId);

    validateFlowMessages(actual, flowId, expected);

    Iterator<AnalyzerMessage> actualIterator = actual.iterator();
    Iterator<Expectations.FlowComment> expectedIterator = expected.iterator();
    while (actualIterator.hasNext() && expectedIterator.hasNext()) {
      AnalyzerMessage.TextSpan flowLocation = actualIterator.next().primaryLocation();
      assertThat(flowLocation).isNotNull();
      Expectations.FlowComment flowComment = expectedIterator.next();
      validateLocation(flowComment.attributes, flowLocation);
    }
  }

  private void validateFlowMessages(List<AnalyzerMessage> actual, String flowId, SortedSet<Expectations.FlowComment> expected) {
    List<String> actualMessages = actual.stream().map(AnalyzerMessage::getMessage).collect(Collectors.toList());
    List<String> expectedMessages = expected.stream().map(Expectations.FlowComment::message).collect(Collectors.toList());

    replaceExpectedNullWithActual(actualMessages, expectedMessages);

    assertThat(actualMessages).as("Wrong messages in flow " + flowId + " [" + expectations.flowToLines(flowId) + "]").isEqualTo(expectedMessages);
  }

  private void replaceExpectedNullWithActual(List<String> actualMessages, List<String> expectedMessages) {
    if (actualMessages.size() == expectedMessages.size()) {
      for (int i =0; i < actualMessages.size(); i++) {
        if (expectedMessages.get(i) == null) {
          expectedMessages.set(i, actualMessages.get(i));
        }
      }
    }
  }

  private static String flowToString(List<AnalyzerMessage> flow) {
    return flow.stream().map(m -> String.valueOf(m.getLine())).collect(joining(",","[","]"));
  }

  private static void validateSecondaryLocations(List<AnalyzerMessage> actual, List<Integer> expected) {
    Multiset<Integer> actualLines = HashMultiset.create();
    actualLines.addAll(actual.stream().map(secondaryLocation -> secondaryLocation.getLine()).collect(Collectors.toList()));
    List<Integer> unexpected = new ArrayList<>();
    for (Integer actualLine : actualLines) {
      if (expected.contains(actualLine)) {
        expected.remove(actualLine);
      } else {
        unexpected.add(actualLine);
      }
    }
    if (!expected.isEmpty() || !unexpected.isEmpty()) {
      fail("Secondary locations: expected: " + expected + " unexpected:" + unexpected);
    }
  }

  private static int normalizeColumn(int startCharacter) {
    return startCharacter + 1;
  }

  private static void assertAttributeMatch(Object value, Map<Expectations.IssueAttribute, Object> attributes, Expectations.IssueAttribute attribute) {
    if (attributes.containsKey(attribute)) {
      assertThat(value).as("attribute mismatch for " + attribute + ": " + attributes).isEqualTo(attribute.getter.apply(attributes.get(attribute)));
    }
  }

  private static void assertAttributeMatch(AnalyzerMessage issue, Map<Expectations.IssueAttribute, Object> attributes, Expectations.IssueAttribute attribute) {
    if (attributes.containsKey(attribute)) {
      assertThat(issue.getMessage())
        .as("line " + issue.getLine() + " attribute mismatch for " + attribute + ": " + attributes)
        .isEqualTo(attribute.getter.apply(attributes.get(attribute)));
    }
  }

  private static void assertSingleIssue(Integer expectFileIssueOnline, String expectFileIssue, Set<AnalyzerMessage> issues) {
    Preconditions.checkState(issues.size() == 1, "A single issue is expected with line " + expectFileIssueOnline);
    AnalyzerMessage issue = Iterables.getFirst(issues, null);
    assertThat(issue.getLine()).isEqualTo(expectFileIssueOnline);
    assertThat(issue.getMessage()).isEqualTo(expectFileIssue);
  }

  private static void assertNoIssues(Multimap<Integer, Expectations.Issue> expected, Set<AnalyzerMessage> issues) {
    assertThat(issues).overridingErrorMessage("No issues expected but got: " + issues).isEmpty();
    // make sure we do not copy&paste verifyNoIssue call when we intend to call verify
    assertThat(expected.isEmpty()).overridingErrorMessage("The file should not declare noncompliants when no issues are expected").isTrue();
  }

}
