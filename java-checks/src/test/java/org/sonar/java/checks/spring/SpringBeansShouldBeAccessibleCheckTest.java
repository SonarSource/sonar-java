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
package org.sonar.java.checks.spring;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalysisException;
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
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class SpringBeansShouldBeAccessibleCheckTest {

  private static final String BASE_PATH = "checks/spring/s4605/";

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
  void testComponentScan() {
    final String testFolder = BASE_PATH + "componentScan/";
    List<String> files = Arrays.asList(
      mainCodeSourcesPath("SpringBootAppInDefaultPackage.java"),
      mainCodeSourcesPath(testFolder + "packageA/ComponentA.java"),
      mainCodeSourcesPath(testFolder + "packageB/ComponentB.java"),
      mainCodeSourcesPath(testFolder + "packageC/ComponentC.java"),
      mainCodeSourcesPath(testFolder + "packageX/ComponentX.java"),
      mainCodeSourcesPath(testFolder + "packageY/ComponentY.java"),
      mainCodeSourcesPath(testFolder + "packageZ/ComponentZ.java"),
      mainCodeSourcesPath(testFolder + "packageFP/ComponentFP.java"),
      mainCodeSourcesPath(testFolder + "ComponentScan.java"));

    CheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void testSpringBootApplication() {
    final String testFolder = BASE_PATH + "springBootApplication/";
    List<String> files = Arrays.asList(
      mainCodeSourcesPath(testFolder + "Ko/Ko.java"),
      mainCodeSourcesPath(testFolder + "app/Ok/Ok.java"),
      mainCodeSourcesPath(testFolder + "app/SpringBootApp1.java"),
      mainCodeSourcesPath(testFolder + "secondApp/AnotherOk.java"),
      mainCodeSourcesPath(testFolder + "secondApp/SpringBootApp2.java"));

    CheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFiles(files)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void testSpringBootApplicationWithAnnotation() {
    final String testFolderThirdApp = BASE_PATH + "springBootApplication/thirdApp/";
    List<String> thirdAppTestFiles = Arrays.asList(
      mainCodeSourcesPath(testFolderThirdApp + "SpringBootApp3.java"),
      mainCodeSourcesPath(testFolderThirdApp + "domain/SomeClass.java"),
      mainCodeSourcesPath(testFolderThirdApp + "controller/Controller.java"));

    CheckVerifier.newVerifier()
      .onFiles(thirdAppTestFiles)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyIssues();

    final String testFolderFourthApp = BASE_PATH + "springBootApplication/fourthApp/";
    List<String> fourthAppTestFiles = Arrays.asList(
      mainCodeSourcesPath(testFolderFourthApp + "SpringBootApp4.java"),
      nonCompilingTestSourcesPath(testFolderFourthApp + "SpringBootApp4b.java"),
      mainCodeSourcesPath(testFolderFourthApp + "domain/SomeClass.java"),
      mainCodeSourcesPath(testFolderFourthApp + "utility/SomeUtilityClass.java"),
      mainCodeSourcesPath(testFolderFourthApp + "controller/Controller.java"));

    CheckVerifier.newVerifier()
      .onFiles(fourthAppTestFiles)
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyIssues();
  }

  @Test
  void caching() throws IOException, ClassNotFoundException {
    var unchangedFiles = Stream.of(
      "app/SpringBootApp1.java",
      "fourthApp/SpringBootApp4.java"
    ).map(path -> mainCodeSourcesPath(BASE_PATH + "springBootApplication/" + path)).collect(Collectors.toList());
    var changedFiles = Stream.of(
      "app/Ok/Ok.java",
      "fourthApp/controller/Controller.java",
      "fourthApp/domain/SomeClass.java",
      "fourthApp/utility/SomeUtilityClass.java",
      "Ko/Ko.java"
    ).map(path -> mainCodeSourcesPath(BASE_PATH + "springBootApplication/" + path)).collect(Collectors.toList());

    var check = spy(new SpringBeansShouldBeAccessibleCheck());
    verifier
      .addFiles(InputFile.Status.SAME, unchangedFiles)
      .addFiles(InputFile.Status.CHANGED, changedFiles)
      .withCheck(check)
      .verifyIssues();

    verify(check, times(15)).visitNode(any());
    verify(check, times(2)).scanWithoutParsing(any());
    assertThat(writeCache.getData())
      .hasSize(2);


    check = spy(new SpringBeansShouldBeAccessibleCheck());

    var populatedReadCache = new InternalReadCache().putAll(writeCache);
    var writeCache2 = new InternalWriteCache().bind(populatedReadCache);
    CheckVerifier.newVerifier()
      .withCache(populatedReadCache, writeCache2)
      .addFiles(InputFile.Status.SAME, unchangedFiles)
      .addFiles(InputFile.Status.CHANGED, changedFiles)
      .withCheck(check)
      .verifyIssues();

    verify(check, times(12)).visitNode(any());
    verify(check, times(2)).scanWithoutParsing(any());
    assertThat(writeCache2.getData())
      .hasSize(2)
      .containsExactlyEntriesOf(writeCache.getData());
  }

  @Test
  void cache_deserialization_throws_IOException() throws IOException {
    var inputStream = mock(InputStream.class);
    doThrow(new IOException()).when(inputStream).readAllBytes();
    var readCache = mock(ReadCache.class);
    doReturn(inputStream).when(readCache).read(any());

    var verifier = CheckVerifier.newVerifier()
      .withCache(readCache, writeCache)
      .addFiles(InputFile.Status.SAME,
        mainCodeSourcesPath(BASE_PATH + "springBootApplication/app/SpringBootApp1.java")
      )
      .withCheck(new SpringBeansShouldBeAccessibleCheck());

    assertThatThrownBy(verifier::verifyNoIssues)
      .isInstanceOf(AnalysisException.class)
      .hasRootCauseInstanceOf(IOException.class);
  }

  @Test
  void write_cache_multiple_writes() {
    verifier
      .addFiles(InputFile.Status.SAME,
        mainCodeSourcesPath(BASE_PATH + "springBootApplication/app/SpringBootApp1.java")
      )
      .withCheck(new SpringBeansShouldBeAccessibleCheck());

    verifier.verifyNoIssues();
    assertThatThrownBy(verifier::verifyNoIssues)
      .isInstanceOf(AnalysisException.class)
      .hasRootCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void emptyCache() {
    logTester.setLevel(LoggerLevel.TRACE);
    verifier
      .addFiles(InputFile.Status.SAME,
        mainCodeSourcesPath(BASE_PATH + "springBootApplication/app/SpringBootApp1.java")
      )
      .withCheck(new SpringBeansShouldBeAccessibleCheck())
      .verifyNoIssues();

    assertThat(logTester.logs(LoggerLevel.TRACE).stream().filter(
      msg -> msg.matches("Could not load cached data for key '[^']+' due to an IllegalArgumentException: .+")
    )).hasSize(1);
  }
}
