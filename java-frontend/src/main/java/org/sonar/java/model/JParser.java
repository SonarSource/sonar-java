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
package org.sonar.java.model;

import com.google.gson.JsonObject;
import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.java.semsitter.EmbeddedUdgGen;
import org.sonar.java.semsitter.SemsitterBridge;

public final class JParser {

  private JParser() {
  }

  /**
   * Parses and attributes one compilation unit on its own, against the running JDK only. Batch analysis goes
   * through {@link JParserConfig}, which scans every file of the project before translating.
   */
  public static JavaTree.CompilationUnitTreeImpl parse(String version, String unitName, String source) {
    return parse(version, unitName, source, List.of());
  }

  /** {@link #parse(String, String, String)} with a classpath, as ECJ attributed single sources. */
  public static JavaTree.CompilationUnitTreeImpl parse(String version, String unitName, String source, List<File> classpath) {
    Objects.requireNonNull(version);
    Objects.requireNonNull(unitName);
    Objects.requireNonNull(source);
    Path directory = null;
    try {
      SemsitterBridge bridge = unitBridge();
      directory = privateTempDirectory();
      Path file = directory.resolve(Path.of(unitName.endsWith(".java") ? unitName : (unitName + ".java")).getFileName().toString());
      Files.writeString(file, source, StandardCharsets.UTF_8);
      JsonObject document;
      synchronized (bridge) {
        bridge.analyze(directory, List.of(file), JParserConfig.bridgeClasspath(classpath));
        document = bridge.cst(file.toString());
      }
      JSema sema = new JSema();
      sema.ingestDocument(document);
      // Types load lazily while rules run, so the bridge outlives this call.
      sema.loadTypes(names -> {
        try {
          return bridge.types(names);
        } catch (IOException failure) {
          throw new UncheckedIOException(failure);
        }
      });
      return SemsitterTranslator.translate(sema, document);
    } catch (RecognitionException syntaxError) {
      throw syntaxError;
    } catch (Exception failure) {
      discardUnitBridge();
      throw new RecognitionException(1, "SemSitter: unable to parse " + unitName, failure);
    } finally {
      deleteQuietly(directory);
    }
  }

  @Nullable
  private static SemsitterBridge unitBridge;

  /** One bridge per JVM for single-source parses, stopped at shutdown. */
  private static synchronized SemsitterBridge unitBridge() throws IOException {
    if (unitBridge == null) {
      SemsitterBridge started = SemsitterBridge.start(bridgePath());
      Runtime.getRuntime().addShutdownHook(new Thread(started::close));
      unitBridge = started;
    }
    return unitBridge;
  }

  private static synchronized void discardUnitBridge() {
    if (unitBridge != null) {
      unitBridge.close();
      unitBridge = null;
    }
  }

  private static void deleteQuietly(@Nullable Path directory) {
    if (directory == null) {
      return;
    }
    try (Stream<Path> paths = Files.walk(directory)) {
      paths.sorted(Comparator.reverseOrder()).forEach(JParser::deleteIfPresent);
    } catch (IOException | UncheckedIOException ignored) {
      // A leftover temporary file does not affect the analysis.
    }
  }

  private static void deleteIfPresent(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }

  /** A temporary directory only the current user can read, where the file system supports it. */
  private static Path privateTempDirectory() throws IOException {
    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      return Files.createTempDirectory("sonar-java-unit", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
    }
    return Files.createTempDirectory("sonar-java-unit");
  }

  /** The {@code udg-gen} executable: {@code -Dsonar.java.semsitter.udg}, else {@code SEMSITTER_UDG_GEN}, else the packaged one. */
  static Path bridgePath() {
    String configured = System.getProperty("sonar.java.semsitter.udg");
    if (configured == null || configured.isBlank()) {
      configured = System.getenv("SEMSITTER_UDG_GEN");
    }
    if (configured != null && !configured.isBlank()) {
      return Path.of(configured);
    }
    try {
      return EmbeddedUdgGen.extract();
    } catch (IOException failure) {
      throw new RecognitionException(1, "SemSitter server is unavailable: " + failure.getMessage(), failure);
    }
  }

}
