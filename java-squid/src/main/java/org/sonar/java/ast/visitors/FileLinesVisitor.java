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

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.java.SonarComponents;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

/**
 * Saves information about lines directly into Sonar by using {@link FileLinesContext}.
 */
public class FileLinesVisitor extends JavaAstVisitor implements AstAndTokenVisitor {

  private final SonarComponents sonarComponents;
  private final Charset charset;
  private final Set<Integer> linesOfCode = Sets.newHashSet();
  private final Set<Integer> linesOfComments = Sets.newHashSet();

  public FileLinesVisitor(SonarComponents sonarComponents, Charset charset) {
    this.sonarComponents = sonarComponents;
    this.charset = charset;
  }

  @Override
  public void leaveFile(AstNode astNode) {
    FileLinesContext fileLinesContext = sonarComponents.fileLinesContextFor(getContext().getFile());

    // TODO minimize access to files, another one in LinesVisitor
    int fileLength;
    try {
      fileLength = Files.readLines(getContext().getFile(), charset).size();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
    for (int line = 1; line <= fileLength; line++) {
      fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, linesOfCode.contains(line) ? 1 : 0);
      fileLinesContext.setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, line, linesOfComments.contains(line) ? 1 : 0);
    }
    fileLinesContext.save();

    linesOfCode.clear();
    linesOfComments.clear();
  }

  @Override
  public void visitToken(Token token) {
    if (token.getType().equals(GenericTokenType.EOF)) {
      return;
    }

    linesOfCode.add(token.getLine());
    List<Trivia> trivias = token.getTrivia();
    for (Trivia trivia : trivias) {
      if (trivia.isComment()) {
        int baseLine = trivia.getToken().getLine();
        String[] lines = trivia.getToken().getOriginalValue().split("(\r)?\n|\r", -1);
        for (int i = 0; i < lines.length; i++) {
          linesOfComments.add(baseLine + i);
        }
      }
    }
  }

}
