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
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavaFileTypeClassifierTest {

  /**
   * A {@link Configuration} that does not declare {@code sonar.tests}, so the path/naming
   * heuristic in {@link org.sonarsource.analyzer.commons.appsec.TestFileClassifier} is active.
   */
  private static final Configuration NO_SONAR_TESTS_CONFIG = new MapSettings().asConfig();

  // -------------------------------------------------------------------------
  // isPlatformTestFile
  // -------------------------------------------------------------------------

  @Test
  void isPlatformTestFile_returnsTrue_forTestType() {
    JavaFileScannerContext context = contextWithInputFile(TestUtils.emptyInputFile("Foo.java", InputFile.Type.TEST));
    assertThat(JavaFileTypeClassifier.isPlatformTestFile(context)).isTrue();
  }

  @Test
  void isPlatformTestFile_returnsFalse_forMainType() {
    JavaFileScannerContext context = contextWithInputFile(TestUtils.emptyInputFile("Foo.java", InputFile.Type.MAIN));
    assertThat(JavaFileTypeClassifier.isPlatformTestFile(context)).isFalse();
  }

  // -------------------------------------------------------------------------
  // hasTestFrameworkAnnotation
  // -------------------------------------------------------------------------

  @Test
  void hasTestFrameworkAnnotation_returnsFalse_whenNotParsed() {
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.fileParsed()).thenReturn(false);
    assertThat(JavaFileTypeClassifier.hasTestFrameworkAnnotation(context)).isFalse();
  }

  @Test
  void hasTestFrameworkAnnotation_detects_RunWith() {
    assertAnnotationSignal(true, "src/test/files/utils/SampleWithRunWith.java");
  }

  @Test
  void hasTestFrameworkAnnotation_detects_ExtendWith() {
    assertAnnotationSignal(true, "src/test/files/utils/SampleWithExtendWith.java");
  }

  @Test
  void hasTestFrameworkAnnotation_detects_SpringBootTest() {
    assertAnnotationSignal(true, "src/test/files/utils/SampleWithSpringBootTest.java");
  }

  @Test
  void hasTestFrameworkAnnotation_detects_SpringAutoconfigure_WebMvcTest() {
    assertAnnotationSignal(true, "src/test/files/utils/SampleWithWebMvcTest.java");
  }

  @Test
  void hasTestFrameworkAnnotation_returnsFalse_forPlainClass() {
    assertAnnotationSignal(false, "src/test/files/utils/PlainSample.java");
  }

  private void assertAnnotationSignal(boolean expected, String filePath) {
    var classpath = TestClasspathUtils.DEFAULT_MODULE.getClassPath();
    var bridge = new VisitorsBridgeForTests.Builder(List.of())
      .enableSemanticWithProjectClasspath(classpath)
      .build();
    JavaAstScanner.scanSingleFileForTests(TestUtils.inputFile(filePath), bridge);

    JavaFileScannerContext context = bridge.testContexts().get(0);
    assertThat(JavaFileTypeClassifier.hasTestFrameworkAnnotation(context))
      .as("Expected hasTestFrameworkAnnotation=%s for '%s'", expected, filePath)
      .isEqualTo(expected);
  }

  // -------------------------------------------------------------------------
  // isTestFile — platform signal
  // -------------------------------------------------------------------------

  @Test
  void isTestFile_returnsTrue_whenPlatformSaysTest() {
    JavaFileScannerContext context = contextWithInputFileAndConfig(
      TestUtils.emptyInputFile("Foo.java", InputFile.Type.TEST), NO_SONAR_TESTS_CONFIG);
    assertThat(JavaFileTypeClassifier.isTestFile(context)).isTrue();
  }

  // -------------------------------------------------------------------------
  // isTestFile — filename suffix conventions
  // -------------------------------------------------------------------------

  @Test
  void isTestFile_recognizes_testFileSuffixes() {
    assertIsTestFile(true, "FooTest.java");
    assertIsTestFile(true, "FooTests.java");
    assertIsTestFile(true, "FooTestCase.java");
    assertIsTestFile(true, "FooIT.java");
    assertIsTestFile(true, "FooITCase.java");
    assertIsTestFile(true, "FooSpec.java");
    assertIsTestFile(true, "FooSpecs.java");
  }

  @Test
  void isTestFile_returnsFalse_forProductionNames() {
    assertIsTestFile(false, "Foo.java");
    assertIsTestFile(false, "FooService.java");
    assertIsTestFile(false, "FooController.java");
  }

  // -------------------------------------------------------------------------
  // isTestFile — path/directory segment conventions
  // -------------------------------------------------------------------------

  @Test
  void isTestFile_recognizes_mavenTestPath() {
    assertIsTestFile(true, "src/test/java/Foo.java");
  }

  @Test
  void isTestFile_recognizes_mavenItPath() {
    assertIsTestFile(true, "src/it/java/Foo.java");
  }

  @Test
  void isTestFile_recognizes_mavenItsPath() {
    assertIsTestFile(true, "src/its/java/Foo.java");
  }

  @Test
  void isTestFile_recognizes_testDirectorySegment() {
    assertIsTestFile(true, "src/test/Foo.java");
    assertIsTestFile(true, "modules/core/test/Foo.java");
  }

  @Test
  void isTestFile_recognizes_testsDirectorySegment() {
    assertIsTestFile(true, "src/tests/Foo.java");
  }

  @Test
  void isTestFile_recognizes_testingDirectorySegment() {
    assertIsTestFile(true, "src/testing/Foo.java");
  }

  @Test
  void isTestFile_returnsFalse_forProductionPath() {
    assertIsTestFile(false, "src/main/java/Foo.java");
  }

  // -------------------------------------------------------------------------
  // isTestFile — sonar.tests configured: path/naming heuristic disabled
  // -------------------------------------------------------------------------

  @Test
  void isTestFile_withSonarTestsConfigured_ignoresNamingAndPathSignals() {
    // When sonar.tests is set, TestFileClassifier suppresses the heuristic because
    // the platform already classifies test files as InputFile.Type.TEST.
    // Names and paths that would otherwise trigger the heuristic must return false.
    var config = new MapSettings().setProperty("sonar.tests", "src/test/java").asConfig();

    assertIsTestFileWithConfig(false, "FooTest.java", config);
    assertIsTestFileWithConfig(false, "src/test/java/Foo.java", config);
    assertIsTestFileWithConfig(false, "src/it/java/Foo.java", config);
  }

  @Test
  void isTestFile_withSonarTestsConfigured_platformTypeStillApplies() {
    var config = new MapSettings().setProperty("sonar.tests", "src/test/java").asConfig();

    // InputFile.Type.TEST is checked before the heuristic and is always authoritative.
    var context = contextWithInputFileAndConfig(
      TestUtils.emptyInputFile("Foo.java", InputFile.Type.TEST), config);
    assertThat(JavaFileTypeClassifier.isTestFile(context)).isTrue();
  }

  @Test
  void isTestFile_withSonarTestsConfigured_annotationSignalStillApplies() {
    // hasTestFrameworkAnnotation() reads the AST, not the configuration —
    // it must keep working regardless of whether sonar.tests is set.
    assertAnnotationSignal(true, "src/test/files/utils/SampleWithRunWith.java");
  }

  // -------------------------------------------------------------------------
  // isTestFile — no signal
  // -------------------------------------------------------------------------

  @Test
  void isTestFile_returnsFalse_whenNoSignal() {
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.getInputFile()).thenReturn(TestUtils.emptyInputFile("Foo.java", InputFile.Type.MAIN));
    when(context.getConfiguration()).thenReturn(NO_SONAR_TESTS_CONFIG);
    when(context.fileParsed()).thenReturn(false);
    assertThat(JavaFileTypeClassifier.isTestFile(context)).isFalse();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static void assertIsTestFileWithConfig(boolean expected, String filename, Configuration config) {
    JavaFileScannerContext context = contextWithInputFileAndConfig(
      TestUtils.emptyInputFile(filename, InputFile.Type.MAIN), config);
    assertThat(JavaFileTypeClassifier.isTestFile(context))
      .as("Expected isTestFile=%s for '%s' with sonar.tests config", expected, filename)
      .isEqualTo(expected);
  }

  private static void assertIsTestFile(boolean expected, String filename) {
    JavaFileScannerContext context = contextWithInputFileAndConfig(
      TestUtils.emptyInputFile(filename, InputFile.Type.MAIN), NO_SONAR_TESTS_CONFIG);
    assertThat(JavaFileTypeClassifier.isTestFile(context))
      .as("Expected isTestFile=%s for '%s'", expected, filename)
      .isEqualTo(expected);
  }

  private static JavaFileScannerContext contextWithInputFile(InputFile inputFile) {
    return contextWithInputFileAndConfig(inputFile, NO_SONAR_TESTS_CONFIG);
  }

  private static JavaFileScannerContext contextWithInputFileAndConfig(InputFile inputFile, Configuration config) {
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getConfiguration()).thenReturn(config);
    return context;
  }
}
