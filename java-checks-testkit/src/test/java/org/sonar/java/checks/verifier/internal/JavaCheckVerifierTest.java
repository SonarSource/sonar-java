/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.java.AnalysisException;
import org.sonar.java.caching.DummyCache;
import org.sonar.java.caching.FileHashingUtils;
import org.sonar.java.caching.JavaReadCacheImpl;
import org.sonar.java.caching.JavaWriteCacheImpl;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.IssueWithQuickFix;
import org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.NoEffectEndOfAnalysisCheck;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.FAILING_CHECK;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.FILE_ISSUE_CHECK;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.FILE_ISSUE_CHECK_IN_ANDROID;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.FILE_LINE_ISSUE_CHECK;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.NO_EFFECT_CHECK;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.PROJECT_ISSUE_CHECK;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE_NONCOMPLIANT;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE_NONCOMPLIANT_ISSUE_ON_FILE;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE_PARSE_ERROR;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE_WITH_QUICK_FIX;

class JavaCheckVerifierTest {

  @Test
  void failing_check_should_make_verifier_fail() {
    Throwable e = catchThrowable(() -> JavaCheckVerifier.newInstance()
      .withCheck(FAILING_CHECK)
      .onFile(CheckVerifierTestUtils.TEST_FILE)
      .verifyNoIssues());

    assertThat(e)
      .isInstanceOf(AnalysisException.class)
      .hasMessage("Failing check");
  }

  @Test
  void invalid_file_should_make_verifier_fail() {
    Throwable e = catchThrowable(() -> JavaCheckVerifier.newInstance()
      .withCheck(NO_EFFECT_CHECK)
      .onFile(TEST_FILE_PARSE_ERROR)
      .verifyNoIssues());

    assertThat(e)
      .isInstanceOf(AssertionError.class)
      .hasMessage("Should not fail analysis (Parse error at line 1 column 8: Syntax error, insert \"}\" to complete ClassBody)");
  }

  @Test
  void setting_check_is_required() {
    Throwable e = catchThrowable(() -> JavaCheckVerifier.newInstance()
      .withJavaVersion(21)
      .onFile(TEST_FILE)
      .verifyNoIssues());

    assertThat(e)
      .isInstanceOf(AssertionError.class)
      .hasMessage("Set check(s) before calling any verification method!");
  }

  @Test
  void setting_checks_is_required() {
    Throwable e = catchThrowable(() -> JavaCheckVerifier.newInstance()
      .withJavaVersion(21)
      .withChecks()
      .onFile(TEST_FILE)
      .verifyNoIssues());

    assertThat(e)
      .isInstanceOf(AssertionError.class)
      .hasMessage("Provide at least one check!");
  }

  @Test
  void setting_no_issues_without_semantic_should_fail_if_issue_is_raised() {
    Throwable e = catchThrowable(() -> JavaCheckVerifier.newInstance()
      .withJavaVersion(11)
      .onFile(TEST_FILE)
      .withCheck(FILE_LINE_ISSUE_CHECK)
      .withoutSemantic()
      .verifyNoIssues());

    assertThat(e)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("ERROR: No issues were expected, but some were found. expected:<0> but was:<1>");
  }

  @Test
  void preview_features_can_only_be_enabled_for_the_latest_java_version() {
    int desiredJavaVersion = JavaVersionImpl.MAX_SUPPORTED - 1;
    final CheckVerifier verifier = JavaCheckVerifier.newInstance();
    assertThatThrownBy(() -> verifier.withJavaVersion(desiredJavaVersion, true))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(
        String.format("Preview features can only be enabled when the version == latest supported Java version (%d != %d)", desiredJavaVersion, JavaVersionImpl.MAX_SUPPORTED));
  }

  @Test
  void verify_on_project_is_not_implemented() {
    Throwable e = catchThrowable(() -> JavaCheckVerifier.newInstance()
      .onFile(TEST_FILE)
      .withCheck(PROJECT_ISSUE_CHECK)
      .verifyIssueOnProject("issueOnProject"));

    assertThat(e)
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("Not implemented!");
  }

  @Test
  void no_issue_if_not_in_android_context() {
    JavaCheckVerifier.newInstance()
      .onFile(TEST_FILE)
      .withChecks(FILE_ISSUE_CHECK_IN_ANDROID)
      .withinAndroidContext(false)
      .verifyNoIssues();
  }

  @Test
  void issue_if_in_android_context() {
    Throwable e = catchThrowable(() -> JavaCheckVerifier.newInstance()
      .onFile(TEST_FILE)
      .withChecks(FILE_ISSUE_CHECK_IN_ANDROID)
      .withinAndroidContext(true)
      .verifyNoIssues());

    assertThat(e)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("ERROR: No issues were expected, but some were found. expected:<0> but was:<1>");
  }

  @Test
  void raising_issues_while_expecting_none_should_fail() {
    Throwable e = catchThrowable(() -> JavaCheckVerifier.newInstance()
      .onFile(TEST_FILE)
      .withChecks(
        FILE_ISSUE_CHECK,
        PROJECT_ISSUE_CHECK,
        FILE_LINE_ISSUE_CHECK,
        FILE_LINE_ISSUE_CHECK)
      .verifyNoIssues());

    assertThat(e)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("ERROR: No issues were expected, but some were found. expected:<0> but was:<3>");
  }

  @Test
  void verify_on_file_should_work() {
    JavaCheckVerifier.newInstance()
      .onFile(TEST_FILE_NONCOMPLIANT_ISSUE_ON_FILE)
      .withCheck(FILE_ISSUE_CHECK)
      .verifyIssueOnFile("issueOnFile");
  }

  @Test
  void raising_no_issue_while_expecting_some_should_fail() {
    Throwable e = catchThrowable(() -> JavaCheckVerifier.newInstance()
      .onFile(TEST_FILE)
      .withChecks(NO_EFFECT_CHECK)
      .verifyIssues());

    assertThat(e)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("ERROR: 'assertOneOrMoreIssues()' is called but there's no 'Noncompliant' comments.");
  }

  @Test
  void test_one_quick_fix() {
    Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
      .addTextEdit(JavaTextEdit.replaceTextSpan(
        new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
      .build();
    JavaCheckVerifier.newInstance()
      .onFile(TEST_FILE_WITH_QUICK_FIX)
      .withCheck(IssueWithQuickFix.of(quickFix))
      .verifyIssues();
  }

  @Test
  void addFiles_registers_file_to_be_analyzed() {
    JavaCheckVerifier.newInstance()
      .addFiles(InputFile.Status.ADDED, TEST_FILE)
      .withCheck(NO_EFFECT_CHECK)
      .verifyNoIssues();

    JavaCheckVerifier.newInstance()
      .addFiles(InputFile.Status.ADDED, TEST_FILE)
      .addFiles(InputFile.Status.ADDED, TEST_FILE_NONCOMPLIANT)
      .withCheck(NO_EFFECT_CHECK)
      .verifyNoIssues();
  }

  @Test
  void addFiles_throws_an_IllegalArgumentException_if_file_added_before() {
    JavaCheckVerifier checkVerifier = JavaCheckVerifier.newInstance();
    checkVerifier.onFiles(TEST_FILE);
    assertThatThrownBy(() -> {
      checkVerifier.addFiles(InputFile.Status.ADDED, TEST_FILE);
    }).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(String.format("File %s was already added.", Path.of(TEST_FILE)));
  }

  @Test
  void withCache_effectively_sets_the_caches_for_scanWithoutParsing() throws IOException, NoSuchAlgorithmException {
    InputFile inputFile = InternalInputFile.inputFile("", new File(TEST_FILE), InputFile.Status.SAME);
    ReadCache readCache = new InternalReadCache().put("java:contentHash:MD5::" + TEST_FILE, FileHashingUtils.inputFileContentHash(inputFile));
    WriteCache writeCache = new InternalWriteCache().bind(readCache);
    CacheContext cacheContext = new InternalCacheContext(
      true,
      new JavaReadCacheImpl(readCache),
      new JavaWriteCacheImpl(writeCache));

    var check = spy(new NoEffectEndOfAnalysisCheck());

    JavaCheckVerifier.newInstance()
      .withCache(readCache, writeCache)
      .onFile(TEST_FILE)
      .withCheck(check)
      .verifyNoIssues();

    verify(check, times(1)).scanWithoutParsing(argThat(context -> CheckVerifierTestUtils.equivalent(cacheContext, context.getCacheContext())));
    verify(check, times(1)).endOfAnalysis(argThat(context -> CheckVerifierTestUtils.equivalent(cacheContext, context.getCacheContext())));
  }

  @Test
  void withCache_can_handle_a_mix_of_caches_combination() {
    JavaCheckVerifier dummyReadDummyWrite = JavaCheckVerifier.newInstance();
    dummyReadDummyWrite.withCache(null, null);
    assertThat(dummyReadDummyWrite.cacheContext.getReadCache()).isInstanceOf(DummyCache.class);
    assertThat(dummyReadDummyWrite.cacheContext.getWriteCache()).isInstanceOf(DummyCache.class);

    JavaCheckVerifier internalReadDummyWrite = JavaCheckVerifier.newInstance();
    internalReadDummyWrite.withCache(new InternalReadCache(), null);
    assertThat(internalReadDummyWrite.cacheContext.getReadCache()).isInstanceOf(JavaReadCache.class);
    assertThat(internalReadDummyWrite.cacheContext.getWriteCache()).isInstanceOf(DummyCache.class);

    JavaCheckVerifier internalReadInternalWrite = JavaCheckVerifier.newInstance();
    internalReadInternalWrite.withCache(new InternalReadCache(), new InternalWriteCache());
    assertThat(internalReadInternalWrite.cacheContext.getReadCache()).isInstanceOf(JavaReadCache.class);
    assertThat(internalReadInternalWrite.cacheContext.getWriteCache()).isInstanceOf(JavaWriteCache.class);

    JavaCheckVerifier dummyReadInternalWrite = JavaCheckVerifier.newInstance();
    dummyReadInternalWrite.withCache(null, new InternalWriteCache());
    assertThat(dummyReadInternalWrite.cacheContext.getReadCache()).isInstanceOf(JavaReadCache.class);
    assertThat(dummyReadInternalWrite.cacheContext.getWriteCache()).isInstanceOf(JavaWriteCache.class);
  }

}
