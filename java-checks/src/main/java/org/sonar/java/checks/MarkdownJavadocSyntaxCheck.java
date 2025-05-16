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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.model.LineColumnConverter;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S7474")
public class MarkdownJavadocSyntaxCheck extends IssuableSubscriptionVisitor {

  /**
   * Pattern to find Javadoc or HTML tags that can be replaced with Markdown.
   */
  @VisibleForTesting
  static final Pattern NON_MARKDOWN_JAVADOC_PATTERN = Pattern.compile("<b>|<i>|<p>|<pre>|<ul>|<ol>|<table>|\\{@code |\\{@link ");

  private static final Pattern TRIPLE_QUOTE = Pattern.compile("```");
  private static final String MESSAGE = "replace HTML syntax with Markdown syntax in javadoc";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(PublicApiChecker.apiKinds());
  }

  @Override
  public void visitNode(Tree tree) {
    List<SyntaxTrivia> markdownJavadoc =
      Optional.ofNullable(tree.firstToken())
        .stream()
        .flatMap(token -> token.trivias().stream())
        .filter(trivia -> trivia.isComment(SyntaxTrivia.CommentKind.MARKDOWN))
        .toList();

    for (SyntaxTrivia trivia : markdownJavadoc) {
      String withoutQuotedCode = replaceQuotedCodeWithBlanks(trivia.comment());
      Matcher matcher = NON_MARKDOWN_JAVADOC_PATTERN.matcher(withoutQuotedCode);
      LineColumnConverter lineColumnConverter = new LineColumnConverter(withoutQuotedCode);
      if (matcher.find()) {
        Position startPosition = lineColumnConverter.toPosition(matcher.start());
        int endIndex = endIndexOfTag(matcher, withoutQuotedCode);
        Position endPosition = lineColumnConverter.toPosition(endIndex);
        reportNonMarkdownSyntax(trivia, startPosition, endPosition);
      }
    }
  }

  @VisibleForTesting
  static int endIndexOfTag(Matcher matcher, String comment) {
    if (!matcher.group().startsWith("{")) {
      return matcher.end();
    }
    int index = indexOfClosingBracket(comment, matcher.end());
    if (index == -1) {
      return comment.length();
    }
    return index + 1;
  }

  private static int indexOfClosingBracket(String comment, int fromIndex) {
    int unclosedBrackets = 1;
    for (int i = fromIndex; i < comment.length(); i++) {
      if (comment.charAt(i) == '{') {
        unclosedBrackets++;
      } else if (comment.charAt(i) == '}') {
        unclosedBrackets--;
      }
      if (unclosedBrackets == 0) {
        return i;
      }
    }
    return -1;
  }

  void reportNonMarkdownSyntax(SyntaxTrivia trivia, Position start, Position end) {
    Position triviaPosition = trivia.range().start();
    Position absoluteStart = start.relativeTo(triviaPosition);
    Position absoluteEnd = end.relativeTo(triviaPosition);
    var textSpan = new AnalyzerMessage.TextSpan(
      absoluteStart.line(), absoluteStart.columnOffset(),
      absoluteEnd.line(), absoluteEnd.columnOffset());
    ((DefaultModuleScannerContext) this.context).reportIssue(
      new AnalyzerMessage(this, context.getInputFile(), textSpan, MESSAGE, 0));
  }

  /**
   * Return a new string, where parts of the text that are between backquotes ({@code `}) and triple backquote ({@code ```}),
   * are replaced with blank spaces, while preserving the number of characters and line numbers.
   */
  @VisibleForTesting
  static String replaceQuotedCodeWithBlanks(String javadoc) {
    StringBuilder result = new StringBuilder();
    int currentPosition = 0;
    while (currentPosition != -1) {
      int nextQuote = javadoc.indexOf("`", currentPosition);
      if (nextQuote != -1) {
        result.append(javadoc, currentPosition, nextQuote);
        currentPosition = findEndOfMarkdownQuote(javadoc, nextQuote);
        int endOfQuote = currentPosition == -1 ? javadoc.length() : currentPosition;
        // Replace all printable characters by spaces, so that they can't be interpreted as tags.
        // Don't replace non-printable characters, as this could interfere with line counting.
        result.append(javadoc.substring(nextQuote, endOfQuote)
          .replaceAll("\\p{Print}", " "));
      } else {
        result.append(javadoc, currentPosition, javadoc.length());
        currentPosition = -1;
      }
    }
    return result.toString();
  }

  @VisibleForTesting
  static int findEndOfMarkdownQuote(String javadoc, int startPosition) {
    Matcher matcher = TRIPLE_QUOTE.matcher(javadoc);
    matcher.region(startPosition, javadoc.length());
    if (matcher.lookingAt()) {
      boolean closingQuotesFound = matcher.find(startPosition + 3);
      return !closingQuotesFound ? -1 : matcher.end();
    } else {
      int closingQuotePosition = javadoc.indexOf("`", startPosition + 1);
      return closingQuotePosition == -1 ? -1 : (closingQuotePosition + 1);
    }
  }
}
