package org.sonar.java.extractor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestExtractor {

  private record CheckTestMapping(String testFilePath, String testOrchestratorPath, String mainFilePath, String ruleKey) {}

  private List<CheckTestMapping> extractMappingsFromTestOrchestrator(String testOrchestratorPath) throws Exception {
    Path testFile = Path.of(testOrchestratorPath);
    String content = Files.readString(testFile);

    // Extract package declaration
    String packageName = null;
    java.util.regex.Pattern packagePattern = java.util.regex.Pattern.compile("package\\s+([a-zA-Z0-9_.]+);", java.util.regex.Pattern.MULTILINE);
    java.util.regex.Matcher packageMatcher = packagePattern.matcher(content);
    if (packageMatcher.find()) {
      packageName = packageMatcher.group(1);
    } else {
      packageName = "org.sonar.java.checks"; // fallback
    }

    // Extract all .onFile(...) calls (supporting multiple method variants)
    List<String> supportedMethods = List.of("mainCodeSourcesPath", "testCodeSourcesPath", "nonCompilingTestSourcesPath");
    String methodRegex = String.join("|", supportedMethods);
    java.util.regex.Pattern filePattern = java.util.regex.Pattern.compile("\\.onFile\\((" + methodRegex + ")\\(\"([^\"]+)\"\\)\\)");
    // FIX: Correct escaping for Java string literal
    filePattern = java.util.regex.Pattern.compile("\\.onFile\\((" + methodRegex + ")\\(\"([^\"]+)\"\\)\\)");
    java.util.regex.Matcher fileMatcher = filePattern.matcher(content);
    List<String> testFilePaths = new ArrayList<>();
    List<String> methodNames = new ArrayList<>();
    while (fileMatcher.find()) {
      String method = fileMatcher.group(1);
      String fileArg = fileMatcher.group(2);
      methodNames.add(method);
      if ("mainCodeSourcesPath".equals(method)) {
        testFilePaths.add("src/main/java/" + fileArg);
      } else if ("testCodeSourcesPath".equals(method)) {
        testFilePaths.add("src/test/java/" + fileArg);
      } else if ("nonCompilingTestSourcesPath".equals(method)) {
        testFilePaths.add("src/test/java/" + fileArg); // treat as test source
      }
    }
    // Also support direct string literal .onFile("...")
    java.util.regex.Pattern filePatternDirect = java.util.regex.Pattern.compile("\\.onFile\\(\"([^\"]+)\"\\)");
    java.util.regex.Matcher fileMatcherDirect = filePatternDirect.matcher(content);
    while (fileMatcherDirect.find()) {
      testFilePaths.add(fileMatcherDirect.group(1));
      methodNames.add("direct");
    }

    // Extract check class name (assume one per orchestrator for now)
    java.util.regex.Pattern checkPattern = java.util.regex.Pattern.compile("\\.withCheck\\(\\s*new\\s+([a-zA-Z0-9_$.]+)\\s*\\(\\s*\\)\\s*\\)");
    java.util.regex.Matcher checkMatcher = checkPattern.matcher(content);
    String checkClassName = null;
    if (checkMatcher.find()) {
      checkClassName = checkMatcher.group(1);
    }

    // Use reflection to get annotation info from the check class
    String fqcn = null;
    if (checkClassName != null) {
      if (checkClassName.contains(".")) {
        fqcn = checkClassName;
      } else {
        fqcn = packageName + "." + checkClassName;
      }
    }
    String ruleKey = null;
    try {
      if (fqcn != null) {
        Class<?> clazz = Class.forName(fqcn);
        java.lang.annotation.Annotation ruleAnnotation = clazz.getAnnotation(org.sonar.check.Rule.class);
        if (ruleAnnotation != null) {
          ruleKey = (String) ruleAnnotation.annotationType().getMethod("key").invoke(ruleAnnotation);
        }
      }
    } catch (Exception e) {
      System.err.println("Reflection failed for: " + fqcn + " - " + e.getMessage());
    }

    // Generate mapping records for each test file
    List<CheckTestMapping> mappings = new ArrayList<>();
    for (String testFilePath : testFilePaths) {
      String mainFilePath = null;
      if (testFilePath != null) {
        mainFilePath = testFilePath.replace("src/test/files/checks/", "src/main/java/org/sonar/java/checks/").replace(".java", "Check.java");
      }
      mappings.add(new CheckTestMapping(testFilePath, testOrchestratorPath, mainFilePath, ruleKey));
    }
    return mappings;
  }

  @Test
  void extractAll() throws IOException {
    List<CheckTestMapping> mappings = new ArrayList<>();
    Path testOrchestratorDir = Path.of("../java-checks/src/test/java/org/sonar/java/checks/");
    try (Stream<Path> files = Files.walk(testOrchestratorDir)) {
      List<Path> testFiles = files
        .filter(Files::isRegularFile)
        .filter(p -> p.getFileName().toString().endsWith("CheckTest.java"))
        .collect(Collectors.toList());
      for (Path testFile : testFiles) {
        try {
          List<CheckTestMapping> fileMappings = extractMappingsFromTestOrchestrator(testFile.toString());
          mappings.addAll(fileMappings);
        } catch (Exception e) {
          System.err.println("Failed to extract mapping for: " + testFile + " - " + e.getMessage());
        }
      }
    }
    System.out.println("yay");
    long nullCount = mappings.stream().filter(m -> m.testFilePath() == null || m.mainFilePath() == null).count();
    List<CheckTestMapping> nullEmeents = mappings.stream().filter(m -> m.testFilePath() == null || m.mainFilePath() == null || m.ruleKey() == null).toList();
    System.out.println("Mappings with null testFilePath or mainFilePath: " + nullCount + " out of " + mappings.size());
  }

  @Test
  void extractCheckAndTestFile() throws Exception {
    String testOrchestratorPath = "../java-checks/src/test/java/org/sonar/java/checks/AbsOnNegativeCheckTest.java";
    var smth = extractMappingsFromTestOrchestrator("../java-checks/src/test/java/org/sonar/java/checks/AbsOnNegativeCheckTest.java");
    Path testFile = Path.of(testOrchestratorPath);
    String content = Files.readString(testFile);

    // Step 1: Extract test file path
    java.util.regex.Pattern filePattern = java.util.regex.Pattern.compile("\\.onFile\\(\\\"([^\\\"]+)\\\"\\)");
    java.util.regex.Matcher fileMatcher = filePattern.matcher(content);
    String testFilePath = fileMatcher.find() ? fileMatcher.group(1) : null;

    // Step 2: Extract check class name (robust to whitespace and newlines)
    java.util.regex.Pattern checkPattern = java.util.regex.Pattern.compile("\\.withCheck\\(\\s*new ([A-Za-z0-9_]+)\\s*\\(\\s*\\)\\s*\\)");
    java.util.regex.Matcher checkMatcher = checkPattern.matcher(content);
    String checkClassName = checkMatcher.find() ? checkMatcher.group(1) : null;

    // Step 3: Use reflection to get annotation info from the check class
    String fqcn = "org.sonar.java.checks." + checkClassName;
    Class<?> clazz = Class.forName(fqcn);
    java.lang.annotation.Annotation ruleAnnotation = clazz.getAnnotation(org.sonar.check.Rule.class);
    String ruleKey = null;
    if (ruleAnnotation != null) {
      ruleKey = (String) ruleAnnotation.annotationType().getMethod("key").invoke(ruleAnnotation);
      System.out.println("Extracted rule key: " + ruleKey);
    } else {
      System.out.println("No @Rule annotation found on class: " + fqcn);
    }

    System.out.println("Extracted test file: " + testFilePath);
    System.out.println("Extracted check class: " + checkClassName);
    assert testFilePath != null : "Test file path not found";
    assert checkClassName != null : "Check class name not found";
    assert ruleKey != null : "Rule key not found";
  }
}
