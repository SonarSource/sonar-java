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

import javax.annotation.Nullable;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaTextEdit {
  private final AnalyzerMessage.TextSpan textSpan;
  private final String replacement;

  private JavaTextEdit(AnalyzerMessage.TextSpan textSpan, String replacement) {
    this.textSpan = textSpan;
    this.replacement = replacement;
  }

  private JavaTextEdit(Tree tree, String replacement) {
    this(AnalyzerMessage.textSpanFor(tree), replacement);
  }

  public AnalyzerMessage.TextSpan getTextSpan() {
    return textSpan;
  }

  public String getReplacement() {
    return replacement;
  }

  public static JavaTextEdit removeTree(Tree tree) {
    return new JavaTextEdit(tree, "");
  }

  public static JavaTextEdit replaceTree(Tree tree, String replacement) {
    return new JavaTextEdit(tree, replacement);
  }

  /**
   * From startTree first token to endTree last token.
   */
  public static JavaTextEdit replaceBetweenTree(Tree startTree, Tree endTree, String replacement) {
    return new JavaTextEdit(AnalyzerMessage.textSpanBetween(startTree, endTree), replacement);
  }

  public static JavaTextEdit insertAfterTree(Tree tree, String addition) {
    return new JavaTextEdit(textSpanForToken(tree.lastToken()), addition);
  }

  public static JavaTextEdit insertBeforeTree(Tree tree, String addition) {
    return new JavaTextEdit(textSpanForToken(tree.firstToken()), addition);
  }

  private static AnalyzerMessage.TextSpan textSpanForToken(@Nullable SyntaxToken token) {
    if (token == null) {
      throw new IllegalStateException("Trying to insert a quick fix after a Tree without token.");
    }
    int line = token.line();
    int column = token.column();
    return new AnalyzerMessage.TextSpan(line, column, line, column);
  }

}
