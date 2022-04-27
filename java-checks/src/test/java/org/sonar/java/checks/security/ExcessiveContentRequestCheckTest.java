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
package org.sonar.java.checks.security;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalysisException;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalInputFile;
import org.sonar.java.checks.verifier.internal.InternalReadCache;
import org.sonar.java.checks.verifier.internal.InternalWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class ExcessiveContentRequestCheckTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Nested
  class Caching {
    private InternalReadCache readCache;
    private InternalWriteCache writeCache;
    private CheckVerifier verifier;

    private final String safeSourceFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/Safe.java");
    private final String unsafeSourceFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/Unsafe.java");
    private final String sanitizerSourceFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/Sanitizer.java");
    private final Map<String, byte[]> expectedFinalCacheState = Map.of(
      "java:S5693:instantiate:" + computeFileKey(unsafeSourceFile), new byte[]{1},
      "java:S5693:cached:" + computeFileKey(unsafeSourceFile), new byte[]{1},
      "java:S5693:instantiate:" + computeFileKey(safeSourceFile), new byte[]{1},
      "java:S5693:maximumSize:" + computeFileKey(safeSourceFile), new byte[]{1},
      "java:S5693:cached:" + computeFileKey(safeSourceFile), new byte[]{1},
      "java:S5693:maximumSize:" + computeFileKey(sanitizerSourceFile), new byte[]{1},
      "java:S5693:cached:" + computeFileKey(sanitizerSourceFile), new byte[]{1}
    );

    @BeforeEach
    void initVerifier() {
      readCache = new InternalReadCache();
      writeCache = new InternalWriteCache();
      writeCache.bind(readCache);

      verifier = CheckVerifier.newVerifier()
        .withCache(readCache, writeCache);
    }

    String computeFileKey(String path) {
      return InternalInputFile.inputFile("", new File(path)).key();
    }

    @Test
    void no_issue_raised_on_unchanged_files_with_empty_cache() {
      logTester.setLevel(LoggerLevel.TRACE);
      var check = spy(new ExcessiveContentRequestCheck());

      verifier
        .addFiles(InputFile.Status.SAME, safeSourceFile, unsafeSourceFile, sanitizerSourceFile)
        .withCheck(check)
        .verifyNoIssues();

      verify(check, times(3)).scanWithoutParsing(any());
      verify(check, times(3)).setContext(any());

      assertThat(writeCache.getData()).containsExactlyInAnyOrderEntriesOf(expectedFinalCacheState);
      List<String> logs = logTester.getLogs(LoggerLevel.TRACE).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
      assertThat(logs).
        contains(
          "No cached data for " + safeSourceFile,
          "No cached data for " + unsafeSourceFile,
          "No cached data for " + sanitizerSourceFile
        );
    }

    @Test
    void no_issue_raised_when_changed_unsafe_file_is_covered_by_unchanged_cached_safe_files() {
      readCache.put("java:S5693:cached:" + computeFileKey(safeSourceFile), new byte[]{1});
      readCache.put("java:S5693:instantiate:" + computeFileKey(safeSourceFile), new byte[]{1});
      readCache.put("java:S5693:maximumSize:" + computeFileKey(safeSourceFile), new byte[]{1});
      readCache.put("java:S5693:cached:" + computeFileKey(sanitizerSourceFile), new byte[]{1});
      readCache.put("java:S5693:maximumSize:" + computeFileKey(sanitizerSourceFile), new byte[]{1});


      var check = spy(new ExcessiveContentRequestCheck());
      verifier
        .addFiles(InputFile.Status.SAME, safeSourceFile, sanitizerSourceFile)
        .addFiles(InputFile.Status.CHANGED, unsafeSourceFile)
        .withCheck(check)
        .verifyNoIssues();

      verify(check, times(2)).scanWithoutParsing(any());
      verify(check, times(1)).setContext(any());

      assertThat(writeCache.getData()).containsExactlyInAnyOrderEntriesOf(expectedFinalCacheState);
    }

    @Test
    void no_issue_raised_when_cached_unsafe_file_is_covered_by_changed_safe_files() {
      readCache.put("java:S5693:cached:" + computeFileKey(unsafeSourceFile), new byte[]{1});
      readCache.put("java:S5693:instantiate:" + computeFileKey(unsafeSourceFile), new byte[]{1});
      readCache.put("java:S5693:maximumSize:" + computeFileKey(sanitizerSourceFile), new byte[]{1});
      readCache.put("java:S5693:cached:" + computeFileKey(sanitizerSourceFile), new byte[]{1});

      var check = spy(new ExcessiveContentRequestCheck());
      verifier
        .addFiles(InputFile.Status.SAME, unsafeSourceFile)
        .addFiles(InputFile.Status.CHANGED, safeSourceFile, sanitizerSourceFile)
        .withCheck(check)
        .verifyNoIssues();

      verify(check, times(1)).scanWithoutParsing(any());
      verify(check, times(2)).setContext(any());

      assertThat(writeCache.getData()).containsExactlyInAnyOrderEntriesOf(expectedFinalCacheState);
    }

    @Test
    void no_issue_raised_when_all_results_are_cached() {
      readCache.putAll(expectedFinalCacheState);

      var check = spy(new ExcessiveContentRequestCheck());
      verifier
        .addFiles(InputFile.Status.SAME, unsafeSourceFile, safeSourceFile, sanitizerSourceFile)
        .withCheck(check)
        .verifyNoIssues();

      verify(check, times(3)).scanWithoutParsing(any());
      verify(check, never()).setContext(any());

      assertThat(writeCache.getData()).containsExactlyInAnyOrderEntriesOf(expectedFinalCacheState);
    }

    @Test
    void log_when_failing_to_write_to_cache() throws IOException {

      var spyOnWriteCache = spy(writeCache);
      IllegalArgumentException expectedException = new IllegalArgumentException("boom");
      doThrow(expectedException).when(spyOnWriteCache).write(any(), any(byte[].class));

      logTester.setLevel(LoggerLevel.TRACE);

      verifier
        .addFiles(InputFile.Status.SAME, safeSourceFile)
        .addFiles(InputFile.Status.CHANGED, unsafeSourceFile, sanitizerSourceFile)
        .withCheck(new ExcessiveContentRequestCheck())
        .withCache(readCache, spyOnWriteCache);

      assertThatThrownBy(verifier::verifyNoIssues)
        .isInstanceOf(AnalysisException.class)
        .hasRootCause(expectedException);

      assertThat(logTester.getLogs(LoggerLevel.TRACE))
        .map(LogAndArguments::getFormattedMsg)
        .contains(
          "Failed to write to cache for file " + safeSourceFile
        );
    }

    @Test
    void log_when_copying_from_previous_cache() throws IOException {

      readCache.putAll(expectedFinalCacheState);
      var spyOnWriteCache = spy(writeCache);
      IllegalArgumentException expectedException = new IllegalArgumentException("boom");
      doThrow(expectedException).when(spyOnWriteCache).copyFromPrevious(any());

      logTester.setLevel(LoggerLevel.TRACE);

      verifier
        .addFiles(InputFile.Status.SAME, safeSourceFile)
        .addFiles(InputFile.Status.CHANGED, unsafeSourceFile, sanitizerSourceFile)
        .withCheck(new ExcessiveContentRequestCheck())
        .withCache(readCache, spyOnWriteCache);

      assertThatThrownBy(verifier::verifyNoIssues)
        .isInstanceOf(AnalysisException.class)
        .hasRootCause(expectedException);

      assertThat(logTester.getLogs(LoggerLevel.TRACE))
        .map(LogAndArguments::getFormattedMsg)
        .contains(
          "Failed to copy from previous cache for file " + safeSourceFile
        );
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
