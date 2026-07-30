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
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavaFileTypeClassifierTest {

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
  // hasTestNamingConvention
  // -------------------------------------------------------------------------

  @Test
  void hasTestNamingConvention_recognizesSuffixes() {
    assertNamingConvention(true, "FooTest.java");
    assertNamingConvention(true, "FooTests.java");
    assertNamingConvention(true, "FooTestCase.java");
    assertNamingConvention(true, "FooIT.java");
    assertNamingConvention(true, "FooITCase.java");
    assertNamingConvention(true, "FooSpec.java");
    assertNamingConvention(true, "FooSpecs.java");
  }

  @Test
  void hasTestNamingConvention_recognizesPrefixes() {
    assertNamingConvention(true, "TestFoo.java");
    assertNamingConvention(true, "ITFoo.java");
  }

  @Test
  void hasTestNamingConvention_returnsFalse_forProductionNames() {
    assertNamingConvention(false, "Foo.java");
    assertNamingConvention(false, "FooService.java");
    assertNamingConvention(false, "FooController.java");
    // Lower-case 'test' in the middle is not a match
    assertNamingConvention(false, "MyTestedCode.java");
  }

  private void assertNamingConvention(boolean expected, String filename) {
    JavaFileScannerContext context = contextWithInputFile(TestUtils.emptyInputFile(filename, InputFile.Type.MAIN));
    assertThat(JavaFileTypeClassifier.hasTestNamingConvention(context))
      .as("Expected hasTestNamingConvention=%s for '%s'", expected, filename)
      .isEqualTo(expected);
  }

  // -------------------------------------------------------------------------
  // hasTestPathSegment
  // -------------------------------------------------------------------------

  @Test
  void hasTestPathSegment_returnsTrue_forItSegment() {
    assertPathSegment(true, "src/it/java/Foo.java");
  }

  @Test
  void hasTestPathSegment_returnsTrue_forItsSegment() {
    assertPathSegment(true, "src/its/java/Foo.java");
  }

  @Test
  void hasTestPathSegment_isCaseInsensitive() {
    assertPathSegment(true, "src/IT/java/Foo.java");
    assertPathSegment(true, "src/ITS/java/Foo.java");
  }

  @Test
  void hasTestPathSegment_returnsFalse_forMainPath() {
    assertPathSegment(false, "src/main/java/Foo.java");
    assertPathSegment(false, "src/test/java/Foo.java");
  }

  @Test
  void hasTestPathSegment_returnsFalse_whenSegmentIsSubstring() {
    // "itself" or "iteration" should not match — only exact segment
    assertPathSegment(false, "src/itself/java/Foo.java");
    assertPathSegment(false, "src/iteration/java/Foo.java");
  }

  private void assertPathSegment(boolean expected, String relativePath) {
    JavaFileScannerContext context = contextWithInputFile(TestUtils.emptyInputFile(relativePath, InputFile.Type.MAIN));
    assertThat(JavaFileTypeClassifier.hasTestPathSegment(context))
      .as("Expected hasTestPathSegment=%s for '%s'", expected, relativePath)
      .isEqualTo(expected);
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
  // isTestFile (combined signal)
  // -------------------------------------------------------------------------

  @Test
  void isTestFile_returnsTrue_whenPlatformSaysTest() {
    JavaFileScannerContext context = contextWithInputFile(TestUtils.emptyInputFile("Foo.java", InputFile.Type.TEST));
    assertThat(JavaFileTypeClassifier.isTestFile(context)).isTrue();
  }

  @Test
  void isTestFile_returnsTrue_whenPathSegmentMatches_evenIfPlatformSaysMain() {
    JavaFileScannerContext context = contextWithInputFile(TestUtils.emptyInputFile("src/it/java/Foo.java", InputFile.Type.MAIN));
    assertThat(JavaFileTypeClassifier.isTestFile(context)).isTrue();
  }

  @Test
  void isTestFile_returnsTrue_whenNamingMatches_evenIfPlatformSaysMain() {
    JavaFileScannerContext context = contextWithInputFile(TestUtils.emptyInputFile("FooTest.java", InputFile.Type.MAIN));
    // fileParsed() not set → defaults to false (Mockito default for boolean), no AST signal
    assertThat(JavaFileTypeClassifier.isTestFile(context)).isTrue();
  }

  @Test
  void isTestFile_returnsFalse_whenNoSignal() {
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.getInputFile()).thenReturn(TestUtils.emptyInputFile("Foo.java", InputFile.Type.MAIN));
    when(context.fileParsed()).thenReturn(false);
    assertThat(JavaFileTypeClassifier.isTestFile(context)).isFalse();
  }

  // -------------------------------------------------------------------------
  // Helper
  // -------------------------------------------------------------------------

  private static JavaFileScannerContext contextWithInputFile(InputFile inputFile) {
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    return context;
  }
}
