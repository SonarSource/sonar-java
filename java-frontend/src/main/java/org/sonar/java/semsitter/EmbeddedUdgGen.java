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
import javax.annotation.Nullable;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

/** The {@code udg-gen} executable packaged in the plugin, extracted once per JVM to a temporary file. */
public final class EmbeddedUdgGen {

  private static Path extracted;

  private EmbeddedUdgGen() {
  }

  /** The resource name for a platform, or null when no executable is packaged for it. */
  @Nullable
  static String resourceName(@Nullable String osName, @Nullable String osArch) {
    String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
    String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
    boolean x64 = arch.equals("amd64") || arch.equals("x86_64");
    boolean arm64 = arch.equals("aarch64") || arch.equals("arm64");
    // The Linux executables are statically linked, so they also run on musl distributions.
    if (os.contains("linux") && x64) {
      return "/udg-gen/udg-gen-linux-x64.xz";
    }
    if (os.contains("linux") && arm64) {
      return "/udg-gen/udg-gen-linux-aarch64.xz";
    }
    if (os.contains("mac") && arm64) {
      return "/udg-gen/udg-gen-macos-aarch64.xz";
    }
    return null;
  }

  public static synchronized Path extract() throws IOException {
    if (extracted != null && Files.isExecutable(extracted)) {
      return extracted;
    }
    String osName = System.getProperty("os.name");
    String osArch = System.getProperty("os.arch");
    String resource = resourceName(osName, osArch);
    if (resource == null) {
      throw new IOException("no packaged udg-gen for " + osName + " " + osArch
        + ": set -Dsonar.java.semsitter.udg or SEMSITTER_UDG_GEN");
    }
    Path dir = Files.createTempDirectory("sonar-java-udg-gen");
    Path target = dir.resolve("udg-gen");
    try (InputStream packaged = EmbeddedUdgGen.class.getResourceAsStream(resource)) {
      if (packaged == null) {
        throw new IOException("packaged udg-gen " + resource + " is missing from the plugin");
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
