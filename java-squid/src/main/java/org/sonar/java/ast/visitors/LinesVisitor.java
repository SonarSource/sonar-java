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

import com.google.common.io.Files;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaMetric;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Workaround for SSLRSQBR-10
 */
public class LinesVisitor extends JavaAstVisitor {

  private final Charset charset;

  public LinesVisitor(Charset charset) {
    this.charset = charset;
  }

  @Override
  public void leaveFile(AstNode astNode) {
    try {
      List<String> lines = Files.readLines(getContext().getFile(), charset);
      getContext().peekSourceCode().setMeasure(JavaMetric.LINES, lines.size());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
