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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.fest.assertions.Fail;
import org.junit.Test;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
      assertThat(e).hasMessage("Expected {1=[message]}, Unexpected at [4]");
    }
  }

  @Test
  public void verify_missing_expected_issue() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withoutIssue(1);

    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected {1=[message]}");
    }
  }

  @Test
  public void verify_issue_on_file() {
    String expectedMessage = "messageOnFile";
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withIssueOnFile(expectedMessage);
    JavaCheckVerifier.verifyIssueOnFile(FILENAME_ISSUES, expectedMessage, visitor);
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

  private static class FakeVisitor extends IssuableSubscriptionVisitor {

    Multimap<Integer, String> issues = LinkedListMultimap.create();
    List<String> issuesOnFile = Lists.newLinkedList();

    private FakeVisitor withDefaultIssues() {
      return this.withIssue(1, "message")
        .withIssue(3, "message1")
        .withIssue(7, "message2")
        .withIssue(8, "message3")
        .withIssue(8, "message3")
        .withIssue(10, "message4")
        .withIssue(10, "message4")
        .withIssue(11, "no message");
    }

    private FakeVisitor withIssue(int line, String message) {
      issues.put(line, message);
      return this;
    }

    private FakeVisitor withoutIssue(int line) {
      issues.removeAll(line);
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
      List<Integer> lines = Lists.newArrayList(issues.keySet());
      Collections.sort(lines);
      for (Integer line : lines) {
        for (String message : issues.get(line)) {
          addIssue(line, message);
        }
      }
      for (String message : issuesOnFile) {
        addIssueOnFile(message);
      }
    }
  }
}
