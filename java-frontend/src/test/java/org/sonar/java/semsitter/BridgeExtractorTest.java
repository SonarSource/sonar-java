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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.semsitter.PlatformDetection.Platform;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BridgeExtractorTest {

  @TempDir
  Path tempPath;

  static Stream<Arguments> getArchiveNameForPlatform() {
    return Stream.of(
      Arguments.of(Platform.LINUX_X64, "bridge-linux-x64.xz"),
      Arguments.of(Platform.LINUX_AARCH64, "bridge-linux-aarch64.xz"),
      Arguments.of(Platform.ALPINE_X64, "bridge-linux-x64.xz"),
      Arguments.of(Platform.ALPINE_AARCH64, "bridge-linux-aarch64.xz"),
      Arguments.of(Platform.MACOS_X64, "bridge-macos-x64.xz"),
      Arguments.of(Platform.MACOS_AARCH64, "bridge-macos-aarch64.xz"),
      Arguments.of(Platform.WINDOWS_X64, "bridge-windows-x64.xz"));
  }

  @ParameterizedTest
  @MethodSource
  void getArchiveNameForPlatform(Platform platform, String expectedArchiveName) {
    assertThat(BridgeExtractor.getArchiveNameForPlatform(platform)).isEqualTo(expectedArchiveName);
  }

  @Test
  void getArchiveNameForPlatform_unsupportedPlatform() {
    assertThatThrownBy(() -> BridgeExtractor.getArchiveNameForPlatform(Platform.UNSUPPORTED))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Cannot extract bridge for unsupported platform");
  }

  @Test
  void extract_unsupportedPlatform() {
    assertThatThrownBy(() -> BridgeExtractor.extract(tempPath, Platform.UNSUPPORTED))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Cannot extract bridge for unsupported platform");
  }

  @Test
  void extract_missingResource() {
    assertThatThrownBy(() -> BridgeExtractor.extract(tempPath, Platform.LINUX_X64, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Bridge resource not found");
  }

  @Test
  void extract_emptyArchive() throws IOException {
    assertThatThrownBy(() -> BridgeExtractor.extract(tempPath, Platform.LINUX_X64, emptyTarXz()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Bridge archive is empty: /bridge/bridge-linux-x64.xz");
  }

  @Test
  void extract_ignoresMetadataEntryBeforeBridge() throws IOException {
    BridgeExtractor.extract(tempPath, Platform.LINUX_X64, archiveWithMetadataBeforeBridge());

    assertThat(Files.readString(tempPath.resolve("bridge"))).isEqualTo("bridge executable");
    PosixFileAttributeView view = Files.getFileAttributeView(tempPath.resolve("bridge"), PosixFileAttributeView.class);
    if (view != null) {
      assertThat(view.readAttributes().permissions()).contains(OWNER_EXECUTE);
    }
    assertThat(tempPath.resolve("bridge.ext")).doesNotExist();
  }

  @Test
  void extract_reusesExistingBridge() throws IOException {
    Path bridgePath = tempPath.resolve("bridge");
    Files.writeString(bridgePath, "already extracted");

    BridgeExtractor.extract(tempPath, Platform.LINUX_X64, new ByteArrayInputStream(new byte[0]));

    assertThat(Files.readString(bridgePath)).isEqualTo("already extracted");
  }

  @Test
  void detects_macos_aarch64() {
    assertThat(new PlatformDetection("Mac OS X", "aarch64").getPlatform()).isEqualTo(Platform.MACOS_AARCH64);
  }

  @Test
  void detects_linux_x64() {
    assertThat(new PlatformDetection("Linux", "amd64").getPlatform()).isEqualTo(Platform.LINUX_X64);
  }

  private static ByteArrayInputStream emptyTarXz() throws IOException {
    ByteArrayOutputStream archive = new ByteArrayOutputStream();
    try (XZCompressorOutputStream xzStream = new XZCompressorOutputStream(archive);
      TarArchiveOutputStream tarStream = new TarArchiveOutputStream(xzStream)) {
      tarStream.finish();
    }
    return new ByteArrayInputStream(archive.toByteArray());
  }

  private static ByteArrayInputStream archiveWithMetadataBeforeBridge() throws IOException {
    ByteArrayOutputStream archive = new ByteArrayOutputStream();
    try (XZCompressorOutputStream xzStream = new XZCompressorOutputStream(archive);
      TarArchiveOutputStream tarStream = new TarArchiveOutputStream(xzStream)) {
      addArchiveEntry(tarStream, "._bridge", "metadata");
      addArchiveEntry(tarStream, "bridge", "bridge executable");
    }
    return new ByteArrayInputStream(archive.toByteArray());
  }

  private static void addArchiveEntry(TarArchiveOutputStream tarStream, String name, String content) throws IOException {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    TarArchiveEntry entry = new TarArchiveEntry(name);
    entry.setSize(bytes.length);
    tarStream.putArchiveEntry(entry);
    tarStream.write(bytes);
    tarStream.closeArchiveEntry();
  }
}
