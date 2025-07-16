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
package org.sonar.java.telemetry;

/**
 * Telemetry keys used by the Java analyzer.
 */
public enum TelemetryKey {
  JAVA_LANGUAGE_VERSION("java.language.version"),
  JAVA_SCANNER_APP("java.scanner_app"),
  JAVA_IS_AUTOSCAN("java.is_autoscan"),
  JAVA_ANALYSIS_MAIN_SUCCESS_SIZE_CHARS("java.analysis.main.success.size_chars"),
  JAVA_ANALYSIS_MAIN_SUCCESS_TIME_MS("java.analysis.main.success.time_ms"),
  JAVA_ANALYSIS_MAIN_SUCCESS_TYPE_ERROR_COUNT("java.analysis.main.success.type_error_count"),
  JAVA_ANALYSIS_MAIN_PARSE_ERRORS_SIZE_CHARS("java.analysis.main.parse_errors.size_chars"),
  JAVA_ANALYSIS_MAIN_PARSE_ERRORS_TIME_MS("java.analysis.main.parse_errors.time_ms"),
  JAVA_ANALYSIS_MAIN_EXCEPTIONS_SIZE_CHARS("java.analysis.main.exceptions.size_chars"),
  JAVA_ANALYSIS_MAIN_EXCEPTIONS_TIME_MS("java.analysis.main.exceptions.time_ms"),
  JAVA_ANALYSIS_TEST_SUCCESS_SIZE_CHARS("java.analysis.test.success.size_chars"),
  JAVA_ANALYSIS_TEST_SUCCESS_TIME_MS("java.analysis.test.success.time_ms"),
  JAVA_ANALYSIS_TEST_SUCCESS_TYPE_ERROR_COUNT("java.analysis.test.success.type_error_count"),
  JAVA_ANALYSIS_TEST_PARSE_ERRORS_SIZE_CHARS("java.analysis.test.parse_errors.size_chars"),
  JAVA_ANALYSIS_TEST_PARSE_ERRORS_TIME_MS("java.analysis.test.parse_errors.time_ms"),
  JAVA_ANALYSIS_TEST_EXCEPTIONS_SIZE_CHARS("java.analysis.test.exceptions.size_chars"),
  JAVA_ANALYSIS_TEST_EXCEPTIONS_TIME_MS("java.analysis.test.exceptions.time_ms"),
  JAVA_ANALYSIS_GENERATED_SUCCESS_SIZE_CHARS("java.analysis.generated.success.size_chars"),
  JAVA_ANALYSIS_GENERATED_SUCCESS_TIME_MS("java.analysis.generated.success.time_ms"),
  JAVA_ANALYSIS_GENERATED_SUCCESS_TYPE_ERROR_COUNT("java.analysis.generated.success.type_error_count"),
  JAVA_ANALYSIS_GENERATED_PARSE_ERRORS_SIZE_CHARS("java.analysis.generated.parse_errors.size_chars"),
  JAVA_ANALYSIS_GENERATED_PARSE_ERRORS_TIME_MS("java.analysis.generated.parse_errors.time_ms"),
  JAVA_ANALYSIS_GENERATED_EXCEPTIONS_SIZE_CHARS("java.analysis.generated.exceptions.size_chars"),
  JAVA_ANALYSIS_GENERATED_EXCEPTIONS_TIME_MS("java.analysis.generated.exceptions.time_ms"),
  JAVA_MODULE_COUNT("java.module_count"),

  // The last element of dependency keys should be the same as the name of its jar.
  JAVA_DEPENDENCY_LOMBOK("java.dependency.lombok"),
  JAVA_DEPENDENCY_SPRING_BOOT("java.dependency.spring-boot"),
  JAVA_DEPENDENCY_SPRING_WEB("java.dependency.spring-web");

  public interface SpeedKeys {
    TelemetryKey sizeCharsKey();
    TelemetryKey timeMsKey();
  }

  public record SizeAndTimeKeys(TelemetryKey sizeCharsKey, TelemetryKey timeMsKey) implements SpeedKeys {
  }

  public record SizeTimeAndTypeErrorKeys(TelemetryKey sizeCharsKey, TelemetryKey timeMsKey, TelemetryKey typeErrorCountKey)  implements SpeedKeys {
  }

  public record JavaAnalysisKeys(SizeTimeAndTypeErrorKeys success, SpeedKeys parseErrors, SpeedKeys exceptions) {
  }

  public static final JavaAnalysisKeys JAVA_ANALYSIS_MAIN = new JavaAnalysisKeys(
    new SizeTimeAndTypeErrorKeys(JAVA_ANALYSIS_MAIN_SUCCESS_SIZE_CHARS, JAVA_ANALYSIS_MAIN_SUCCESS_TIME_MS,
      JAVA_ANALYSIS_MAIN_SUCCESS_TYPE_ERROR_COUNT),
    new SizeAndTimeKeys(JAVA_ANALYSIS_MAIN_PARSE_ERRORS_SIZE_CHARS, JAVA_ANALYSIS_MAIN_PARSE_ERRORS_TIME_MS),
    new SizeAndTimeKeys(JAVA_ANALYSIS_MAIN_EXCEPTIONS_SIZE_CHARS, JAVA_ANALYSIS_MAIN_EXCEPTIONS_TIME_MS));

  public static final JavaAnalysisKeys JAVA_ANALYSIS_TEST = new JavaAnalysisKeys(
    new SizeTimeAndTypeErrorKeys(JAVA_ANALYSIS_TEST_SUCCESS_SIZE_CHARS, JAVA_ANALYSIS_TEST_SUCCESS_TIME_MS,
      JAVA_ANALYSIS_TEST_SUCCESS_TYPE_ERROR_COUNT),
    new SizeAndTimeKeys(JAVA_ANALYSIS_TEST_PARSE_ERRORS_SIZE_CHARS, JAVA_ANALYSIS_TEST_PARSE_ERRORS_TIME_MS),
    new SizeAndTimeKeys(JAVA_ANALYSIS_TEST_EXCEPTIONS_SIZE_CHARS, JAVA_ANALYSIS_TEST_EXCEPTIONS_TIME_MS));

  public static final JavaAnalysisKeys JAVA_ANALYSIS_GENERATED = new JavaAnalysisKeys(
    new SizeTimeAndTypeErrorKeys(JAVA_ANALYSIS_GENERATED_SUCCESS_SIZE_CHARS, JAVA_ANALYSIS_GENERATED_SUCCESS_TIME_MS,
      JAVA_ANALYSIS_GENERATED_SUCCESS_TYPE_ERROR_COUNT),
    new SizeAndTimeKeys(JAVA_ANALYSIS_GENERATED_PARSE_ERRORS_SIZE_CHARS, JAVA_ANALYSIS_GENERATED_PARSE_ERRORS_TIME_MS),
    new SizeAndTimeKeys(JAVA_ANALYSIS_GENERATED_EXCEPTIONS_SIZE_CHARS, JAVA_ANALYSIS_GENERATED_EXCEPTIONS_TIME_MS));

  private final String key;

  TelemetryKey(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }
}
