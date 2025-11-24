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

  private CheckTestMapping extractMappingFromTestOrchestrator(String testOrchestratorPath) throws Exception {
    Path testFile = Path.of(testOrchestratorPath);
    String content = Files.readString(testFile);

    // Step 0: Extract package declaration
    String packageName = null;
    java.util.regex.Pattern packagePattern = java.util.regex.Pattern.compile("package\\s+([a-zA-Z0-9_.]+);", java.util.regex.Pattern.MULTILINE);
    java.util.regex.Matcher packageMatcher = packagePattern.matcher(content);
    if (packageMatcher.find()) {
      packageName = packageMatcher.group(1);
    } else {
      packageName = "org.sonar.java.checks"; // fallback
    }

    // Step 1: Extract test file path
    String testFilePath = null;
    java.util.regex.Pattern filePatternDirect = java.util.regex.Pattern.compile("\\.onFile\\(\"([^\"]+)\"\\)");
    java.util.regex.Matcher fileMatcherDirect = filePatternDirect.matcher(content);
    if (fileMatcherDirect.find()) {
      testFilePath = fileMatcherDirect.group(1);
    } else {
      // Modular regex for method calls
      java.util.regex.Pattern filePatternFunc = java.util.regex.Pattern.compile("\\.onFile\\((mainCodeSourcesPath|testCodeSourcesPath)\\(\"([^\"]+)\"\\)\\)");
      java.util.regex.Matcher fileMatcherFunc = filePatternFunc.matcher(content);
      if (fileMatcherFunc.find()) {
        String method = fileMatcherFunc.group(1);
        String fileArg = fileMatcherFunc.group(2);
        if ("mainCodeSourcesPath".equals(method)) {
          testFilePath = "src/main/java/" + fileArg;
        } else if ("testCodeSourcesPath".equals(method)) {
          testFilePath = "src/test/java/" + fileArg;
        }
      }
    }

    // Step 2: Extract check class name (robust to whitespace, FQCN, etc.)
    String checkClassName = null;
    java.util.regex.Pattern checkPattern = java.util.regex.Pattern.compile("\\.withCheck\\(\\s*new\\s+([a-zA-Z0-9_$.]+)\\s*\\(\\s*\\)\\s*\\)");
    java.util.regex.Matcher checkMatcher = checkPattern.matcher(content);
    if (checkMatcher.find()) {
      checkClassName = checkMatcher.group(1);
    }

    // Step 3: Use reflection to get annotation info from the check class
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

    // Step 4: Derive main file path from test file path
    String mainFilePath = null;
    if (testFilePath != null) {
      mainFilePath = testFilePath.replace("src/test/files/checks/", "src/main/java/org/sonar/java/checks/").replace(".java", "Check.java");
    }

    return new CheckTestMapping(testFilePath, testOrchestratorPath, mainFilePath, ruleKey);
  }

  @Test
  void extractAll() throws IOException {
    List<CheckTestMapping> mappings = new ArrayList<>();
    // Path to the test orchestrator folder
    Path testOrchestratorDir = Path.of("../java-checks/src/test/java/org/sonar/java/checks/");
    try (Stream<Path> files = Files.walk(testOrchestratorDir)) {
      List<Path> testFiles = files
        .filter(Files::isRegularFile)
        .filter(p -> p.getFileName().toString().endsWith("CheckTest.java"))
        .collect(Collectors.toList());
      for (Path testFile : testFiles) {
        try {
          CheckTestMapping mapping = extractMappingFromTestOrchestrator(testFile.toString());
          mappings.add(mapping);
        } catch (Exception e) {
          System.err.println("Failed to extract mapping for: " + testFile + " - " + e.getMessage());
        }
      }
    }
    System.out.println("yay");
    long nullCount = mappings.stream().filter(m -> m.testFilePath() == null || m.mainFilePath() == null).count();
    List<CheckTestMapping> nullElems = mappings.stream().filter(m -> m.testFilePath() == null || m.mainFilePath() == null).toList();
    System.out.println("Mappings with null testFilePath or mainFilePath: " + nullCount + " out of " + mappings.size());
  }

  @Test
  void extractCheckAndTestFile() throws Exception {
    String testOrchestratorPath = "../java-checks/src/test/java/org/sonar/java/checks/AbsOnNegativeCheckTest.java";
    var smth = extractMappingFromTestOrchestrator("../java-checks/src/test/java/org/sonar/java/checks/AbsOnNegativeCheckTest.java");
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
