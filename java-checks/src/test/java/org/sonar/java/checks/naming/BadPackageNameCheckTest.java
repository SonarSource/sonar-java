/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.naming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalReadCache;
import org.sonar.java.checks.verifier.internal.InternalWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class BadPackageNameCheckTest {

  private static final String DEFAULT_FORMAT = "^[a-z_]+(\\.[a-z_][a-z0-9_]*)*$";
  private static final String NONCOMPLIANT_FILE = "src/test/files/checks/PACKAGE/BadPackageNameNoncompliant.java";

  private ReadCache readCache;
  private InternalWriteCache writeCache;

  @BeforeEach
  void initCaches() {
    this.readCache = new InternalReadCache();
    this.writeCache = new InternalWriteCache().bind(readCache);
  }

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(NONCOMPLIANT_FILE)
      .withCheck(new BadPackageNameCheck())
      .verifyIssueOnProject("Rename package \"PACKAGE\" to match the regular expression '" + DEFAULT_FORMAT + "'.");
  }

  @Test
  void test2() {
    BadPackageNameCheck check = new BadPackageNameCheck();
    check.format = "^[a-zA-Z0-9]*$";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/PACKAGE/BadPackageName.java")
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void test3() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/PACKAGE/BadQualifiedIdentifierPackageName.java")
      .withCheck(new BadPackageNameCheck())
      .verifyIssueOnProject("Rename package \"com.foo.PACKAGE\" to match the regular expression '" + DEFAULT_FORMAT + "'.");
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(NONCOMPLIANT_FILE)
      .withCheck(new BadPackageNameCheck())
      .withoutSemantic()
      .verifyIssueOnProject("Rename package \"PACKAGE\" to match the regular expression '" + DEFAULT_FORMAT + "'.");
  }

  @Test
  void caching() {
    String expectedMessage = "Rename package \"PACKAGE\" to match the regular expression '" + DEFAULT_FORMAT + "'.";

    CheckVerifier.newVerifier()
      .onFile(NONCOMPLIANT_FILE)
      .withCheck(new BadPackageNameCheck())
      .withCache(readCache, writeCache)
      .verifyIssueOnProject(expectedMessage);

    var check = spy(new BadPackageNameCheck());
    var populatedReadCache = new InternalReadCache().putAll(writeCache);
    var writeCache2 = new InternalWriteCache().bind(populatedReadCache);
    CheckVerifier.newVerifier()
      .withCache(populatedReadCache, writeCache2)
      .addFiles(InputFile.Status.SAME, NONCOMPLIANT_FILE)
      .withCheck(check)
      .verifyIssueOnProject(expectedMessage);

    verify(check, times(0)).scanFile(any());
    verify(check, times(1)).scanWithoutParsing(any());
    assertThat(writeCache2.getData()).containsExactlyInAnyOrderEntriesOf(writeCache.getData());
  }

  @Test
  void caching_default_package() {
    String defaultPackageFile = mainCodeSourcesPath("DefaultPackage.java");

    CheckVerifier.newVerifier()
      .onFile(defaultPackageFile)
      .withCheck(new BadPackageNameCheck())
      .withCache(readCache, writeCache)
      .verifyNoIssues();

    var check = spy(new BadPackageNameCheck());
    var populatedReadCache = new InternalReadCache().putAll(writeCache);
    var writeCache2 = new InternalWriteCache().bind(populatedReadCache);
    CheckVerifier.newVerifier()
      .withCache(populatedReadCache, writeCache2)
      .addFiles(InputFile.Status.SAME, defaultPackageFile)
      .withCheck(check)
      .verifyNoIssues();

    verify(check, times(0)).scanFile(any());
    verify(check, times(1)).scanWithoutParsing(any());
    assertThat(writeCache2.getData()).containsExactlyInAnyOrderEntriesOf(writeCache.getData());
  }

  @Test
  void caching_no_issue_on_compliant_package() {
    BadPackageNameCheck check1 = new BadPackageNameCheck();
    check1.format = "^[a-zA-Z0-9]*$";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/PACKAGE/BadPackageName.java")
      .withCheck(check1)
      .withCache(readCache, writeCache)
      .verifyNoIssues();

    var check = spy(new BadPackageNameCheck());
    check.format = "^[a-zA-Z0-9]*$";
    var populatedReadCache = new InternalReadCache().putAll(writeCache);
    var writeCache2 = new InternalWriteCache().bind(populatedReadCache);
    CheckVerifier.newVerifier()
      .withCache(populatedReadCache, writeCache2)
      .addFiles(InputFile.Status.SAME, "src/test/files/checks/PACKAGE/BadPackageName.java")
      .withCheck(check)
      .verifyNoIssues();

    verify(check, times(0)).scanFile(any());
    verify(check, times(1)).scanWithoutParsing(any());
  }
}
