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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JavaVersionImpl;
import org.sonarsource.semsitter.wire.Document;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Records what a real SemSitter server answers for every replay sample. Run it after adding a sample or when the server
 * output changes, with {@code -Dsemsitter.executable=<udg-java-server> -Dsemsitter.record=true}.
 */
@EnabledIfSystemProperty(named = "semsitter.record", matches = "true")
@EnabledIfSystemProperty(named = "semsitter.executable", matches = ".+")
class ReplayRecorderTest {

  @TempDir
  Path project;

  @Test
  void record_every_sample() throws Exception {
    JsonObject types = new JsonObject();
    JsonObject packages = new JsonObject();
    List<String> samples = Replays.samples();
    for (String sample : samples) {
      recordSample(sample, types, packages);
    }
    Replays.writeSorted(Replays.TYPES, types);
    Replays.writeSorted(Replays.PACKAGES, packages);
    assertThat(samples).isNotEmpty();
  }

  private void recordSample(String sample, JsonObject types, JsonObject packages) throws Exception {
    Path directory;
    String fileName;
    List<String> analyzed;
    if (Replays.isProjectFile(sample)) {
      directory = Replays.PROJECT.toAbsolutePath().normalize();
      fileName = Replays.projectPath(sample);
      analyzed = projectFiles(directory);
      Files.copy(directory.resolve(fileName), Replays.DIRECTORY.resolve(sample + ".java"), StandardCopyOption.REPLACE_EXISTING);
    } else {
      directory = Files.createDirectories(project.resolve(sample));
      fileName = Path.of(sample).getFileName() + ".java";
      Files.writeString(directory.resolve(fileName), Replays.source(sample), StandardCharsets.UTF_8);
      analyzed = List.of(fileName);
    }
    try (SemSitterClient client = SemSitterClient.start(Path.of(System.getProperty("semsitter.executable")), directory)) {
      client.setSettings(SemSitterSettings.of(new JavaVersionImpl(Replays.javaVersion(sample)), List.of(), Path.of(System.getProperty("java.home"))));
      client.fullGen(analyzed);
      Document document = client.getFile(fileName, 0);
      Replays.write(Replays.recording(sample), TextFormat.printer().printToString(document));
      if (document.getHasError()) {
        return;
      }
      JParseResult result = SemSitterTranslator.translate(document, names -> {
        JsonObject answer = JsonParser.parseString(new String(answer(client, names), StandardCharsets.UTF_8)).getAsJsonObject();
        answer.entrySet().forEach(entry -> types.add(entry.getKey(), entry.getValue()));
        return answer;
      }, name -> {
        Boolean exists = hasPackage(client, name);
        packages.addProperty(name, exists);
        return exists;
      }, true, Replays.javaVersion(sample));
      TreeDescription.describe(result.get());
    }
  }

  private static List<String> projectFiles(Path root) throws IOException {
    try (Stream<Path> files = Files.walk(root)) {
      return files.filter(file -> file.toString().endsWith(".java")).map(file -> root.relativize(file).toString().replace('\\', '/')).sorted().toList();
    }
  }

  private static byte[] answer(SemSitterClient client, java.util.Set<String> names) {
    try {
      return client.getTypes(names);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Boolean hasPackage(SemSitterClient client, String name) {
    try {
      return client.hasPackage(List.of(name)).get(name);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
