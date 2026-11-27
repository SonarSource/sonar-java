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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.TextFormat;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.jdt.JDTParser;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JavaVersionImpl;
import org.sonarsource.semsitter.wire.Document;

/**
 * Recorded SemSitter server answers. Each sample {@code replay/X.java} has the document the server answered for it in
 * {@code replay/X.textproto.gz}. {@code replay/types.json.gz} holds a separately recorded snapshot of JDK type declarations;
 * {@link ReplayRecorderTest} does not refresh it.
 */
final class Replays {

  private static final int JAVA_VERSION = 21;
  private static final Pattern VERSIONED = Pattern.compile("java(\\d+)/.*");
  static final Path DIRECTORY = Path.of("src", "test", "resources", "org", "sonar", "java", "semsitter", "replay");
  static final Path TYPES = DIRECTORY.resolve("types.json.gz");
  static final Path PROJECT = Path.of("..", "java-frontend", "src", "main", "java");

  private static final String PROJECT_PREFIX = "project/";

  private Replays() {
  }

  /** The samples written for the replay, then the ones of {@code replay/corpus/}, taken from the check test sources. */
  static List<String> samples() throws IOException {
    try (Stream<Path> files = Files.walk(DIRECTORY)) {
      return files.map(file -> DIRECTORY.relativize(file).toString().replace('\\', '/')).filter(name -> name.endsWith(".java"))
        .map(name -> name.substring(0, name.length() - 5)).sorted().toList();
    }
  }

  static boolean isCorpus(String sample) {
    return sample.startsWith("corpus/") || isProjectFile(sample);
  }

  /** A sample of {@code replay/project/}: a file of {@link #PROJECT}, recorded with the whole project around it. */
  static boolean isProjectFile(String sample) {
    return sample.startsWith(PROJECT_PREFIX);
  }

  /** The language level of a sample: the one of its {@code replay/javaNN/} directory, 21 elsewhere. */
  static int javaVersion(String sample) {
    Matcher versioned = VERSIONED.matcher(sample);
    return versioned.matches() ? Integer.parseInt(versioned.group(1)) : JAVA_VERSION;
  }

  static String projectPath(String sample) {
    return sample.substring(PROJECT_PREFIX.length()) + ".java";
  }

  static String source(String sample) throws IOException {
    return Files.readString(DIRECTORY.resolve(sample + ".java"), StandardCharsets.UTF_8);
  }

  static Document document(String sample) throws IOException {
    Document.Builder document = Document.newBuilder();
    TextFormat.merge(read(recording(sample)), document);
    return document.build();
  }

  static Path recording(String sample) {
    return DIRECTORY.resolve(sample + ".textproto.gz");
  }

  static String read(Path file) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return new String(isGzip(file) ? new GZIPInputStream(in).readAllBytes() : in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  static void write(Path file, String content) throws IOException {
    try (OutputStream out = isGzip(file) ? new GZIPOutputStream(Files.newOutputStream(file)) : Files.newOutputStream(file)) {
      out.write(content.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static boolean isGzip(Path file) {
    return file.getFileName().toString().endsWith(".gz");
  }

  static JsonObject readJson(Path file) throws IOException {
    if (!Files.exists(file)) {
      return new JsonObject();
    }
    return JsonParser.parseString(read(file)).getAsJsonObject();
  }

  static InputFile inputFile(Path baseDir, String relativePath, String source) {
    return new TestInputFileBuilder("module", relativePath)
      .setModuleBaseDir(baseDir)
      .setCharset(StandardCharsets.UTF_8)
      .setLanguage("java")
      .setContents(source)
      .build();
  }

  static JParseResult parseWithEcj(Path baseDir, String sample, String source) {
    List<JParseResult> results = new ArrayList<>();
    JDTParser.create(JParser.Mode.BATCH, new JavaVersionImpl(javaVersion(sample)), List.of(), false, true)
      .parse(List.of(inputFile(baseDir, sample + ".java", source)), () -> false, new AnalysisProgress(1), (file, result) -> results.add(result));
    return results.get(0);
  }
}
