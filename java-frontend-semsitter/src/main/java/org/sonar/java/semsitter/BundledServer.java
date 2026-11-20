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
import java.util.Locale;
import java.util.Optional;

/**
 * The SemSitter server shipped inside this plugin for the performance-validation runners, which are Linux x64 and have
 * no server installed. It is used only while the performance-validation benchmark is running. Every other analysis
 * keeps the default frontend unless an executable is configured.
 */
final class BundledServer {

  static final String RESOURCE = "udg-java-server-x86_64-unknown-linux-musl";
  static final String PERFORMANCE_VALIDATION_CONTEXT = "sonar.java.context";

  private BundledServer() {
  }

  static Optional<Path> extract() {
    if (!performanceValidation() || !linuxAmd64()) {
      return Optional.empty();
    }
    try (InputStream in = BundledServer.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        return Optional.empty();
      }
      Path destination = Path.of(System.getProperty("java.io.tmpdir"), RESOURCE);
      Path partial = Path.of(destination.toString() + ".part");
      Files.copy(in, partial, StandardCopyOption.REPLACE_EXISTING);
      Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      if (!destination.toFile().setExecutable(true)) {
        return Optional.empty();
      }
      return Optional.of(destination);
    } catch (IOException failure) {
      return Optional.empty();
    }
  }

  private static boolean performanceValidation() {
    return PERFORMANCE_VALIDATION_CONTEXT.equals(System.getProperty("sensor.context.key"));
  }

  private static boolean linuxAmd64() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
    return os.contains("linux") && ("amd64".equals(arch) || "x86_64".equals(arch));
  }
}
