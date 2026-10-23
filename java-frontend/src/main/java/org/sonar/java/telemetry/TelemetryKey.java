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
  JAVA_ANALYSIS_MAIN_SUCCESS_FILE_COUNT("java.analysis.main.success.file_count"),
  JAVA_ANALYSIS_MAIN_PARSE_ERRORS_FILE_COUNT("java.analysis.main.parse_errors.file_count"),
  JAVA_ANALYSIS_MAIN_EXCEPTIONS_FILE_COUNT("java.analysis.main.exceptions.file_count"),
  JAVA_ANALYSIS_MAIN_CACHED_FILE_COUNT("java.analysis.main.cached_file_count"),
  JAVA_ANALYSIS_MAIN_SEMANTIC_FILES_EVALUATED("java.analysis.main.semantic.files_evaluated"),
  JAVA_ANALYSIS_MAIN_SEMANTIC_FILES_WITH_RESOLUTION_ERRORS("java.analysis.main.semantic.files_with_resolution_errors"),
  JAVA_ANALYSIS_MAIN_SEMANTIC_RESOLUTION_ERROR_COUNT("java.analysis.main.semantic.resolution_error_count"),
  JAVA_ANALYSIS_MAIN_SEMANTIC_MODULES_EVALUATED("java.analysis.main.semantic.modules_evaluated"),
  JAVA_ANALYSIS_MAIN_SEMANTIC_MODULES_WITH_RESOLUTION_ERRORS("java.analysis.main.semantic.modules_with_resolution_errors"),
  JAVA_ANALYSIS_TEST_SUCCESS_FILE_COUNT("java.analysis.test.success.file_count"),
  JAVA_ANALYSIS_TEST_PARSE_ERRORS_FILE_COUNT("java.analysis.test.parse_errors.file_count"),
  JAVA_ANALYSIS_TEST_EXCEPTIONS_FILE_COUNT("java.analysis.test.exceptions.file_count"),
  JAVA_ANALYSIS_TEST_CACHED_FILE_COUNT("java.analysis.test.cached_file_count"),
  JAVA_ANALYSIS_TEST_SEMANTIC_FILES_EVALUATED("java.analysis.test.semantic.files_evaluated"),
  JAVA_ANALYSIS_TEST_SEMANTIC_FILES_WITH_RESOLUTION_ERRORS("java.analysis.test.semantic.files_with_resolution_errors"),
  JAVA_ANALYSIS_TEST_SEMANTIC_RESOLUTION_ERROR_COUNT("java.analysis.test.semantic.resolution_error_count"),
  JAVA_ANALYSIS_TEST_SEMANTIC_MODULES_EVALUATED("java.analysis.test.semantic.modules_evaluated"),
  JAVA_ANALYSIS_TEST_SEMANTIC_MODULES_WITH_RESOLUTION_ERRORS("java.analysis.test.semantic.modules_with_resolution_errors"),
  JAVA_ANALYSIS_GENERATED_SUCCESS_FILE_COUNT("java.analysis.generated.success.file_count"),
  JAVA_ANALYSIS_GENERATED_PARSE_ERRORS_FILE_COUNT("java.analysis.generated.parse_errors.file_count"),
  JAVA_ANALYSIS_GENERATED_EXCEPTIONS_FILE_COUNT("java.analysis.generated.exceptions.file_count"),
  JAVA_ANALYSIS_GENERATED_CACHED_FILE_COUNT("java.analysis.generated.cached_file_count"),
  JAVA_ANALYSIS_GENERATED_SEMANTIC_FILES_EVALUATED("java.analysis.generated.semantic.files_evaluated"),
  JAVA_ANALYSIS_GENERATED_SEMANTIC_FILES_WITH_RESOLUTION_ERRORS("java.analysis.generated.semantic.files_with_resolution_errors"),
  JAVA_ANALYSIS_GENERATED_SEMANTIC_RESOLUTION_ERROR_COUNT("java.analysis.generated.semantic.resolution_error_count"),
  JAVA_ANALYSIS_GENERATED_SEMANTIC_MODULES_EVALUATED("java.analysis.generated.semantic.modules_evaluated"),
  JAVA_ANALYSIS_GENERATED_SEMANTIC_MODULES_WITH_RESOLUTION_ERRORS("java.analysis.generated.semantic.modules_with_resolution_errors"),
  JAVA_CLASSPATH_MAIN_MODULE_COUNT("java.classpath.main.module_count"),
  JAVA_CLASSPATH_MAIN_MODULES_MISSING_BINARIES("java.classpath.main.modules_missing_binaries"),
  JAVA_CLASSPATH_MAIN_MODULES_WITHOUT_BINARY_ENTRIES("java.classpath.main.modules_without_binary_entries"),
  JAVA_CLASSPATH_MAIN_MODULES_MISSING_LIBRARIES("java.classpath.main.modules_missing_libraries"),
  JAVA_CLASSPATH_MAIN_MODULES_WITHOUT_LIBRARY_ENTRIES("java.classpath.main.modules_without_library_entries"),
  JAVA_CLASSPATH_MAIN_MODULES_WITH_INVALID_JDK("java.classpath.main.modules_with_invalid_jdk"),
  JAVA_CLASSPATH_MAIN_MODULES_WITH_UNMATCHED_ENTRIES("java.classpath.main.modules_with_unmatched_entries"),
  JAVA_CLASSPATH_TEST_MODULE_COUNT("java.classpath.test.module_count"),
  JAVA_CLASSPATH_TEST_MODULES_MISSING_BINARIES("java.classpath.test.modules_missing_binaries"),
  JAVA_CLASSPATH_TEST_MODULES_WITHOUT_BINARY_ENTRIES("java.classpath.test.modules_without_binary_entries"),
  JAVA_CLASSPATH_TEST_MODULES_MISSING_LIBRARIES("java.classpath.test.modules_missing_libraries"),
  JAVA_CLASSPATH_TEST_MODULES_WITHOUT_LIBRARY_ENTRIES("java.classpath.test.modules_without_library_entries"),
  JAVA_CLASSPATH_TEST_MODULES_WITH_INVALID_JDK("java.classpath.test.modules_with_invalid_jdk"),
  JAVA_CLASSPATH_TEST_MODULES_WITH_UNMATCHED_ENTRIES("java.classpath.test.modules_with_unmatched_entries"),

  JAVA_MODULE_COUNT("java.module_count"),

  // The last element of dependency keys should be the same as the name of its jar.
  JAVA_DEPENDENCY_LOMBOK("java.dependency.lombok"),
  JAVA_DEPENDENCY_SPRING_BOOT("java.dependency.spring-boot"),
  JAVA_DEPENDENCY_SPRING_WEB("java.dependency.spring-web"),

  JAVA_FEATURE_MODULE_IMPORT("java.feature.module_import"),
  JAVA_FEATURE_COMPACT_SOURCE_FILES("java.feature.compact_source_files"),
  JAVA_FEATURE_FLEXIBLE_CONSTRUCTOR_BODY("java.feature.flexible_constructor_body"),

  JAVA_SPRING_BEAN_COUNT("java.spring.bean_count"),
  JAVA_SPRING_BEAN_NAME_COUNT("java.spring.bean_name_count"),
  JAVA_SPRING_INJECTION_POINT_COUNT("java.spring.injection_point_count"),
  JAVA_SPRING_COMPONENT_SCAN_PACKAGE_COUNT("java.spring.component_scan_package_count"),
  JAVA_SPRING_CONTEXT_MODEL_GATHERING_TIME_MS("java.spring.context_model_gathering_time_ms"),
  JAVA_SPRING_CONTEXT_CHECKS_TIME_MS("java.spring.context_checks_time_ms"),
  JAVA_SPRING_CONTEXT_MODEL_SIZE_BYTES("java.spring.context_model_size_bytes");

  public interface SpeedKeys {
    TelemetryKey sizeCharsKey();

    TelemetryKey timeMsKey();

    TelemetryKey fileCountKey();
  }

  public record SizeAndTimeKeys(TelemetryKey sizeCharsKey, TelemetryKey timeMsKey, TelemetryKey fileCountKey) implements SpeedKeys {
  }

  public record SizeTimeAndTypeErrorKeys(TelemetryKey sizeCharsKey, TelemetryKey timeMsKey, TelemetryKey typeErrorCountKey, TelemetryKey fileCountKey) implements SpeedKeys {
  }

  public record JavaAnalysisKeys(SizeTimeAndTypeErrorKeys success, SpeedKeys parseErrors, SpeedKeys exceptions,
    TelemetryKey cachedFileCountKey, SemanticKeys semantic) {
  }

  public record SemanticKeys(TelemetryKey filesEvaluated, TelemetryKey filesWithResolutionErrors, TelemetryKey resolutionErrorCount,
    TelemetryKey modulesEvaluated, TelemetryKey modulesWithResolutionErrors) {
  }

  public record ClasspathKeys(TelemetryKey moduleCount, TelemetryKey modulesMissingBinaries, TelemetryKey modulesWithoutBinaryEntries,
    TelemetryKey modulesMissingLibraries, TelemetryKey modulesWithoutLibraryEntries, TelemetryKey modulesWithInvalidJdk, TelemetryKey modulesWithUnmatchedEntries) {
  }

  public static final JavaAnalysisKeys JAVA_ANALYSIS_MAIN = new JavaAnalysisKeys(
    new SizeTimeAndTypeErrorKeys(JAVA_ANALYSIS_MAIN_SUCCESS_SIZE_CHARS, JAVA_ANALYSIS_MAIN_SUCCESS_TIME_MS,
      JAVA_ANALYSIS_MAIN_SUCCESS_TYPE_ERROR_COUNT, JAVA_ANALYSIS_MAIN_SUCCESS_FILE_COUNT),
    new SizeAndTimeKeys(JAVA_ANALYSIS_MAIN_PARSE_ERRORS_SIZE_CHARS, JAVA_ANALYSIS_MAIN_PARSE_ERRORS_TIME_MS,
      JAVA_ANALYSIS_MAIN_PARSE_ERRORS_FILE_COUNT),
    new SizeAndTimeKeys(JAVA_ANALYSIS_MAIN_EXCEPTIONS_SIZE_CHARS, JAVA_ANALYSIS_MAIN_EXCEPTIONS_TIME_MS,
      JAVA_ANALYSIS_MAIN_EXCEPTIONS_FILE_COUNT),
    JAVA_ANALYSIS_MAIN_CACHED_FILE_COUNT,
    new SemanticKeys(JAVA_ANALYSIS_MAIN_SEMANTIC_FILES_EVALUATED, JAVA_ANALYSIS_MAIN_SEMANTIC_FILES_WITH_RESOLUTION_ERRORS,
      JAVA_ANALYSIS_MAIN_SEMANTIC_RESOLUTION_ERROR_COUNT, JAVA_ANALYSIS_MAIN_SEMANTIC_MODULES_EVALUATED,
      JAVA_ANALYSIS_MAIN_SEMANTIC_MODULES_WITH_RESOLUTION_ERRORS));

  public static final JavaAnalysisKeys JAVA_ANALYSIS_TEST = new JavaAnalysisKeys(
    new SizeTimeAndTypeErrorKeys(JAVA_ANALYSIS_TEST_SUCCESS_SIZE_CHARS, JAVA_ANALYSIS_TEST_SUCCESS_TIME_MS,
      JAVA_ANALYSIS_TEST_SUCCESS_TYPE_ERROR_COUNT, JAVA_ANALYSIS_TEST_SUCCESS_FILE_COUNT),
    new SizeAndTimeKeys(JAVA_ANALYSIS_TEST_PARSE_ERRORS_SIZE_CHARS, JAVA_ANALYSIS_TEST_PARSE_ERRORS_TIME_MS,
      JAVA_ANALYSIS_TEST_PARSE_ERRORS_FILE_COUNT),
    new SizeAndTimeKeys(JAVA_ANALYSIS_TEST_EXCEPTIONS_SIZE_CHARS, JAVA_ANALYSIS_TEST_EXCEPTIONS_TIME_MS,
      JAVA_ANALYSIS_TEST_EXCEPTIONS_FILE_COUNT),
    JAVA_ANALYSIS_TEST_CACHED_FILE_COUNT,
    new SemanticKeys(JAVA_ANALYSIS_TEST_SEMANTIC_FILES_EVALUATED, JAVA_ANALYSIS_TEST_SEMANTIC_FILES_WITH_RESOLUTION_ERRORS,
      JAVA_ANALYSIS_TEST_SEMANTIC_RESOLUTION_ERROR_COUNT, JAVA_ANALYSIS_TEST_SEMANTIC_MODULES_EVALUATED,
      JAVA_ANALYSIS_TEST_SEMANTIC_MODULES_WITH_RESOLUTION_ERRORS));

  public static final JavaAnalysisKeys JAVA_ANALYSIS_GENERATED = new JavaAnalysisKeys(
    new SizeTimeAndTypeErrorKeys(JAVA_ANALYSIS_GENERATED_SUCCESS_SIZE_CHARS, JAVA_ANALYSIS_GENERATED_SUCCESS_TIME_MS,
      JAVA_ANALYSIS_GENERATED_SUCCESS_TYPE_ERROR_COUNT, JAVA_ANALYSIS_GENERATED_SUCCESS_FILE_COUNT),
    new SizeAndTimeKeys(JAVA_ANALYSIS_GENERATED_PARSE_ERRORS_SIZE_CHARS, JAVA_ANALYSIS_GENERATED_PARSE_ERRORS_TIME_MS,
      JAVA_ANALYSIS_GENERATED_PARSE_ERRORS_FILE_COUNT),
    new SizeAndTimeKeys(JAVA_ANALYSIS_GENERATED_EXCEPTIONS_SIZE_CHARS, JAVA_ANALYSIS_GENERATED_EXCEPTIONS_TIME_MS,
      JAVA_ANALYSIS_GENERATED_EXCEPTIONS_FILE_COUNT),
    JAVA_ANALYSIS_GENERATED_CACHED_FILE_COUNT,
    new SemanticKeys(JAVA_ANALYSIS_GENERATED_SEMANTIC_FILES_EVALUATED, JAVA_ANALYSIS_GENERATED_SEMANTIC_FILES_WITH_RESOLUTION_ERRORS,
      JAVA_ANALYSIS_GENERATED_SEMANTIC_RESOLUTION_ERROR_COUNT, JAVA_ANALYSIS_GENERATED_SEMANTIC_MODULES_EVALUATED,
      JAVA_ANALYSIS_GENERATED_SEMANTIC_MODULES_WITH_RESOLUTION_ERRORS));

  public static final ClasspathKeys JAVA_CLASSPATH_MAIN = new ClasspathKeys(
    JAVA_CLASSPATH_MAIN_MODULE_COUNT, JAVA_CLASSPATH_MAIN_MODULES_MISSING_BINARIES,
    JAVA_CLASSPATH_MAIN_MODULES_WITHOUT_BINARY_ENTRIES, JAVA_CLASSPATH_MAIN_MODULES_MISSING_LIBRARIES,
    JAVA_CLASSPATH_MAIN_MODULES_WITHOUT_LIBRARY_ENTRIES, JAVA_CLASSPATH_MAIN_MODULES_WITH_INVALID_JDK,
    JAVA_CLASSPATH_MAIN_MODULES_WITH_UNMATCHED_ENTRIES);

  public static final ClasspathKeys JAVA_CLASSPATH_TEST = new ClasspathKeys(
    JAVA_CLASSPATH_TEST_MODULE_COUNT, JAVA_CLASSPATH_TEST_MODULES_MISSING_BINARIES,
    JAVA_CLASSPATH_TEST_MODULES_WITHOUT_BINARY_ENTRIES, JAVA_CLASSPATH_TEST_MODULES_MISSING_LIBRARIES,
    JAVA_CLASSPATH_TEST_MODULES_WITHOUT_LIBRARY_ENTRIES, JAVA_CLASSPATH_TEST_MODULES_WITH_INVALID_JDK,
    JAVA_CLASSPATH_TEST_MODULES_WITH_UNMATCHED_ENTRIES);

  private final String key;

  TelemetryKey(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }
}
