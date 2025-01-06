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
package org.sonar.java.reporting;

import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaTextEdit {
  private final TextSpan textSpan;
  private final String replacement;

  private JavaTextEdit(TextSpan textSpan, String replacement) {
    this.textSpan = textSpan;
    this.replacement = replacement;
  }

  public TextSpan getTextSpan() {
    return textSpan;
  }

  public String getReplacement() {
    return replacement;
  }

  public static JavaTextEdit removeTree(Tree tree) {
    return removeTextSpan(AnalyzerMessage.textSpanFor(tree));
  }

  public static JavaTextEdit removeTextSpan(TextSpan textSpan) {
    return new JavaTextEdit(textSpan, "");
  }

  public static JavaTextEdit removeBetweenTree(Tree startTree, Tree endTree) {
    return replaceBetweenTree(startTree, endTree, "");
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
  
  public static JavaTextEdit replaceBetweenTree(Tree startTree, boolean includeStart, Tree endTree, boolean includeEnd, String replacement) {
    return replaceTextSpan(AnalyzerMessage.textSpanBetween(startTree, includeStart, endTree, includeEnd), replacement);
  }

  public static JavaTextEdit replaceTextSpan(TextSpan textSpan, String replacement) {
    return new JavaTextEdit(textSpan, replacement);
  }

  public static JavaTextEdit insertAfterTree(Tree tree, String addition) {
    SyntaxToken lastToken = tree.lastToken();
    if (lastToken == null) {
      throw new IllegalStateException("Trying to insert a quick fix after a Tree without token.");
    }
    Position lastPosition = Position.endOf(lastToken);
    return insertAtPosition(lastPosition.line(), lastPosition.columnOffset(), addition);
  }

  public static JavaTextEdit insertBeforeTree(Tree tree, String addition) {
    SyntaxToken firstToken = tree.firstToken();
    if (firstToken == null) {
      throw new IllegalStateException("Trying to insert a quick fix before a Tree without token.");
    }
    Position firstPosition = Position.startOf(firstToken);
    return insertAtPosition(firstPosition.line(), firstPosition.columnOffset(), addition);
  }

  public static JavaTextEdit insertAtPosition(int line, int column, String addition) {
    return new JavaTextEdit(position(line, column), addition);
  }

  public static TextSpan position(int line, int column) {
    return textSpan(line, column, line, column);
  }

  public static TextSpan textSpan(int startLine, int startColumn, int endLine, int endColumn) {
    return new TextSpan(startLine, startColumn, endLine, endColumn);
  }
}
