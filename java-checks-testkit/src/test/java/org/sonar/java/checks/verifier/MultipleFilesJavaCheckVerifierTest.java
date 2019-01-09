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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Fail;
import org.junit.Test;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipleFilesJavaCheckVerifierTest {

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
  public void verify_unexpected_issue() {
    IssuableSubscriptionVisitor visitor = new JavaCheckVerifierTest.FakeVisitor().withDefaultIssues().withIssue(4, "extra message");
    try {
      MultipleFilesJavaCheckVerifier.verify(Arrays.asList(FILENAME_ISSUES_FIRST, FILENAME_NO_ISSUE), visitor);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Unexpected at [4]");
    }
  }

  @Test
  public void verify_combined_missing_expected_and_unexpected_issues() {
    IssuableSubscriptionVisitor visitor = new JavaCheckVerifierTest.FakeVisitor().withDefaultIssues().withIssue(4, "extra message").withoutIssue(1);
    try {
      MultipleFilesJavaCheckVerifier.verify(Arrays.asList(FILENAME_ISSUES_FIRST, FILENAME_NO_ISSUE), visitor);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected {1=[{MESSAGE=message}]}, Unexpected at [4]");
    }
  }

  @Test
  public void verify_issues_in_multiple_files() {
    MultipleFilesJavaCheckVerifier.verify(Arrays.asList(FILENAME_ISSUES_FIRST, FILENAME_ISSUES_SECOND),
        new JavaCheckVerifierTest.FakeVisitor().withDefaultIssues().withIssue(2, "message B"));
  }

  @Test
  public void test_issues_with_no_semantic() {
    try {
      MultipleFilesJavaCheckVerifier.verifyNoIssueWithoutSemantic(Arrays.asList(FILENAME_ISSUES_FIRST, FILENAME_NO_ISSUE),
        new JavaCheckVerifierTest.FakeVisitor().withDefaultIssues());
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e.getMessage()).contains("No issues expected but got:");
    }
  }

  @Test
  public void test_with_no_semantic() {
    MultipleFilesJavaCheckVerifier.verifyNoIssueWithoutSemantic(Arrays.asList(FILENAME_ISSUES_FIRST, FILENAME_NO_ISSUE),
      new JavaCheckVerifierTest.FakeVisitor());
  }

  @Test
  public void verify_no_issue() {
    MultipleFilesJavaCheckVerifier.verifyNoIssue(Collections.singletonList(FILENAME_NO_ISSUE), NO_EFFECT_VISITOR);
  }

}
