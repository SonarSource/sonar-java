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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JavaVersionImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemSitterParserTest {

  @TempDir
  Path project;

  private final ScriptedServer server = new ScriptedServer();
  private final List<InputFile> fallbackFiles = new ArrayList<>();
  private final Map<String, JParseResult> results = new LinkedHashMap<>();

  private final JParser fallback = (files, canceled, progress, action) -> files.forEach(fallbackFiles::add);

  private final DocumentTranslator translator = (document, types, packages, noLibraries, javaVersion) -> JParseResult.failure(new RecognitionException(1, "translated"));

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
      assertThatThrownBy(result::get).isInstanceOf(RecognitionException.class).hasMessage("translated");
    }
    assertThat(fallbackFiles).isEmpty();
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
  void default_frontend_parses_the_files_when_the_server_cannot_start() {
    InputFile a = file("A.java");
    InputFile b = file("B.java");

    parser(JParser.Mode.BATCH, new JavaVersionImpl(), List.of(), (SemSitterParser.Launcher) root -> {
      throw new IOException("no executable");
    }).parse(List.of(a, b), () -> false, new AnalysisProgress(2), this::collect);

    assertThat(fallbackFiles).containsExactly(a, b);
    assertThat(results).isEmpty();
  }

  @Test
  void default_frontend_parses_the_files_and_the_server_stops_when_the_session_cannot_be_prepared() {
    server.settingsFailure = new IOException("rejected");
    InputFile a = file("A.java");

    parser(JParser.Mode.FILE_BY_FILE, new JavaVersionImpl(), List.of(), root -> server).parse(List.of(a), () -> false, new AnalysisProgress(1), this::collect);

    assertThat(fallbackFiles).containsExactly(a);
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
    assertThatThrownBy(results.get("B.java")::get).isInstanceOf(RecognitionException.class).hasMessage("translated");
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
    return new SemSitterParser(mode, version, classpath, launcher, () -> fallback, translator, project.resolve("no-jdk"));
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
