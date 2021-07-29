/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.reporting;

import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaTextEdit {
  private final AnalyzerMessage.TextSpan textSpan;
  private final String replacement;

  private JavaTextEdit(AnalyzerMessage.TextSpan textSpan, String replacement) {
    this.textSpan = textSpan;
    this.replacement = replacement;
  }

  public AnalyzerMessage.TextSpan getTextSpan() {
    return textSpan;
  }

  public String getReplacement() {
    return replacement;
  }

  public static JavaTextEdit removeTree(Tree tree) {
    return removeTextSpan(AnalyzerMessage.textSpanFor(tree));
  }

  public static JavaTextEdit removeTextSpan(AnalyzerMessage.TextSpan textSpan) {
    return new JavaTextEdit(textSpan, "");
  }

  public static JavaTextEdit replaceTree(Tree tree, String replacement) {
    return replaceTextSpan(AnalyzerMessage.textSpanFor(tree), replacement);
  }

  /**
   * From startTree first token to endTree last token.
   */
  public static JavaTextEdit replaceBetweenTree(Tree startTree, Tree endTree, String replacement) {
    return replaceTextSpan(AnalyzerMessage.textSpanBetween(startTree, endTree), replacement);
  }

  public static JavaTextEdit replaceTextSpan(AnalyzerMessage.TextSpan textSpan, String replacement) {
    return new JavaTextEdit(textSpan, replacement);
  }

  public static JavaTextEdit insertAfterTree(Tree tree, String addition) {
    SyntaxToken lastToken = tree.lastToken();
    if (lastToken == null) {
      throw new IllegalStateException("Trying to insert a quick fix after a Tree without token.");
    }
    return insertAtPosition(lastToken.line(), lastToken.column() + lastToken.text().length(), addition);
  }

  public static JavaTextEdit insertBeforeTree(Tree tree, String addition) {
    SyntaxToken firstToken = tree.firstToken();
    if (firstToken == null) {
      throw new IllegalStateException("Trying to insert a quick fix before a Tree without token.");
    }
    return insertAtPosition(firstToken.line(), firstToken.column(), addition);
  }

  public static JavaTextEdit insertAtPosition(int line, int column, String addition) {
    return new JavaTextEdit(position(line, column), addition);
  }

  public static AnalyzerMessage.TextSpan position(int line, int column) {
    return textSpan(line, column, line, column);
  }

  public static AnalyzerMessage.TextSpan textSpan(int startLine, int startColumn, int endLine, int endColumn) {
    return new AnalyzerMessage.TextSpan(startLine, startColumn, endLine, endColumn);
  }
}
