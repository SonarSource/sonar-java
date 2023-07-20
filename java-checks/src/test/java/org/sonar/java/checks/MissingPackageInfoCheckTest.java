/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
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

class MissingPackageInfoCheckTest {

  private static final String EXPECTED_PACKAGE = "checks.packageInfo.nopackageinfo";
  private static final String EXPECTED_MESSAGE = "Add a 'package-info.java' file to document the '" + EXPECTED_PACKAGE + "' package";

  @RegisterExtension
  public final LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private ReadCache readCache;
  private InternalWriteCache writeCache;
  private CheckVerifier verifier;

  @BeforeEach
  void initVerifier() {
    this.readCache = new InternalReadCache();
    this.writeCache = new InternalWriteCache().bind(readCache);
    this.verifier = CheckVerifier.newVerifier()
      .withCache(readCache, writeCache);
  }

  @Test
  void no_package_info() {
    MissingPackageInfoCheck check = new MissingPackageInfoCheck();

    CheckVerifier.newVerifier()
      .onFiles(
        mainCodeSourcesPath("DefaultPackage.java"),
        mainCodeSourcesPath("checks/packageInfo/HelloWorld.java"),
        mainCodeSourcesPath("checks/packageInfo/package-info.java"),
        mainCodeSourcesPath("checks/packageInfo/nopackageinfo/HelloWorld.java"),
        mainCodeSourcesPath("checks/packageInfo/nopackageinfo/nopackageinfo.java"))
      .withCheck(check)
      .verifyIssueOnProject(EXPECTED_MESSAGE);

    Set<String> set = check.missingPackageWithoutPackageFile;
    assertThat(set).hasSize(1);
    assertThat(set.iterator().next()).isEqualTo(EXPECTED_PACKAGE);
  }

  @Test
  void caching() {
    verifier
      .onFiles(
        mainCodeSourcesPath("DefaultPackage.java"),
        mainCodeSourcesPath("checks/packageInfo/HelloWorld.java"),
        mainCodeSourcesPath("checks/packageInfo/package-info.java"),
        mainCodeSourcesPath("checks/packageInfo/nopackageinfo/HelloWorld.java"),
        mainCodeSourcesPath("checks/packageInfo/nopackageinfo/nopackageinfo.java")
      )
      .withCheck(new MissingPackageInfoCheck())
      .withCache(readCache, writeCache)
      .verifyIssueOnProject(EXPECTED_MESSAGE);

    var check = spy(new MissingPackageInfoCheck());

    var populatedReadCache = new InternalReadCache().putAll(writeCache);
    var writeCache2 = new InternalWriteCache().bind(populatedReadCache);
    CheckVerifier.newVerifier()
      .withCache(populatedReadCache, writeCache2)
      .addFiles(InputFile.Status.SAME,
        mainCodeSourcesPath("checks/packageInfo/HelloWorld.java"),
        mainCodeSourcesPath("checks/packageInfo/package-info.java"),
        mainCodeSourcesPath("checks/packageInfo/nopackageinfo/HelloWorld.java"),
        mainCodeSourcesPath("checks/packageInfo/nopackageinfo/nopackageinfo.java"),
        mainCodeSourcesPath("DefaultPackage.java")
      )
      .withCheck(check)
      .verifyIssueOnProject(EXPECTED_MESSAGE);

    verify(check, times(0)).scanFile(any());
    verify(check, times(5)).scanWithoutParsing(any());
    assertThat(writeCache2.getData())
      .hasSizeGreaterThanOrEqualTo(5)
      .containsExactlyInAnyOrderEntriesOf(writeCache.getData());
  }

  @Test
  void cache_deserialization_throws_IOException() throws IOException, NoSuchAlgorithmException {
    String filePath = mainCodeSourcesPath("checks/packageInfo/HelloWorld.java");
    InputFile cachedFile = HashCacheTestHelper.inputFileFromPath(filePath);
    byte[] cachedHash = FileHashingUtils.inputFileContentHash(cachedFile);
    var inputStream = mock(InputStream.class);
    doThrow(new IOException()).when(inputStream).readAllBytes();
    var localReadCache = mock(ReadCache.class);
    InternalWriteCache localWriteCache = new InternalWriteCache().bind(localReadCache);
    doReturn(inputStream).when(localReadCache).read("java:S1228;S4032:package:" + cachedFile.key());
    doReturn(true).when(localReadCache).contains(any());
    doReturn(new ByteArrayInputStream(cachedHash))
      .when(localReadCache).read("java:contentHash:MD5:" + cachedFile.key());

    var localVerifier = CheckVerifier.newVerifier()
      .withCache(localReadCache, localWriteCache)
      .addFiles(InputFile.Status.SAME, filePath)
      .withCheck(new MissingPackageInfoCheck());

    assertThatThrownBy(localVerifier::verifyNoIssues)
      .isInstanceOf(AnalysisException.class)
      .hasRootCauseInstanceOf(IOException.class);
  }

  @Test
  void write_cache_multiple_writes() {
    logTester.setLevel(Level.TRACE);
    verifier
      .addFiles(InputFile.Status.SAME,
        mainCodeSourcesPath("checks/packageInfo/HelloWorld.java")
      )
      .withCheck(new MissingPackageInfoCheck());

    verifier.verifyNoIssues();
    verifier.verifyNoIssues();
    assertThat(logTester.logs(Level.TRACE))
      .anyMatch(msg -> msg.matches("Could not store data to cache key '[^']+': .+"));
  }

  @Test
  void emptyCache() throws NoSuchAlgorithmException, IOException {
    logTester.setLevel(Level.TRACE);
    String filePath = mainCodeSourcesPath("checks/packageInfo/HelloWorld.java");
    ReadCache populatedReadCache = HashCacheTestHelper.internalReadCacheFromFile(filePath);
    CheckVerifier.newVerifier()
      .addFiles(InputFile.Status.SAME, filePath)
      .withCache(populatedReadCache, new InternalWriteCache().bind(populatedReadCache))
      .withCheck(new MissingPackageInfoCheck())
      .verifyNoIssues();

    assertThat(logTester.logs(Level.TRACE).stream()
      .filter(msg -> msg.matches("Cache miss for key '[^']+'")))
      .hasSize(1);
  }

}
