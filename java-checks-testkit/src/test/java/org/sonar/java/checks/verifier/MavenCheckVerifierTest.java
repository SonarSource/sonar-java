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

import com.google.common.collect.Lists;
import org.fest.assertions.Fail;
import org.junit.Test;
import org.sonar.maven.MavenFileScanner;
import org.sonar.maven.MavenFileScannerContext;
import org.sonar.maven.model.maven2.Dependency;
import org.sonar.maven.model.maven2.MavenProject;

import static org.fest.assertions.Assertions.assertThat;

public class MavenCheckVerifierTest {
  private static final String POM_WITH_ISSUES = "src/test/files/MavenCheckVerifier.xml";
  private static final String POM_WITH_ISSUES_AND_SECONDARIES = "src/test/files/MavenCheckVerifierWithSecondary.xml";
  private static final String POM_WITH_NO_ISSUE = "src/test/files/MavenCheckVerifierNoIssue.xml";
  private static final String POM_PARSE_ISSUE = "src/test/files/MavenCheckVerifierParseIssue.xml";

  @Test
  public void should_detect_issues() {
    MavenCheckVerifier.verify(POM_WITH_ISSUES, new MavenFileScanner() {
      @Override
      public void scanFile(MavenFileScannerContext context) {
        context.reportIssue(this, 7, "Message1");
        context.reportIssue(this, 28, "Message2");
        context.reportIssue(this, 40, "Message3");
      }
    });
  }

  @Test
  public void should_detect_issues_using_trees() {
    MavenCheckVerifier.verify(POM_WITH_ISSUES, new MavenFileScanner() {
      @Override
      public void scanFile(MavenFileScannerContext context) {
        MavenProject mavenProject = context.getMavenProject();
        context.reportIssue(this, mavenProject.getVersion(), "Message1");
        context.reportIssue(this, mavenProject.getDependencies().getDependencies().get(0).getArtifactId(), "Message2");
        context.reportIssue(this, mavenProject.getDependencies().getDependencies().get(2), "Message3");
      }
    });
  }

  @Test
  public void should_detect_issues_using_secondaries() {
    MavenCheckVerifier.verify(POM_WITH_ISSUES_AND_SECONDARIES, new MavenFileScanner() {
      @Override
      public void scanFile(MavenFileScannerContext context) {
        Dependency dependency = context.getMavenProject().getDependencies().getDependencies().get(0);
        context.reportIssue(this,
          dependency.startLocation().line(),
          "Message1",
          Lists.newArrayList(new MavenFileScannerContext.Location("", dependency.getVersion())));
      }
    });
  }

  @Test
  public void should_get_MavenProject() {
    MavenCheckVerifier.verifyNoIssue(POM_WITH_NO_ISSUE, new MavenFileScanner() {
      @Override
      public void scanFile(MavenFileScannerContext context) {
        assertThat(context.getMavenProject()).isNotNull();
      }
    });
  }

  @Test
  public void should_detect_issue_on_file() {
    MavenCheckVerifier.verifyIssueOnFile(POM_WITH_NO_ISSUE, "Message", new MavenFileScanner() {
      @Override
      public void scanFile(MavenFileScannerContext context) {
        context.reportIssueOnFile(this, "Message");
      }
    });
  }

  @Test
  public void should_fail_when_cannot_parse() {
    try {
      MavenCheckVerifier.verify(POM_PARSE_ISSUE, new MavenFileScanner() {
        @Override
        public void scanFile(MavenFileScannerContext context) {
        }
      });
    } catch (AssertionError e) {
      assertThat(e).hasMessage("The test file can not be parsed");
    }
  }

  @Test
  public void should_fail_when_file_does_not_exist() {
    try {
      MavenCheckVerifier.verify("", new MavenFileScanner() {
        @Override
        public void scanFile(MavenFileScannerContext context) {
        }
      });
    } catch (AssertionError e) {
      assertThat(e).hasMessage("The test file can not be parsed");
    }
  }

  @Test
  public void should_fail_if_detect_issues_but_not_in_file() {
    try {
      MavenCheckVerifier.verify(POM_WITH_NO_ISSUE, new MavenFileScanner() {
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
  public void should_fail_if_check_raise_no_issue_when_issues_are_expected() {
    try {
      MavenCheckVerifier.verify(POM_WITH_ISSUES, new MavenFileScanner() {
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
  public void should_fail_if_check_raise_more_issues_than_expected() {
    try {
      MavenCheckVerifier.verify(POM_WITH_ISSUES, new MavenFileScanner() {
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
  public void should_not_detect_issue() {
    MavenCheckVerifier.verifyNoIssue(POM_WITH_NO_ISSUE, new MavenFileScanner() {
      @Override
      public void scanFile(MavenFileScannerContext context) {
        // do nothing
      }
    });
  }

  @Test
  public void should_fail_if_issue_reported_when_checking_for_no_issue() {
    try {
      MavenCheckVerifier.verifyNoIssue(POM_WITH_NO_ISSUE, new MavenFileScanner() {
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
