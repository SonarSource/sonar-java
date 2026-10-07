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
package com.sonar.it.java.suite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

record ComparisonSettings(String serverVersion, String scannerVersion, int repetitions, Map<String, String> candidateProperties) {

  private static final Set<String> PROTECTED_PROPERTIES = Set.of(
    "sonar.projectkey", "sonar.sources", "sonar.tests", "sonar.java.source", "sonar.java.jdkhome",
    "sonar.projectbasedir", "sonar.modules", "sonar.inclusions", "sonar.exclusions", "sonar.test.inclusions", "sonar.test.exclusions",
    "sonar.scanner.skipjreprovisioning",
    "sonar.java.binaries", "sonar.java.libraries", "sonar.java.test.binaries", "sonar.java.test.libraries",
    "sonar.java.skipunchanged", "sonar.internal.analysis.autoscan", "sonar.internal.analysis.autoscan.filtering",
    "sonar.java.internal.semantic.report", "sonar.verbose", "sonar.log.level", "sonar.scm.disabled", "style.color",
    "sonar.host.url", "sonar.token", "sonar.login", "sonar.password");

  ComparisonSettings {
    candidateProperties = Map.copyOf(candidateProperties);
  }

  static ComparisonSettings load() throws IOException {
    return load(System.getProperties());
  }

  static ComparisonSettings load(Properties properties) throws IOException {
    String server = pinnedVersion(properties, "sonar.runtimeVersion", "26.10.0.132816");
    String scanner = pinnedVersion(properties, "comparison.scannerVersion", "5.9.0.7291");
    String repetitionsValue = properties.getProperty("comparison.repetitions", "3");
    int repetitions;
    try {
      repetitions = Integer.parseInt(repetitionsValue);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("comparison.repetitions must be a positive integer: " + repetitionsValue, e);
    }
    if (repetitions < 1) {
      throw new IllegalArgumentException("comparison.repetitions must be a positive integer: " + repetitionsValue);
    }
    return new ComparisonSettings(server, scanner, repetitions, readCandidateProperties(properties));
  }

  private static String pinnedVersion(Properties properties, String key, String defaultValue) {
    String value = properties.getProperty(key, defaultValue);
    if (!value.matches("[0-9]+(?:[.][0-9]+)*(?:-[A-Za-z0-9]+(?:[.-][A-Za-z0-9]+)*)?")
      || value.toUpperCase(Locale.ROOT).contains("SNAPSHOT")) {
      throw new IllegalArgumentException(key + " must identify a pinned release version: " + value);
    }
    return value;
  }

  private static Map<String, String> readCandidateProperties(Properties settings) throws IOException {
    String filename = settings.getProperty("comparison.candidateProperties");
    if (filename == null) {
      return Map.of();
    }
    if (filename.isBlank()) {
      throw new IllegalArgumentException("comparison.candidateProperties must point to a properties file");
    }
    Path path = Path.of(filename).toAbsolutePath();
    Properties properties = new Properties();
    try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      properties.load(reader);
    } catch (IOException | IllegalArgumentException e) {
      throw new IOException("Failed to read candidate properties from " + path, e);
    }
    Map<String, String> candidate = new TreeMap<>();
    for (String key : properties.stringPropertyNames()) {
      validateCandidateProperty(key);
      candidate.put(key, properties.getProperty(key));
    }
    return candidate;
  }

  private static void validateCandidateProperty(String key) {
    String normalized = key.toLowerCase(Locale.ROOT);
    if (PROTECTED_PROPERTIES.contains(normalized) || normalized.startsWith("org.slf4j.")
      || normalized.matches(".*(?:password|passwd|secret|token|credential|apikey|api[._-]key).*")) {
      throw new IllegalArgumentException("Candidate property overrides shared configuration or contains credentials: " + key);
    }
  }
}
