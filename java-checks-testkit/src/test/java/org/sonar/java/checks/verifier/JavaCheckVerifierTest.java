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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.check.Rule;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.RspecKey;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@EnableRuleMigrationSupport
class JavaCheckVerifierTest {

  @org.junit.Rule
  public LogTester logTester = new LogTester().setLevel(LoggerLevel.INFO);

  private static final String FILENAME_ISSUES = "src/test/files/JavaCheckVerifier.java";
  private static final String FILENAME_NO_ISSUE = "src/test/files/JavaCheckVerifierNoIssue.java";
  private static final String FILENAME_PARSING_ISSUE = "src/test/files/JavaCheckVerifierParsingIssue.java";
  private static final IssuableSubscriptionVisitor NO_EFFECT_VISITOR = new IssuableSubscriptionVisitor() {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.emptyList();
    }
  };

  @Test
  void verify_line_issues() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
  }

  @Test
  void verify_line_issues_with_java_version() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    JavaCheckVerifier.verify(FILENAME_ISSUES, visitor, 7);
  }

  @Test
  void verify_unexpected_issue() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withIssue(4, "extra message");

    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Unexpected at [4]");
    }
  }

  @Test
  void verify_combined_missing_expected_and_unexpected_issues() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withIssue(4, "extra message").withoutIssue(1);

    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected at [1], Unexpected at [4]");
    }
  }

  @Test
  void verify_missing_expected_issue() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withoutIssue(1);

    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected at [1]");
    }
  }

  @Test
  void verify_issue_on_file() {
    String expectedMessage = "messageOnFile";
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withIssueOnFile(expectedMessage);
    JavaCheckVerifier.verifyIssueOnFile(FILENAME_ISSUES, expectedMessage, visitor);
  }

  @Test
  void verify_issue_on_project() {
    String expectedMessage = "messageOnProject";
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withIssueOnProject(expectedMessage);
    JavaCheckVerifier.verifyIssueOnProject(FILENAME_ISSUES, expectedMessage, visitor);
  }

  @Test
  void verify_issue_on_file_incorrect() {
    FakeVisitor visitor = new FakeVisitor().withDefaultIssues();
    try {
      JavaCheckVerifier.verifyIssueOnFile(FILENAME_ISSUES, "messageOnFile", visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("A single issue is expected on the file, but 10 issues have been raised");
    }
  }

  @Test
  void verify_no_issue() {
    JavaCheckVerifier.verifyNoIssue(FILENAME_NO_ISSUE, NO_EFFECT_VISITOR);
  }

  @Test
  void verify_no_issue_with_version() {
    JavaCheckVerifier.verifyNoIssue(FILENAME_NO_ISSUE, NO_EFFECT_VISITOR, 8);
  }

  @Test
  void verify_with_provided_classes() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();

    JavaCheckVerifier.verify(FILENAME_ISSUES, visitor, new ArrayList<File>());
  }

  @Test
  void verify_with_default_test_jar() throws IOException {
    File file = new File("target/test-jars");
    if (file.exists()) {
      file.delete();
    }
    if (file.mkdir()) {
      JavaCheckVerifier.verifyNoIssue(FILENAME_NO_ISSUE, NO_EFFECT_VISITOR);
      file.delete();
    } else {
      Fail.fail("Should have failed");
    }
  }

  @Test
  void verify_with_provided_test_jar() throws IOException {
    String testJarsPathname = "target/my-test-jars";
    File file = new File(testJarsPathname);
    if (file.exists()) {
      file.delete();
    }
    if (file.mkdir()) {
      IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();

      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor, testJarsPathname);
      file.delete();
    } else {
      Fail.fail("Should have failed");
    }
  }

  @Test
  void verify_with_unknown_directory_should_fail() throws IOException {
    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, NO_EFFECT_VISITOR, "unknown/test-jars");
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      String message = e.getMessage();
      assertThat(message).startsWith("The directory to be used to extend class path does not exists (");
      assertThat(message).contains("unknown");
      assertThat(message).endsWith("test-jars).");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_shift() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectShift.java", NO_EFFECT_VISITOR);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Use only '@+N' or '@-N' to shifts messages.");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_attribute() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectAttribute.java", NO_EFFECT_VISITOR);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("// Noncompliant attributes not valid: 'invalid=1'");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_attribute2() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectAttribute2.java", NO_EFFECT_VISITOR);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("// Noncompliant attributes not valid: 'invalid=1=2'");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_endLine() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectEndLine.java", NO_EFFECT_VISITOR);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("endLine attribute should be relative to the line and must be +N with N integer");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_secondaryLocation() throws IOException {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    try {
      JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierIncorrectSecondaryLocation.java", visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Secondary locations: expected: [] unexpected: [3]. In JavaCheckVerifierIncorrectSecondaryLocation.java:10");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_secondaryLocation2() throws IOException {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    try {
      JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierIncorrectSecondaryLocation2.java", visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Secondary locations: expected: [5] unexpected: []. In JavaCheckVerifierIncorrectSecondaryLocation2.java:10");
    }
  }

  @Test
  void test_with_no_semantic() throws Exception {
    IssuableSubscriptionVisitor noIssueVisitor = new FakeVisitor();
    JavaCheckVerifier.verifyNoIssueWithoutSemantic(FILENAME_ISSUES, noIssueVisitor);
    JavaCheckVerifier.verifyNoIssueWithoutSemantic(FILENAME_NO_ISSUE, noIssueVisitor);
    FakeVisitor visitor = new FakeVisitor().withDefaultIssues();
    try {
      JavaCheckVerifier.verifyNoIssueWithoutSemantic(FILENAME_ISSUES, visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e.getMessage()).contains("No issues expected but got 10 issue(s):");
    }
  }

  @Test
  void test_with_no_semantic_and_java_version() throws Exception {
    int java_8 = 8;
    IssuableSubscriptionVisitor noIssueVisitor = new FakeVisitor();
    JavaCheckVerifier.verifyNoIssueWithoutSemantic(FILENAME_ISSUES, noIssueVisitor, java_8);
    JavaCheckVerifier.verifyNoIssueWithoutSemantic(FILENAME_NO_ISSUE, noIssueVisitor, java_8);
    FakeVisitor visitor = new FakeVisitor().withDefaultIssues();
    try {
      JavaCheckVerifier.verifyNoIssueWithoutSemantic(FILENAME_ISSUES, visitor, java_8);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e.getMessage()).contains("No issues expected but got 10 issue(s):");
    }
  }

  @Test
  void rule_without_annotation_should_fail_with_a_clear_message() {
    class NotAnnotatedCheck extends IssuableSubscriptionVisitor {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD);
      }

      @Override
      public void visitNode(Tree tree) {
        reportIssue(tree, "yolo", Collections.emptyList(), 42);
      }
    }
    NotAnnotatedCheck check = new NotAnnotatedCheck();
    Throwable throwable = catchThrowable(() -> JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierNoIssue.java", check));
    assertThat(throwable)
      .isInstanceOf(AssertionError.class)
      .hasMessage("Rules should be annotated with '@Rule(key = \"...\")' annotation (org.sonar.check.Rule).");
  }

  @Test
  void verify_should_fail_if_files_does_not_parse() {
    try {
      JavaCheckVerifier.verify(FILENAME_PARSING_ISSUE, NO_EFFECT_VISITOR);
      Fail.fail("Should have failed");
    } catch (Error e) {
      assertThat(e).isInstanceOf(AssertionError.class);
      assertThat(e.getMessage()).isEqualTo("Should not fail analysis (Parse error at line 1 column 8: Syntax error, insert \"}\" to complete ClassBody)");
    }
  }

  @Test
  void verifyNoIssue_should_fail_if_files_does_not_parse() {
    try {
      JavaCheckVerifier.verifyNoIssue(FILENAME_PARSING_ISSUE, NO_EFFECT_VISITOR);
      Fail.fail("Should have failed");
    } catch (Error e) {
      assertThat(e).isInstanceOf(AssertionError.class);
      assertThat(e.getMessage()).isEqualTo("Should not fail analysis (Parse error at line 1 column 8: Syntax error, insert \"}\" to complete ClassBody)");
    }
  }

  private static DefaultInputFile emptyInputFile() {
    return new TestInputFileBuilder("", "randomFile")
      .setCharset(UTF_8)
      .setLanguage("java")
      .build();
  }

  @RspecKey("Dummy_fake_JSON")
  private static class NoJsonVisitor extends FakeVisitor {
  }

  @Rule(key = "LinearJSON")
  private static class LinearFakeVisitor extends FakeVisitor {
  }

  @Rule(key = "ConstantJSON")
  static class FakeVisitor extends IssuableSubscriptionVisitor {

    Map<Integer, List<String>> issues = new LinkedHashMap<>();
    Map<Integer, List<AnalyzerMessage>> preciseIssues = new LinkedHashMap<>();
    List<String> issuesOnFile = new LinkedList<>();
    List<String> issuesOnProject = new LinkedList<>();

    protected FakeVisitor withDefaultIssues() {
      AnalyzerMessage withMultipleLocation = new AnalyzerMessage(this, emptyInputFile(), new AnalyzerMessage.TextSpan(10, 9, 10, 10), "message4", 0);
      withMultipleLocation.flows.add(Collections.singletonList(new AnalyzerMessage(this, emptyInputFile(), 3, "no message", 0)));
      withMultipleLocation.flows.add(Collections.singletonList(new AnalyzerMessage(this, emptyInputFile(), 4, "no message", 0)));
      return this.withIssue(1, "message")
        .withIssue(3, "message1")
        .withIssue(7, "message2")
        .withIssue(8, "message3")
        .withIssue(8, "message3")
        .withPreciseIssue(withMultipleLocation)
        .withPreciseIssue(new AnalyzerMessage(this, emptyInputFile(), 11, "no message", 0))
        .withPreciseIssue(new AnalyzerMessage(this, emptyInputFile(), 12, "message12", 0))
        .withPreciseIssue(new AnalyzerMessage(this, emptyInputFile(), new AnalyzerMessage.TextSpan(14, 5, 15, 11), "message12", 0))
        .withIssue(17, "message17");
    }

    FakeVisitor withPreciseIssue(AnalyzerMessage message) {
      preciseIssues.computeIfAbsent(message.getLine(), key -> new LinkedList<>()).add(message);
      return this;
    }

    FakeVisitor withIssue(int line, String message) {
      issues.computeIfAbsent(line, key -> new LinkedList<>()).add(message);
      return this;
    }

    protected FakeVisitor withoutIssue(int line) {
      issues.remove(line);
      preciseIssues.remove(line);
      return this;
    }

    private FakeVisitor withIssueOnFile(String message) {
      issuesOnFile.add(message);
      return this;
    }

    private FakeVisitor withIssueOnProject(String message) {
      issuesOnProject.add(message);
      return this;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.emptyList();
    }

    @Override
    public void setContext(JavaFileScannerContext context) {
      super.setContext(context);
      for (Integer line : issues.keySet()) {
        for (String message : issues.get(line)) {
          addIssue(line, message);
        }
      }
      List<AnalyzerMessage> anamyerMessages = preciseIssues.values().stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());
      for (AnalyzerMessage analyzerMessage : anamyerMessages) {
        Double messageCost = analyzerMessage.getCost();
        Integer cost = messageCost != null ? messageCost.intValue() : null;
        List<JavaFileScannerContext.Location> secLocations = analyzerMessage.flows.stream()
          .map(l -> new JavaFileScannerContext.Location("", mockTree(l.get(0))))
          .collect(Collectors.toList());
        reportIssue(mockTree(analyzerMessage), analyzerMessage.getMessage(), secLocations, cost);
      }
      for (String message : issuesOnFile) {
        addIssueOnFile(message);
      }
      for (String message : issuesOnProject) {
        context.addIssueOnProject(this, message);
      }
    }

    private static Tree mockTree(final AnalyzerMessage analyzerMessage) {
      AnalyzerMessage.TextSpan textSpan = analyzerMessage.primaryLocation();
      if (textSpan.onLine()) {
        return new InternalSyntaxToken(textSpan.startLine, 0, "mock", new ArrayList<>(), false);
      }
      return new ReturnStatementTreeImpl(
        new InternalSyntaxToken(textSpan.startLine, textSpan.startCharacter - 1, "", new ArrayList<>(), false),
        null,
        new InternalSyntaxToken(textSpan.endLine, textSpan.endCharacter - 1, "", new ArrayList<>(), false));
    }
  }
}
