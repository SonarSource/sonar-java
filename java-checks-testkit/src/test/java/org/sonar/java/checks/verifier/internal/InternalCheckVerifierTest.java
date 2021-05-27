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
package org.sonar.java.checks.verifier.internal;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.check.Rule;
import org.sonar.java.AnalysisException;
import org.sonar.java.RspecKey;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class InternalCheckVerifierTest {

  private static final String TEST_FILE = "src/test/files/testing/Compliant.java";
  private static final String TEST_FILE_PARSE_ERROR = "src/test/files/testing/ParsingError.java";
  private static final String TEST_FILE_NONCOMPLIANT = "src/test/files/testing/Noncompliant.java";
  private static final JavaFileScanner FAILING_CHECK = new FailingCheck();
  private static final JavaFileScanner NO_EFFECT_CHECK = new NoEffectCheck();
  private static final JavaFileScanner FILE_LINE_ISSUE_CHECK = new FileLineIssueCheck();
  private static final JavaFileScanner PROJECT_ISSUE_CHECK = new ProjectIssueCheck();
  private static final JavaFileScanner FILE_ISSUE_CHECK = new FileIssueCheck();

  @Nested
  class TestingCheckVerifierInitialConfiguration {

    @Test
    void failing_check_should_make_verifier_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withCheck(FAILING_CHECK)
        .onFile(TEST_FILE)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AnalysisException.class)
        .hasMessage("Failing check");
    }

    @Test
    void invalid_file_should_make_verifier_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withCheck(NO_EFFECT_CHECK)
        .onFile(TEST_FILE_PARSE_ERROR)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Should not fail analysis (Parse error at line 1 column 8: Syntax error, insert \"}\" to complete ClassBody)");
    }

    @Test
    void setting_check_is_required() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withJavaVersion(11)
        .onFile(TEST_FILE)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Set check(s) before calling any verification method!");
    }

    @Test
    void setting_checks_is_required() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withJavaVersion(11)
        .withChecks(new JavaFileScanner[0])
        .onFile(TEST_FILE)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Provide at least one check!");
    }

    @Test
    void setting_no_issues_without_semantic_should_fail_if_issue_is_raised() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withJavaVersion(11)
        .onFile(TEST_FILE)
        .withCheck(FILE_LINE_ISSUE_CHECK)
        .withoutSemantic()
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("No issues expected but got 1 issue(s):")
        .hasMessageContaining("--> 'issueOnLine' in")
        .hasMessageEndingWith("Compliant.java:1");
    }

    @Test
    void setting_valid_file_is_required() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile("dummy.test")
        .withoutSemantic()
        .withCheck(NO_EFFECT_CHECK)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("Unable to read file")
        .hasMessageEndingWith("dummy.test'");
    }

    @Test
    void setting_files_is_required() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withoutSemantic()
        .withCheck(NO_EFFECT_CHECK)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Set file(s) before calling any verification method!");
    }

    @Test
    void setting_multiple_times_java_version_fails() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withJavaVersion(6)
        .withJavaVersion(7));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Do not set java version multiple times!");
    }

    @Test
    void setting_multiple_times_one_files_fails() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .onFile(TEST_FILE));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Do not set file(s) multiple times!");
    }

    @Test
    void setting_multiple_times_multiple_files_fails() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFiles(TEST_FILE)
        .onFile(TEST_FILE));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Do not set file(s) multiple times!");
    }

    @Test
    void setting_custom_verifier_which_fails() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(FILE_ISSUE_CHECK)
        .withCustomIssueVerifier(issues -> {
          throw new IllegalStateException("Rejected");
        })
        .verifyIssueOnFile("issueOnFile"));

      assertThat(e)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Rejected");
    }

    @Test
    void setting_custom_verifier_which_accepts() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(FILE_ISSUE_CHECK)
        .withCustomIssueVerifier(issues -> {
          /* do nothing */ })
        .verifyIssueOnFile("issueOnFile");
    }
  }

  @Nested
  class TestingRuleMetadata {

    @Test
    void rule_without_annotation_should_fail() {
      class WithoutAnnotationCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "issueOnLine");
        }
      }

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new WithoutAnnotationCheck())
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Rules should be annotated with '@Rule(key = \"...\")' annotation (org.sonar.check.Rule).");
    }

    @Test
    void rule_with_constant_remediation_function_should_not_provide_cost() {
      @Rule(key = "ConstantJSON")
      class ConstantCostCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message", 42);
        }
      }

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new ConstantCostCheck())
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Rule with constant remediation function shall not provide cost");
    }

    @Test
    void absent_rule_matadata_does_not_make_verifier_fail() {
      @Rule(key = "DoesntExists")
      class DoesntExistsMetadata implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message", 42);
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new DoesntExistsMetadata())
        .verifyIssues();
    }

    @Test
    void borken_rule_metadata_does_not_make_verifier_fail() {
      @Rule(key = "BrokenJSON")
      class BorkenMetadata implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message", 42);
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new BorkenMetadata())
        .verifyIssues();
    }

    @Test
    void rule_metadata_unknown_remediation_function() {
      @Rule(key = "ExponentialRemediationFunc")
      class ExponentialRemediationFunctionCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message", 42);
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new ExponentialRemediationFunctionCheck())
        .verifyIssues();
    }

    @Test
    void rule_metadata_undefined_remediation_function() {
      @Rule(key = "UndefinedRemediationFunc")
      class UndefinedRemediationFunctionCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message", 42);
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new UndefinedRemediationFunctionCheck())
        .verifyIssues();
    }

    @Test
    void should_fail_when_no_cost() throws Exception {
      @Rule(key = "LinearJSON")
      class LinearRemediationFunctionCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message");
        }
      }

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new LinearRemediationFunctionCheck())
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("A cost should be provided for a rule with linear remediation function");
    }

    @Test
    void test_rspec_key_with_no_metadata_should_not_fail() throws Exception {
      @RspecKey("Dummy_fake_JSON")
      class DoesntExistsMetadataCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message");
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new DoesntExistsMetadataCheck())
        .verifyIssues();
    }
  }

  @Nested
  class TestingProjectIssues {

    @Test
    void verify_should_work() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(PROJECT_ISSUE_CHECK)
        .verifyIssueOnProject("issueOnProject");
    }

    @Test
    void not_raising_issues_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(NO_EFFECT_CHECK)
        .verifyIssueOnProject("issueOnProject"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("A single issue is expected on the project, but none has been raised");
    }

    @Test
    void raising_too_many_issues_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(PROJECT_ISSUE_CHECK, PROJECT_ISSUE_CHECK)
        .verifyIssueOnProject("issueOnProject"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("A single issue is expected on the project, but 2 issues have been raised");
    }

    @Test
    void raissing_a_different_message_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(PROJECT_ISSUE_CHECK)
        .verifyIssueOnProject("expected"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage(String.format("%s%n%s%n%s%n%s",
          "Expected the issue message to be:",
          "\t\"expected\"",
          "but was:",
          "\t\"issueOnProject\""));
    }

    @Test
    void raising_an_issue_line_instead_of_project_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(FILE_LINE_ISSUE_CHECK)
        .verifyIssueOnProject("issueOnProject"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Expected an issue directly on project but was raised on line 1");
    }

    @Test
    void raising_an_issue_on_file_instead_of_project_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(FILE_ISSUE_CHECK)
        .verifyIssueOnProject("issueOnProject"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Expected the issue to be raised at project level, not at file level");
    }
  }

  @Nested
  class TestingNoIssues {

    @Test
    void verify_should_work() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(NO_EFFECT_CHECK)
        .verifyNoIssues();
    }

    @Test
    void raising_issues_while_expecting_none_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(
          FILE_ISSUE_CHECK,
          PROJECT_ISSUE_CHECK,
          FILE_LINE_ISSUE_CHECK,
          FILE_LINE_ISSUE_CHECK)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("No issues expected but got 4 issue(s):")
        .hasMessageContaining("--> 'issueOnLine'")
        .hasMessageContaining("--> 'issueOnProject'")
        .hasMessageContaining("--> 'issueOnFile'");
    }
  }

  @Nested
  class TestingFileIssues {

    @Test
    void verify_should_work() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(FILE_ISSUE_CHECK)
        .verifyIssueOnFile("issueOnFile");
    }

    @Test
    void not_raising_issues_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(NO_EFFECT_CHECK)
        .verifyIssueOnFile("issueOnFile"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("A single issue is expected on the file, but none has been raised");
    }

    @Test
    void raising_too_many_issues_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(FILE_ISSUE_CHECK, FILE_ISSUE_CHECK)
        .verifyIssueOnFile("issueOnFile"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("A single issue is expected on the file, but 2 issues have been raised");
    }

    @Test
    void raissing_a_different_message_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(FILE_ISSUE_CHECK)
        .verifyIssueOnFile("expected"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage(String.format("%s%n%s%n%s%n%s",
          "Expected the issue message to be:",
          "\t\"expected\"",
          "but was:",
          "\t\"issueOnFile\""));
    }

    @Test
    void raising_an_issue_line_instead_of_file_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(FILE_LINE_ISSUE_CHECK)
        .verifyIssueOnFile("issueOnFile"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Expected an issue directly on file but was raised on line 1");
    }

    @Test
    void raising_an_issue_on_project_instead_of_file_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(PROJECT_ISSUE_CHECK)
        .verifyIssueOnFile("issueOnFile"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Expected the issue to be raised at file level, not at project level");
    }
  }

  @Nested
  class TestingMulitpleFileIssues {

    @Test
    void raising_no_issue_while_expecting_some_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(NO_EFFECT_CHECK)
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("No issue raised. At least one issue expected");
    }

    @Test
    void should_verify() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withChecks(FILE_LINE_ISSUE_CHECK)
        .verifyIssues();
    }

    @Test
    void order_of_expected_issue_on_same_line_is_relevant() {
      InternalCheckVerifier.newInstance()
        .onFile("src/test/files/testing/MultipleIssuesSameLine.java")
        .withChecks(new MultipleIssuePerLineCheck())
        .verifyIssues();
    }

    @Test
    void wrong_order_of_expected_issue_on_same_line_should_fail() {
      MultipleIssuePerLineCheck check = new MultipleIssuePerLineCheck();
      check.flipOrder = true;

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile("src/test/files/testing/MultipleIssuesSameLine.java")
        .withChecks(check)
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("line 7 attribute mismatch for 'MESSAGE'. Expected: 'msg 1', but was: 'msg 2'");
    }

    @Test
    void wrong_message_of_expected_issue_on_same_line_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile("src/test/files/testing/MultipleIssuesSameLine.java")
        .withChecks(new MultipleIssuePerLineCheck("msg 1", "wrong message"))
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("line 4 attribute mismatch for 'MESSAGE'. Expected: 'msg 2', but was: 'wrong message'");
    }
  }

  @Rule(key = "FailingCheck")
  private static final class FailingCheck implements JavaFileScanner {
    @Override
    public void scanFile(JavaFileScannerContext context) {
      throw new RuntimeException("This checks fails systemmatically with a RuntimeException");
    }
  }

  @Rule(key = "NoEffectCheck")
  private static final class NoEffectCheck implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      // do nothing
    }
  }

  @Rule(key = "FileIssueCheck")
  private static final class FileIssueCheck implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      context.addIssueOnFile(this, "issueOnFile");
    }
  }

  @Rule(key = "FileLineIssueCheck")
  private static final class FileLineIssueCheck implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      context.addIssue(1, this, "issueOnLine");
    }
  }

  @Rule(key = "ProjectIssueCheck")
  private static final class ProjectIssueCheck implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      context.addIssueOnProject(this, "issueOnProject");
    }
  }

  @Rule(key = "MultipleIssuePerLineCheck")
  private static final class MultipleIssuePerLineCheck implements JavaFileScanner {

    private final String msg1;
    private final String msg2;
    private boolean flipOrder = false;

    MultipleIssuePerLineCheck() {
      this("msg 1", "msg 2");
    }

    MultipleIssuePerLineCheck(String msg1, String msg2) {
      this.msg1 = msg1;
      this.msg2 = msg2;
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
      String[] msgs = {msg1, msg2};
      report(context, 4, msgs);

      if (flipOrder) {
        msgs = new String[] {msg2, msg1};
      }
      report(context, 7, msgs);
    }

    private void report(JavaFileScannerContext context, int line, String... messages) {
      Stream.of(messages).forEach(msg -> context.addIssue(line, this, msg));
    }
  }
}
