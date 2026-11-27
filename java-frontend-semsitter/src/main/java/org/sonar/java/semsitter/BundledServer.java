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
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * The SemSitter server shipped inside this plugin for the performance-validation runners, which are Linux x64 and have
 * no server installed. It is used only while the performance-validation benchmark is running. Every other analysis
 * keeps the default frontend unless an executable is configured.
 */
final class BundledServer {

  static final String RESOURCE = "udg-java-server-x86_64-unknown-linux-musl";
  static final String PERFORMANCE_VALIDATION_CONTEXT = "sonar.java.context";
  private static final AtomicReference<Path> COPY = new AtomicReference<>();

  private BundledServer() {
  }

  static Optional<Path> extract() {
    if (!performanceValidation() || !linuxAmd64()) {
      return Optional.empty();
    }
    return Optional.of(copyOnce());
  }

  /**
   * Copies the server once per JVM, to a file of the user that no concurrent analysis on the same host writes to. A
   * server that cannot be extracted fails the analysis, so that a performance validation never silently runs ECJ.
   */
  private static synchronized Path copyOnce() {
    Path copy = COPY.get();
    if (copy != null) {
      return copy;
    }
    try (InputStream in = BundledServer.class.getResourceAsStream(RESOURCE)) {
      copy = install(in, Files.createDirectories(Path.of(System.getProperty("user.home"), ".sonar", "semsitter")));
      COPY.set(copy);
      return copy;
    } catch (IOException failure) {
      throw new IllegalStateException("The bundled SemSitter server could not be extracted: " + failure.getMessage(), failure);
    }
  }

  /** Writes the server to a new executable file of {@code directory}, deleted when the JVM exits or right away if the copy fails. */
  static Path install(@Nullable InputStream in, Path directory) throws IOException {
    if (in == null) {
      throw new IllegalStateException("The SemSitter server is not bundled in this plugin.");
    }
    Path destination = Files.createTempFile(directory, RESOURCE, "");
    if (!copyTo(in, destination)) {
      delete(destination);
      throw new IllegalStateException("The bundled SemSitter server could not be copied to " + destination);
    }
    Runtime.getRuntime().addShutdownHook(new Thread(() -> delete(destination)));
    return destination;
  }

  private static boolean copyTo(InputStream in, Path destination) {
    try {
      Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
      return destination.toFile().setExecutable(true);
    } catch (IOException failure) {
      return false;
    }
  }

  private static boolean delete(Path path) {
    try {
      return Files.deleteIfExists(path);
    } catch (IOException failure) {
      return false;
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
