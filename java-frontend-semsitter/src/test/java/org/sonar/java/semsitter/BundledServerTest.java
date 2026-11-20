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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
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
