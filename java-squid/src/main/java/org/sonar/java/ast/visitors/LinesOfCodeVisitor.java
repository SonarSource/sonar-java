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

import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.squidbridge.api.SourceCode;

import java.util.Set;

import static com.sonar.sslr.api.GenericTokenType.EOF;

public class LinesOfCodeVisitor extends JavaAstVisitor implements AstAndTokenVisitor {

  private Set<Integer> lines = Sets.newHashSet();

  @Override
  public void init() {
    subscribeTo(JavaPunctuator.RWING);
  }

  @Override
  public void visitFile(AstNode astNode) {
    lines.clear();
  }

  public void visitToken(Token token) {
    if (token.getType() != EOF) {
      lines.add(token.getLine());
    }
  }

  @Override
  public void leaveNode(AstNode astNode) {
    SourceCode sourceCode = getContext().peekSourceCode();
    int linesOfCode = 0;
    for (int line = sourceCode.getStartAtLine(); line <= sourceCode.getEndAtLine(); line++) {
      if (lines.contains(line)) {
        linesOfCode++;
      }
    }
    sourceCode.setMeasure(JavaMetric.LINES_OF_CODE, linesOfCode);
  }

  @Override
  public void leaveFile(AstNode astNode) {
    getContext().peekSourceCode().setMeasure(JavaMetric.LINES_OF_CODE, lines.size());
    lines.clear();
  }

}
