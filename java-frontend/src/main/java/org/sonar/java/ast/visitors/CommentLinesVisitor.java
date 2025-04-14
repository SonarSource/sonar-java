/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.ast.visitors;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

public class CommentLinesVisitor extends SubscriptionVisitor {

  private Set<Integer> comments = new HashSet<>();
  private Set<Integer> noSonarLines = new HashSet<>();
  private Map<Path, Set<SyntaxTrivia>> syntaxTrivia = new HashMap<>();
  private boolean seenFirstToken;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TOKEN);
  }

  public void analyzeCommentLines(CompilationUnitTree tree) {
    comments.clear();
    noSonarLines.clear();
    syntaxTrivia.clear();
    seenFirstToken = false;
    scanTree(tree);
  }

  @Override
  public void visitToken(SyntaxToken syntaxToken) {
    for (SyntaxTrivia trivia : syntaxToken.trivias()) {
      if (seenFirstToken) {
        handleCommentsForTrivia(trivia);
      } else {
        seenFirstToken = true;
      }
    }
    seenFirstToken = true;
  }

  private void handleCommentsForTrivia(SyntaxTrivia trivia) {
    String[] commentLines = trivia.commentContent().split("\\R", -1);
    int line = LineUtils.startLine(trivia);
    for (String commentLine : commentLines) {
      if (commentLine.contains("NOSONAR")) {
        noSonarLines.add(line);
      } else if (!isBlank(commentLine)) {
        Path path = Path.of("");
        if (context != null) {
          path = Paths.get(context.getInputFile().uri());
        }
        syntaxTrivia.computeIfAbsent(path, k -> new HashSet<>()).add(trivia);
        comments.add(line);
      }
      line++;
    }
  }

  public Map<Path, Set<SyntaxTrivia>> getSyntaxTrivia() {
    return syntaxTrivia;
  }

  public Set<Integer> noSonarLines() {
    return noSonarLines;
  }

  public int commentLinesMetric() {
    return comments.size();
  }

  private static boolean isBlank(String line) {
    // TODO Godin: for some languages we use Character.isLetterOrDigit instead of Character.isWhitespace
    for (int i = 0; i < line.length(); i++) {
      char character = line.charAt(i);
      if (!Character.isWhitespace(character) && character != '*' && character != '/') {
        return false;
      }
    }
    return true;
  }

}
