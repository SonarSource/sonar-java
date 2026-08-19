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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.appsec.TestFileClassifier;

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
 *   <li>Path and naming heuristics: delegated to {@link TestFileClassifier} from
 *       sonar-analyzer-commons, extended with Java-specific path conventions</li>
 *   <li>AST annotations: class-level test framework annotations ({@code @RunWith},
 *       {@code @SpringBootTest}, etc.)</li>
 * </ol>
 *
 * <p>A file is considered a test file if <em>any</em> signal indicates it. The platform type
 * takes priority when {@code TEST}, but a {@code MAIN}-typed file can be upgraded to test scope
 * by the path, naming, or annotation signals.
 *
 * <p>The path/naming heuristic is only applied when {@code sonar.tests} is not configured; if it
 * is configured the platform already classifies test files as {@link InputFile.Type#TEST}.
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
   * {@link org.sonar.api.utils.WildcardPattern}-compatible path patterns for test file detection,
   * passed to {@link TestFileClassifier#of(Configuration, String...)}.
   *
   * <p>Covers:
   * <ul>
   *   <li>Directory segments: {@code test}, {@code tests}, {@code testing}, {@code Test},
   *       {@code Tests}, {@code __tests__}</li>
   *   <li>Maven integration-test source trees: {@code src/it/java}, {@code src/its/java}</li>
   *   <li>Filename suffixes: {@code Test}, {@code Tests}, {@code TestCase}, {@code IT},
   *       {@code ITCase}, {@code Spec}, {@code Specs}</li>
   * </ul>
   */
  private static final String[] JAVA_TEST_PATTERNS = {
    // Directory segment patterns (superset of commons defaults + testing + Java-specific)
    "**/Test/**",
    "**/Tests/**",
    "**/test/**",
    "**/tests/**",
    "**/testing/**",
    "**/__tests__/**",
    // Maven integration test source trees
    "**/it/java/**",
    "**/its/java/**",
    // Filename suffix patterns
    "**/*Test.java",
    "**/*Tests.java",
    "**/*TestCase.java",
    "**/*IT.java",
    "**/*ITCase.java",
    "**/*Spec.java",
    "**/*Specs.java"
  };

  /**
   * Cached {@link TestFileClassifier} for the most recently seen {@link Configuration}.
   * In practice there is exactly one {@link Configuration} per analysis run, so a single
   * cached entry avoids recompiling WildcardPatterns for every analyzed file.
   * The {@link AtomicReference} ensures the config+classifier pair is always observed
   * consistently. A race on simultaneous updates is benign: both threads produce an
   * identical classifier for the same config.
   */
  private static final AtomicReference<Map.Entry<Configuration, TestFileClassifier>> CLASSIFIER_REF =
    new AtomicReference<>();

  private JavaFileTypeClassifier() {
    // utility class
  }

  /**
   * Returns {@code true} if the file should be treated as test code.
   * Combines all signals (platform type, path/naming heuristics, AST annotations) with OR semantics.
   *
   * @param context the current file scanner context
   */
  public static boolean isTestFile(JavaFileScannerContext context) {
    return isPlatformTestFile(context)
      || getClassifier(context.getConfiguration()).looksLikeTestFile(context.getInputFile())
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

  private static TestFileClassifier getClassifier(Configuration config) {
    var entry = CLASSIFIER_REF.get();
    if (entry == null || entry.getKey() != config) {
      var fresh = Map.entry(config, TestFileClassifier.of(config, JAVA_TEST_PATTERNS));
      CLASSIFIER_REF.compareAndSet(entry, fresh);
      entry = CLASSIFIER_REF.get();
    }
    return entry.getValue();
  }
}
