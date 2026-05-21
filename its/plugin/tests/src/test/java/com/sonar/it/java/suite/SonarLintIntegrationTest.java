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
package com.sonar.it.java.suite;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.jupiter.api.io.TempDir;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesAndTrackParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.RaisedIssueDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ClientFileDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TextRangeDto;
import org.sonarsource.sonarlint.core.test.utils.SonarLintBackendFixture;
import org.sonarsource.sonarlint.core.test.utils.SonarLintTestRpcServer;
import org.sonarsource.sonarlint.core.test.utils.junit5.SonarLintTest;
import org.sonarsource.sonarlint.core.test.utils.junit5.SonarLintTestHarness;
import org.sonarsource.sonarlint.core.test.utils.plugins.Plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.fail;
import static org.sonarsource.sonarlint.core.rpc.protocol.common.Language.JAVA;

public class SonarLintIntegrationTest {
  private static final String CONFIG_SCOPE_ID = "CONFIG_SCOPE_ID";

  private SonarLintBackendFixture.FakeSonarLintRpcClient client;
  private SonarLintTestRpcServer backend;

  @SonarLintTest
  public void simpleJava(SonarLintTestHarness harness, @TempDir Path baseDir) throws IOException {
    ClientFileDto inputFileDto = createFile(baseDir, "Foo.java", """
        public class Foo {
          public void foo() {
            int x;
            System.out.println("Foo");
            System.out.println("Foo"); //NOSONAR
          }
        }
        """,
      false);
    initWithFiles(harness, baseDir, inputFileDto);

    List<RaisedIssueDto> issues = analyzeFileAndGetIssues(inputFileDto.getUri());

    assertThat(issues)
      .extracting(
        RaisedIssueDto::getRuleKey,
        RaisedIssueDto::getTextRange)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(
        tuple("java:S106", new TextRangeDto(4, 4, 4, 14)),
        tuple("java:S1220", null),
        tuple("java:S1481", new TextRangeDto(3, 8, 3, 9))
      );
  }

  @SonarLintTest
  void simpleTestFileJava(SonarLintTestHarness harness, @TempDir Path baseDir) throws IOException {
    ClientFileDto inputFileDto = createFile(baseDir, "FooTest.java", """
        public class FooTest {
          @org.junit.Test
          @org.junit.Ignore
          public void testName() throws Exception {
            Foo foo = new Foo();
            org.assertj.core.api.Assertions.assertThat(foo.isFooActive());
            java.lang.Thread.sleep(Long.MAX_VALUE);
          }
        
          private static class Foo {
            public boolean isFooActive() {
              return false;
            }
          }
        }
        """,
      true);

    initWithFiles(harness, baseDir, inputFileDto);

    List<RaisedIssueDto> issues = analyzeFileAndGetIssues(inputFileDto.getUri());

    assertThat(issues)
      .extracting(
        RaisedIssueDto::getRuleKey,
        RaisedIssueDto::getTextRange)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(
        tuple("java:S1220", null),
        tuple("java:S2925", new TextRangeDto(7, 21, 7, 26))
      );

    fail("Validate that SonarLintIntegrationTest runs on CI");
  }

  @SonarLintTest
  void supportJavaSuppressWarning(SonarLintTestHarness harness, @TempDir Path baseDir) throws IOException {
    ClientFileDto inputFileDto = createFile(baseDir, "Foo.java", """
        public class Foo {
          @SuppressWarnings("java:S106")
          public void foo() {
            int x;
            System.out.println("Foo");
            System.out.println("Foo"); //NOSONAR
          }
        }
        """,
      false);
    initWithFiles(harness, baseDir, inputFileDto);

    List<RaisedIssueDto> issues = analyzeFileAndGetIssues(inputFileDto.getUri());

    assertThat(issues)
      .extracting(
        RaisedIssueDto::getRuleKey,
        RaisedIssueDto::getTextRange)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(
        tuple("java:S1220", null),
        tuple("java:S1481", new TextRangeDto(4, 8, 4, 9))
      );

  }

  @SonarLintTest
  void parse_error_should_report_analysis_error(SonarLintTestHarness harness, @TempDir Path baseDir) throws IOException {
    ClientFileDto inputFileDto = createFile(baseDir, "ParseError.java", "class ParseError {", false);

    initWithFiles(harness, baseDir, inputFileDto);
    AnalyzeFilesResponse analysisResult = analyzeFile(inputFileDto.getUri());

    assertThat(analysisResult.getFailedAnalysisFiles()).containsExactly(inputFileDto.getUri());
  }

  private AnalyzeFilesResponse analyzeFile(URI fileUri) {
    UUID analysisId = UUID.randomUUID();
    AnalyzeFilesAndTrackParams params = new AnalyzeFilesAndTrackParams(CONFIG_SCOPE_ID, analysisId, List.of(fileUri), Map.of(), false);
    return backend
      .getAnalysisService()
      .analyzeFilesAndTrack(params)
      .join();
  }

  private List<RaisedIssueDto> analyzeFileAndGetIssues(URI fileUri) {
    AnalyzeFilesResponse analysisResult = analyzeFile(fileUri);
    assertThat(analysisResult.getFailedAnalysisFiles()).isEmpty();
    await()
      .atMost(15, TimeUnit.SECONDS)
      .untilAsserted(() -> assertThat(client.getRaisedIssuesForScopeIdAsList(CONFIG_SCOPE_ID)).isNotEmpty());
    return client.getRaisedIssuesForScopeId(CONFIG_SCOPE_ID).get(fileUri);
  }

  private void initWithFiles(SonarLintTestHarness harness, Path baseDir, ClientFileDto fileDTOs) {
    Path javaPluginPath;

    // `PROJECT VERSION` is set on GitHub CIT, where is points to the current version of the plugin.
    // Locally, we use the local build, which requires `mvn clean install` in top-level project.
    String pluginVersion = System.getenv("PROJECT_VERSION");
    if(pluginVersion != null) {
      Locators locators = new Locators(Configuration.createEnv());
      MavenLocation mavenLocation = MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", pluginVersion);
      javaPluginPath = locators.locate(mavenLocation).toPath();
    }
    else {
      javaPluginPath = FileLocation.byWildcardMavenFilename(new File("../../../sonar-java-plugin/target"), "sonar-java-plugin-*.jar").getFile().toPath();
    }

    client = harness
      .newFakeClient()
      .withInitialFs(CONFIG_SCOPE_ID, baseDir, List.of(fileDTOs))
      .build();
    backend = harness
      .newBackend()
      .withStandaloneEmbeddedPluginAndEnabledLanguage(new Plugin(Set.of(JAVA), javaPluginPath, "", ""))
      .withUnboundConfigScope(CONFIG_SCOPE_ID)
      .start(client);
  }

  private static ClientFileDto createFile(Path folderPath, String fileName, String content, boolean isTest) throws IOException {
    Path filePath = folderPath.resolve(fileName);
    Files.writeString(filePath, content);
    return new ClientFileDto(
      filePath.toUri(), folderPath.relativize(filePath), CONFIG_SCOPE_ID, isTest, null, filePath, null, JAVA, true);
  }
}
