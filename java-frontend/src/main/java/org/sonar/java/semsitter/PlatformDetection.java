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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public class PlatformDetection {

  private final String rawOsName;
  private final String rawOsArch;

  public enum Platform {
    LINUX_X64("linux-x64"),
    LINUX_AARCH64("linux-aarch64"),
    ALPINE_X64("alpine-x64"),
    ALPINE_AARCH64("alpine-aarch64"),
    MACOS_X64("macos-x64"),
    MACOS_AARCH64("macos-aarch64"),
    WINDOWS_X64("windows-x64"),
    UNSUPPORTED("unsupported");

    private final String name;

    Platform(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public PlatformDetection() {
    this(System.getProperty("os.name"), System.getProperty("os.arch"));
  }

  PlatformDetection(String rawOsName, String rawOsArch) {
    this.rawOsName = rawOsName;
    this.rawOsArch = rawOsArch;
  }

  public Platform getPlatform() {
    String platformString = osName() + "-" + osArch();
    for (Platform platform : Platform.values()) {
      if (platform.toString().equalsIgnoreCase(platformString)) {
        return platform;
      }
    }
    return Platform.UNSUPPORTED;
  }

  private String osName() {
    String osName = Optional.ofNullable(rawOsName).orElse("generic").toLowerCase(Locale.ROOT);
    if (osName.contains("win")) {
      return "windows";
    } else if (osName.contains("mac")) {
      return "macos";
    } else if (osName.contains("nux")) {
      if (isAlpineLinux()) {
        return "alpine";
      }
      return "Linux";
    }
    return osName.replaceAll("\\s+", "");
  }

  private String osArch() {
    String osArch = Optional.ofNullable(rawOsArch).orElse("unknown").toLowerCase(Locale.ROOT);
    return switch (osArch) {
      case "amd64", "x86_64" -> "x64";
      case "aarch64", "arm64" -> "aarch64";
      case "x86", "i386", "i686" -> "x86";
      default -> osArch;
    };
  }

  private static boolean isAlpineLinux() {
    return Files.exists(Path.of("/etc/alpine-release"));
  }
}
