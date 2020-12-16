/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifierTest.FakeVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class MultipleFilesJavaCheckVerifierTest {

  private static final String FILENAME_ISSUES_FIRST = "src/test/files/JavaCheckVerifier.java";
  private static final String FILENAME_ISSUES_SECOND = "src/test/files/MultipleFilesJavaCheckVerifier.java";
  private static final String FILENAME_NO_ISSUE = "src/test/files/JavaCheckVerifierNoIssue.java";
  private static final IssuableSubscriptionVisitor NO_EFFECT_VISITOR = new IssuableSubscriptionVisitor() {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.emptyList();
    }
  };

  @Test
  void verify_unexpected_issue() {
    IssuableSubscriptionVisitor visitor = new JavaCheckVerifierTest.FakeVisitor().withDefaultIssues().withIssue(4, "extra message");
    List<String> files = Arrays.asList(FILENAME_ISSUES_FIRST, FILENAME_NO_ISSUE);
    try {
      MultipleFilesJavaCheckVerifier.verify(files, visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      // Line 4 of file FILENAME_ISSUES_FIRST, because we added an extra unexpected issue.
      // All others line of file FILENAME_NO_ISSUE, because no issues were expected.
      assertThat(e).hasMessage("Unexpected at [1, 3, 4, 4, 7, 8, 8, 10, 11, 12, 14, 17]");
    }
  }

  @Test
  void verify_combined_missing_expected_and_unexpected_issues() {
    IssuableSubscriptionVisitor visitor = new JavaCheckVerifierTest.FakeVisitor().withDefaultIssues().withIssue(4, "extra message").withoutIssue(1);
    List<String> files = Arrays.asList(FILENAME_ISSUES_FIRST, FILENAME_NO_ISSUE);
    try {
      MultipleFilesJavaCheckVerifier.verify(files, visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      // We removed issue on line 1, but still have a Noncompliant comment in FILENAME_ISSUES_FIRST.
      // In Unexpected:
      // - Line 4 of file FILENAME_ISSUES_FIRST, because we added an extra unexpected issue.
      // - All others lines of file FILENAME_NO_ISSUE, because no issues were expected.
      assertThat(e).hasMessage("Expected at [1], Unexpected at [3, 4, 4, 7, 8, 8, 10, 11, 12, 14, 17]");
    }
  }

  @Test
  void verify_issues_in_multiple_files() {
    IssuableSubscriptionVisitor visitor = new JavaCheckVerifierTest.FakeVisitor().withDefaultIssues().withIssue(2, "message B");
    List<String> files = Arrays.asList(FILENAME_ISSUES_FIRST, FILENAME_ISSUES_SECOND);
    try {
      MultipleFilesJavaCheckVerifier.verify(files, visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Unexpected at [1, 2, 3, 7, 8, 8, 10, 11, 12, 14, 17]");
    }
  }

  @Test
  void test_issues_with_no_semantic() {
    List<String> files = Arrays.asList(FILENAME_ISSUES_FIRST, FILENAME_NO_ISSUE);
    FakeVisitor visitor = new JavaCheckVerifierTest.FakeVisitor().withDefaultIssues();
    try {
      MultipleFilesJavaCheckVerifier.verifyNoIssueWithoutSemantic(files, visitor);
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e.getMessage()).contains("No issues expected but got 20 issue(s):"); // 10 per files
    }
  }

  @Test
  void test_with_no_semantic() {
    MultipleFilesJavaCheckVerifier.verifyNoIssueWithoutSemantic(Arrays.asList(FILENAME_ISSUES_FIRST, FILENAME_NO_ISSUE),
      new JavaCheckVerifierTest.FakeVisitor());
  }

  @Test
  void verify_no_issue() {
    MultipleFilesJavaCheckVerifier.verifyNoIssue(Collections.singletonList(FILENAME_NO_ISSUE), NO_EFFECT_VISITOR);
  }

}
