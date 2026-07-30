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
package org.sonar.java.utils;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Enriches test scope determination beyond the platform's {@link InputFile.Type}.
 *
 * <p>The platform scanner (Maven, Gradle, CLI) assigns {@link InputFile.Type#TEST} or
 * {@link InputFile.Type#MAIN} based on build-tool conventions, but this classification
 * can be incomplete (e.g. test helpers in {@code src/main/java}, misconfigured projects).
 *
 * <p>This classifier combines three signals:
 * <ol>
 *   <li>Platform truth: {@link InputFile#type()} from the Sonar scanner</li>
 *   <li>Naming conventions: file name patterns like {@code FooTest}, {@code ITFoo}, {@code FooSpec}</li>
 *   <li>AST annotations: class-level test framework annotations ({@code @RunWith}, {@code @SpringBootTest}, etc.)</li>
 * </ol>
 *
 * <p>A file is considered a test file if <em>any</em> signal indicates it — the platform type
 * takes priority when {@code TEST}, but a {@code MAIN}-typed file can be upgraded to test scope
 * by the naming or annotation signals.
 *
 * <p>Usage example in a check's {@code scanFile} method:
 * <pre>{@code
 *   if (JavaFileTypeClassifier.isTestFile(context)) {
 *     return; // skip test files
 *   }
 * }</pre>
 */
public final class JavaFileTypeClassifier {

  /**
   * Class-level annotations that unambiguously mark a class as part of a test framework.
   * Method-level annotations (e.g. JUnit 4/5 {@code @Test}) are intentionally excluded here;
   * use {@code UnitTestUtils.isTestClass(ClassTree)} for method-level detection.
   */
  private static final Set<String> TEST_CLASS_ANNOTATIONS = Set.of(
    // JUnit 4
    "org.junit.runner.RunWith",
    // JUnit 5
    "org.junit.jupiter.api.extension.ExtendWith",
    // TestNG (class-level @Test marks all methods as tests)
    "org.testng.annotations.Test",
    // Spring Boot Test
    "org.springframework.boot.test.context.SpringBootTest",
    // Spring Test (used for integration tests without Spring Boot)
    "org.springframework.test.context.ContextConfiguration"
  );

  /**
   * Annotation package prefixes for Spring test slice annotations
   * (e.g. {@code @WebMvcTest}, {@code @DataJpaTest}, {@code @JsonTest}).
   */
  private static final List<String> TEST_ANNOTATION_PACKAGE_PREFIXES = List.of(
    "org.springframework.boot.test.autoconfigure.",
    "org.springframework.test."
  );

  /**
   * Path substrings that indicate a file lives in an integration-test source tree,
   * following the Maven convention of {@code src/it/java} or {@code src/its/java}.
   */
  private static final List<String> TEST_PATH_SUBPATHS = List.of("src/it/java", "src/its/java");

  /**
   * Matches file names (without {@code .java} extension) that follow standard test naming conventions:
   * <ul>
   *   <li>Prefix: {@code Test}, {@code IT} (e.g. {@code TestFoo}, {@code ITFoo})</li>
   *   <li>Suffix: {@code Test}, {@code Tests}, {@code TestCase}, {@code IT}, {@code ITCase}, {@code Spec}, {@code Specs}
   *       (e.g. {@code FooTest}, {@code FooSpec})</li>
   * </ul>
   */
  private static final Pattern TEST_NAME_PATTERN = Pattern.compile(
    "^((Test|IT)[a-zA-Z0-9_]+|[A-Z][a-zA-Z0-9_]*(Test|Tests|TestCase|IT|ITCase|Spec|Specs))$"
  );

  private JavaFileTypeClassifier() {
    // utility class
  }

  /**
   * Returns {@code true} if the file should be treated as test code.
   * Combines all three signals (platform type, naming, AST annotations) with OR semantics.
   *
   * @param context the current file scanner context
   */
  public static boolean isTestFile(JavaFileScannerContext context) {
    return isPlatformTestFile(context)
      || hasTestNamingConvention(context)
      || hasTestPathSegment(context)
      || hasTestFrameworkAnnotation(context);
  }

  /**
   * Returns {@code true} if the platform scanner classified this file as {@link InputFile.Type#TEST}.
   * This is the authoritative signal — it cannot be overridden by the other signals.
   *
   * @param context the current file scanner context
   */
  static boolean isPlatformTestFile(JavaFileScannerContext context) {
    return context.getInputFile().type() == InputFile.Type.TEST;
  }

  /**
   * Returns {@code true} if the file name (without {@code .java} extension) matches a standard
   * test naming convention: prefixes {@code Test}/  {@code IT}, or suffixes
   * {@code Test}/{@code Tests}/{@code TestCase}/{@code IT}/{@code ITCase}/{@code Spec}/{@code Specs}.
   *
   * @param context the current file scanner context
   */
  static boolean hasTestNamingConvention(JavaFileScannerContext context) {
    String filename = context.getInputFile().filename();
    String baseName = filename.endsWith(".java") ? filename.substring(0, filename.length() - 5) : filename;
    return TEST_NAME_PATTERN.matcher(baseName).matches();
  }

  /**
   * Returns {@code true} if the file's URI path contains a known integration-test source tree
   * substring: {@code src/it/java} or {@code src/its/java}.
   *
   * <p>This covers the Maven convention of placing integration tests under
   * {@code src/it/java} or {@code src/its/java}.
   *
   * @param context the current file scanner context
   */
  static boolean hasTestPathSegment(JavaFileScannerContext context) {
    String path = context.getInputFile().uri().getPath().toLowerCase(Locale.ROOT);
    return TEST_PATH_SUBPATHS.stream().anyMatch(path::contains);
  }

  /**
   * Returns {@code true} if any top-level class in the compilation unit carries a recognized
   * test framework annotation at the class level.
   *
   * <p>Gracefully returns {@code false} if the file was not successfully parsed
   * ({@link JavaFileScannerContext#fileParsed()} is {@code false}).
   *
   * @param context the current file scanner context
   */
  static boolean hasTestFrameworkAnnotation(JavaFileScannerContext context) {
    if (!context.fileParsed()) {
      return false;
    }
    return context.getTree().types().stream()
      .filter(tree -> tree.is(Tree.Kind.CLASS))
      .map(ClassTree.class::cast)
      .anyMatch(JavaFileTypeClassifier::hasTestAnnotation);
  }

  private static boolean hasTestAnnotation(ClassTree classTree) {
    SymbolMetadata metadata = classTree.symbol().metadata();
    if (TEST_CLASS_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith)) {
      return true;
    }
    return metadata.annotations().stream()
      .map(ann -> ann.symbol().type().fullyQualifiedName())
      .anyMatch(fqn -> TEST_ANNOTATION_PACKAGE_PREFIXES.stream().anyMatch(fqn::startsWith));
  }
}
