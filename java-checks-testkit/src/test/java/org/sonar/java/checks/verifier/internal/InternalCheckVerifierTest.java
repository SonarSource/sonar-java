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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.check.Rule;
import org.sonar.java.AnalysisException;
import org.sonar.java.RspecKey;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.java.testing.JavaFileScannerContextForTests;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class InternalCheckVerifierTest {

  private static final String TEST_FILE = "src/test/files/testing/Compliant.java";
  private static final String TEST_FILE_PARSE_ERROR = "src/test/files/testing/ParsingError.java";
  private static final String TEST_FILE_NONCOMPLIANT = "src/test/files/testing/Noncompliant.java";
  private static final String TEST_FILE_WITH_QUICK_FIX = "src/test/files/testing/IssueWithQuickFix.java";
  private static final String TEST_FILE_WITH_QUICK_FIX_ON_MULTIPLE_LINE = "src/test/files/testing/IssueWithQuickFixMultipleLine.java";
  private static final String TEST_FILE_WITH_TWO_QUICK_FIX = "src/test/files/testing/IssueWithTwoQuickFixes.java";
  private static final String TEST_FILE_WITH_NO_EXPECTED = "src/test/files/testing/IssueWithNoQuickFixExpected.java";
  private static final JavaFileScanner FAILING_CHECK = new FailingCheck();
  private static final JavaFileScanner NO_EFFECT_CHECK = new NoEffectCheck();
  private static final JavaFileScanner FILE_LINE_ISSUE_CHECK = new FileLineIssueCheck();
  private static final JavaFileScanner PROJECT_ISSUE_CHECK = new ProjectIssueCheck();
  private static final JavaFileScanner FILE_ISSUE_CHECK = new FileIssueCheck();
  private static final JavaFileScanner FILE_ISSUE_CHECK_IN_ANDROID = new FileIssueCheckInAndroidContext();

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
        .withChecks()
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
          /* do nothing */
        })
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

  @Test
  void no_issue_if_not_in_android_context() {
    InternalCheckVerifier.newInstance()
      .onFile(TEST_FILE)
      .withChecks(FILE_ISSUE_CHECK_IN_ANDROID)
      .withinAndroidContext(false)
      .verifyNoIssues();
  }

  @Test
  void issue_if_in_android_context() {
    Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
      .onFile(TEST_FILE)
      .withChecks(FILE_ISSUE_CHECK_IN_ANDROID)
      .withinAndroidContext(true)
      .verifyNoIssues());

    assertThat(e)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("No issues expected but got 1 issue(s):");
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
    void raising_a_different_message_should_fail() {
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
    void multi_variable_declaration_should_create_only_one_expected_issue() {

      @Rule(key = "check")
      class Check implements JavaFileScanner {

        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "issue");
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile("src/test/files/testing/MultiVariableDeclaration.java")
        .withChecks(new Check())
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

  @Nested
  class TestingQuickFix {

    @Test
    void test_one_quick_fix() {
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(JavaTextEdit.replaceTextSpan(
          new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
        .build();
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues();
    }

    @Test
    void test_one_quick_fix_wrong_description() {
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("wrong")
        .addTextEdit(JavaTextEdit.replaceTextSpan(
          new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Wrong description for issue on line 1.")
        .hasMessageContaining(  "Expected: {{Description}}")
        .hasMessageContaining(    "but was:     {{wrong}}");
    }

    @Test
    void test_one_quick_fix_wrong_number_of_edits() {
      JavaTextEdit edit = JavaTextEdit.replaceTextSpan(
        new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement");
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(edit, edit)
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Wrong number of edits for issue on line 1.")
        .hasMessageContaining("Expected: {{1}}")
        .hasMessageContaining(    "but was:     {{2}}");
    }

    @Test
    void test_one_quick_fix_wrong_text_replacement() {
      JavaTextEdit edit = JavaTextEdit.replaceTextSpan(
        new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Wrong");
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(edit)
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Wrong text replacement of edit 1 for issue on line 1.")
        .hasMessageContaining("Expected: {{Replacement}}")
        .hasMessageContaining( "but was:     {{Wrong}}");
    }

    @Test
    void test_one_quick_fix_wrong_edit_position() {
      JavaTextEdit edit = JavaTextEdit.replaceTextSpan(
        new AnalyzerMessage.TextSpan(4, 2, 6, 5), "Replacement");
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(edit)
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Wrong change location of edit 1 for issue on line 1.")
        .hasMessageContaining("Expected: {{(1:7)-(1:8)}}")
        .hasMessageContaining("but was:     {{(4:3)-(6:6)}}");
    }

    @Test
    void test_one_quick_fix_missing_from_actual() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(new IssueWithQuickFix(Collections::emptyList))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("[Quick Fix] Missing quick fix for issue on line 1");
    }

    @Test
    void test_no_quick_fix_expected() {
      JavaTextEdit edit = JavaTextEdit.replaceTextSpan(
        new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement");
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(edit)
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_NO_EXPECTED)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("[Quick Fix] Issue on line 1 contains quick fixes while none where expected");
    }

    @Test
    void test_no_quick_fix_expected_no_actual() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_NO_EXPECTED)
        .withCheck(new IssueWithQuickFix(Collections::emptyList))
        .withQuickFixes()
        .verifyIssues();
    }

    @Test
    void test_one_quick_fix_not_tested_is_accepted() {
      // One file with one Noncompliant comment but no QF specified in the comment is fine, we don't have to always test quick fixes
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(JavaTextEdit.replaceTextSpan(
          new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
        .build();

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues();
    }

    @Test
    void test_two_quick_fix_for_one_issue() {
      Supplier<List<JavaQuickFix>> quickFixes = () -> Arrays.asList(
        JavaQuickFix.newQuickFix("Description")
          .addTextEdit(JavaTextEdit.replaceTextSpan(
            new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
          .build(),
        JavaQuickFix.newQuickFix("Description2")
          .addTextEdit(JavaTextEdit.replaceTextSpan(
            new AnalyzerMessage.TextSpan(1, 1, 1, 2), "Replacement2"))
          .build()
      );

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_TWO_QUICK_FIX)
        .withCheck(new IssueWithQuickFix(quickFixes))
        .withQuickFixes()
        .verifyIssues();
    }

    @Test
    void test_two_quick_fix_for_one_issue_1_actual_missing() {
      Supplier<JavaQuickFix> quickFix1 = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(JavaTextEdit.replaceTextSpan(
          new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_TWO_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix1))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Number of quickfixes expected is not equal to the number of expected on line 1: expected: 2 , actual: 1");
    }

    @Test
    void test_one_quick_fix_two_reported() {
      Supplier<List<JavaQuickFix>> quickFixes = () -> Arrays.asList(
        JavaQuickFix.newQuickFix("Description")
          .addTextEdit(JavaTextEdit.replaceTextSpan(
            new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
          .build(),
        JavaQuickFix.newQuickFix("Description2")
          .addTextEdit(JavaTextEdit.replaceTextSpan(
            new AnalyzerMessage.TextSpan(1, 1, 1, 2), "Replacement2"))
          .build());

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(new IssueWithQuickFix(quickFixes))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Number of quickfixes expected is not equal to the number of expected on line 1: expected: 1 , actual: 2");
    }

    @Test
    void test_warn_when_quick_fix_in_file_not_expected() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(NO_EFFECT_CHECK)
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Add \".withQuickFixes()\" to the verifier. Quick fixes are expected but the verifier is not configured to test them.");
    }

    @Test
    void test_quick_fix_supports_new_lines() {
      Supplier<JavaQuickFix> quickFixMultipleLine = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(JavaTextEdit.replaceTextSpan(new AnalyzerMessage.TextSpan(1, 6, 1, 7), "line1\n  line2;"))
        .build();
      Supplier<JavaQuickFix> quickFixSimple = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(JavaTextEdit.replaceTextSpan(new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
        .build();

      Throwable e1 = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFixMultipleLine))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e1)
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("[Quick Fix] Wrong text replacement of edit 1 for issue on line 1.")
        .hasMessageContaining("Expected: {{Replacement}}")
        .hasMessageContaining("but was:     {{line1\n  line2;}}");

      Throwable e2 = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX_ON_MULTIPLE_LINE)
        .withCheck(IssueWithQuickFix.of(quickFixSimple))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e2)
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("[Quick Fix] Wrong text replacement of edit 1 for issue on line 1.")
        .hasMessageContaining("Expected: {{line1\n  line2;}}")
        .hasMessageContaining("but was:     {{Replacement}}");

      // passes
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX_ON_MULTIPLE_LINE)
        .withCheck(IssueWithQuickFix.of(quickFixMultipleLine))
        .withQuickFixes()
        .verifyIssues();
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
        msgs = new String[]{msg2, msg1};
      }
      report(context, 7, msgs);
    }

    private void report(JavaFileScannerContext context, int line, String... messages) {
      Stream.of(messages).forEach(msg -> context.addIssue(line, this, msg));
    }
  }

  @Rule(key = "IssueWithQuickFix")
  private static final class IssueWithQuickFix extends IssuableSubscriptionVisitor {
    Supplier<List<JavaQuickFix>> quickFixes;

    IssueWithQuickFix(Supplier<List<JavaQuickFix>> quickFixes) {
      this.quickFixes = quickFixes;
    }

    static IssueWithQuickFix of(Supplier<JavaQuickFix> quickFixes) {
      return new IssueWithQuickFix(() -> Collections.singletonList(quickFixes.get()));
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      ClassTree classTree = (ClassTree) tree;
      ((InternalJavaIssueBuilder) ((JavaFileScannerContextForTests) context).newIssue())
        .forRule(this)
        .onTree(classTree.declarationKeyword())
        .withMessage("message")
        .withQuickFixes(quickFixes)
        .report();
    }
  }

  @Rule(key = "FileIssueAndroidCheck")
  private static final class FileIssueCheckInAndroidContext implements JavaFileScanner {
    @Override
    public void scanFile(JavaFileScannerContext context) {
      if (context.inAndroidContext()) {
        context.addIssueOnFile(this, "issueOnFile");
      }
    }
  }

}
