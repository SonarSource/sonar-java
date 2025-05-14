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
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S7476")
public class CommentsMustStartWithCorrectNumberOfSlashesCheck extends IssuableSubscriptionVisitor {
  private DefaultJavaFileScannerContext context;
  private final static String BEFORE_JAVA_23 = "Do not use more than two slashes in a comment.";
  private final static String AFTER_JAVA_23 = "Do not use more than three slashes in a comment.";
  private final static String SLASHES_BEFORE_JAVA_23 = "///";
  private final static String SLASHES_AFTER_JAVA_23 = "////";

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    this.context = (DefaultJavaFileScannerContext) context;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.TRIVIA);
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    if (syntaxTrivia.isComment(SyntaxTrivia.CommentKind.LINE) && syntaxTrivia.comment().trim().startsWith(SLASHES_BEFORE_JAVA_23)) {
      var span = LineSpan.fromComment(syntaxTrivia, 0, 0, SLASHES_BEFORE_JAVA_23.length());
      context.reportIssue(issueSingleLine(LineSpan.fromComment(syntaxTrivia, 0, 0, SLASHES_BEFORE_JAVA_23.length()), BEFORE_JAVA_23));
    }

    if (syntaxTrivia.isComment(SyntaxTrivia.CommentKind.MARKDOWN)) {
      String[] lines = syntaxTrivia.comment().split("\\R");
      for (int idx = 0; idx < lines.length; idx++) {
        String line = lines[idx];

        if (line.trim().startsWith(SLASHES_AFTER_JAVA_23)) {
          int startPos = line.indexOf(SLASHES_AFTER_JAVA_23);
          var span = LineSpan.fromComment(syntaxTrivia, idx, startPos, startPos + SLASHES_AFTER_JAVA_23.length());
          context.reportIssue(issueSingleLine(span, AFTER_JAVA_23));
        }
      }
    }
  }

  private AnalyzerMessage issueSingleLine(LineSpan span, String message) {
    AnalyzerMessage.TextSpan textSpan = new AnalyzerMessage.TextSpan(span.line, span.start, span.line, span.end);
    return new AnalyzerMessage(this, context.getInputFile(), textSpan, message, 0);
  }

  private record LineSpan(int line, int start, int end) {
    public static LineSpan fromComment(SyntaxTrivia comment, int line, int start, int end) {
      int sourceLine = comment.range().start().line() + line;
      if (sourceLine > comment.range().end().line()) {
        throw new IllegalArgumentException("Invalid line span");
      }
      if (line == 0) {
        int offset = comment.range().start().columnOffset();
        return new LineSpan(sourceLine, offset + start, offset + end);
      } else {
        return new LineSpan(sourceLine, start, end);
      }
    }
  }

}
