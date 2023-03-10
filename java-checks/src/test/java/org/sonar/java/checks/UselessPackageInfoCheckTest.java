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

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalysisException;
import org.sonar.java.caching.CacheReadException;
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

class UselessPackageInfoCheckTest {

  @RegisterExtension
  public final LogTesterJUnit5 logTester = new LogTesterJUnit5();

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
  void withNoOtherFile() {
    verifier
      .onFile(mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFiles/package-info.java"))
      .withCheck(new UselessPackageInfoCheck())
      .verifyIssueOnFile("Remove this package.");
  }

  @Test
  void withOtherFile() {
    verifier
      .onFile(mainCodeSourcesPath("checks/UselessPackageInfoCheck/package-info.java"))
      .withCheck(new UselessPackageInfoCheck())
      .verifyNoIssues();
  }

  @Test
  void notAPackageInfo() {
    verifier
      .onFiles(
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld1.java"),
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld2.java"))
      .withCheck(new UselessPackageInfoCheck())
      .verifyNoIssues();
  }

  @Test
  void notAPackageInfoOnSingleFile() {
    verifier
      .onFile(mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld1.java"))
      .withCheck(new UselessPackageInfoCheck())
      .verifyNoIssues();
  }

  @Test
  void defaultPackage() {
    verifier
      .onFile(mainCodeSourcesPath("DefaultPackage.java"))
      .withCheck(new UselessPackageInfoCheck())
      .verifyNoIssues();
  }

  @Test
  void caching() throws IOException, ClassNotFoundException {
    verifier
      .onFiles(
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld1.java"),
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld2.java"),
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/package-info.java"),
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFiles/package-info.java")
      )
      .withCheck(new UselessPackageInfoCheck())
      .verifyIssueOnFile("Remove this package.");

    var check = spy(new UselessPackageInfoCheck());

    var populatedReadCache = new InternalReadCache().putAll(writeCache);
    var writeCache2 = new InternalWriteCache().bind(populatedReadCache);
    CheckVerifier.newVerifier()
      .withCache(populatedReadCache, writeCache2)
      .addFiles(InputFile.Status.SAME,
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld1.java"),
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld2.java")
      )
      .addFiles(InputFile.Status.CHANGED,
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/package-info.java"),
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFiles/package-info.java")
      )
      .withCheck(check)
      .verifyIssueOnFile("Remove this package.");

    verify(check, times(2)).scanFile(any());
    verify(check, times(2)).scanWithoutParsing(any());
    assertThat(writeCache2.getData())
      .hasSize(4)
      .containsExactlyInAnyOrderEntriesOf(writeCache.getData());
  }

  @Test
  void cache_deserialization_throws_IOException() throws IOException {
    var inputStream = mock(InputStream.class);
    doThrow(new IOException()).when(inputStream).readAllBytes();
    var readCache = mock(ReadCache.class);
    doReturn(inputStream).when(readCache).read(any());
    doReturn(true).when(readCache).contains(any());

    var verifier = CheckVerifier.newVerifier()
      .withCache(readCache, writeCache)
      .addFiles(InputFile.Status.SAME,
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld1.java")
      )
      .withCheck(new UselessPackageInfoCheck());

    assertThatThrownBy(verifier::verifyNoIssues)
      .isInstanceOf(AnalysisException.class)
      .hasRootCauseInstanceOf(IOException.class);
  }

  @Test
  void write_cache_multiple_writes() {
    logTester.setLevel(LoggerLevel.TRACE);
    verifier
      .addFiles(InputFile.Status.SAME,
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld1.java")
      )
      .withCheck(new UselessPackageInfoCheck());

    verifier.verifyNoIssues();
    verifier.verifyNoIssues();
    assertThat(logTester.logs(LoggerLevel.TRACE))
      .anyMatch(msg -> msg.matches("Could not store data to cache key '[^']+': .+"));
  }

  @Test
  void emptyCache() {
    logTester.setLevel(LoggerLevel.TRACE);
    verifier
      .addFiles(InputFile.Status.SAME,
        mainCodeSourcesPath("checks/UselessPackageInfoCheck/packageWithNoOtherFilesButNotPackageInfo/HelloWorld1.java")
      )
      .withCheck(new UselessPackageInfoCheck())
      .verifyNoIssues();

    assertThat(logTester.logs(LoggerLevel.TRACE).stream()
      .filter(msg -> msg.matches("Cache miss for key '[^']+'")))
      .hasSize(1);
  }
}
