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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S7474")
public class MarkdownJavadocSyntaxCheck extends IssuableSubscriptionVisitor {

  /**
   * Pattern to find Javadoc or HTML tags that can be replaced with Markdown.
   */
  private static final Pattern NON_MARKDOWN_JAVADOC_PATTERN = Pattern.compile("<b>|<i>|<p>|<pre>|<ul>|<table>|\\{@code |\\{@link ");

  private static final Pattern TRIPLE_QUOTE = Pattern.compile("```");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(PublicApiChecker.apiKinds());
  }

  @Override
  public void visitNode(Tree tree) {
    SyntaxToken firstToken = tree.firstToken();
    if (firstToken == null) {
      return;
    }
    boolean containsNonMarkdownSyntax = firstToken
      .trivias()
      .stream()
      .filter(trivia -> trivia.isComment(SyntaxTrivia.CommentKind.MARKDOWN))
      .map(SyntaxTrivia::comment)
      .flatMap(comment -> removeQuotedCode(comment).stream())
      .anyMatch(comment -> NON_MARKDOWN_JAVADOC_PATTERN.matcher(comment).find());

    if (containsNonMarkdownSyntax) {
      context.reportIssue(this, tree, "replace HTML syntax with Markdown syntax in javadoc");
    }
  }

  /**
   * Remove from the text, parts that are between backquotes ({@code `}) and triple backquote ({@code ```}) for Markdown code.
   */
  @VisibleForTesting
  static List<String> removeQuotedCode(String javadoc) {
    List<String> nonEscaped = new ArrayList<>();
    int currentPosition = 0;
    while (currentPosition != -1) {
      int nextQuote = javadoc.indexOf("`", currentPosition);
      if (nextQuote != -1) {
        nonEscaped.add(javadoc.substring(currentPosition, nextQuote));
        currentPosition = findEndOfMarkdownQuote(javadoc, nextQuote);
      } else {
        nonEscaped.add(javadoc.substring(currentPosition));
        currentPosition = -1;
      }
    }
    return nonEscaped;
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
