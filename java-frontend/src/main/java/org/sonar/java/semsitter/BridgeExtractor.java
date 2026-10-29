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
      String archiveName = archiveName(platform);
      String resourcePath = "/bridge/" + archiveName;
      InputStream resource = BridgeExtractor.class.getResourceAsStream(resourcePath);
      if (resource == null) {
        Path archive = findPrebuilt(archiveName);
        if (archive == null) {
          throw new IllegalStateException("Bridge resource not found");
        }
        resource = Files.newInputStream(archive);
      }
      try (InputStream stream = resource) {
        extractArchive(targetDir, bridgePath, stream, resourcePath, bridgeName);
      }
    }
    return bridgePath;
  }

  private static Path findPrebuilt(String archiveName) {
    Path found = walk(Path.of("").toAbsolutePath(), archiveName);
    if (found != null) {
      return found;
    }
    Path origin = codeOrigin();
    return origin == null ? null : walk(origin, archiveName);
  }

  private static Path codeOrigin() {
    try {
      var source = BridgeExtractor.class.getProtectionDomain().getCodeSource();
      if (source == null || source.getLocation() == null) {
        return null;
      }
      return Path.of(source.getLocation().toURI());
    } catch (Exception failure) {
      return null;
    }
  }

  private static Path walk(Path dir, String archiveName) {
    for (int i = 0; i < 8 && dir != null; i++) {
      Path archive = dir.resolve("semsitter-bridge").resolve("prebuilt").resolve(archiveName);
      if (Files.isRegularFile(archive)) {
        return archive;
      }
      dir = dir.getParent();
    }
    return null;
  }

  private static String archiveName(Platform platform) {
    String suffix = switch (platform) {
      case ALPINE_AARCH64 -> Platform.LINUX_AARCH64.toString();
      case ALPINE_X64 -> Platform.LINUX_X64.toString();
      default -> platform.toString();
    };
    return "bridge-" + suffix + ".xz";
  }

  private static String bridgeName(Platform platform) {
    return platform == Platform.WINDOWS_X64 ? "bridge.exe" : "bridge";
  }

  private static void extractArchive(Path target, Path bridgePath, InputStream archive, String resourcePath, String bridgeName) throws IOException {
    Path stagingPath = target.resolve(bridgeName + ".ext");
    try {
      try (var xzStream = new XZCompressorInputStream(archive);
        var tarStream = new TarArchiveInputStream(xzStream)) {
        TarArchiveEntry entry = tarStream.getNextEntry();
        while (entry != null && (!bridgeName.equals(entry.getName()) || entry.isDirectory())) {
          entry = tarStream.getNextEntry();
        }
        if (entry == null) {
          throw new IllegalStateException("Bridge binary not found in archive: " + resourcePath);
        }
        Files.createDirectories(target);
        Files.copy(tarStream, stagingPath, StandardCopyOption.REPLACE_EXISTING);
      }
      Files.move(stagingPath, bridgePath, StandardCopyOption.ATOMIC_MOVE);
      PosixFileAttributeView view = Files.getFileAttributeView(bridgePath, PosixFileAttributeView.class);
      if (view != null) {
        view.setPermissions(Set.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE));
      }
    } catch (Exception failure) {
      Files.deleteIfExists(stagingPath);
      if (failure instanceof IOException ioException) {
        throw ioException;
      }
      if (failure instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException(failure);
    }
  }
}
