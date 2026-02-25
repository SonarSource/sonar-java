/*
 * Copyright (C) 2021-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validates that generated license files match committed license files.
 * Used during Maven verify phase to ensure license files are up-to-date.
 */
public final class LicenseValidator {

  private LicenseValidator() {
    // Utility class
  }

  public static void main(String[] args) {
    try {
      var arguments = parseArguments(args);
      var tempLicensesPath = Path.of(arguments.get("temp_licenses"));
      var committedLicensesPath = Path.of(arguments.get("committed_licenses"));

      validateDirectoriesExist(tempLicensesPath, committedLicensesPath);

      var result = compareDirectories(tempLicensesPath, committedLicensesPath);

      if (result.hasErrors()) {
        printFailureMessage(result);
        System.exit(1);
      } else {
        System.out.println("[SUCCESS] License validation passed - generated files match committed files");
        System.exit(0);
      }
    } catch (IllegalArgumentException e) {
      System.err.println("Error: " + e.getMessage());
      printUsage();
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  static Map<String, String> parseArguments(String[] args) {
    var arguments = new java.util.HashMap<String, String>();
    for (var arg : args) {
      if (arg.startsWith("--")) {
        var parts = arg.substring(2).split("=", 2);
        if (parts.length == 2) {
          arguments.put(parts[0], parts[1]);
        }
      }
    }

    if (!arguments.containsKey("temp_licenses") || !arguments.containsKey("committed_licenses")) {
      throw new IllegalArgumentException("Missing required arguments");
    }

    return arguments;
  }

  static void validateDirectoriesExist(Path tempLicenses, Path committedLicenses) throws IOException {
    if (!Files.exists(tempLicenses)) {
      throw new IOException(
        "Temporary licenses directory not found: " + tempLicenses + "\n" +
        "Please run: mvn clean package -PupdateLicenses"
      );
    }
    if (!Files.exists(committedLicenses)) {
      throw new IOException(
        "Committed licenses directory not found: " + committedLicenses + "\n" +
        "Please run: mvn clean package -PupdateLicenses"
      );
    }
  }

  static ValidationResult compareDirectories(Path tempDir, Path committedDir) throws IOException {
    var tempFiles = buildFileMap(tempDir);
    var committedFiles = buildFileMap(committedDir);

    var newFiles = new ArrayList<String>();
    var missingFiles = new ArrayList<String>();
    var differentFiles = new ArrayList<String>();

    // Find new files (in temp but not in committed)
    for (var relativePath : tempFiles.keySet()) {
      if (!committedFiles.containsKey(relativePath)) {
        newFiles.add(relativePath);
      }
    }

    // Find missing files (in committed but not in temp)
    for (var relativePath : committedFiles.keySet()) {
      if (!tempFiles.containsKey(relativePath)) {
        missingFiles.add(relativePath);
      }
    }

    // Find files with different content
    for (var relativePath : tempFiles.keySet()) {
      if (committedFiles.containsKey(relativePath)) {
        var tempFile = tempFiles.get(relativePath);
        var committedFile = committedFiles.get(relativePath);
        if (Files.mismatch(tempFile, committedFile) != -1L) {
          differentFiles.add(relativePath);
        }
      }
    }

    newFiles.sort(String::compareTo);
    missingFiles.sort(String::compareTo);
    differentFiles.sort(String::compareTo);

    return new ValidationResult(newFiles, missingFiles, differentFiles);
  }

  static Map<String, Path> buildFileMap(Path rootDir) throws IOException {
    try (Stream<Path> paths = Files.walk(rootDir)) {
      return paths
        .filter(Files::isRegularFile)
        .collect(Collectors.toMap(
          path -> rootDir.relativize(path).toString().replace('\\', '/'),
          path -> path
        ));
    }
  }

  static void printFailureMessage(ValidationResult result) {
    System.err.println("[FAILURE] License validation failed!");
    System.err.println();

    if (!result.newFiles().isEmpty()) {
      System.err.println("New files in generated licenses (not in committed):");
      for (var file : result.newFiles()) {
        System.err.println("  + " + file);
      }
      System.err.println();
    }

    if (!result.missingFiles().isEmpty()) {
      System.err.println("Missing files in generated licenses (present in committed):");
      for (var file : result.missingFiles()) {
        System.err.println("  - " + file);
      }
      System.err.println();
    }

    if (!result.differentFiles().isEmpty()) {
      System.err.println("Files with different content:");
      for (var file : result.differentFiles()) {
        System.err.println("  ~ " + file);
      }
      System.err.println();
    }

    System.err.println("To fix this, run: mvn clean package -PupdateLicenses");
  }

  static void printUsage() {
    System.err.println("Usage: LicenseValidator --temp_licenses=<path> --committed_licenses=<path>");
  }

  record ValidationResult(
    List<String> newFiles,
    List<String> missingFiles,
    List<String> differentFiles
  ) {
    boolean hasErrors() {
      return !newFiles.isEmpty() || !missingFiles.isEmpty() || !differentFiles.isEmpty();
    }
  }
}
