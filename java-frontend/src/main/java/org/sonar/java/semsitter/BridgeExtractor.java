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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Set;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.sonar.java.semsitter.PlatformDetection.Platform;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

public final class BridgeExtractor {

  private BridgeExtractor() {
  }

  public static Path extract(Path targetDir, Platform platform) throws IOException {
    if (platform == Platform.UNSUPPORTED) {
      throw new IllegalStateException("Cannot extract bridge for unsupported platform");
    }
    String bridgeName = bridgeName(platform);
    Path bridgePath = targetDir.resolve(bridgeName);
    if (!Files.exists(bridgePath)) {
      String archiveName = getArchiveNameForPlatform(platform);
      String resourcePath = "/bridge/" + archiveName;
      try (InputStream stream = BridgeExtractor.class.getResourceAsStream(resourcePath)) {
        if (stream == null) {
          throw new IllegalStateException("Bridge resource not found");
        }
        extractArchive(targetDir, bridgePath, stream, resourcePath, bridgeName);
      }
    }
    return bridgePath;
  }

  static Path extract(Path targetDir, Platform platform, InputStream archive) throws IOException {
    if (platform == Platform.UNSUPPORTED) {
      throw new IllegalStateException("Cannot extract bridge for unsupported platform");
    }
    String bridgeName = bridgeName(platform);
    Path bridgePath = targetDir.resolve(bridgeName);
    if (!Files.exists(bridgePath)) {
      String resourcePath = "/bridge/" + getArchiveNameForPlatform(platform);
      if (archive == null) {
        throw new IllegalStateException("Bridge resource not found");
      }
      extractArchive(targetDir, bridgePath, archive, resourcePath, bridgeName);
    }
    return bridgePath;
  }

  public static String getArchiveNameForPlatform(Platform platform) {
    if (platform == Platform.UNSUPPORTED) {
      throw new IllegalStateException("Cannot extract bridge for unsupported platform");
    }
    String platformSuffix = switch (platform) {
      case ALPINE_AARCH64 -> Platform.LINUX_AARCH64.toString();
      case ALPINE_X64 -> Platform.LINUX_X64.toString();
      default -> platform.toString();
    };
    return "bridge-" + platformSuffix + ".xz";
  }

  private static String bridgeName(Platform platform) {
    return platform == Platform.WINDOWS_X64 ? "bridge.exe" : "bridge";
  }

  private static void extractArchive(Path target, Path bridgePath, InputStream archive, String resourcePath, String bridgeName) throws IOException {
    Path stagingPath = target.resolve(bridgeName + ".ext");
    try {
      extractArchiveToStaging(target, stagingPath, archive, resourcePath, bridgeName);
      Files.move(stagingPath, bridgePath, StandardCopyOption.ATOMIC_MOVE);
      if (!Files.exists(bridgePath)) {
        throw new IllegalStateException("Bridge binary not found at " + bridgePath);
      }
    } catch (Exception failure) {
      try {
        Files.deleteIfExists(stagingPath);
      } catch (IOException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      if (failure instanceof IOException ioException) {
        throw ioException;
      }
      if (failure instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException(failure);
    }
  }

  private static void extractArchiveToStaging(Path target, Path stagingPath, InputStream archive, String resourcePath, String bridgeName) throws IOException {
    try (var xzStream = new XZCompressorInputStream(archive);
      var tarStream = new TarArchiveInputStream(xzStream)) {
      TarArchiveEntry entry = tarStream.getNextEntry();
      if (entry == null) {
        throw new IllegalStateException("Bridge archive is empty: " + resourcePath);
      }
      while (!bridgeName.equals(entry.getName()) || entry.isDirectory()) {
        entry = tarStream.getNextEntry();
        if (entry == null) {
          throw new IllegalStateException("Bridge binary not found in archive: " + resourcePath);
        }
      }
      Files.createDirectories(target);
      Files.copy(tarStream, stagingPath, StandardCopyOption.REPLACE_EXISTING);
      PosixFileAttributeView view = Files.getFileAttributeView(stagingPath, PosixFileAttributeView.class);
      if (view != null) {
        view.setPermissions(Set.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE));
      }
    }
  }
}
