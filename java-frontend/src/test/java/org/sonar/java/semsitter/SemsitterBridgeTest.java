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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SemsitterBridgeTest {

  private Path executable;

  @BeforeEach
  void locateBridge() {
    String configured = System.getenv("SEMSITTER_BRIDGE");
    assumeTrue(configured != null && Files.isExecutable(Path.of(configured)));
    executable = Path.of(configured);
  }

  @Test
  void analyze_then_cst(@TempDir Path root) throws IOException {
    Path file = Files.writeString(root.resolve("A.java"), "class A { int f; int g() { return f; } }");
    try (SemsitterBridge bridge = SemsitterBridge.start(executable)) {
      JsonObject analysis = bridge.analyze(root, List.of(file), List.of());
      assertThat(analysis.getAsJsonArray("files").get(0).getAsString()).isEqualTo("A.java");
      JsonObject cst = bridge.cst(file.toString());
      assertThat(cst.getAsJsonObject("tree").get("k").getAsString()).isEqualTo("program");
      assertThat(cst.getAsJsonArray("symbols")).isNotEmpty();
      assertThat(cst.getAsJsonObject("facts").has("A#g()")).isTrue();
    }
  }

  @Test
  void errors_are_reported_and_the_bridge_survives() throws IOException {
    try (SemsitterBridge bridge = SemsitterBridge.start(executable)) {
      assertThatThrownBy(() -> bridge.cst("A.java")).isInstanceOf(IOException.class).hasMessageContaining("no analyze request yet");
      assertThat(bridge.parse("class B {}").get("hasError").getAsBoolean()).isFalse();
    }
  }
}
