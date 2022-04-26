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
package org.sonar.java.checks.verifier.internal;

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.config.Configuration;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.Beta;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.caching.DummyCache;
import org.sonar.java.caching.JavaReadCacheImpl;
import org.sonar.java.caching.JavaWriteCacheImpl;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.FilesUtils;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.java.testing.JavaFileScannerContextForTests;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.caching.CacheContext;

import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.EFFORT_TO_FIX;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.END_COLUMN;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.END_LINE;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.FLOWS;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.MESSAGE;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.SECONDARY_LOCATIONS;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.START_COLUMN;

public class InternalCheckVerifier implements CheckVerifier {

  private static final String CHECK_OR_CHECKS = "check(s)";
  private static final String FILE_OR_FILES = "file(s)";

  private static final JavaVersion DEFAULT_JAVA_VERSION = new JavaVersionImpl();
  private static final List<File> DEFAULT_CLASSPATH = FilesUtils.getClassPath(FilesUtils.DEFAULT_TEST_JARS_DIRECTORY);

  static {
    Optional.of(new File(FilesUtils.DEFAULT_TEST_CLASSES_DIRECTORY)).filter(File::exists).ifPresent(DEFAULT_CLASSPATH::add);
  }

  private boolean withoutSemantic = false;

  // should be set by user
  private List<JavaFileScanner> checks = null;
  private List<InputFile> files = null;
  private JavaVersion javaVersion = null;
  private boolean inAndroidContext = false;
  private boolean isCacheEnabled = false;
  private List<File> classpath = null;
  private Consumer<Set<AnalyzerMessage>> customIssueVerifier = null;
  private boolean collectQuickFixes = false;

  private Expectations expectations = new Expectations();
  @VisibleForTesting
  CacheContext cacheContext = null;
  private ReadCache readCache;
  private WriteCache writeCache;

  private InternalCheckVerifier() {
  }

  public static InternalCheckVerifier newInstance() {
    return new InternalCheckVerifier();
  }

  @Override
  public InternalCheckVerifier withCheck(JavaFileScanner check) {
    requiresNull(checks, CHECK_OR_CHECKS);
    checks = Collections.singletonList(check);
    return this;
  }

  @Override
  public InternalCheckVerifier withChecks(JavaFileScanner... checks) {
    requiresNull(this.checks, CHECK_OR_CHECKS);
    requiresNonEmpty(Arrays.asList(checks), "check");
    this.checks = Arrays.asList(checks);
    return this;
  }

  @Override
  public InternalCheckVerifier withClassPath(Collection<File> classpath) {
    requiresNull(this.classpath, "classpath");
    // it is completely OK to provide nothing (empty list) as classpath
    this.classpath = new ArrayList<>(classpath);
    return this;
  }

  @Beta
  public InternalCheckVerifier withCustomIssueVerifier(Consumer<Set<AnalyzerMessage>> customIssueVerifier) {
    requiresNull(this.customIssueVerifier, "custom issue verifier");
    this.customIssueVerifier = customIssueVerifier;
    return this;
  }

  @Beta
  public InternalCheckVerifier withQuickFixes() {
    this.collectQuickFixes = true;
    this.expectations.setCollectQuickFixes();
    return this;
  }

  @Override
  public InternalCheckVerifier withJavaVersion(int javaVersionAsInt) {
    requiresNull(javaVersion, "java version");
    javaVersion = new JavaVersionImpl(javaVersionAsInt);
    return this;
  }

  @Override
  public CheckVerifier withinAndroidContext(boolean inAndroidContext) {
    this.inAndroidContext = inAndroidContext;
    return this;
  }

  @Override
  public InternalCheckVerifier onFile(String filename) {
    requiresNull(files, FILE_OR_FILES);
    return onFiles(Collections.singletonList(filename));
  }

  @Override
  public InternalCheckVerifier onFiles(String... filenames) {
    List<String> asList = Arrays.asList(filenames);
    requiresNonEmpty(asList, "file");
    return onFiles(Arrays.asList(filenames));
  }

  @Override
  public InternalCheckVerifier onFiles(Collection<String> filenames) {
    requiresNull(files, FILE_OR_FILES);
    requiresNonEmpty(filenames, "file");
    files = new ArrayList<>();
    return addFiles(InputFile.Status.SAME, filenames);
  }

  @Override
  public InternalCheckVerifier addFiles(InputFile.Status status, String... modifiedFileNames) {
    return addFiles(status, Arrays.asList(modifiedFileNames));
  }

  @Override
  public InternalCheckVerifier addFiles(InputFile.Status status, Collection<String> modifiedFileNames) {
    requiresNonEmpty(modifiedFileNames, "file");

    if (files == null) {
      files = new ArrayList<>(modifiedFileNames.size());
    }

    var filesToAdd = modifiedFileNames.stream()
        .map(name -> InternalInputFile.inputFile("", new File(name), status))
          .collect(Collectors.toList());

    var filesToAddStrings = filesToAdd.stream().map(Object::toString).collect(Collectors.toList());

    files.forEach(inputFile -> {
      if (filesToAddStrings.contains(inputFile.toString())) {
        throw new IllegalArgumentException(String.format("File %s was already added.", inputFile));
      }
    });

    files.addAll(filesToAdd);

    return this;
  }

  @Override
  public InternalCheckVerifier withoutSemantic() {
    // can be called any number of time
    withoutSemantic = true;
    return this;
  }

  @Override
  public CheckVerifier withCache(@Nullable ReadCache readCache, @Nullable WriteCache writeCache) {
    this.isCacheEnabled = true;
    this.readCache = readCache;
    this.writeCache = writeCache;
    this.cacheContext = new InternalCacheContext(
      true,
      readCache == null ? new DummyCache() : new JavaReadCacheImpl(readCache),
      writeCache == null ? new DummyCache() : new JavaWriteCacheImpl(writeCache)
    );
    return this;
  }

  @Override
  public void verifyIssues() {
    requiresNonNull(checks, CHECK_OR_CHECKS);
    requiresNonNull(files, FILE_OR_FILES);

    verifyAll();
  }

  @Override
  public void verifyIssueOnFile(String expectedIssueMessage) {
    requiresNonNull(checks, CHECK_OR_CHECKS);
    requiresNonNull(files, FILE_OR_FILES);

    expectations.setExpectedFileIssue(expectedIssueMessage);

    verifyAll();
  }

  @Override
  public void verifyIssueOnProject(String expectedIssueMessage) {
    requiresNonNull(checks, CHECK_OR_CHECKS);
    requiresNonNull(files, FILE_OR_FILES);

    expectations.setExpectedProjectIssue(expectedIssueMessage);

    verifyAll();
  }

  @Override
  public void verifyNoIssues() {
    requiresNonNull(checks, CHECK_OR_CHECKS);
    requiresNonNull(files, FILE_OR_FILES);

    expectations.setExpectNoIssues();

    verifyAll();
  }

  private void verifyAll() {
    List<JavaFileScanner> visitors = new ArrayList<>(checks);
    if (withoutSemantic && expectations.expectNoIssues()) {
      visitors.add(expectations.noEffectParser());
    } else {
      visitors.add(expectations.parser());
    }
    SonarComponents sonarComponents = sonarComponents();
    VisitorsBridgeForTests visitorsBridge;
    JavaVersion actualVersion = javaVersion == null ? DEFAULT_JAVA_VERSION : javaVersion;
    if (withoutSemantic) {
      visitorsBridge = new VisitorsBridgeForTests(visitors, sonarComponents, actualVersion);
    } else {
      List<File> actualClasspath = classpath == null ? DEFAULT_CLASSPATH : classpath;
      visitorsBridge = new VisitorsBridgeForTests(visitors, actualClasspath, sonarComponents, actualVersion);
    }

    JavaAstScanner astScanner = new JavaAstScanner(sonarComponents);
    visitorsBridge.setInAndroidContext(inAndroidContext);
    astScanner.setVisitorBridge(visitorsBridge);

    List<InputFile> filesToParse = files;
    if (isCacheEnabled) {
      visitorsBridge.setCacheContext(cacheContext);
      filesToParse = astScanner.scanWithoutParsing(files).get(false);
    }
    astScanner.scan(filesToParse);

    JavaFileScannerContextForTests testJavaFileScannerContext = visitorsBridge.lastCreatedTestContext();
    if (testJavaFileScannerContext != null) {
      checkIssues(testJavaFileScannerContext.getIssues(), testJavaFileScannerContext.getQuickFixes());
    } else {
      checkIssues(Collections.emptySet(), Collections.emptyMap());
    }
  }

  private void checkIssues(Set<AnalyzerMessage> issues, Map<TextSpan, List<JavaQuickFix>> quickFixes) {
    if (expectations.expectNoIssues()) {
      assertNoIssues(issues);
    } else if (expectations.expectIssueAtFileLevel() || expectations.expectIssueAtProjectLevel()) {
      assertComponentIssue(issues);
    } else {
      assertMultipleIssues(issues, quickFixes);
    }
    if (customIssueVerifier != null) {
      customIssueVerifier.accept(issues);
    }
  }

  private static void assertNoIssues(Set<AnalyzerMessage> issues) {
    if (issues.isEmpty()) {
      return;
    }
    String issuesAsString = issues.stream()
      .sorted(issueLineSorter())
      .map(issue -> String.format("'%s' in %s%s", issue.getMessage(), issue.getInputComponent(), (issue.getLine() == null ? "" : (":" + issue.getLine()))))
      .collect(Collectors.joining("\n--> ", "\n--> ", ""));
    throw new AssertionError(String.format("No issues expected but got %d issue(s):%s", issues.size(), issuesAsString));
  }

  private static Comparator<? super AnalyzerMessage> issueLineSorter() {
    return (i1, i2) -> {
      if (i1.getLine() == null) {
        return 1;
      }
      if (i2.getLine() == null) {
        return -1;
      }
      return Integer.compare(i1.getLine(), i2.getLine());
    };
  }

  private void assertComponentIssue(Set<AnalyzerMessage> issues) {
    String expectedMessage = expectations.expectedFileIssue();
    String component = "file";
    String otherComponent = "project";

    if (expectations.expectIssueAtProjectLevel()) {
      expectedMessage = expectations.expectedProjectIssue();
      component = "project";
      otherComponent = "file";
    }

    if (issues.size() != 1) {
      String issueNumberMessage = issues.isEmpty() ? "none has been raised" : String.format("%d issues have been raised", issues.size());
      throw new AssertionError(String.format("A single issue is expected on the %s, but %s", component, issueNumberMessage));
    }
    AnalyzerMessage issue = issues.iterator().next();
    if (issue.getLine() != null) {
      throw new AssertionError(String.format("Expected an issue directly on %s but was raised on line %d", component, issue.getLine()));
    }
    if ((expectations.expectIssueAtProjectLevel() && issue.getInputComponent().isFile())
      || (expectations.expectIssueAtFileLevel() && !issue.getInputComponent().isFile())) {
      throw new AssertionError(String.format("Expected the issue to be raised at %s level, not at %s level", component, otherComponent));
    }
    if (!expectedMessage.equals(issue.getMessage())) {
      throw new AssertionError(String.format("Expected the issue message to be:%n\t\"%s\"%nbut was:%n\t\"%s\"", expectedMessage, issue.getMessage()));
    }
  }

  private void assertMultipleIssues(Set<AnalyzerMessage> issues, Map<TextSpan, List<JavaQuickFix>> quickFixes) throws AssertionError {
    if (issues.isEmpty()) {
      throw new AssertionError("No issue raised. At least one issue expected");
    }
    List<Integer> unexpectedLines = new LinkedList<>();
    AnalyzerMessage firstIssue = issues.iterator().next();
    Expectations.RuleJSON ruleJson = Expectations.getRuleJSON(firstIssue);
    Expectations.RemediationFunction remediationFunction = Expectations.remediationFunction(ruleJson);
    Map<Integer, List<Expectations.Issue>> expected = expectations.issues;

    for (AnalyzerMessage issue : issues) {
      validateIssue(expected, unexpectedLines, issue, remediationFunction);
    }
    if (!expected.isEmpty() || !unexpectedLines.isEmpty()) {
      Collections.sort(unexpectedLines);
      List<Integer> expectedLines = expected.keySet().stream().sorted().collect(Collectors.toList());
      throw new AssertionError(new StringBuilder()
        .append(expectedLines.isEmpty() ? "" : String.format("Expected at %s", expectedLines))
        .append(expectedLines.isEmpty() || unexpectedLines.isEmpty() ? "" : ", ")
        .append(unexpectedLines.isEmpty() ? "" : String.format("Unexpected at %s", unexpectedLines))
        .toString());
    }
    assertSuperfluousFlows();

    if (collectQuickFixes) {
      new QuickFixesVerifier(expectations.quickFixes(), quickFixes).accept(issues);
      assertJsonMetadataQuickfixIsCovered(ruleJson, !quickFixes.isEmpty());
    }
  }

  private static void assertJsonMetadataQuickfixIsCovered(@Nullable Expectations.RuleJSON ruleJson, boolean hasQuickFix) {
    if (hasQuickFix && ruleJson != null &&
      !("covered".equals(ruleJson.quickfix) || "partial".equals(ruleJson.quickfix))) {
      throw new AssertionError("The json file associated with this rule has \"quickfix\": \"" +
        ruleJson.quickfix + "\" instead of \"covered\" or \"partial\"");
    }
  }

  private void validateIssue(
    Map<Integer, List<Expectations.Issue>> expected,
    List<Integer> unexpectedLines,
    AnalyzerMessage issue,
    @Nullable Expectations.RemediationFunction remediationFunction) {

    int line = issue.getLine();
    if (expected.containsKey(line)) {
      Expectations.Issue attrs = expected.get(line).get(0);
      validateRemediationFunction(attrs, issue, remediationFunction);
      validateAnalyzerMessageAttributes(attrs, issue);
      expected.computeIfPresent(line, (l, issues) -> {
        issues.remove(attrs);
        // remove the key if nothing remaining
        return issues.isEmpty() ? null : issues;
      });
    } else {
      unexpectedLines.add(line);
    }
  }

  private static void validateRemediationFunction(Expectations.Issue attributes, AnalyzerMessage issue, @Nullable Expectations.RemediationFunction remediationFunction) {
    if (remediationFunction == null) {
      return;
    }
    Double effortToFix = issue.getCost();
    if (effortToFix != null) {
      if (remediationFunction == Expectations.RemediationFunction.CONST) {
        throw new AssertionError("Rule with constant remediation function shall not provide cost");
      }
      assertAttributeMatch(issue, effortToFix, attributes, EFFORT_TO_FIX);
    } else if (remediationFunction == Expectations.RemediationFunction.LINEAR) {
      throw new AssertionError("A cost should be provided for a rule with linear remediation function");
    }

  }

  private void assertSuperfluousFlows() {
    Set<String> unseenFlowIds = expectations.unseenFlowIds();
    Map<String, String> unseenFlowWithLines = unseenFlowIds.stream()
      .collect(Collectors.toMap(Function.identity(), expectations::flowToLines));

    if (!unseenFlowWithLines.isEmpty()) {
      throw new AssertionError(String.format("Following flow comments were observed, but not referenced by any issue: %s", unseenFlowWithLines));
    }
  }

  private static void assertAttributeMatch(AnalyzerMessage issue, Object value, Map<Expectations.IssueAttribute, Object> attributes, Expectations.IssueAttribute attribute) {
    if (attributes.containsKey(attribute) && !value.equals(attribute.get(attributes))) {
      throw new AssertionError(
        String.format("line %d attribute mismatch for '%s'. Expected: '%s', but was: '%s'",
          issue.getLine(),
          attribute,
          attribute.get(attributes),
          value));
    }
  }

  private void validateAnalyzerMessageAttributes(Expectations.Issue attrs, AnalyzerMessage analyzerMessage) {
    assertAttributeMatch(analyzerMessage, analyzerMessage.getMessage(), attrs, MESSAGE);

    validateLocation(analyzerMessage, attrs);
    if (attrs.containsKey(SECONDARY_LOCATIONS)) {
      List<AnalyzerMessage> actual = analyzerMessage.flows.stream().map(l -> l.isEmpty() ? null : l.get(0)).filter(Objects::nonNull).collect(Collectors.toList());
      List<Integer> expected = (List<Integer>) attrs.get(SECONDARY_LOCATIONS);
      validateSecondaryLocations(analyzerMessage, actual, expected);
    }
    if (attrs.containsKey(FLOWS)) {
      validateFlows(analyzerMessage.flows, (List<String>) attrs.get(FLOWS));
    }
  }

  private static void validateSecondaryLocations(AnalyzerMessage parentIssue, List<AnalyzerMessage> actual, List<Integer> expected) {
    List<Integer> actualLines = actual.stream().map(AnalyzerMessage::getLine).collect(Collectors.toList());
    List<Integer> unexpected = new ArrayList<>();
    for (Integer actualLine : actualLines) {
      if (expected.contains(actualLine)) {
        expected.remove(actualLine);
      } else {
        unexpected.add(actualLine);
      }
    }
    if (!expected.isEmpty() || !unexpected.isEmpty()) {
      throw new AssertionError(
        String.format("Secondary locations: expected: %s unexpected: %s. In %s:%d",
          expected,
          unexpected,
          ((InputFile) parentIssue.getInputComponent()).filename(),
          parentIssue.getLine()));
    }
  }

  private static void validateLocation(AnalyzerMessage analyzerMessage, Map<Expectations.IssueAttribute, Object> attrs) {
    AnalyzerMessage.TextSpan textSpan = analyzerMessage.primaryLocation();
    Objects.requireNonNull(textSpan);
    assertAttributeMatch(analyzerMessage, normalizeColumn(textSpan.startCharacter), attrs, START_COLUMN);
    assertAttributeMatch(analyzerMessage, textSpan.endLine, attrs, END_LINE);
    assertAttributeMatch(analyzerMessage, normalizeColumn(textSpan.endCharacter), attrs, END_COLUMN);
  }

  private static int normalizeColumn(int startCharacter) {
    return startCharacter + 1;
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
      .map(InternalCheckVerifier::flowToString)
      .collect(Collectors.joining("\n"));

    String missingMsg = expectedFlowIds.stream().map(fid -> String.format("%s [%s]", fid, expectations.flowToLines(fid))).collect(Collectors.joining(","));

    if (!unexpectedMsg.isEmpty() || !missingMsg.isEmpty()) {
      unexpectedMsg = unexpectedMsg.isEmpty() ? "" : String.format("Unexpected flows: %s. ", unexpectedMsg);
      missingMsg = missingMsg.isEmpty() ? "" : String.format("Missing flows: %s.", missingMsg);
      throw new AssertionError(unexpectedMsg + missingMsg);
    }
  }

  private void assertSoleFlowDiscrepancy(String expectedId, List<AnalyzerMessage> actualFlow) {
    Set<Expectations.FlowComment> expected = expectations.flows.get(expectedId);
    List<Integer> expectedLines = expected.stream().map(flow -> flow.line).collect(Collectors.toList());
    List<Integer> actualLines = actualFlow.stream().map(AnalyzerMessage::getLine).collect(Collectors.toList());
    if (!actualLines.equals(expectedLines)) {
      throw new AssertionError(String.format("Flow %s has line differences. Expected: %s but was: %s", expectedId, expectedLines, actualLines));
    }
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
      AnalyzerMessage actualFlow = actualIterator.next();
      if (actualFlow.primaryLocation() == null) {
        throw new AssertionError(String.format("Flow without location: %s", actualFlow));
      }
      validateLocation(actualFlow, expectedIterator.next().attributes);
    }
  }

  private void validateFlowMessages(List<AnalyzerMessage> actual, String flowId, SortedSet<Expectations.FlowComment> expected) {
    List<String> actualMessages = actual.stream()
      .map(AnalyzerMessage::getMessage)
      .map(InternalCheckVerifier::addQuotes)
      .collect(Collectors.toList());
    List<String> expectedMessages = expected.stream()
      .map(Expectations.FlowComment::message)
      .map(InternalCheckVerifier::addQuotes)
      .collect(Collectors.toList());

    replaceExpectedNullWithActual(actualMessages, expectedMessages);
    if (!actualMessages.equals(expectedMessages)) {
      throw new AssertionError(
        String.format("Wrong messages in flow %s [%s]. Expected: %s but was: %s",
          flowId,
          expectations.flowToLines(flowId),
          expectedMessages,
          actualMessages));
    }
  }

  private static String addQuotes(@Nullable String s) {
    return s != null ? String.format("\"%s\"", s) : s;
  }

  private static void replaceExpectedNullWithActual(List<String> actualMessages, List<String> expectedMessages) {
    if (actualMessages.size() == expectedMessages.size()) {
      for (int i = 0; i < actualMessages.size(); i++) {
        if (expectedMessages.get(i) == null) {
          expectedMessages.set(i, actualMessages.get(i));
        }
      }
    }
  }

  private static String flowToString(List<AnalyzerMessage> flow) {
    return flow.stream().map(m -> String.valueOf(m.getLine())).collect(Collectors.joining(",", "[", "]"));
  }

  private static void requiresNull(@Nullable Object obj, String fieldName) {
    if (obj != null) {
      throw new AssertionError(String.format("Do not set %s multiple times!", fieldName));
    }
  }

  private static void requiresNonNull(@Nullable Object obj, String fieldName) {
    if (obj == null) {
      throw new AssertionError(String.format("Set %s before calling any verification method!", fieldName));
    }
  }

  private static void requiresNonEmpty(Collection<?> objects, String fieldName) {
    if (objects.isEmpty()) {
      throw new AssertionError(String.format("Provide at least one %s!", fieldName));
    }
  }

  private SonarComponents sonarComponents() {
    SensorContext sensorContext;
    if (isCacheEnabled) {
      sensorContext = new CacheEnabledSensorContext(readCache, writeCache);
    } else {
      sensorContext = new InternalSensorContext();
    }
    FileSystem fileSystem = sensorContext.fileSystem();
    Configuration config = sensorContext.config();

    ClasspathForMain classpathForMain = new ClasspathForMain(config, fileSystem);
    ClasspathForTest classpathForTest = new ClasspathForTest(config, fileSystem);

    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, classpathForMain, classpathForTest, null) {
      @Override
      public boolean reportAnalysisError(RecognitionException re, InputFile inputFile) {
        throw new AssertionError(String.format("Should not fail analysis (%s)", re.getMessage()));
      }

      @Override
      public boolean canSkipUnchangedFiles() {
        return isCacheEnabled;
      }
    };
    sonarComponents.setSensorContext(sensorContext);
    return sonarComponents;
  }

  private static class QuickFixesVerifier implements Consumer<Set<AnalyzerMessage>> {

    private final Map<AnalyzerMessage.TextSpan, List<JavaQuickFix>> expectedQuickFixes;
    private final Map<AnalyzerMessage.TextSpan, List<JavaQuickFix>> actualQuickFixes;

    public QuickFixesVerifier(Map<TextSpan, List<JavaQuickFix>> expectedQuickFixes, Map<TextSpan, List<JavaQuickFix>> actualQuickFixes) {
      this.expectedQuickFixes = expectedQuickFixes;
      this.actualQuickFixes = actualQuickFixes;
    }

    @Override
    public void accept(Set<AnalyzerMessage> issues) {
      for (AnalyzerMessage issue : issues) {
        AnalyzerMessage.TextSpan primaryLocation = issue.primaryLocation();
        List<JavaQuickFix> expected = expectedQuickFixes.get(primaryLocation);
        if (expected == null) {
          // We don't have to always test quick fixes, we do nothing if there is no expected quick fix.
          continue;
        }
        List<JavaQuickFix> actual = actualQuickFixes.get(primaryLocation);
        if (expected.isEmpty()) {
          if (actual != null && !actual.isEmpty()) {
            throw new AssertionError(String.format("[Quick Fix] Issue on line %d contains quick fixes while none where expected", primaryLocation.startLine));
          }
          // Else: no issue in both expected and actual, nothing to do
        } else {
          validateIfSameSize(expected, actual, issue);
        }
      }
    }

    private static void validateIfSameSize(List<JavaQuickFix> expected, @Nullable List<JavaQuickFix> actual, AnalyzerMessage issue) {
      AnalyzerMessage.TextSpan primaryLocation = issue.primaryLocation();
      if (actual == null || actual.isEmpty()) {
        // At this point, we know that expected is not empty
        throw new AssertionError(String.format("[Quick Fix] Missing quick fix for issue on line %d", primaryLocation.startLine));
      }
      int actualSize = actual.size();
      int expectedSize = expected.size();
      if (actualSize != expectedSize) {
        throw new AssertionError(
          String.format("[Quick Fix] Number of quickfixes expected is not equal to the number of expected on line %d: expected: %d , actual: %d",
            primaryLocation.startLine,
            expectedSize,
            actualSize));
      }
      for (int i = 0; i < actualSize; i++) {
        validate(issue, actual.get(i), expected.get(i));
      }
    }

    private static void validate(AnalyzerMessage actualIssue, JavaQuickFix actual, JavaQuickFix expected) {
      String actualDescription = actual.getDescription();
      String expectedDescription = expected.getDescription();
      if (!actualDescription.equals(expectedDescription)) {
        throw new AssertionError(String.format("[Quick Fix] Wrong description for issue on line %d.%nExpected: {{%s}}%nbut was:     {{%s}}",
          actualIssue.getLine(),
          expectedDescription,
          actualDescription));
      }
      List<JavaTextEdit> actualTextEdits = actual.getTextEdits();
      List<JavaTextEdit> expectedTextEdits = expected.getTextEdits();
      if (actualTextEdits.size() != expectedTextEdits.size()) {
        throw new AssertionError(String.format("[Quick Fix] Wrong number of edits for issue on line %d.%nExpected: {{%d}}%nbut was:     {{%d}}",
          actualIssue.getLine(),
          expectedTextEdits.size(),
          actualTextEdits.size()));
      }
      for (int i = 0; i < actualTextEdits.size(); i++) {
        JavaTextEdit actualTextEdit = actualTextEdits.get(i);
        JavaTextEdit expectedTextEdit = expectedTextEdits.get(i);

        String expectedReplacement = expectedTextEdit.getReplacement();
        String actualReplacement = actualTextEdit.getReplacement();
        if (expectedReplacement.contains("\\n")) {
          // new lines are expected
          expectedReplacement = expectedReplacement.replace("\\n", "\n");
        }
        if (!actualReplacement.equals(expectedReplacement)) {
          throw new AssertionError(String.format("[Quick Fix] Wrong text replacement of edit %d for issue on line %d.%nExpected: {{%s}}%nbut was:     {{%s}}",
            (i + 1),
            actualIssue.getLine(),
            expectedReplacement,
            actualReplacement));
        }
        AnalyzerMessage.TextSpan actualNormalizedTextSpan = actualTextEdit.getTextSpan();
        if (!actualNormalizedTextSpan.equals(expectedTextEdit.getTextSpan())) {
          throw new AssertionError(String.format("[Quick Fix] Wrong change location of edit %d for issue on line %d.%nExpected: {{%s}}%nbut was:     {{%s}}",
            (i + 1),
            actualIssue.getLine(),
            editorTextSpan(expectedTextEdit.getTextSpan()),
            editorTextSpan(actualNormalizedTextSpan)));
        }
      }
    }

    private static TextSpan editorTextSpan(TextSpan textSpan) {
      return new TextSpan(textSpan.startLine, textSpan.startCharacter + 1, textSpan.endLine, textSpan.endCharacter + 1);
    }
  }
}
