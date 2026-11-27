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
package org.sonar.java.semsitter;

import com.sonar.sslr.api.RecognitionException;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaVersionImpl;
import org.sonarsource.semsitter.wire.Document;
import org.sonarsource.semsitter.wire.Node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemSitterParserTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @TempDir
  Path project;

  private final ScriptedServer server = new ScriptedServer();
  private final Map<String, JParseResult> results = new LinkedHashMap<>();

  private final Set<String> unsupported = new HashSet<>();

  @Test
  void every_file_is_parsed_against_one_server_started_on_the_common_directory() throws Exception {
    InputFile b = file("src/p/B.java");
    InputFile a = file("src/p/q/A.java");
    AtomicInteger starts = new AtomicInteger();
    Path[] startedOn = new Path[1];

    parser(JParser.Mode.FILE_BY_FILE, new JavaVersionImpl(17), List.of(new File("lib.jar")), root -> {
      starts.incrementAndGet();
      startedOn[0] = root;
      return server;
    }).parse(List.of(b, a), () -> false, new AnalysisProgress(2), this::collect);

    assertThat(starts).hasValue(1);
    assertThat(realPath(startedOn[0])).isEqualTo(realPath(project.resolve("src").resolve("p")));
    assertThat(server.scanned).containsExactly("B.java", "q/A.java");
    assertThat(server.requests).containsExactly("set-settings", "full-gen", "get-file B.java", "get-file q/A.java");
    assertThat(server.closed).isTrue();
    assertThat(results.keySet()).containsExactly("B.java", "A.java");
    for (JParseResult result : results.values()) {
      assertThat(result.get()).isInstanceOf(JavaTree.CompilationUnitTreeImpl.class);
    }
  }

  @Test
  void files_are_parsed_in_the_order_the_server_scans_them() {
    InputFile z = file("Z.java");
    InputFile a = file("A.java");

    parser(JParser.Mode.BATCH, new JavaVersionImpl(), List.of(), root -> server).parse(List.of(z, a), () -> false, new AnalysisProgress(2), this::collect);

    assertThat(server.requests).containsExactly("set-settings", "full-gen", "get-file A.java", "get-file Z.java");
    assertThat(server.ahead).isEqualTo(SemSitterParser.FILES_AHEAD);
  }

  @Test
  void every_file_gets_the_types_the_server_already_sent_for_another_file() {
    server.declarations.put("org.junit.Test", "{\"kind\":\"annotation\"}");
    Map<String, Set<String>> declared = new LinkedHashMap<>();
    DocumentTranslator recording = (document, loaders, noLibraries, javaVersion) -> {
      declared.put(document.getPath(), loaders.types(Set.of("org.junit.Test")).keySet());
      return JParseResult.failure(new RecognitionException(1, "translated"));
    };

    new SemSitterParser(JParser.Mode.BATCH, new JavaVersionImpl(), List.of(), root -> server, recording, project.resolve("no-jdk"))
      .parse(List.of(file("ATest.java"), file("BTest.java")), () -> false, new AnalysisProgress(2), this::collect);

    assertThat(declared).containsOnlyKeys("ATest.java", "BTest.java").allSatisfy((path, types) -> assertThat(types).containsExactly("org.junit.Test"));
    assertThat(server.requests).containsOnlyOnce("get-types");
  }

  @Test
  void loaders_answer_while_the_session_is_open_and_nothing_once_it_is_closed() {
    server.packages.put("p", true);
    List<DocumentTranslator.ServerLoaders> sessions = new ArrayList<>();
    List<Boolean> answers = new ArrayList<>();
    DocumentTranslator recording = (document, loaders, noLibraries, javaVersion) -> {
      sessions.add(loaders);
      answers.add(loaders.hasPackage("p"));
      answers.add(loaders.hasPackage("zz"));
      return JParseResult.failure(new RecognitionException(1, "translated"));
    };

    new SemSitterParser(JParser.Mode.BATCH, new JavaVersionImpl(), List.of(), root -> server, recording, project.resolve("no-jdk"))
      .parse(List.of(file("A.java")), () -> false, new AnalysisProgress(1), this::collect);

    assertThat(answers).containsExactly(true, false);
    assertThat(sessions.getFirst().types(Set.of("java.lang.String"))).isNull();
    assertThat(sessions.getFirst().hasPackage("p")).isNull();
  }

  @Test
  void file_fails_alone_when_the_server_cannot_answer_a_loader() {
    server.loaderFailure = new IOException("broken pipe");
    DocumentTranslator loading = (document, loaders, noLibraries, javaVersion) -> {
      if ("A.java".equals(document.getPath())) {
        loaders.types(Set.of("java.lang.String"));
      } else {
        loaders.hasPackage("p");
      }
      return JParseResult.failure(new RecognitionException(1, "translated"));
    };

    new SemSitterParser(JParser.Mode.FILE_BY_FILE, new JavaVersionImpl(), List.of(), root -> server, loading, project.resolve("no-jdk"))
      .parse(List.of(file("A.java"), file("B.java")), () -> false, new AnalysisProgress(2), this::collect);

    assertThatThrownBy(results.get("A.java")::get).hasMessageContaining("SemSitter: unable to parse file.");
    assertThatThrownBy(results.get("B.java")::get).hasMessageContaining("SemSitter: unable to parse file.");
  }

  @Test
  void session_settings_describe_the_project() {
    parser(JParser.Mode.BATCH, new JavaVersionImpl(21), List.of(new File("lib.jar")), root -> server)
      .parse(List.of(file("A.java")), () -> false, new AnalysisProgress(1), this::collect);

    assertThat(server.settings).containsEntry(SemSitterSettings.JAVA_VERSION, List.of("21"));
    assertThat(server.settings.get(SemSitterSettings.LIBRARIES)).contains(new File("lib.jar").getAbsolutePath());
  }

  @Test
  void nothing_is_started_without_files() {
    AtomicInteger starts = new AtomicInteger();

    parser(JParser.Mode.BATCH, new JavaVersionImpl(), List.of(), (SemSitterParser.Launcher) root -> {
      starts.incrementAndGet();
      return server;
    }).parse(List.of(), () -> false, new AnalysisProgress(0), this::collect);

    assertThat(starts).hasValue(0);
    assertThat(results).isEmpty();
  }

  @Test
  void the_analysis_fails_when_the_server_cannot_start() {
    SemSitterParser parser = parser(JParser.Mode.BATCH, new JavaVersionImpl(), List.of(), (SemSitterParser.Launcher) root -> {
      throw new IOException("no executable");
    });
    List<InputFile> files = List.of(file("A.java"), file("B.java"));
    AnalysisProgress progress = new AnalysisProgress(2);

    assertThatThrownBy(() -> parser.parse(files, () -> false, progress, this::collect))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The SemSitter server could not start: no executable");
    assertThat(results).isEmpty();
  }

  @Test
  void the_analysis_fails_and_the_server_stops_when_the_session_cannot_be_prepared() {
    server.settingsFailure = new IOException("rejected");
    SemSitterParser parser = parser(JParser.Mode.FILE_BY_FILE, new JavaVersionImpl(), List.of(), root -> server);
    List<InputFile> files = List.of(file("A.java"));
    AnalysisProgress progress = new AnalysisProgress(1);

    assertThatThrownBy(() -> parser.parse(files, () -> false, progress, this::collect)).hasMessage("The SemSitter server could not start: rejected");
    assertThat(results).isEmpty();
    assertThat(server.closed).isTrue();
    assertThat(server.requests).containsExactly("set-settings");
  }

  @Test
  void file_the_server_cannot_answer_fails_alone() throws Exception {
    server.failing.add("A.java");

    parser(JParser.Mode.FILE_BY_FILE, new JavaVersionImpl(), List.of(), root -> server)
      .parse(List.of(file("A.java"), file("B.java")), () -> false, new AnalysisProgress(2), this::collect);

    assertThatThrownBy(results.get("A.java")::get)
      .isInstanceOf(RecognitionException.class)
      .hasMessageContaining("SemSitter: unable to parse file.");
    assertThat(results.get("B.java").get()).isNotNull();
  }

  @Test
  void syntax_error_of_the_translator_is_the_parse_error_of_the_file() {
    server.documents.put("A.java", Document.newBuilder().setPath("A.java").setHasError(true)
      .setTree(Node.newBuilder().setKind("program")).build());

    parser(JParser.Mode.FILE_BY_FILE, new JavaVersionImpl(), List.of(), root -> server)
      .parse(List.of(file("A.java")), () -> false, new AnalysisProgress(1), this::collect);

    assertThatThrownBy(results.get("A.java")::get)
      .isInstanceOf(RecognitionException.class)
      .hasMessage("Syntax error in A.java");
  }

  @Test
  void a_file_the_translator_does_not_support_is_a_parse_error_and_the_others_are_parsed() throws Exception {
    unsupported.add("A.java");

    parser(JParser.Mode.FILE_BY_FILE, new JavaVersionImpl(), List.of(), root -> server)
      .parse(List.of(file("A.java"), file("B.java")), () -> false, new AnalysisProgress(2), this::collect);

    assertThat(results.keySet()).containsExactly("A.java", "B.java");
    assertThatThrownBy(results.get("A.java")::get)
      .isInstanceOf(RecognitionException.class)
      .hasMessage("Unsupported construct: lambda")
      .hasCauseInstanceOf(UnsupportedConstructException.class);
    assertThat(results.get("B.java").get()).isInstanceOf(JavaTree.CompilationUnitTreeImpl.class);
  }

  @Test
  void batches_log_the_progress_of_the_whole_analysis() {
    AnalysisProgress analysisProgress = new AnalysisProgress(4);
    SemSitterParser parser = parser(JParser.Mode.BATCH, new JavaVersionImpl(), List.of(), root -> server);

    analysisProgress.startBatch(2);
    parser.parse(List.of(file("A.java"), file("B.java")), () -> false, analysisProgress, this::collect);
    analysisProgress.endBatch();
    assertThat(logTester.logs(Level.INFO)).contains("Starting batch processing.").doesNotContain("100% analyzed", "Batch processing: Done.");

    analysisProgress.startBatch(2);
    parser.parse(List.of(file("C.java"), file("D.java")), () -> false, analysisProgress, this::collect);
    analysisProgress.endBatch();
    assertThat(logTester.logs(Level.INFO))
      .containsOnlyOnce("Starting batch processing.")
      .containsSubsequence("Starting batch processing.", "100% analyzed", "Batch processing: Done.");
  }

  @Test
  void batch_logs_the_share_of_the_analysis_done_once_a_period_has_passed() {
    AnalysisProgress analysisProgress = new AnalysisProgress(4);
    analysisProgress.startBatch(2);
    analysisProgress.endBatch();
    analysisProgress.startBatch(2);

    ParseProgress progress = new ParseProgress.Batch(analysisProgress, 2, 0);
    progress.nextFile();
    progress.end(false);

    assertThat(logTester.logs(Level.INFO)).containsExactly("75% analyzed");
  }

  @Test
  void file_with_another_suffix_is_not_parsed() {
    parser(JParser.Mode.FILE_BY_FILE, new JavaVersionImpl(), List.of(), root -> server)
      .parse(List.of(file("A.jav")), () -> false, new AnalysisProgress(1), this::collect);

    assertThatThrownBy(results.get("A.jav")::get)
      .isInstanceOf(RecognitionException.class)
      .hasMessage("Parse error: unsupported file suffix for A.jav");
    assertThat(server.requests).doesNotContain("get-file A.jav");
  }

  @Test
  void file_by_file_stops_quietly_when_the_analysis_is_cancelled() {
    BooleanSupplier cancelAfterFirst = () -> !results.isEmpty();

    parser(JParser.Mode.FILE_BY_FILE, new JavaVersionImpl(), List.of(), root -> server)
      .parse(List.of(file("A.java"), file("B.java")), cancelAfterFirst, new AnalysisProgress(2), this::collect);

    assertThat(results.keySet()).containsExactly("A.java");
    assertThat(server.closed).isTrue();
  }

  @Test
  void batch_raises_a_cancellation_and_still_stops_the_server() {
    BooleanSupplier cancelAfterFirst = () -> !results.isEmpty();
    SemSitterParser parser = parser(JParser.Mode.BATCH, new JavaVersionImpl(), List.of(), root -> server);
    List<InputFile> files = List.of(file("A.java"), file("B.java"));
    AnalysisProgress progress = new AnalysisProgress(2);

    assertThatThrownBy(() -> parser.parse(files, cancelAfterFirst, progress, this::collect)).isInstanceOf(CancellationException.class);

    assertThat(results.keySet()).containsExactly("A.java");
    assertThat(server.closed).isTrue();
  }

  @Test
  void files_sharing_no_directory_fail_the_analysis() throws IOException {
    try (FileSystem otherDrive = FileSystems.newFileSystem(project.resolve("drive.zip"), Map.of("create", "true"))) {
      InputFile elsewhere = new TestInputFileBuilder("module", "B.java")
        .setModuleBaseDir(otherDrive.getPath("/"))
        .setCharset(StandardCharsets.UTF_8)
        .setLanguage("java")
        .setContents("class B {}")
        .build();

      SemSitterParser parser = parser(JParser.Mode.BATCH, new JavaVersionImpl(), List.of(), root -> server);
      List<InputFile> files = List.of(file("A.java"), elsewhere);
      AnalysisProgress progress = new AnalysisProgress(2);

      assertThatThrownBy(() -> parser.parse(files, () -> false, progress, this::collect))
        .hasMessage("The files share no directory to start the SemSitter server on.");
    }

    assertThat(server.requests).isEmpty();
  }

  @Test
  void server_stops_when_the_callback_fails() {
    SemSitterParser parser = parser(JParser.Mode.BATCH, new JavaVersionImpl(), List.of(), root -> server);
    List<InputFile> files = List.of(file("A.java"));
    AnalysisProgress progress = new AnalysisProgress(1);

    assertThatThrownBy(() -> parser.parse(files, () -> false, progress, (input, result) -> {
      throw new IllegalStateException("check failed");
    })).isInstanceOf(IllegalStateException.class);

    assertThat(server.closed).isTrue();
  }

  private SemSitterParser parser(JParser.Mode mode, JavaVersionImpl version, List<File> classpath, SemSitterParser.Launcher launcher) {
    return new SemSitterParser(mode, version, classpath, launcher, this::translate, project.resolve("no-jdk"));
  }

  private JParseResult translate(Document document, DocumentTranslator.ServerLoaders loaders, boolean noLibraries, int javaVersion) {
    if (unsupported.contains(document.getPath())) {
      throw new UnsupportedConstructException("lambda");
    }
    if (document.getHasError()) {
      throw new RecognitionException(1, "Syntax error in " + document.getPath());
    }
    return JParseResult.success(JParser.parse(file(document.getPath()), List.of(), new JavaVersionImpl()));
  }

  private void collect(InputFile file, JParseResult result) {
    results.put(file.filename(), result);
  }

  /**
   * The real path of {@code path}, resolved from the nearest existing parent so a short Windows name and a
   * macOS {@code /var} symlink compare equal to the path the scanner reports.
   */
  private static Path realPath(Path path) throws IOException {
    Path absolute = path.toAbsolutePath().normalize();
    Path existing = absolute;
    while (existing != null && !Files.exists(existing)) {
      existing = existing.getParent();
    }
    if (existing == null) {
      return absolute;
    }
    return existing.toRealPath().resolve(existing.relativize(absolute));
  }

  private InputFile file(String relativePath) {
    return new TestInputFileBuilder("module", relativePath)
      .setModuleBaseDir(project)
      .setCharset(StandardCharsets.UTF_8)
      .setLanguage("java")
      .setContents("class A {}")
      .build();
  }
}
