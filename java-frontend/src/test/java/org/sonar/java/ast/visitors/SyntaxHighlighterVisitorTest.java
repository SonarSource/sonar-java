/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.ast.visitors;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaSquid;
import org.sonar.java.JavaTestClasspath;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SyntaxHighlighterVisitorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private SensorContextTester context;
  private SonarComponents sonarComponents;

  private SyntaxHighlighterVisitor syntaxHighlighterVisitor;

  private String eol;

  @Before
  public void setUp() throws Exception {
    context = SensorContextTester.create(temp.getRoot());
    sonarComponents = new SonarComponents(mock(FileLinesContextFactory.class), context.fileSystem(),
      mock(JavaClasspath.class), mock(JavaTestClasspath.class), mock(CheckFactory.class));
    sonarComponents.setSensorContext(context);
    syntaxHighlighterVisitor = new SyntaxHighlighterVisitor(sonarComponents);
  }

  @Test
  public void parse_error() throws Exception {
    SensorContextTester spy = spy(context);
    File file = temp.newFile().getAbsoluteFile();
    Files.asCharSink(file, StandardCharsets.UTF_8).write("ParseError");
    scan(TestUtils.inputFile(file));
    verify(spy, never()).newHighlighting();
  }

  @Test
  public void test_LF() throws Exception {
    this.eol = "\n";
    InputFile inputFile = generateDefaultTestFile();
    scan(inputFile);
    verifyHighlighting(inputFile);
  }

  @Test
  public void test_CR_LF() throws Exception {
    this.eol = "\r\n";
    InputFile inputFile = generateDefaultTestFile();
    scan(inputFile);
    verifyHighlighting(inputFile);
  }

  @Test
  public void test_CR() throws Exception {
    this.eol = "\r";
    InputFile inputFile = generateDefaultTestFile();
    scan(inputFile);
    verifyHighlighting(inputFile);
  }

  /**
   * Java 9 modules introduces restricted keywords only used in their context
   */
  @Test
  public void test_restricted_keywords_within_module() throws Exception {
    this.eol = "\n";
    InputFile inputFile = generateTestFile("src/test/files/highlighter/module-info.java");
    scan(inputFile);

    String componentKey = inputFile.key();
    assertThatHasBeenHighlighted(componentKey, 1, 1, 3, 4, TypeOfText.COMMENT);
    assertThatHasBeenHighlighted(componentKey, 4, 1, 4, 7, TypeOfText.KEYWORD); // import
    assertThatHasBeenHighlighted(componentKey, 6, 1, 8, 4, TypeOfText.STRUCTURED_COMMENT);
    assertThatHasBeenHighlighted(componentKey, 9, 1, 9, 6, TypeOfText.ANNOTATION); // @Beta
    assertThatHasBeenHighlighted(componentKey, 10, 1, 10, 5, TypeOfText.KEYWORD); // open
    assertThatHasBeenHighlighted(componentKey, 10, 6, 10, 12, TypeOfText.KEYWORD); // module
    assertThatHasBeenHighlighted(componentKey, 11, 3, 11, 11, TypeOfText.KEYWORD); // requires
    assertThatHasBeenHighlighted(componentKey, 11, 12, 11, 22, /* due to bug in ECJ */ TypeOfText.KEYWORD); // transitive as identifier
    assertThatHasBeenHighlighted(componentKey, 12, 3, 12, 11, TypeOfText.KEYWORD); // requires
    assertThatHasBeenHighlighted(componentKey, 12, 12, 12, 18, TypeOfText.KEYWORD); // static
    assertThatHasBeenHighlighted(componentKey, 12, 19, 12, 29, /* due to bug in ECJ */ TypeOfText.KEYWORD); // transitive as identifier
    assertThatHasBeenHighlighted(componentKey, 13, 3, 13, 11, TypeOfText.KEYWORD); // requires
    assertThatHasBeenHighlighted(componentKey, 13, 12, 13, 18, TypeOfText.KEYWORD); // static
    assertThatHasBeenHighlighted(componentKey, 13, 19, 13, 29, TypeOfText.KEYWORD); // transitive as keyword
    assertThatHasBeenHighlighted(componentKey, 14, 3, 14, 10, TypeOfText.KEYWORD); // exports
    assertThatHasBeenHighlighted(componentKey, 14, 19, 14, 21, TypeOfText.KEYWORD); // to
    assertThatHasBeenHighlighted(componentKey, 15, 3, 15, 8, TypeOfText.KEYWORD); // opens
    assertThatHasBeenHighlighted(componentKey, 15, 17, 15, 19, TypeOfText.KEYWORD); // to
    assertThatHasBeenHighlighted(componentKey, 16, 3, 16, 7, TypeOfText.KEYWORD); // uses
    assertThatHasBeenHighlighted(componentKey, 17, 3, 17, 11, TypeOfText.KEYWORD); // provides
    assertThatHasBeenHighlighted(componentKey, 17, 26, 17, 30, TypeOfText.KEYWORD); // with

    // usages of restricted keywords in module name and package names
    assertThatHasBeenHighlighted(componentKey, 18, 3, 18, 60, TypeOfText.COMMENT);
    assertThatHasBeenHighlighted(componentKey, 19, 3, 19, 10, TypeOfText.KEYWORD); // exports
    assertThatHasNotBeenHighlighted(componentKey, 19, 28, 19, 34); // 'module' used in package name
    assertThatHasBeenHighlighted(componentKey, 20, 3, 20, 11, TypeOfText.KEYWORD); // provides
    assertThatHasBeenHighlighted(componentKey, 20, 34, 20, 38, TypeOfText.KEYWORD); // with
    assertThatHasNotBeenHighlighted(componentKey, 20, 45, 20, 49); // 'with' used in package name
    assertThatHasNotBeenHighlighted(componentKey, 20, 50, 20, 52); // 'to' used in package name
    assertThatHasNotBeenHighlighted(componentKey, 20, 53, 20, 60); // 'exports' used in package name
    assertThatHasNotBeenHighlighted(componentKey, 20, 61, 20, 67); // 'module' used in package name
  }

  @Test
  public void test_restricted_keywords_outside_module() throws Exception {
    this.eol = "\n";
    InputFile inputFile = generateTestFile("src/test/files/highlighter/ExampleWithModuleKeywords.java");
    scan(inputFile);

    String componentKey = inputFile.key();
    assertThatHasBeenHighlighted(componentKey, 1, 1, 3, 4, TypeOfText.COMMENT);
    assertThatHasBeenHighlighted(componentKey, 4, 1, 4, 7, TypeOfText.KEYWORD); // import
    assertThatHasBeenHighlighted(componentKey, 6, 1, 8, 4, TypeOfText.STRUCTURED_COMMENT);
    assertThatHasBeenHighlighted(componentKey, 9, 1, 9, 6, TypeOfText.ANNOTATION); // @Beta
    assertThatHasBeenHighlighted(componentKey, 10, 1, 10, 9, TypeOfText.KEYWORD); // abstract
    assertThatHasBeenHighlighted(componentKey, 10, 10, 10, 15, TypeOfText.KEYWORD); // class
    assertThatHasNotBeenHighlighted(componentKey, 10, 16, 10, 22); // module
    assertThatHasNotBeenHighlighted(componentKey, 11, 10, 11, 14); // open
    assertThatHasNotBeenHighlighted(componentKey, 11, 16, 11, 26); // transitive
    assertThatHasBeenHighlighted(componentKey, 13, 3, 13, 7, TypeOfText.KEYWORD); // void
    assertThatHasNotBeenHighlighted(componentKey, 13, 8, 13, 16); // requires
    assertThatHasNotBeenHighlighted(componentKey, 13, 24, 13, 31); // exports
    assertThatHasNotBeenHighlighted(componentKey, 13, 40, 13, 45); // opens
    assertThatHasBeenHighlighted(componentKey, 14, 5, 14, 8, TypeOfText.KEYWORD); // int
    assertThatHasNotBeenHighlighted(componentKey, 14, 9, 14, 11); // to
    assertThatHasBeenHighlighted(componentKey, 15, 5, 15, 11, TypeOfText.KEYWORD); // double
    assertThatHasNotBeenHighlighted(componentKey, 15, 12, 15, 16); // with
    assertThatHasNotBeenHighlighted(componentKey, 16, 12, 16, 16); // uses
    assertThatHasNotBeenHighlighted(componentKey, 17, 5, 17, 13); // provides
    assertThatHasBeenHighlighted(componentKey, 20, 3, 20, 11, TypeOfText.KEYWORD); // abstract
    assertThatHasBeenHighlighted(componentKey, 20, 12, 20, 16, TypeOfText.KEYWORD); // void
    assertThatHasNotBeenHighlighted(componentKey, 20, 17, 20, 25); // provides
  }

  @Test
  public void test_java10_var() throws Exception {
    this.eol = "\n";
    InputFile inputFile = generateTestFile("src/test/files/highlighter/Java10Var.java");
    scan(inputFile);

    String componentKey = inputFile.key();
    assertThatHasBeenHighlighted(componentKey, 10, 5, 10, 8, TypeOfText.KEYWORD); // var a = ...
    assertThatHasBeenHighlighted(componentKey, 12, 5, 12, 8, TypeOfText.KEYWORD); // var list = ...
    assertThatHasBeenHighlighted(componentKey, 17, 10, 17, 13, TypeOfText.KEYWORD); // for (var counter = ...
    assertThatHasBeenHighlighted(componentKey, 21, 10, 21, 13, TypeOfText.KEYWORD); // for (var value : ...
    assertThatHasBeenHighlighted(componentKey, 27, 10, 27, 13, TypeOfText.KEYWORD); // try (var reader = ...
    assertThatHasBeenHighlighted(componentKey, 32, 5, 32, 8, TypeOfText.KEYWORD); // var myA = new A() { ...
    assertThatHasNotBeenHighlighted(componentKey, 51, 12, 51, 15); // Object var;
  }

  private void scan(InputFile inputFile) {
    JavaSquid squid = new JavaSquid(new JavaVersionImpl(), null, null, null, null, new JavaCheck[] {syntaxHighlighterVisitor});
    squid.scan(Collections.singletonList(inputFile), Collections.emptyList());
  }

  private InputFile generateDefaultTestFile() throws IOException {
    return generateTestFile("src/test/files/highlighter/Example.java");
  }

  private InputFile generateTestFile(String sourceFile) throws IOException {
    File source = new File(sourceFile);
    File target = new File(temp.newFolder(), source.getName()).getAbsoluteFile();
    String content = Files.asCharSource(source, StandardCharsets.UTF_8)
      .read()
      .replaceAll("\\r\\n", "\n")
      .replaceAll("\\r", "\n")
      .replaceAll("\\n", eol);
    Files.asCharSink(target, StandardCharsets.UTF_8).write(content);
    return TestUtils.inputFile(target);
  }

  private void verifyHighlighting(InputFile inputFile) throws IOException {
    String componentKey = inputFile.key();
    assertThatHasBeenHighlighted(componentKey, 1, 1, 3, 4, TypeOfText.COMMENT);
    assertThatHasBeenHighlighted(componentKey, 5, 1, 7, 4, TypeOfText.STRUCTURED_COMMENT);
    assertThatHasBeenHighlighted(componentKey, 8, 1, 8, 18, TypeOfText.ANNOTATION);
    assertThatHasBeenHighlighted(componentKey, 8, 19, 8, 27, TypeOfText.STRING);
    assertThatHasBeenHighlighted(componentKey, 9, 1, 9, 6, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 11, 3, 11, 24, TypeOfText.ANNOTATION);
    assertThatHasBeenHighlighted(componentKey, 12, 3, 12, 6, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 13, 5, 13, 11, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 13, 12, 13, 14, TypeOfText.CONSTANT);
    assertThatHasBeenHighlighted(componentKey, 18, 1, 18, 18, TypeOfText.COMMENT);
    assertThatHasBeenHighlighted(componentKey, 19, 1, 19, 11, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 20, 21, 20, 28, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 20, 29, 20, 30, TypeOfText.CONSTANT);
    assertThatHasBeenHighlighted(componentKey, 23, 1, 23, 10, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 24, 3, 24, 7, TypeOfText.KEYWORD);
  }

  private void assertThatHasBeenHighlighted(String componentKey, int startLine, int startColumn, int endLine, int endColumn, TypeOfText expected) {
    assertThat(context.highlightingTypeAt(componentKey, startLine, startColumn - 1)).hasSize(1).contains(expected);
    // -1 because of offset (column start at 0) and -1 to be within the range.
    assertThat(context.highlightingTypeAt(componentKey, endLine, endColumn - 1 - 1)).hasSize(1).contains(expected);
  }

  private void assertThatHasNotBeenHighlighted(String componentKey, int startLine, int startColumn, int endLine, int endColumn) {
    assertThat(context.highlightingTypeAt(componentKey, startLine, startColumn - 1)).isEmpty();
    // -1 because of offset (column start at 0) and -1 to be within the range.
    assertThat(context.highlightingTypeAt(componentKey, endLine, endColumn - 1 - 1)).isEmpty();
  }

}
