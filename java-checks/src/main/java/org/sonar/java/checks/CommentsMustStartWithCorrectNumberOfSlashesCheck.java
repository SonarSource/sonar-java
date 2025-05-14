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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S7476")
public class CommentsMustStartWithCorrectNumberOfSlashesCheck extends IssuableSubscriptionVisitor {
  private static final String BEFORE_JAVA_23 = "A single-line comment should start with exactly two slashes, no more.";
  private static final String AFTER_JAVA_23 = "Markdown documentation should start with exactly three slashes, no more.";
  private static final String INCORRECT_SLASHES_BEFORE_JAVA_23 = "///";
  private static final String INCORRECT_SLASHES_AFTER_JAVA_23 = "////";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.TRIVIA);
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    if (syntaxTrivia.isComment(SyntaxTrivia.CommentKind.LINE) && syntaxTrivia.comment().startsWith(INCORRECT_SLASHES_BEFORE_JAVA_23)) {
      var span = LineSpan.fromComment(syntaxTrivia, 0, 0, INCORRECT_SLASHES_BEFORE_JAVA_23.length());
      reportIssue(span, BEFORE_JAVA_23);
    }

    if (syntaxTrivia.isComment(SyntaxTrivia.CommentKind.MARKDOWN)) {
      String[] lines = syntaxTrivia.comment().split("\\R");
      for (int idx = 0; idx < lines.length; idx++) {
        String line = lines[idx];

        if (line.trim().startsWith(INCORRECT_SLASHES_AFTER_JAVA_23)) {
          int startPos = line.indexOf(INCORRECT_SLASHES_AFTER_JAVA_23);
          var span = LineSpan.fromComment(syntaxTrivia, idx, startPos, startPos + INCORRECT_SLASHES_AFTER_JAVA_23.length());
          reportIssue(span, AFTER_JAVA_23);
        }
      }
    }
  }

  private void reportIssue(LineSpan span, String message) {
    ((DefaultModuleScannerContext) this.context).reportIssue(issueSingleLine(span,message));
  }

  private AnalyzerMessage issueSingleLine(LineSpan span, String message) {
    var textSpan = new AnalyzerMessage.TextSpan(span.line, span.start, span.line, span.end);
    return new AnalyzerMessage(this, context.getInputFile(), textSpan, message, 0);
  }

  /*
   * Represents a span of text within a single line, defined by its line number and start/end positions.
   *
   * @param line The line number where the span is located (1-based index).
   * 
   * @param start The starting column position of the span (0-based index).
   * 
   * @param end The ending column position of the span (0-based index, exclusive).
   *
   * example for line below:
   * /// a comment
   * ^^^
   * the LineSpan corresponds to the caret is LineSpan(aLine, 0, 3)
   */
  private record LineSpan(int line, int start, int end) {
    public static LineSpan fromComment(SyntaxTrivia comment, int line, int startChar, int endChar) {
      int sourceLine = comment.range().start().line() + line;
      if (line == 0) {
        int offset = comment.range().start().columnOffset();
        return new LineSpan(sourceLine, offset + startChar, offset + endChar);
      } else {
        return new LineSpan(sourceLine, startChar, endChar);
      }
    }
  }

}
