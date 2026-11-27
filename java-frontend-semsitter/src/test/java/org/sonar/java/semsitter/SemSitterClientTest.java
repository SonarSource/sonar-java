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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.sonarsource.semsitter.wire.Node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemSitterClientTest {

  private static final Path ROOT = Path.of("project").toAbsolutePath();

  @Test
  void command_starts_the_server_on_the_project_root() {
    assertThat(SemSitterClient.command(List.of("/bin/udg-java-server"), ROOT))
      .startsWith("/bin/udg-java-server", "--server", "--lang", "java")
      .endsWith("--src", ROOT.toString(), "--project-root", ROOT.toString());
  }

  @Test
  void start_takes_the_executable_path() {
    Path missing = Path.of("missing-semsitter-executable");
    assertThatThrownBy(() -> SemSitterClient.start(missing, ROOT)).isInstanceOf(IOException.class);
  }

  @Test
  void requests_are_answered_in_order() throws IOException {
    try (SemSitterClient client = fakeServer("2.1.0")) {
      client.setSettings(Map.of("jvm.libraries", List.of("a.jar", "b.jar")));
      assertThat(client.fullGen(List.of("p/A.java", "p/B.java"))).isEqualTo(2);
      assertThat(client.getFile("p/A.java", 1).getPath()).isEqualTo("p/A.java");
      assertThat(JsonParser.parseString(new String(client.getTypes(List.of("java.lang.String")), StandardCharsets.UTF_8)).getAsJsonObject()
        .getAsJsonObject("java.lang.String").get("kind").getAsString()).isEqualTo("class");
      assertThat(client.hasPackage(List.of("java.util", "zz"))).containsExactlyInAnyOrderEntriesOf(Map.of("java.util", true, "zz", false));
    }
  }

  @Test
  void server_error_fails_the_request_only() throws IOException {
    try (SemSitterClient client = fakeServer("2.0.0")) {
      assertThatThrownBy(() -> client.getFile("p/Nope.java", 0))
        .isInstanceOf(IOException.class)
        .hasMessage("SemSitter server: no such file p/Nope.java");
      assertThat(client.getFile("p/A.java", 0).getPath()).isEqualTo("p/A.java");
    }
  }

  @Test
  void deeply_nested_document_is_read() throws IOException {
    try (SemSitterClient client = fakeServer("2.0.0")) {
      Node node = client.getFile("p/Deep.java", 0).getTree();
      int depth = 0;
      while (node.getChildrenCount() > 0) {
        node = node.getChildren(0);
        depth++;
      }
      assertThat(depth).isEqualTo(FakeServer.DEEP_TREE_DEPTH);
      assertThat(node.getKind()).isEqualTo("program");
    }
  }

  @Test
  void too_deeply_nested_document_is_an_io_error() throws IOException {
    try (SemSitterClient client = fakeServer("2.0.0")) {
      assertThatThrownBy(() -> client.getFile("p/TooDeep.java", 0)).isInstanceOf(IOException.class);
      assertThat(client.getFile("p/A.java", 0).getPath()).isEqualTo("p/A.java");
    }
  }

  @Test
  void truncated_payload_is_an_io_error() throws IOException {
    try (SemSitterClient client = fakeServer("2.0.0", "--truncate")) {
      assertThatThrownBy(() -> client.getFile("p/A.java", 0))
        .isInstanceOf(IOException.class)
        .hasMessageMatching("SemSitter server sent \\d+ of \\d+ payload bytes");
    }
  }

  @Test
  void other_protocol_major_is_rejected() {
    assertThatThrownBy(() -> fakeServer("1.8.0"))
      .isInstanceOf(IOException.class)
      .hasMessage("Unsupported SemSitter protocol 1.8.0");
  }

  @Test
  void answer_to_another_request_is_rejected() throws IOException {
    try (SemSitterClient client = fakeServer("2.0.0", "--wrong-id")) {
      assertThatThrownBy(() -> client.getFile("p/A.java", 0))
        .isInstanceOf(IOException.class)
        .hasMessage("SemSitter server answered request 0 instead of 2");
    }
  }

  @Test
  void malformed_answer_is_an_io_error() throws IOException {
    try (SemSitterClient client = fakeServer("2.0.0", "--garbage")) {
      assertThatThrownBy(() -> client.getFile("p/A.java", 0))
        .isInstanceOf(IOException.class)
        .hasMessageStartingWith("SemSitter server sent a malformed answer");
    }
  }

  @Test
  @Timeout(5)
  void close_does_not_wait_for_the_quit_answer() throws IOException {
    SemSitterClient client = fakeServer("2.0.0", "--silent-quit");
    client.close();
    assertThatThrownBy(() -> client.fullGen(List.of())).isInstanceOf(IOException.class);
  }

  @Test
  void server_exit_fails_the_next_request() throws IOException {
    try (SemSitterClient client = fakeServer("2.0.0", "--exit-after-hello")) {
      assertThatThrownBy(() -> client.fullGen(List.of())).isInstanceOf(IOException.class);
    }
  }

  private static SemSitterClient fakeServer(String... args) throws IOException {
    List<String> launcher = new ArrayList<>(List.of(
      Path.of(System.getProperty("java.home"), "bin", "java").toString(),
      "-cp", System.getProperty("java.class.path"),
      FakeServer.class.getName()));
    launcher.addAll(List.of(args));
    return SemSitterClient.start(SemSitterClient.command(launcher, ROOT));
  }
}
