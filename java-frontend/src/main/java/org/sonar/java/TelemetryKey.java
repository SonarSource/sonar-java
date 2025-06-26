/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java;

/**
 * Telemetry keys used by the Java analyzer.
 */
public enum TelemetryKey {
  JAVA_LANGUAGE_VERSION("java.language.version"),
  JAVA_AUTOSCAN("java.autoscan"),
  JAVA_SCANNER_APP("java.scanner_app"),

  JAVA_SERVER_CACHING_ENABLED("java.server.caching.enabled"),
  JAVA_SERVER_CACHING_FILES_USED("java.server.caching.files_used"),
  JAVA_SERVER_CACHING_FILES_TOTAL("java.server.caching.files_total"),

  // The last element of dependency keys should be the same as the name of its jar.
  JAVA_DEPENDENCY_LOMBOK("java.dependency.lombok"),
  JAVA_DEPENDENCY_SPRING_BOOT("java.dependency.spring-boot");

  private final String key;

  TelemetryKey(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }
}
