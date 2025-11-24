package org.sonar.java.extractor;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestExtractor {

  @Test
  void extractCheckAndTestFile() throws Exception {
    Path testFile = Path.of("../java-checks/src/test/java/org/sonar/java/checks/AbsOnNegativeCheckTest.java");
    String content = Files.readString(testFile);

    // Step 1: Extract test file path
    java.util.regex.Pattern filePattern = java.util.regex.Pattern.compile("\\.onFile\\(\"([^\"]+)\"\\)");
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
