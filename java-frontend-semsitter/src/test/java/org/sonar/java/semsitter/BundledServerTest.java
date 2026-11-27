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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BundledServerTest {

  @TempDir
  Path project;

  @Test
  void bundled_server_stays_unused_outside_performance_validation() {
    System.clearProperty("sensor.context.key");

    assertThat(BundledServer.extract()).isEmpty();
  }

  @Test
  void the_installed_copy_is_executable() throws Exception {
    Path copy = BundledServer.install(new ByteArrayInputStream(new byte[] {1, 2, 3}), project);

    assertThat(Files.isExecutable(copy)).isTrue();
    assertThat(Files.readAllBytes(copy)).containsExactly(1, 2, 3);
  }

  @Test
  void a_failed_copy_fails_and_leaves_no_file_behind() throws Exception {
    InputStream broken = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("broken");
      }
    };

    assertThatThrownBy(() -> BundledServer.install(broken, project)).isInstanceOf(IllegalStateException.class).hasMessageContaining("could not be copied");
    try (var files = Files.list(project)) {
      assertThat(files).isEmpty();
    }
  }

  @Test
  void a_missing_bundled_server_fails() {
    assertThatThrownBy(() -> BundledServer.install(null, project)).hasMessage("The SemSitter server is not bundled in this plugin.");
  }

  @Test
  void bundled_linux_server_speaks_the_protocol() throws Exception {
    System.setProperty("sensor.context.key", BundledServer.PERFORMANCE_VALIDATION_CONTEXT);
    try {
      Optional<Path> server = BundledServer.extract();
      assumeTrue(server.isPresent());

      try (SemSitterClient client = SemSitterClient.start(server.get(), project)) {
        assertThat(client).isNotNull();
      }
      assertThat(Files.isExecutable(server.get())).isTrue();
    } finally {
      System.clearProperty("sensor.context.key");
    }
  }
}
