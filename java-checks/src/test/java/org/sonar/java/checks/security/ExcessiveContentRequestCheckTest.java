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

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalReadCache;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class ExcessiveContentRequestCheckTest {

  @Nested
  class Caching {
    @Test
    /**
     * On a module made of files A.java and B.java, where the rule is applied in incremental mode,
     * where A.java is unmodified and B.java is modified, the check should:
     * [x] Not analyze the unmodified source file
     * [x] Recover the results from a previous analysis from the injected cache
     * [x] Raise no issue
     * [] Write the result to the cache
     */
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
  }

  @Test
  void test_default_max() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck.java"))
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
      .onFile(testSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_max8000000.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_max_not_set() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_sizeNotSet.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      .verifyIssues();
  }

  @Test
  void test_max_set_in_another_file() {
    // As soon as the size is set somewhere in the project, do not report an issue.
    CheckVerifier.newVerifier()
      .onFiles(
        testSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_setSize.java"),
        testSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_sizeNotSet.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      // Note that this will check that no issue îs reported on the second file (order is therefore important).
      .verifyNoIssues();
  }

}
