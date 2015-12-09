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
package org.sonar.java.checks.verifier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.fest.assertions.Fail;
import org.junit.Test;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class JavaCheckVerifierTest {

  private static final String FILENAME_ISSUES = "src/test/files/JavaCheckVerifier.java";
  private static final String FILENAME_NO_ISSUE = "src/test/files/JavaCheckVerifierNoIssue.java";
  private static final IssuableSubscriptionVisitor NO_EFFECT_VISITOR = new IssuableSubscriptionVisitor() {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of();
    }
  };

  @Test
  public void verify_line_issues() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    JavaCheckVerifier.verify("src/test/files/JavaCheckVerifier.java", visitor);
  }

  @Test
  public void verify_line_issues_with_java_version() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    JavaCheckVerifier.verify("src/test/files/JavaCheckVerifier.java", visitor, 7);
  }

  @Test
  public void verify_unexpected_issue() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withIssue(4, "extra message");

    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Unexpected at [4]");
    }
  }

  @Test
  public void verify_combined_missing_expected_and_unexpected_issues() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withIssue(4, "extra message").withoutIssue(1);

    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected {1=[{MESSAGE=message}]}, Unexpected at [4]");
    }
  }

  @Test
  public void verify_missing_expected_issue() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withoutIssue(1);

    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected {1=[{MESSAGE=message}]}");
    }
  }

  @Test
  public void verify_issue_on_file() {
    String expectedMessage = "messageOnFile";
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withIssueOnFile(expectedMessage);
    JavaCheckVerifier.verifyIssueOnFile(FILENAME_ISSUES, expectedMessage, visitor);
  }

  @Test(expected = IllegalStateException.class)
  public void verify_issue_on_file_incorrect() {
    JavaCheckVerifier.verifyIssueOnFile(FILENAME_ISSUES, "messageOnFile", new FakeVisitor().withDefaultIssues());
  }

  @Test
  public void verify_no_issue_fail_if_noncompliant() {
    try {
      JavaCheckVerifier.verifyNoIssue(FILENAME_ISSUES, NO_EFFECT_VISITOR);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("The file should not declare noncompliants when no issues are expected");
    }
  }

  @Test
  public void verify_no_issue() {
    JavaCheckVerifier.verifyNoIssue(FILENAME_NO_ISSUE, NO_EFFECT_VISITOR);
  }

  @Test
  public void verify_with_provided_classes() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();

    JavaCheckVerifier.verify(FILENAME_ISSUES, visitor, new ArrayList<File>());
  }

  @Test
  public void verify_with_default_test_jar() throws IOException {
    File file = new File("target/test-jars");
    if (file.exists()) {
      file.delete();
    }
    if (file.mkdir()) {
      JavaCheckVerifier.verifyNoIssue(FILENAME_NO_ISSUE, NO_EFFECT_VISITOR);
      file.delete();
    } else {
      Fail.fail();
    }
  }

  @Test
  public void verify_with_provided_test_jar() throws IOException {
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
      Fail.fail();
    }
  }

  @Test
  public void verify_with_unknown_directory_should_fail() throws IOException {
    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, NO_EFFECT_VISITOR, "unknown/test-jars");
      Fail.fail();
    } catch (AssertionError e) {
      String message = e.getMessage();
      assertThat(message).startsWith("The directory to be used to extend class path does not exists (");
      assertThat(message).contains("unknown");
      assertThat(message).endsWith("test-jars).");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_shift() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectShift.java", NO_EFFECT_VISITOR);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Use only '@+N' or '@-N' to shifts messages.");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_attribute() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectAttribute.java", NO_EFFECT_VISITOR);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("// Noncompliant attributes not valid: invalid=1");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_attribute2() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectAttribute2.java", NO_EFFECT_VISITOR);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("// Noncompliant attributes not valid: invalid=1=2");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_endLine() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectEndLine.java", NO_EFFECT_VISITOR);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("endLine attribute should be relative to the line and must be +N with N integer");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_secondaryLocation() throws IOException {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    try {
      JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierIncorrectSecondaryLocation.java", visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Secondary locations: expected: [] unexpected:[3]");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_secondaryLocation2() throws IOException {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    try {
      JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierIncorrectSecondaryLocation2.java", visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Secondary locations: expected: [5] unexpected:[]");
    }
  }

  private static class FakeVisitor extends IssuableSubscriptionVisitor {

    Multimap<Integer, String> issues = LinkedListMultimap.create();
    Multimap<Integer, AnalyzerMessage> preciseIssues = LinkedListMultimap.create();
    List<String> issuesOnFile = Lists.newLinkedList();

    private FakeVisitor withDefaultIssues() {
      AnalyzerMessage withMultipleLocation = new AnalyzerMessage(this, new File("a"), new AnalyzerMessage.TextSpan(10, 9, 10, 10), "message4", 3);
      withMultipleLocation.secondaryLocations.add(new AnalyzerMessage(this, new File("a"), 3, "no message", 0));
      withMultipleLocation.secondaryLocations.add(new AnalyzerMessage(this, new File("a"), 4, "no message", 0));
      return this.withIssue(1, "message")
        .withIssue(3, "message1")
        .withIssue(7, "message2")
        .withIssue(8, "message3")
        .withIssue(8, "message3")
        .withPreciseIssue(withMultipleLocation)
        .withPreciseIssue(new AnalyzerMessage(this, new File("a"), 11, "no message", 4))
        .withPreciseIssue(new AnalyzerMessage(this, new File("a"), 12, "message12", 0))
        .withPreciseIssue(new AnalyzerMessage(this, new File("a"), new AnalyzerMessage.TextSpan(14, 5, 15, 11), "message12", 0))
        .withIssue(17, "message17");
    }

    private FakeVisitor withPreciseIssue(AnalyzerMessage message) {
      preciseIssues.put(message.getLine(), message);
      return this;
    }

    private FakeVisitor withIssue(int line, String message) {
      issues.put(line, message);
      return this;
    }

    private FakeVisitor withoutIssue(int line) {
      issues.removeAll(line);
      preciseIssues.removeAll(line);
      return this;
    }

    private FakeVisitor withIssueOnFile(String message) {
      issuesOnFile.add(message);
      return this;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of();
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
      super.scanFile(context);
      for (Integer line : issues.keySet()) {
        for (String message : issues.get(line)) {
          addIssue(line, message);
        }
      }
      for (AnalyzerMessage analyzerMessage : preciseIssues.values()) {
        Double messageCost = analyzerMessage.getCost();
        Integer cost = messageCost != null ? messageCost.intValue() : null;
        List<JavaFileScannerContext.Location> secLocations = new ArrayList<>();
        for (AnalyzerMessage secondaryLocation : analyzerMessage.secondaryLocations) {
          secLocations.add(new JavaFileScannerContext.Location("", mockTree(secondaryLocation)));
        }
        reportIssue(mockTree(analyzerMessage), analyzerMessage.getMessage(), secLocations, cost);
      }
      for (String message : issuesOnFile) {
        addIssueOnFile(message);
      }
    }

    private static Tree mockTree(final AnalyzerMessage analyzerMessage) {
      AnalyzerMessage.TextSpan textSpan = analyzerMessage.primaryLocation();
      return new ReturnStatementTreeImpl(
        new InternalSyntaxToken(textSpan.startLine, textSpan.startCharacter - 1, "", Lists.<SyntaxTrivia>newArrayList(), 0, 0, false),
        null,
        new InternalSyntaxToken(textSpan.endLine, textSpan.endCharacter - 1, "", Lists.<SyntaxTrivia>newArrayList(), 0, 0, false));
    }

  }
}
