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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S7474")
public class MarkdownJavadocSyntaxCheck extends IssuableSubscriptionVisitor {

  /**
   * Pattern to find Javadoc or HTML tags that can be replaced with Markdown.
   */
  @VisibleForTesting
  static final Pattern NON_MARKDOWN_JAVADOC_PATTERN = Pattern.compile("<b>|<i>|<p>|<pre>|<ul>|<table>|\\{@code |\\{@link ");

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
      String comment = trivia.comment();
      Matcher matcher = NON_MARKDOWN_JAVADOC_PATTERN.matcher(comment);
      for (Pair<Integer, Integer> range : rangeOfNonQuotedCode(comment)) {
        matcher.region(range.getLeft(), range.getRight());
        if (matcher.find()) {
          List<Integer> lineLengths = lineLengths(comment);
          Position startPosition = Position.ofStringIndex(matcher.start(), lineLengths);
          Position endPosition = Position.ofStringIndex(matcher.end(), lineLengths);
          reportNonMarkdownSyntax(trivia, startPosition, endPosition);
        }
      }
    }
  }

  void reportNonMarkdownSyntax(SyntaxTrivia trivia, Position start, Position end) {
    int triviaLine = trivia.range().start().line();
    int triviaColumn = trivia.range().start().columnOffset();

    int startLine = triviaLine + start.lineNumber;
    int endLine = triviaLine + end.lineNumber;

    int startColumn = start.lineNumber == 0
      ? (triviaColumn + start.columnNumber)
      : start.columnNumber;
    int endColumn = end.lineNumber == 0
      ? (triviaColumn + end.columnNumber)
      : end.columnNumber;
    var textSpan = new AnalyzerMessage.TextSpan(startLine, startColumn, endLine, endColumn);
    ((DefaultModuleScannerContext) this.context).reportIssue(
      new AnalyzerMessage(this, context.getInputFile(), textSpan, MESSAGE, 0));
  }

  /**
   * Remove from the text, parts that are between backquotes ({@code `}) and triple backquote ({@code ```}) for Markdown code.
   */
  @VisibleForTesting
  static List<Pair<Integer, Integer>> rangeOfNonQuotedCode(String javadoc) {
    List<Pair<Integer, Integer>> nonQuoted = new ArrayList<>();
    int currentPosition = 0;
    while (currentPosition != -1) {
      int nextQuote = javadoc.indexOf("`", currentPosition);
      if (nextQuote != -1) {
        nonQuoted.add(Pair.of(currentPosition, nextQuote));
        currentPosition = findEndOfMarkdownQuote(javadoc, nextQuote);
      } else {
        nonQuoted.add(Pair.of(currentPosition, javadoc.length()));
        currentPosition = -1;
      }
    }
    return nonQuoted;
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

  record Position(int lineNumber, int columnNumber) {
    @VisibleForTesting
    static Position ofStringIndex(int index, List<Integer> lineLengths) {
      int currentLine = 0;
      int indexOfCurrentLine = 0;
      while (currentLine < lineLengths.size() &&
        index >= indexOfCurrentLine + lineLengths.get(currentLine) + 1) {
        // We need to add 1 to account for the newline character
        indexOfCurrentLine += lineLengths.get(currentLine) + 1;
        currentLine++;
      }
      int column = index - indexOfCurrentLine;
      return new Position(currentLine, column);
    }
  }

  @VisibleForTesting
  static List<Integer> lineLengths(String text) {
    return Arrays.stream(text.split("\n")).map(String::length).toList();
  }


}
