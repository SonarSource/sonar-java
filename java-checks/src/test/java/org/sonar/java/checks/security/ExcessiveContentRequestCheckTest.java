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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalReadCache;
import org.sonar.java.checks.verifier.internal.InternalWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
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
    @Test
    void no_issue_raised_on_valid_case_when_size_is_set_in_file_with_cached_results() {
      InternalReadCache readCache = new InternalReadCache();
      String unmodifiedFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/A.java");
      readCache.put("java:S5693:maximumSize", unmodifiedFile.getBytes(StandardCharsets.UTF_8));
      readCache.put("java:S5693:instantiate", new byte[]{});
      CheckVerifier.newVerifier()
        .onFiles(unmodifiedFile)
        .addFiles(InputFile.Status.CHANGED, mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/B.java"))
        .withCheck(new ExcessiveContentRequestCheck())
        .withCache(readCache, null)
        .verifyNoIssues();
    }

    @Test
    void no_issue_raised_on_valid_case_when_instantiation_in_file_with_cached_results() {
      InternalReadCache readCache = new InternalReadCache();
      String unmodifiedFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/B.java");
      readCache.put("java:S5693:maximumSize", new byte[]{});
      readCache.put("java:S5693:instantiate", unmodifiedFile.getBytes(StandardCharsets.UTF_8));
      CheckVerifier.newVerifier()
        .onFiles(unmodifiedFile)
        .addFiles(InputFile.Status.CHANGED, mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/A.java"))
        .withCheck(new ExcessiveContentRequestCheck())
        .withCache(readCache, null)
        .verifyNoIssues();
    }

    @Test
    void new_data_is_persisted_to_the_write_cache_at_the_end_of_analysis() {
      String unmodifiedFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/A.java");

      InternalReadCache readCache = new InternalReadCache();
      readCache.put("java:S5693:maximumSize", unmodifiedFile.getBytes(StandardCharsets.UTF_8));

      InternalWriteCache writeCache = spy(new InternalWriteCache());
      writeCache.bind(readCache);

      CheckVerifier.newVerifier()
        .onFiles(unmodifiedFile)
        .addFiles(InputFile.Status.CHANGED, mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/B.java"))
        .withCheck(new ExcessiveContentRequestCheck())
        .withCache(readCache, writeCache)
        .verifyNoIssues();

      verify(writeCache, times(1)).write(eq("java:S5693:instantiate"), any(byte[].class));
      verify(writeCache, times(1)).copyFromPrevious("java:S5693:maximumSize");
    }

    @Test
    void new_data_is_persisted_to_the_write_cache_at_the_end_of_analysis_2() {
      String modifiedFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/A.java");
      String unmodifiedFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/B.java");

      InternalReadCache readCache = new InternalReadCache();
      readCache.put("java:S5693:instantiate", (modifiedFile + ";" + unmodifiedFile).getBytes(StandardCharsets.UTF_8));

      InternalWriteCache writeCache = spy(new InternalWriteCache());
      writeCache.bind(readCache);

      CheckVerifier.newVerifier()
        .onFiles(unmodifiedFile)
        .addFiles(InputFile.Status.CHANGED, modifiedFile)
        .withCheck(new ExcessiveContentRequestCheck())
        .withCache(readCache, writeCache)
        .verifyNoIssues();

      verify(writeCache, times(1)).write(eq("java:S5693:maximumSize"), any(byte[].class));
      verify(writeCache, times(1)).copyFromPrevious("java:S5693:instantiate");
    }

    @Test
    void old_data_is_copied_from_the_read_to_the_write_cache_at_the_end_of_analysis() {
      InternalWriteCache writeCache = spy(new InternalWriteCache());
      String unmodifiedFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/A.java");
      CheckVerifier.newVerifier()
        .onFiles(unmodifiedFile)
        .addFiles(InputFile.Status.CHANGED, mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/B.java"))
        .withCheck(new ExcessiveContentRequestCheck())
        .withCache(null, writeCache)
        .verifyNoIssues();

      verify(writeCache, times(1)).write(eq("java:S5693:maximumSize"), any(byte[].class));
      verify(writeCache, times(1)).write(eq("java:S5693:instantiate"), any(byte[].class));

      verify(writeCache, never()).copyFromPrevious("java:S5693:maximumSize");
      verify(writeCache, never()).copyFromPrevious("java:S5693:instantiate");
    }

    @Test
    void log_when_failing_to_read_from_or_write_to_caches() throws IOException {
      InputStream in = mock(InputStream.class);
      doThrow(new IOException("boom")).when(in).readAllBytes();
      InternalReadCache readCache = mock(InternalReadCache.class);
      doReturn(in).when(readCache).read(any(String.class));

      String unmodifiedFile = mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/A.java");
      CheckVerifier.newVerifier()
        .onFiles(unmodifiedFile)
        .addFiles(InputFile.Status.CHANGED, mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/caching/B.java"))
        .withCheck(new ExcessiveContentRequestCheck())
        .withCache(readCache, null)
        .verifyNoIssues();
      assertThat(logTester.getLogs(LoggerLevel.WARN))
        .hasSize(2)
        .allMatch(logAndArguments -> logAndArguments.getFormattedMsg().equals("boom"));
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
      .onFiles(
        mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_setSize.java"),
        mainCodeSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_sizeNotSet.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      // Note that this will check that no issue îs reported on the second file (order is therefore important).
      .verifyNoIssues();
  }

}
