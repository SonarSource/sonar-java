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

import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.sonar.sslr.api.*;
import org.sonar.api.batch.SquidUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.JavaFile;
import org.sonar.squid.api.SourceFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

/**
 * Saves information about lines directly into Sonar by using {@link FileLinesContext}.
 */
public class FileLinesVisitor extends JavaAstVisitor implements AstAndTokenVisitor {

  private final FileLinesContextFactory fileLinesContextFactory;
  private final Charset charset;
  private final Set<Integer> linesOfCode = Sets.newHashSet();
  private final Set<Integer> linesOfComments = Sets.newHashSet();

  public FileLinesVisitor(FileLinesContextFactory fileLinesContextFactory, Charset charset) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.charset = charset;
  }

  @Override
  public void leaveFile(AstNode astNode) {
    SourceFile file = (SourceFile) getContext().peekSourceCode();
    JavaFile javaFile = SquidUtils.convertJavaFileKeyFromSquidFormat(file.getKey());
    FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(javaFile);

    // TODO minimize access to files, another one in LinesVisitor
    int fileLength;
    try {
      fileLength = Files.readLines(getContext().getFile(), charset).size();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    for (int line = 1; line <= fileLength; line++) {
      fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, linesOfCode.contains(line) ? 1 : 0);
      fileLinesContext.setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, line, linesOfComments.contains(line) ? 1 : 0);
    }
    fileLinesContext.save();

    linesOfCode.clear();
    linesOfComments.clear();
  }

  public void visitToken(Token token) {
    if (token.getType().equals(GenericTokenType.EOF)) {
      return;
    }

    linesOfCode.add(token.getLine());
    List<Trivia> trivias = token.getTrivia();
    for (Trivia trivia : trivias) {
      if (trivia.isComment()) {
        linesOfComments.add(trivia.getToken().getLine());
      }
    }
  }

}
