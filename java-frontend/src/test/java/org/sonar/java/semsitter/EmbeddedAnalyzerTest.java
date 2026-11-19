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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EmbeddedAnalyzerTest {

  @Test
  void packaged_platforms_follow_the_detected_os_and_architecture() {
    assertThat(EmbeddedAnalyzer.resourceName("Linux", "amd64")).isEqualTo("/semsitter/analyzer-linux-x64.xz");
    assertThat(EmbeddedAnalyzer.resourceName("Linux", "x86_64")).isEqualTo("/semsitter/analyzer-linux-x64.xz");
    assertThat(EmbeddedAnalyzer.resourceName("Linux", "aarch64")).isEqualTo("/semsitter/analyzer-linux-aarch64.xz");
    assertThat(EmbeddedAnalyzer.resourceName("Mac OS X", "aarch64")).isEqualTo("/semsitter/analyzer-macos-aarch64.xz");
    assertThat(EmbeddedAnalyzer.resourceName("Mac OS X", "x86_64")).isEqualTo("/semsitter/analyzer-macos-x64.xz");
    assertThat(EmbeddedAnalyzer.resourceName("Windows 11", "amd64")).isEqualTo("/semsitter/analyzer-windows-x64.xz");
    assertThat(EmbeddedAnalyzer.resourceName("Windows 11", "aarch64")).isNull();
    assertThat(EmbeddedAnalyzer.resourceName("FreeBSD", "amd64")).isNull();
    assertThat(EmbeddedAnalyzer.resourceName(null, null)).isNull();
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC, OS.WINDOWS})
  void extracts_an_executable_for_the_running_platform() throws IOException {
    String resource = EmbeddedAnalyzer.resourceName(System.getProperty("os.name"), System.getProperty("os.arch"));
    // The analyzer binaries are packaged at release time, so a plain checkout has none.
    assumeTrue(resource != null && EmbeddedAnalyzer.class.getResource(resource) != null);
    Path executable = EmbeddedAnalyzer.extract();
    assertThat(executable).isExecutable();
    assertThat(Files.size(executable)).isGreaterThan(1_000_000L);
  }
}
