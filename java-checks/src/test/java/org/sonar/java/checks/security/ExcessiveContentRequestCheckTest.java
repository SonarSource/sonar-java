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
package org.sonar.java.checks.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.AnalysisException;
import org.sonar.java.caching.FileHashingUtils;
import org.sonar.java.checks.helpers.HashCacheTestHelper;
import org.sonar.java.checks.security.ExcessiveContentRequestCheck.CachedResult;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalInputFile;
import org.sonar.java.checks.verifier.internal.InternalReadCache;
import org.sonar.java.checks.verifier.internal.InternalWriteCache;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.java.checks.security.ExcessiveContentRequestCheck.CachedResult.INSTANTIATES_VALUE;
import static org.sonar.java.checks.security.ExcessiveContentRequestCheck.CachedResult.SETS_MAXIMUM_SIZE_VALUE;
import static org.sonar.java.checks.security.ExcessiveContentRequestCheck.CachedResult.fromBytes;
import static org.sonar.java.checks.security.ExcessiveContentRequestCheck.CachedResult.toBytes;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class ExcessiveContentRequestCheckTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Nested
  class Caching {
    private InternalReadCache readCache;
    private InternalWriteCache writeCache;
    private CheckVerifier verifier;

    private final String safeSourceFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/Safe.java");
    private final String unsafeSourceFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/Unsafe.java");
    private final String sanitizerSourceFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/Sanitizer.java");
    private final Map<String, byte[]> expectedFinalCacheState = Map.of(
      computeCacheKey(unsafeSourceFile), new byte[]{INSTANTIATES_VALUE},
      computeCacheKey(safeSourceFile), new byte[]{INSTANTIATES_VALUE + SETS_MAXIMUM_SIZE_VALUE},
      computeCacheKey(sanitizerSourceFile), new byte[]{SETS_MAXIMUM_SIZE_VALUE}
    );

    @BeforeEach
    void initVerifier() {
      readCache = new InternalReadCache();
      writeCache = new InternalWriteCache();
      writeCache.bind(readCache);

      verifier = CheckVerifier.newVerifier()
        .withCache(readCache, writeCache);
    }

    String computeCacheKey(String path) {
      return "java:S5693:" + InternalInputFile.inputFile("", new File(path)).key();
    }

    @Test
    void no_issue_raised_on_unchanged_files_with_empty_cache() throws IOException, NoSuchAlgorithmException {
      logTester.setLevel(Level.TRACE);
      var check = spy(new ExcessiveContentRequestCheck());

      verifier
        .addFiles(InputFile.Status.SAME, safeSourceFile, unsafeSourceFile, sanitizerSourceFile)
        .withCheck(check);

      // Add expected file hashes to the cache to match their status
      readCache.put(HashCacheTestHelper.contentHashKey(safeSourceFile), FileHashingUtils.inputFileContentHash(safeSourceFile));
      readCache.put(HashCacheTestHelper.contentHashKey(unsafeSourceFile), FileHashingUtils.inputFileContentHash(unsafeSourceFile));
      readCache.put(HashCacheTestHelper.contentHashKey(sanitizerSourceFile), FileHashingUtils.inputFileContentHash(sanitizerSourceFile));

      verifier.verifyNoIssues();

      verify(check, times(3)).scanWithoutParsing(any());
      verify(check, times(3)).leaveFile(any());

      assertThat(writeCache.getData()).containsAllEntriesOf(expectedFinalCacheState);
      List<String> logs = logTester.getLogs(Level.TRACE).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
      assertThat(logs).
        contains(
          "No cached data for rule java:S5693 on file " + safeSourceFile,
          "No cached data for rule java:S5693 on file " + unsafeSourceFile,
          "No cached data for rule java:S5693 on file " + sanitizerSourceFile
        );
    }

    @Test
    void no_issue_raised_when_changed_unsafe_file_is_covered_by_unchanged_cached_safe_files() throws IOException, NoSuchAlgorithmException {
      readCache.put(computeCacheKey(safeSourceFile), toBytes(new CachedResult(true, true)));
      readCache.put(computeCacheKey(sanitizerSourceFile), toBytes(new CachedResult(false, true)));


      var check = spy(new ExcessiveContentRequestCheck());
      verifier
        .addFiles(InputFile.Status.SAME, safeSourceFile, sanitizerSourceFile)
        .addFiles(InputFile.Status.CHANGED, unsafeSourceFile)
        .withCheck(check);

      // Add expected file hashes to the cache to match their status
      readCache.put(HashCacheTestHelper.contentHashKey(safeSourceFile), FileHashingUtils.inputFileContentHash(safeSourceFile));
      readCache.put(HashCacheTestHelper.contentHashKey(unsafeSourceFile), new byte[]{});
      readCache.put(HashCacheTestHelper.contentHashKey(sanitizerSourceFile), FileHashingUtils.inputFileContentHash(sanitizerSourceFile));

      verifier.verifyNoIssues();

      verify(check, times(2)).scanWithoutParsing(any());
      verify(check, times(1)).leaveFile(any());

      assertThat(writeCache.getData()).containsAllEntriesOf(expectedFinalCacheState);
    }

    @Test
    void no_issue_raised_when_cached_unsafe_file_is_covered_by_changed_safe_files() throws IOException, NoSuchAlgorithmException {
      //readCache.put(computeCacheKey(unsafeSourceFile), new byte[]{1, 0});
      readCache.put(computeCacheKey(unsafeSourceFile), toBytes(new CachedResult(true, false)));

      var check = spy(new ExcessiveContentRequestCheck());
      verifier
        .addFiles(InputFile.Status.SAME, unsafeSourceFile)
        .addFiles(InputFile.Status.CHANGED, safeSourceFile, sanitizerSourceFile)
        .withCheck(check);

      // Add expected file hashes to the cache to match their status
      readCache.put(HashCacheTestHelper.contentHashKey(safeSourceFile), new byte[]{});
      readCache.put(HashCacheTestHelper.contentHashKey(unsafeSourceFile), FileHashingUtils.inputFileContentHash(unsafeSourceFile));
      readCache.put(HashCacheTestHelper.contentHashKey(safeSourceFile), new byte[]{});

      verifier.verifyNoIssues();

      verify(check, times(1)).scanWithoutParsing(any());
      verify(check, times(2)).leaveFile(any());

      assertThat(writeCache.getData()).containsAllEntriesOf(expectedFinalCacheState);
    }

    @Test
    void no_issue_raised_when_all_results_are_cached() throws IOException, NoSuchAlgorithmException {
      readCache.putAll(expectedFinalCacheState);

      var check = spy(new ExcessiveContentRequestCheck());
      verifier
        .addFiles(InputFile.Status.SAME, unsafeSourceFile, safeSourceFile, sanitizerSourceFile)
        .withCheck(check);

      // Add expected file hashes to the cache to match their status
      readCache.put(HashCacheTestHelper.contentHashKey(safeSourceFile), FileHashingUtils.inputFileContentHash(safeSourceFile));
      readCache.put(HashCacheTestHelper.contentHashKey(unsafeSourceFile), FileHashingUtils.inputFileContentHash(unsafeSourceFile));
      readCache.put(HashCacheTestHelper.contentHashKey(sanitizerSourceFile), FileHashingUtils.inputFileContentHash(sanitizerSourceFile));

      verifier.verifyNoIssues();

      verify(check, times(3)).scanWithoutParsing(any());
      verify(check, never()).leaveFile(any());

      assertThat(writeCache.getData()).containsAllEntriesOf(expectedFinalCacheState);
    }

    @Test
    void log_when_failing_to_write_to_cache() throws IOException {

      var spyOnWriteCache = spy(writeCache);
      IllegalArgumentException expectedException = new IllegalArgumentException("boom");
      doThrow(expectedException).when(spyOnWriteCache).write(any(), any(byte[].class));

      logTester.setLevel(Level.TRACE);

      verifier
        .addFiles(InputFile.Status.SAME, safeSourceFile)
        .addFiles(InputFile.Status.CHANGED, unsafeSourceFile, sanitizerSourceFile)
        .withCheck(new ExcessiveContentRequestCheck())
        .withCache(readCache, spyOnWriteCache);

      assertThatThrownBy(verifier::verifyNoIssues)
        .isInstanceOf(AnalysisException.class)
        .hasRootCause(expectedException);

      assertThat(logTester.getLogs(Level.TRACE))
        .map(LogAndArguments::getFormattedMsg)
        .contains(
          "Failed to write to cache for file " + safeSourceFile
        );
    }

    @Test
    void log_when_copying_from_previous_cache() throws IOException, NoSuchAlgorithmException {

      readCache.putAll(expectedFinalCacheState);
      var spyOnWriteCache = spy(writeCache);
      IllegalArgumentException expectedException = new IllegalArgumentException("boom");
      doThrow(expectedException).when(spyOnWriteCache).copyFromPrevious(computeCacheKey(safeSourceFile));

      logTester.setLevel(Level.TRACE);

      verifier
        .addFiles(InputFile.Status.SAME, safeSourceFile)
        .addFiles(InputFile.Status.CHANGED, unsafeSourceFile, sanitizerSourceFile)
        .withCheck(new ExcessiveContentRequestCheck())
        .withCache(readCache, spyOnWriteCache);

      // Add expected file hashes to the cache to match their status
      readCache.put(HashCacheTestHelper.contentHashKey(safeSourceFile), FileHashingUtils.inputFileContentHash(safeSourceFile));
      readCache.put(HashCacheTestHelper.contentHashKey(unsafeSourceFile), new byte[]{});
      readCache.put(HashCacheTestHelper.contentHashKey(sanitizerSourceFile), new byte[]{});

      assertThatThrownBy(verifier::verifyNoIssues)
        .isInstanceOf(AnalysisException.class)
        .hasRootCause(expectedException);

      assertThat(logTester.getLogs(Level.TRACE))
        .map(LogAndArguments::getFormattedMsg)
        .contains(
          "Failed to copy from previous cache for file " + safeSourceFile
        );
    }

    @Test
    void scanWithoutParsing_returns_false_when_cached_data_is_corrupted() throws IOException, NoSuchAlgorithmException {
      var check = spy(new ExcessiveContentRequestCheck());
      readCache.put(computeCacheKey(unsafeSourceFile), null);
      readCache.put(computeCacheKey(safeSourceFile), new byte[0]);
      readCache.put(computeCacheKey(sanitizerSourceFile), new byte[2]);

      logTester.setLevel(Level.TRACE);

      verifier
        .addFiles(InputFile.Status.SAME, unsafeSourceFile, safeSourceFile, sanitizerSourceFile)
        .withCheck(check);

      readCache.put(HashCacheTestHelper.contentHashKey(safeSourceFile), FileHashingUtils.inputFileContentHash(safeSourceFile));
      readCache.put(HashCacheTestHelper.contentHashKey(unsafeSourceFile), FileHashingUtils.inputFileContentHash(unsafeSourceFile));
      readCache.put(HashCacheTestHelper.contentHashKey(sanitizerSourceFile), FileHashingUtils.inputFileContentHash(sanitizerSourceFile));

      verifier.verifyNoIssues();

      verify(check, times(3)).scanWithoutParsing(any());
      verify(check, times(3)).leaveFile(any());

      List<String> logs = logTester.getLogs(Level.TRACE).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());

      assertThat(logs).contains(
        "Cached entry is unreadable for rule java:S5693 on file " + unsafeSourceFile,
        "Cached entry is unreadable for rule java:S5693 on file " + safeSourceFile
      );

      assertThat(writeCache.getData()).containsAllEntriesOf(expectedFinalCacheState);
    }

    @Test
    void fromBytes_returns_the_expected_result() {
      var noRelevantActions = new byte[]{0};
      var instantiate = new byte[]{INSTANTIATES_VALUE};
      var set = new byte[]{SETS_MAXIMUM_SIZE_VALUE};
      var instantiateAndSet = new byte[]{INSTANTIATES_VALUE + SETS_MAXIMUM_SIZE_VALUE};

      CachedResult actual = fromBytes(noRelevantActions);
      assertThat(actual.instantiates).isFalse();
      assertThat(actual.setMaximumSize).isFalse();

      actual = fromBytes(instantiate);
      assertThat(actual.instantiates).isTrue();
      assertThat(actual.setMaximumSize).isFalse();

      actual = fromBytes(set);
      assertThat(actual.instantiates).isFalse();
      assertThat(actual.setMaximumSize).isTrue();

      actual = fromBytes(instantiateAndSet);
      assertThat(actual.instantiates).isTrue();
      assertThat(actual.setMaximumSize).isTrue();

      assertThatThrownBy(() -> fromBytes(new byte[0]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Could not decode cached result: unexpected length (expected = 1, actual = 0)");

      assertThatThrownBy(() -> fromBytes(new byte[2]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Could not decode cached result: unexpected length (expected = 1, actual = 2)");
    }

    @Test
    void CachedResult_toBytes_returns_the_expected_result() {
      assertThat(toBytes(new CachedResult(false, false))).isEqualTo(new byte[]{0});
      assertThat(toBytes(new CachedResult(true, false))).isEqualTo(new byte[]{INSTANTIATES_VALUE});
      assertThat(toBytes(new CachedResult(false, true))).isEqualTo(new byte[]{SETS_MAXIMUM_SIZE_VALUE});
      assertThat(toBytes(new CachedResult(true, true))).isEqualTo(new byte[]{INSTANTIATES_VALUE + SETS_MAXIMUM_SIZE_VALUE});
    }
  }

  @Test
  void test_default_max() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      .verifyIssues();
  }

  @Test
  void test_spring_2_4() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/security/ExcessiveContentRequestCheck_spring_2_4.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      .verifyIssues();
  }

  @Test
  void test_max_8_000_000() {
    ExcessiveContentRequestCheck check = new ExcessiveContentRequestCheck();
    check.fileUploadSizeLimit = 8_000_000L;
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_max8000000.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_max_not_set() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_sizeNotSet.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      .verifyIssues();
  }

  @Test
  void test_max_set_in_another_file() {
    // As soon as the size is set somewhere in the project, do not report an issue.
    CheckVerifier.newVerifier()
      .addFiles(InputFile.Status.SAME,
        mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_setSize.java"),
        mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_sizeNotSet.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      // Note that this will check that no issue îs reported on the second file (order is therefore important).
      .verifyNoIssues();
  }

}
