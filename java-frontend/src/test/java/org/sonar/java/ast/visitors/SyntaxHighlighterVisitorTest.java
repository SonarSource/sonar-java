/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.java.JavaTestClasspath;
import org.sonar.java.SonarComponents;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SyntaxHighlighterVisitorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private SensorContextTester context;
  private DefaultFileSystem fs;
  private SonarComponents sonarComponents;

  private SyntaxHighlighterVisitor syntaxHighlighterVisitor;

  private List<String> lines;
  private String eol;

  @Before
  public void setUp() throws Exception {
    context = SensorContextTester.create(temp.getRoot());
    fs = context.fileSystem();
    sonarComponents = new SonarComponents(mock(FileLinesContextFactory.class), fs,
      mock(JavaClasspath.class), mock(JavaTestClasspath.class), mock(CheckFactory.class));
    sonarComponents.setSensorContext(context);
    syntaxHighlighterVisitor = new SyntaxHighlighterVisitor(sonarComponents);
  }

  @Test
  public void parse_error() throws Exception {
    SensorContextTester spy = spy(context);
    File file = temp.newFile();
    Files.write("ParseError", file, StandardCharsets.UTF_8);
    fs.add(new DefaultInputFile("", file.getName()));
    scan(file);
    verify(spy, never()).newHighlighting();
  }

  @Test
  public void test_LF() throws Exception {
    this.eol = "\n";
    File file = generateTestFile();
    scan(file);
    verifyHighlighting(file);
  }

  @Test
  public void test_CR_LF() throws Exception {
    this.eol = "\r\n";
    File file = generateTestFile();
    scan(file);
    verifyHighlighting(file);
  }

  @Test
  public void test_CR() throws Exception {
    this.eol = "\r";
    File file = generateTestFile();
    scan(file);
    verifyHighlighting(file);
  }

  private void scan(File file) {
    JavaSquid squid = new JavaSquid(new JavaConfiguration(StandardCharsets.UTF_8), null, null, null, null, new CodeVisitor[] {syntaxHighlighterVisitor});
    squid.scan(Lists.newArrayList(file), Collections.<File>emptyList());
  }

  private File generateTestFile() throws IOException {
    File file = temp.newFile();
    Files.write(Files.toString(new File("src/test/files/highlighter/Example.java"), StandardCharsets.UTF_8).replaceAll("\\r\\n", "\n").replaceAll("\\n", eol), file, StandardCharsets.UTF_8);
    lines = Files.readLines(file, StandardCharsets.UTF_8);
    String content  = Joiner.on(eol).join(lines);
    fs.add(new DefaultInputFile("", file.getName()).initMetadata(content));
    return file;
  }

  private void verifyHighlighting(File file) throws IOException {
    String componentKey = ":" + file.getName();
    assertThatHasBeenHighlighted(componentKey, 1, 1, 3, 4, TypeOfText.COMMENT);
    assertThatHasBeenHighlighted(componentKey, 5, 1, 7, 4, TypeOfText.COMMENT);
    assertThatHasBeenHighlighted(componentKey, 8, 1, 8, 18, TypeOfText.ANNOTATION);
    assertThatHasBeenHighlighted(componentKey, 8, 19, 8, 27, TypeOfText.STRING);
    assertThatHasBeenHighlighted(componentKey, 9, 1, 9, 6, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 11, 3, 11, 24, TypeOfText.ANNOTATION);
    assertThatHasBeenHighlighted(componentKey, 12, 3, 12, 6, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 13, 5, 13, 11, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 13, 12, 13, 14, TypeOfText.CONSTANT);
    assertThatHasBeenHighlighted(componentKey, 18, 1, 18, 18, TypeOfText.COMMENT);
    assertThatHasBeenHighlighted(componentKey, 19, 2, 19, 11, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 20, 21, 20, 28, TypeOfText.KEYWORD);
    assertThatHasBeenHighlighted(componentKey, 20, 29, 20, 30, TypeOfText.CONSTANT);
  }

  private void assertThatHasBeenHighlighted(String componentKey, int startLine, int startColumn, int endLine, int endColumn, TypeOfText expected) {
    assertThat(context.highlightingTypeAt(componentKey, startLine, startColumn - 1)).hasSize(1).contains(expected);
    // -1 because of offset (column start at 0) and -1 to be within the range.
    assertThat(context.highlightingTypeAt(componentKey, endLine, endColumn - 1 - 1)).hasSize(1).contains(expected);
  }

}
