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

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisabledOnOs(OS.WINDOWS)
class SemsitterBridgeTest {

  @Test
  void parse_returns_the_jsonl_response() throws IOException {
    Path script = fakeBridge("""
      #!/bin/sh
      while IFS= read -r line; do
        case "$line" in
          *'"op":"quit"'*) printf '%s\\n' '{"id":2,"ok":true}'; exit 0 ;;
          *) printf '%s\\n' '{"id":1,"ok":true,"hasError":false,"tree":{"kind":"source_file","children":[{"kind":"hello"}]}}' ;;
        esac
      done
      """);
    try (SemsitterBridge bridge = SemsitterBridge.start(script)) {
      String response = bridge.parse("hello");
      assertThat(JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("tree").get("kind").getAsString())
        .isEqualTo("source_file");
    } finally {
      Files.deleteIfExists(script);
    }
  }

  @Test
  void parse_hello_with_the_real_bridge() throws IOException {
    String executable = System.getenv("SEMSITTER_BRIDGE");
    assumeTrue(executable != null && Files.isExecutable(Path.of(executable)));
    try (SemsitterBridge bridge = SemsitterBridge.start(Path.of(executable))) {
      String hello = bridge.parse("hello");
      assertThat(JsonParser.parseString(hello).getAsJsonObject().getAsJsonObject("tree").get("kind").getAsString())
        .isEqualTo("source_file");
      String broken = bridge.parse("nope");
      assertThat(JsonParser.parseString(broken).getAsJsonObject().get("hasError").getAsBoolean()).isTrue();
    }
  }

  @Test
  void parse_reports_a_bridge_error() throws IOException {
    Path script = fakeBridge("""
      #!/bin/sh
      while IFS= read -r line; do
        case "$line" in
          *'"op":"quit"'*) printf '%s\\n' '{"id":2,"ok":true}'; exit 0 ;;
          *) printf '%s\\n' '{"id":1,"ok":false,"error":"boom"}' ;;
        esac
      done
      """);
    try (SemsitterBridge bridge = SemsitterBridge.start(script)) {
      assertThatThrownBy(() -> bridge.parse("hello"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("boom");
    } finally {
      Files.deleteIfExists(script);
    }
  }

  private static Path fakeBridge(String script) throws IOException {
    Path path = Files.createTempFile("semsitter-bridge", ".sh");
    Files.writeString(path, script);
    Files.setPosixFilePermissions(path, Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));
    return path;
  }
}
