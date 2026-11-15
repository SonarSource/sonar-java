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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

/** The SemSitter analyzer executable packaged in the plugin, extracted once per JVM to a temporary file. */
public final class EmbeddedAnalyzer {

  private static final AtomicInteger FAILED_EXTRACTIONS = new AtomicInteger();

  private static Path extracted;

  private EmbeddedAnalyzer() {
  }

  /** The resource name for a platform, or null when no executable is packaged for it. */
  @Nullable
  static String resourceName(@Nullable String osName, @Nullable String osArch) {
    String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
    String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
    boolean x64 = arch.equals("amd64") || arch.equals("x86_64");
    boolean arm64 = arch.equals("aarch64") || arch.equals("arm64");
    String platform = null;
    if (os.contains("linux")) {
      // Statically linked, so they also run on musl distributions.
      platform = x64 ? "linux-x64" : arm64 ? "linux-aarch64" : null;
    } else if (os.contains("mac")) {
      platform = x64 ? "macos-x64" : arm64 ? "macos-aarch64" : null;
    } else if (os.contains("win") && x64) {
      platform = "windows-x64";
    }
    return platform == null ? null : "/semsitter/analyzer-" + platform + ".xz";
  }

  /** How many times no packaged analyzer could be provided; tests use it to tell a missing analyzer from a real failure. */
  public static int failedExtractions() {
    return FAILED_EXTRACTIONS.get();
  }

  public static Path extract() throws IOException {
    try {
      return extractOnce();
    } catch (IOException unavailable) {
      FAILED_EXTRACTIONS.incrementAndGet();
      throw unavailable;
    }
  }

  private static synchronized Path extractOnce() throws IOException {
    if (extracted != null && Files.isExecutable(extracted)) {
      return extracted;
    }
    String osName = System.getProperty("os.name");
    String osArch = System.getProperty("os.arch");
    String resource = resourceName(osName, osArch);
    if (resource == null) {
      throw new IOException("no packaged analyzer for " + osName + " " + osArch
        + ": set -Dsonar.java.semsitter.executable or SEMSITTER_ANALYZER");
    }
    Path dir = Files.createTempDirectory("sonar-java-semsitter");
    Path target = dir.resolve(resource.contains("windows") ? "semsitter-analyzer.exe" : "semsitter-analyzer");
    try (InputStream packaged = EmbeddedAnalyzer.class.getResourceAsStream(resource)) {
      if (packaged == null) {
        throw new IOException("packaged analyzer " + resource + " is missing from the plugin");
      }
      try (InputStream xz = new XZCompressorInputStream(packaged)) {
        Files.copy(xz, target);
      }
    }
    if (!target.toFile().setExecutable(true, true)) {
      throw new IOException("cannot make " + target + " executable");
    }
    dir.toFile().deleteOnExit();
    target.toFile().deleteOnExit();
    extracted = target;
    return target;
  }
}
