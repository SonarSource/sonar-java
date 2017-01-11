/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.ast.visitors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.CommentAnalyser;

import java.util.List;
import java.util.Set;

public class CommentLinesVisitor extends SubscriptionVisitor {

  private Set<Integer> comments = Sets.newHashSet();
  private Set<Integer> noSonarLines = Sets.newHashSet();
  private boolean seenFirstToken;
  private JavaCommentAnalyser commentAnalyser = new JavaCommentAnalyser();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TOKEN);
  }

  public void analyzeCommentLines(CompilationUnitTree tree) {
    comments.clear();
    noSonarLines.clear();
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
    String[] commentLines = commentAnalyser.getContents(trivia.comment()).split("(\r)?\n|\r", -1);
    int line = trivia.startLine();
    for (String commentLine : commentLines) {
      if(commentLine.contains("NOSONAR")) {
        noSonarLines.add(line);
      } else if (!commentAnalyser.isBlank(commentLine)) {
        comments.add(line);
      }
      line++;
    }
  }

  public Set<Integer> noSonarLines() {
    return noSonarLines;
  }

  public int commentLinesMetric() {
    return comments.size();
  }

  public static class JavaCommentAnalyser extends CommentAnalyser {

    @Override
    public boolean isBlank(String line) {
      // Implementation of this method was taken from org.sonar.squidbridge.text.Line#isThereBlankComment()
      // TODO Godin: for some languages we use Character.isLetterOrDigit instead of Character.isWhitespace
      for (int i = 0; i < line.length(); i++) {
        char character = line.charAt(i);
        if (!Character.isWhitespace(character) && character != '*' && character != '/') {
          return false;
        }
      }
      return true;
    }

    @Override
    public String getContents(String comment) {
      return comment.startsWith("//") ? comment.substring(2) : comment.substring(2, comment.length() - 2);
    }
  }
}
