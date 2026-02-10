package org.sonar.java.checks;

public class PresuperLogicShoudntBloatConstructor {
  public static class File {
    public File(String path) {
      // Simulate file initialization logic
    }
  }

  public static class NonCompliantSecureFile {
    public NonCompliantSecureFile(String path) {
      if (path == null || path.isBlank()) {  // Noncompliant {{Excessive logic in this "pre-construction" phase makes the code harder to read and maintain.}}
   // ^[el=+19;ec=7]
        throw new IllegalArgumentException("Path cannot be empty");
      }
      if (path.contains("..")) {
        throw new IllegalArgumentException("Relative path traversal is forbidden");
      }
      if (path.startsWith("/root") || path.startsWith("/etc")) {
        throw new SecurityException("Access to system directories is restricted");
      }
      if (path.length() > 255) {
        throw new IllegalArgumentException("Path exceeds maximum length");
      }
      if (!path.matches("^[a-zA-Z0-9/._-]+$")) {
        throw new IllegalArgumentException("Path contains illegal characters");
      }
      String sanitizedPath = path.trim().replace("//", "/");
      if (sanitizedPath.endsWith("/")) {
        sanitizedPath = sanitizedPath.substring(0, sanitizedPath.length() - 1);
      }
      super(sanitizedPath);
    }
  }

  public static class CompliantSecureFile extends File {
    public CompliantSecureFile(String path) {
      // Compliant: Logic is encapsulated in static helpers
      validatePathSecurity(path);
      validatePathFormat(path);
      String sanitizedPath = normalizePath(path);
      super(sanitizedPath);
    }

    private static void validatePathSecurity(String path) {
      if (path == null || path.contains("..")) {
        throw new IllegalArgumentException("Invalid or dangerous path sequence");
      }
      if (path.startsWith("/root") || path.startsWith("/etc")) {
        throw new SecurityException("Access to system directories is restricted");
      }
    }

    private static void validatePathFormat(String path) {
      if (path.length() > 255 || !path.matches("^[a-zA-Z0-9/._-]+$")) {
        throw new IllegalArgumentException("Path format or length is invalid");
      }
    }

    private static String normalizePath(String path) {
      String cleaned = path.trim().replace("//", "/");
      return cleaned.endsWith("/") ? cleaned.substring(0, cleaned.length() - 1) : cleaned;
    }
  }
}
