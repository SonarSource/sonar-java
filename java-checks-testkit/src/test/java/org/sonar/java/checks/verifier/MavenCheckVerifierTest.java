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

import org.fest.assertions.Fail;
import org.junit.Test;
import org.sonar.maven.MavenFileScanner;
import org.sonar.maven.MavenFileScannerContext;

import static org.fest.assertions.Assertions.assertThat;

public class MavenCheckVerifierTest {
  private static final String FILENAME_ISSUES = "src/test/files/MavenCheckVerifier.xml";
  private static final String FILENAME_NO_ISSUE = "src/test/files/MavenCheckVerifierNoIssue.xml";
  private static final String FILENAME_PARSE_ISSUE = "src/test/files/MavenCheckVerifierParseIssue.xml";

  @Test
  public void should_detect_issues() throws Exception {
    MavenCheckVerifier.verify(FILENAME_ISSUES, new MavenFileScanner() {
      @Override
      public void scanFile(MavenFileScannerContext context) {
        context.reportIssue(this, 7, "Message1");
        context.reportIssue(this, 28, "Message2");
        context.reportIssue(this, 40, "Message3");
      }
    });
  }

  @Test
  public void should_detect_issue_on_file() throws Exception {
    MavenCheckVerifier.verifyIssueOnFile(FILENAME_NO_ISSUE, "Message", new MavenFileScanner() {
      @Override
      public void scanFile(MavenFileScannerContext context) {
        context.reportIssueOnFile(this, "Message");
      }
    });
  }

  @Test
  public void should_fail_when_cannot_parse() throws Exception {
    try {
      MavenCheckVerifier.verify(FILENAME_PARSE_ISSUE, new MavenFileScanner() {
        @Override
        public void scanFile(MavenFileScannerContext context) {
        }
      });
    } catch (AssertionError e) {
      assertThat(e).hasMessage("The test file can not be parsed");
    }
  }

  @Test
  public void should_fail_if_detect_issues_but_not_in_file() throws Exception {
    try {
      MavenCheckVerifier.verify(FILENAME_NO_ISSUE, new MavenFileScanner() {
        @Override
        public void scanFile(MavenFileScannerContext context) {
          context.reportIssue(this, 7, "Message1");
          context.reportIssue(this, 28, "Message2");
          context.reportIssue(this, 40, "Message3");
        }
      });
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Unexpected at [7, 28, 40]");
    }
  }

  @Test
  public void should_fail_if_check_raise_no_issue_when_issues_are_expected() throws Exception {
    try {
      MavenCheckVerifier.verify(FILENAME_ISSUES, new MavenFileScanner() {
        @Override
        public void scanFile(MavenFileScannerContext context) {
          // do nothing
        }
      });
      Fail.fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("At least one issue expected");
    }
  }

  @Test
  public void should_fail_if_check_raise_more_issues_than_expected() throws Exception {
    try {
      MavenCheckVerifier.verify(FILENAME_ISSUES, new MavenFileScanner() {
        @Override
        public void scanFile(MavenFileScannerContext context) {
          context.reportIssue(this, 7, "Message1");
          context.reportIssue(this, 28, "Message2");
          context.reportIssue(this, 40, "Message3");
          context.reportIssue(this, 41, "extraMessage");
        }
      });
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Unexpected at [41]");
    }
  }

  @Test
  public void should_not_detect_issue() throws Exception {
    MavenCheckVerifier.verifyNoIssue(FILENAME_NO_ISSUE, new MavenFileScanner() {
      @Override
      public void scanFile(MavenFileScannerContext context) {
        // do nothing
      }
    });
  }

  @Test
  public void should_fail_if_issue_reported_when_checking_for_no_issue() throws Exception {
    try {
      MavenCheckVerifier.verifyNoIssue(FILENAME_NO_ISSUE, new MavenFileScanner() {
        @Override
        public void scanFile(MavenFileScannerContext context) {
          context.reportIssue(this, 2, "Message");
        }
      });
    } catch (AssertionError e) {
      assertThat(e.getMessage()).startsWith("No issues expected");
    }
  }
}
