/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

/**
 * Saves information about lines directly into Sonar by using {@link FileLinesContext}.
 */
public class FileLinesVisitor extends SubscriptionVisitor {

  private final SonarComponents sonarComponents;
  private final Charset charset;
  private final Set<Integer> linesOfCode = Sets.newHashSet();
  private final Set<Integer> linesOfComments = Sets.newHashSet();

  public FileLinesVisitor(SonarComponents sonarComponents, Charset charset) {
    this.sonarComponents = sonarComponents;
    this.charset = charset;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TOKEN);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);

    FileLinesContext fileLinesContext = sonarComponents.fileLinesContextFor(context.getFile());
    // TODO minimize access to files, another one in LinesVisitor
    int fileLength;
    try {
      fileLength = Files.readLines(context.getFile(), charset).size();
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
  public void visitToken(SyntaxToken syntaxToken) {
    linesOfCode.add(syntaxToken.line());
    for (SyntaxTrivia trivia : syntaxToken.trivias()) {
      int baseLine = trivia.startLine();
      String[] lines = trivia.comment().split("(\r)?\n|\r", -1);
      for (int i = 0; i < lines.length; i++) {
        linesOfComments.add(baseLine + i);
      }
    }
  }
}
