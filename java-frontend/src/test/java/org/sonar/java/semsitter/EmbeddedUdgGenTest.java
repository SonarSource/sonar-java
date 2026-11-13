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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedUdgGenTest {

  @Test
  void only_linux_x64_is_packaged() {
    assertThat(EmbeddedUdgGen.resourceName("Linux", "amd64")).isEqualTo("/udg-gen/udg-gen-linux-x64.xz");
    assertThat(EmbeddedUdgGen.resourceName("Linux", "x86_64")).isEqualTo("/udg-gen/udg-gen-linux-x64.xz");
    assertThat(EmbeddedUdgGen.resourceName("Linux", "aarch64")).isNull();
    assertThat(EmbeddedUdgGen.resourceName("Mac OS X", "x86_64")).isNull();
    assertThat(EmbeddedUdgGen.resourceName("Windows 11", "amd64")).isNull();
    assertThat(EmbeddedUdgGen.resourceName(null, null)).isNull();
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  void extracts_an_executable_on_linux() throws IOException {
    if (EmbeddedUdgGen.resourceName(System.getProperty("os.name"), System.getProperty("os.arch")) == null) {
      return;
    }
    Path executable = EmbeddedUdgGen.extract();
    assertThat(executable).isExecutable();
    assertThat(Files.size(executable)).isGreaterThan(1_000_000L);
  }
}
