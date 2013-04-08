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
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.scan.source.Highlightable;
import org.sonar.api.scan.source.HighlightableTextType;
import org.sonar.java.JavaAstScanner;

import java.io.File;

public class SyntaxHighlighterVisitorTest {

  @Test
  public void test() {
    ResourcePerspectives resourcePerspectives = Mockito.mock(ResourcePerspectives.class);
    Highlightable highlightable = Mockito.mock(Highlightable.class);
    Mockito.when(resourcePerspectives.as(Mockito.eq(Highlightable.class), Mockito.any(JavaFile.class))).thenReturn(highlightable);
    SyntaxHighlighterVisitor syntaxHighlighterVisitor = new SyntaxHighlighterVisitor(resourcePerspectives, Charsets.UTF_8);
    JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/Lines.java"), syntaxHighlighterVisitor);
    Mockito.verify(highlightable).highlightText(0, 16, HighlightableTextType.BLOCK_COMMENT);
    Mockito.verify(highlightable).highlightText(18, 25, HighlightableTextType.KEYWORD);
    Mockito.verify(highlightable).highlightText(25, 31, HighlightableTextType.KEYWORD);
    Mockito.verifyNoMoreInteractions(highlightable);
  }

}
