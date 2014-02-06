/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.ast.visitors;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.source.Highlightable;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.SonarComponents;

import java.io.File;
import java.util.List;

public class SyntaxHighlighterVisitorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final SonarComponents sonarComponents = Mockito.mock(SonarComponents.class);
  private final Highlightable highlightable = Mockito.mock(Highlightable.class);
  private final Highlightable.HighlightingBuilder highlighting = Mockito.mock(Highlightable.HighlightingBuilder.class);

  private final SyntaxHighlighterVisitor syntaxHighlighterVisitor = new SyntaxHighlighterVisitor(sonarComponents, Charsets.UTF_8);

  private List<String> lines;
  private String eol;

  @Before
  public void setUp() {
    Mockito.when(sonarComponents.highlightableFor(Mockito.any(File.class))).thenReturn(highlightable);
    Mockito.when(highlightable.newHighlighting()).thenReturn(highlighting);
  }

  @Test
  public void parse_error() throws Exception {
    File file = temp.newFile();
    Files.write("ParseError", file, Charsets.UTF_8);
    JavaAstScanner.scanSingleFile(file, syntaxHighlighterVisitor);

    Mockito.verifyZeroInteractions(highlightable);
  }

  @Test
  public void test_LF() throws Exception {
    test("\n");
  }

  @Test
  public void test_CR_LF() throws Exception {
    test("\r\n");
  }

  @Test
  public void test_CR() throws Exception {
    test("\r");
  }

  private void test(String eol) throws Exception {
    this.eol = eol;
    File file = temp.newFile();
    Files.write(Files.toString(new File("src/test/files/highlighter/Example.java"), Charsets.UTF_8).replaceAll("\\r\\n", "\n").replaceAll("\\n", eol), file, Charsets.UTF_8);

    JavaAstScanner.scanSingleFile(file, syntaxHighlighterVisitor);

    lines = Files.readLines(file, Charsets.UTF_8);
    Mockito.verify(highlighting).highlight(offset(1, 1), offset(3, 4), "cppd");
    Mockito.verify(highlighting).highlight(offset(5, 1), offset(7, 4), "cppd");
    Mockito.verify(highlighting).highlight(offset(8, 1), offset(8, 18), "a");
    Mockito.verify(highlighting).highlight(offset(8, 19), offset(8, 27), "s");
    Mockito.verify(highlighting).highlight(offset(9, 1), offset(9, 7), "k");
    Mockito.verify(highlighting).highlight(offset(11, 3), offset(11, 7), "k");
    Mockito.verify(highlighting).highlight(offset(12, 5), offset(12, 12), "k");
    Mockito.verify(highlighting).highlight(offset(12, 12), offset(12, 14), "c");
    Mockito.verify(highlighting).done();
    Mockito.verifyNoMoreInteractions(highlighting);
  }

  private int offset(int line, int column) {
    int result = 0;
    for (int i = 0; i < line - 1; i++) {
      result += lines.get(i).length() + eol.length();
    }
    result += column - 1;
    return result;
  }

}
