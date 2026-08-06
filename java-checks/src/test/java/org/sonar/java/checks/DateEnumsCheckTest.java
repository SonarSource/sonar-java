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
package org.sonar.java.checks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.AnalysisException;
import org.sonar.java.caching.FileHashingUtils;
import org.sonar.java.checks.helpers.HashCacheTestHelper;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalReadCache;
import org.sonar.java.checks.verifier.internal.InternalWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class DateEnumsCheckTest {

  @RegisterExtension
  public final LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private ReadCache readCache;
  private InternalWriteCache writeCache;
  private CheckVerifier verifier;

  @BeforeEach
  void initCachingVerifier() {
    this.readCache = new InternalReadCache();
    this.writeCache = new InternalWriteCache().bind(readCache);
    this.verifier = CheckVerifier.newVerifier()
      .withCache(readCache, writeCache);
  }

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DateEnumsCheckSample.java"))
      .withCheck(new DateEnumsCheck())
      .verifyIssues();
  }

  @Test
  void test_quickfix_import() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DateEnumsCheckImportSample.java"))
      .withCheck(new DateEnumsCheck())
      .verifyIssues();
  }

  @Test
  void test_above_threshold() {
    // 8 out of 9 total usages use int literals (89%) -> above 80% threshold -> considered code-style -> no issues raised
    CheckVerifier.newVerifier()
      .onFiles(List.of(
        mainCodeSourcesPath("checks/s8694/above/IntLiteralFile.java"),
        mainCodeSourcesPath("checks/s8694/above/EnumFile.java")))
      .withCheck(new DateEnumsCheck())
      .verifyNoIssues();
  }

  @Test
  void test_below_threshold() {
    // 3 out of 7 total usages use int literals (43%) -> below 80% threshold -> issues are raised
    CheckVerifier.newVerifier()
      .onFiles(List.of(
        mainCodeSourcesPath("checks/s8694/below/IntLiteralFile.java"),
        mainCodeSourcesPath("checks/s8694/below/EnumFile.java")))
      .withCheck(new DateEnumsCheck())
      .verifyIssues();
  }

  @Test
  void caching() throws NoSuchAlgorithmException, IOException {
    // ClassWithIssues has 3 Noncompliant int-literal usages; ClassWithoutIssues has 4 compliant enum usages.
    // Together: 3/7 = 43% int literals -> below 80% threshold -> issues raised.
    var classWithIssues = mainCodeSourcesPath("checks/s8694/caching/ClassWithIssues.java");
    var classWithoutIssues = mainCodeSourcesPath("checks/s8694/caching/ClassWithoutIssues.java");

    // First run: ClassWithoutIssues is SAME but has no S8694 data yet -> scanWithoutParsing returns false -> fully scanned.
    // Cache is populated for both files after this run.
    // Note: ClassWithIssues must be CHANGED so the verifier reads its Noncompliant annotations.
    ReadCache existingReadCache = HashCacheTestHelper.internalReadCacheFromFile(classWithoutIssues);
    writeCache.bind(existingReadCache);
    var check = spy(new DateEnumsCheck());
    verifier
      .addFiles(InputFile.Status.SAME, classWithoutIssues)
      .addFiles(InputFile.Status.CHANGED, classWithIssues)
      .withCheck(check)
      .withCache(existingReadCache, writeCache)
      .verifyIssues();

    verify(check, times(1)).scanWithoutParsing(any());
    assertThat(writeCache.getData())
      .containsKey("java:S8694:" + HashCacheTestHelper.inputFileFromPath(classWithIssues).key())
      .containsKey("java:S8694:" + HashCacheTestHelper.inputFileFromPath(classWithoutIssues).key());

    // Second run: verify that files change correctly regardless of whether they had issues cached.
    // - ClassWithIssues (had issues cached) -> CHANGED, cache bypassed, re-scanned
    // - ClassWithoutIssues (had no issues cached) -> CHANGED, cache bypassed, re-scanned
    // Slightly different content hashes ensure the content-hash cache doesn't skip them.
    check = spy(new DateEnumsCheck());
    var populatedReadCache = new InternalReadCache().putAll(writeCache);
    populatedReadCache.put(
      HashCacheTestHelper.contentHashKey(classWithIssues),
      HashCacheTestHelper.getSlightlyDifferentContentHash(classWithIssues));
    populatedReadCache.put(
      HashCacheTestHelper.contentHashKey(classWithoutIssues),
      HashCacheTestHelper.getSlightlyDifferentContentHash(classWithoutIssues));
    var finalWriteCache = new InternalWriteCache().bind(populatedReadCache);
    CheckVerifier.newVerifier()
      .withCache(populatedReadCache, finalWriteCache)
      .addFiles(InputFile.Status.CHANGED, classWithIssues, classWithoutIssues)
      .withCheck(check)
      .verifyIssues();

    verify(check, times(0)).scanWithoutParsing(any());
    assertThat(finalWriteCache.getData())
      .containsExactlyInAnyOrderEntriesOf(writeCache.getData());

    // Third run: verify that a file with cached issues that remained SAME uses the cache correctly.
    // ClassWithIssues alone: 3/3 = 100% -> above 80% threshold -> no issues raised.
    // This validates that cached counts are applied to the threshold decision when the file is skipped.
    // Note: verifyIssues() cannot be used here because the verifier doesn't read Noncompliant annotations
    // from SAME files skipped via scanWithoutParsing. verifyNoIssues() works because the above-threshold
    // count suppresses the issues that would otherwise be raised from cache.
    check = spy(new DateEnumsCheck());
    var thirdReadCache = new InternalReadCache().putAll(finalWriteCache);
    var thirdWriteCache = new InternalWriteCache().bind(thirdReadCache);
    CheckVerifier.newVerifier()
      .withCache(thirdReadCache, thirdWriteCache)
      .addFiles(InputFile.Status.SAME, classWithIssues)
      .withCheck(check)
      .verifyNoIssues();

    verify(check, times(1)).scanWithoutParsing(any());
  }

  @Test
  void cache_deserialization_fails() throws NoSuchAlgorithmException, IOException {
    var inputStream = mock(InputStream.class);
    doThrow(new IOException()).when(inputStream).readAllBytes();
    var localReadCache = mock(ReadCache.class);

    String filePath = mainCodeSourcesPath("checks/s8694/below/IntLiteralFile.java");
    InputFile cachedFile = HashCacheTestHelper.inputFileFromPath(filePath);
    byte[] cachedHash = FileHashingUtils.inputFileContentHash(cachedFile);

    doReturn(true).when(localReadCache).contains(any());
    doReturn(new ByteArrayInputStream(cachedHash))
      .when(localReadCache).read("java:contentHash:MD5:" + cachedFile.key());
    doReturn(inputStream).when(localReadCache).read("java:S8694:" + cachedFile.key());

    var specificVerifier = CheckVerifier.newVerifier()
      .withCache(localReadCache, new InternalWriteCache().bind(localReadCache))
      .addFiles(InputFile.Status.SAME, filePath)
      .withCheck(new DateEnumsCheck());

    assertThatThrownBy(specificVerifier::verifyNoIssues)
      .isInstanceOf(AnalysisException.class)
      .hasRootCauseInstanceOf(IOException.class);
  }

  @Test
  void emptyCache() throws NoSuchAlgorithmException, IOException {
    logTester.setLevel(Level.TRACE);
    // File is SAME with a content hash in cache but no S8694 data -> cache miss -> fully scanned.
    // With only IntLiteralFile (100% int literals -> above 80% threshold) -> no issues.
    String filePath = mainCodeSourcesPath("checks/s8694/below/IntLiteralFile.java");
    ReadCache populatedReadCache = HashCacheTestHelper.internalReadCacheFromFile(filePath);
    verifier
      .addFiles(InputFile.Status.SAME, filePath)
      .withCheck(new DateEnumsCheck())
      .withCache(populatedReadCache, new InternalWriteCache().bind(populatedReadCache))
      .verifyNoIssues();

    assertThat(logTester.logs(Level.TRACE).stream().filter(
      msg -> msg.matches("Cache miss for key '[^']+'")
    )).hasSize(1);
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DateEnumsCheckSample.java"))
      .withCheck(new DateEnumsCheck())
      .withoutSemantic()
      .verifyIssues();
  }
}
