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

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.squid.api.SourceCode;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class LinesVisitor extends JavaAstVisitor {

  private final Charset charset;

  public LinesVisitor(Charset charset) {
    this.charset = charset;
  }

  @Override
  public void init() {
    subscribeTo(JavaPunctuator.RWING);
  }

  @Override
  public void visitNode(AstNode astNode) {
    SourceCode sourceCode = getContext().peekSourceCode();
    Preconditions.checkState(sourceCode.getStartAtLine() != -1 && sourceCode.getEndAtLine() != -1);
    sourceCode.setMeasure(JavaMetric.LINES, sourceCode.getEndAtLine() - sourceCode.getStartAtLine() + 1);
  }

  /**
   * Workaround for SSLRSQBR-10
   */
  @Override
  public void leaveFile(AstNode astNode) {
    if (astNode == null) {
      // TODO do not compute number of lines, when not able to parse
      return;
    }
    try {
      List<String> lines = Files.readLines(getContext().getFile(), charset);
      getContext().peekSourceCode().setMeasure(JavaMetric.LINES, lines.size());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
