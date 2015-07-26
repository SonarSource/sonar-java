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
import org.fest.assertions.Fail;
import org.junit.Test;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class JavaCheckVerifierTest {

  public static final String FILENAME = "src/test/files/JavaCheckVerifier.java";

  @Test
  public void verify_line_issues() {
    IssuableSubscriptionVisitor visitor = new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of();
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        super.scanFile(context);
        addIssue(1, "message");
        addIssue(3, "message1");
        addIssue(7, "message2");
        addIssue(8, "message3");
        addIssue(8, "message3");
        addIssue(10, "message4");
        addIssue(10, "message4");
        addIssue(11, "no message");
      }
    };

    JavaCheckVerifier.verify("src/test/files/JavaCheckVerifier.java", visitor);
  }

  @Test
  public void verify_unexpected_issue() {
    IssuableSubscriptionVisitor visitor = new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of();
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        super.scanFile(context);
        addIssue(1, "message");
        addIssue(3, "message1");
        addIssue(4, "message1");
        addIssue(7, "message2");
        addIssue(8, "message3");
        addIssue(8, "message3");
        addIssue(10, "message4");
        addIssue(10, "message4");
        addIssue(11, "no message");
      }
    };

    try {
      JavaCheckVerifier.verify(FILENAME, visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Unexpected at [4]");
    }
  }

  @Test
  public void verify_combined_missing_expected_and_unexpected_issues() {
    IssuableSubscriptionVisitor visitor = new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of();
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        super.scanFile(context);
        addIssue(3, "message1");
        addIssue(4, "message1");
        addIssue(7, "message2");
        addIssue(8, "message3");
        addIssue(8, "message3");
        addIssue(10, "message4");
        addIssue(10, "message4");
        addIssue(11, "no message");
      }
    };

    try {
      JavaCheckVerifier.verify(FILENAME, visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected {1=[message]}, Unexpected at [4]");
    }
  }

  @Test
  public void verify_missing_expected_issue() {
    IssuableSubscriptionVisitor visitor = new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of();
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        super.scanFile(context);
        addIssue(3, "message1");
        addIssue(7, "message2");
        addIssue(8, "message3");
        addIssue(8, "message3");
        addIssue(10, "message4");
        addIssue(10, "message4");
        addIssue(11, "no message");
      }
    };

    try {
      JavaCheckVerifier.verify(FILENAME, visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected {1=[message]}");
    }
  }

  @Test
  public void verify_issue_on_file() {
    IssuableSubscriptionVisitor visitor = new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of();
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        super.scanFile(context);
        addIssueOnFile("messageOnFile");
      }
    };
    JavaCheckVerifier.verifyIssueOnFile(FILENAME, "messageOnFile", visitor);

  }

  @Test
  public void verify_no_issue_fail_if_noncompliant() {
    IssuableSubscriptionVisitor visitor = new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of();
      }
    };
    try {
      JavaCheckVerifier.verifyNoIssue(FILENAME, visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("The file should not declare noncompliants when no issues are expected");
    }
  }
}
