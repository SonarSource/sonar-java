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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisabledOnOs(OS.WINDOWS)
class AnalyzerSessionTest {

  @TempDir
  Path temp;

  @AfterEach
  void reset() {
    AnalyzerSession.moduleRoot(null);
    AnalyzerSession.moduleSettings(Map.of());
  }

  @Test
  void jdk_image_is_recognized_with_either_separator() {
    assertThat(AnalyzerSession.isJdkImage("/jdk/lib/modules")).isTrue();
    assertThat(AnalyzerSession.isJdkImage("C:\\jdk\\lib\\modules")).isTrue();
    assertThat(AnalyzerSession.isJdkImage("/libs/guava.jar")).isFalse();
  }

  @Test
  void failed_handshake_ends_the_process_and_the_next_request_starts_a_new_one() throws IOException {
    AnalyzerSession session = new AnalyzerSession(fakeServer("2.0.0"));

    assertThatThrownBy(() -> session.analyze(request("a", "x.jar")))
      .hasMessage("unsupported analyzer protocol 2.0.0");
    assertThatThrownBy(() -> session.analyze(request("a", "x.jar")))
      .hasMessage("unsupported analyzer protocol 2.0.0");

    assertThat(starts()).isEqualTo(2);
  }

  @Test
  void new_root_restarts_the_server_and_same_root_does_not() throws IOException {
    AnalyzerSession session = new AnalyzerSession(fakeServer("1.2.0"));

    session.analyze(request("a", "x.jar"));
    session.analyze(request("a", "x.jar"));
    assertThat(starts()).isEqualTo(1);

    session.analyze(request("b", "x.jar"));
    assertThat(starts()).isEqualTo(2);
  }

  @Test
  void classpath_change_rescans_without_restart_and_module_libraries_take_priority() throws IOException {
    AnalyzerSession session = new AnalyzerSession(fakeServer("1.2.0"));

    session.analyze(request("a", "x.jar"));
    session.analyze(request("a", "x.jar", "test.jar"));
    assertThat(starts()).isEqualTo(1);
    assertThat(commands("set-settings")).isEqualTo(2);

    AnalyzerSession.moduleSettings(Map.of("jvm.libraries", List.of("module.jar")));
    session.analyze(request("a", "x.jar", "test.jar"));
    session.analyze(request("a", "other.jar"));
    assertThat(commands("set-settings")).isEqualTo(3);
  }

  private Path fakeServer(String protocol) throws IOException {
    Path script = temp.resolve("fake-server.sh");
    Files.writeString(script, String.join("\n",
      "#!/bin/sh",
      "echo start >> '" + temp.resolve("log") + "'",
      "while read line; do",
      "  echo \"$line\" >> '" + temp.resolve("log") + "'",
      "  echo '{\"ok\":true,\"protocol\":\"" + protocol + "\"}'",
      "done",
      ""));
    assertThat(script.toFile().setExecutable(true)).isTrue();
    return script;
  }

  private JsonObject request(String module, String... classpath) {
    Path root = temp.resolve(module);
    AnalyzerSession.moduleRoot(root);
    JsonObject request = new JsonObject();
    request.addProperty("root", root.toString());
    request.add("files", new JsonArray());
    JsonArray entries = new JsonArray();
    List.of(classpath).forEach(entries::add);
    request.add("classpath", entries);
    return request;
  }

  private long starts() throws IOException {
    return Files.readAllLines(temp.resolve("log")).stream().filter("start"::equals).count();
  }

  private long commands(String name) throws IOException {
    return Files.readAllLines(temp.resolve("log")).stream().filter(line -> line.contains("\"cmd\":\"" + name + "\"")).count();
  }
}
