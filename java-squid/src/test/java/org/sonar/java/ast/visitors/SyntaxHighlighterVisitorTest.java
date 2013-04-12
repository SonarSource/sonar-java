/*
 * Sonar Java
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.scan.source.Highlightable;
import org.sonar.java.JavaAstScanner;

import java.io.File;

public class SyntaxHighlighterVisitorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test() throws Exception {
    ResourcePerspectives resourcePerspectives = Mockito.mock(ResourcePerspectives.class);
    Highlightable highlightable = Mockito.mock(Highlightable.class);
    Mockito.when(resourcePerspectives.as(Mockito.eq(Highlightable.class), Mockito.any(JavaFile.class))).thenReturn(highlightable);
    Highlightable.HighlightingBuilder highlighting = Mockito.mock(Highlightable.HighlightingBuilder.class);
    Mockito.when(highlightable.newHighlighting()).thenReturn(highlighting);

    SyntaxHighlighterVisitor syntaxHighlighterVisitor = new SyntaxHighlighterVisitor(resourcePerspectives, Charsets.UTF_8);
    File file = temp.newFile();
    Files.write(Files.toString(new File("src/test/files/highlighter/Example.java"), Charsets.UTF_8).replaceAll("\\r\\n", "\n"), file, Charsets.UTF_8);
    JavaAstScanner.scanSingleFile(file, syntaxHighlighterVisitor);

    Mockito.verify(highlighting).highlight(0, 16, "cppd");
    Mockito.verify(highlighting).highlight(18, 36, "cppd");
    Mockito.verify(highlighting).highlight(37, 54, "a");
    Mockito.verify(highlighting).highlight(55, 63, "s");
    Mockito.verify(highlighting).highlight(65, 71, "k");
    Mockito.verify(highlighting).highlight(84, 88, "k");
    Mockito.verify(highlighting).highlight(103, 110, "k");
    Mockito.verify(highlighting).highlight(110, 112, "c");
    Mockito.verify(highlighting).done();
    Mockito.verifyNoMoreInteractions(highlighting);
  }

}
