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

import com.sonar.sslr.api.AstNode;
import org.junit.Test;
import org.sonar.squidbridge.SquidAstVisitorContextImpl;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.measures.MetricDef;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileVisitorTest {

  @Test
  public void test() {
    SquidAstVisitorContextImpl context = new SquidAstVisitorContextImpl(new SourceProject(""));
    FileVisitor visitor = new FileVisitor();
    visitor.setContext(context);

    AstNode astNode = mock(AstNode.class);
    File file = mock(File.class);
    when(file.getAbsolutePath()).thenReturn("/some/path");
    when(file.getPath()).thenReturn("/some/other/path");
    context.setFile(file, mock(MetricDef.class));

    assertThat(context.peekSourceCode() instanceof SourceFile).isTrue();

    visitor.visitFile(astNode);
    assertThat(context.peekSourceCode() instanceof SourceFile).isTrue();
    SourceFile sourceFile = (SourceFile) context.peekSourceCode();
    assertThat(sourceFile.getKey()).isEqualTo("/some/path");
    assertThat(sourceFile.getName()).isEqualTo("/some/other/path");

    visitor.leaveFile(astNode);
    assertThat(context.peekSourceCode() instanceof SourceFile).isTrue();
  }

}
