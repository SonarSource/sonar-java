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
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.jdt.JDTParser;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JavaVersionImpl;

/**
 * The JDK type declarations a SemSitter server answered, recorded in {@code replay/types.json.gz}, and ECJ parsing of
 * test sources.
 */
final class Replays {

  private static final int JAVA_VERSION = 21;
  static final Path DIRECTORY = Path.of("src", "test", "resources", "org", "sonar", "java", "semsitter", "replay");
  static final Path TYPES = DIRECTORY.resolve("types.json.gz");

  private Replays() {
  }

  static String read(Path file) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return new String(isGzip(file) ? new GZIPInputStream(in).readAllBytes() : in.readAllBytes(), StandardCharsets.UTF_8);
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
    JDTParser.create(JParser.Mode.BATCH, new JavaVersionImpl(JAVA_VERSION), List.of(), false, true)
      .parse(List.of(inputFile(baseDir, sample + ".java", source)), () -> false, new AnalysisProgress(1), (file, result) -> results.add(result));
    return results.get(0);
  }
}
