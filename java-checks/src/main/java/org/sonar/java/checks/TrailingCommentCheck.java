/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.Strings;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "TrailingCommentCheck", repositoryKey = "squid")
@Rule(key = "S139")
public class TrailingCommentCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_LEGAL_COMMENT_PATTERN = "^\\s*+[^\\s]++$";
  private static final List<String> EXCLUDED_PATTERNS = Arrays.asList("NOSONAR", "NOPMD", "CHECKSTYLE:", "$NON-NLS");

  @RuleProperty(
    key = "legalTrailingCommentPattern",
    description = "Description Pattern for text of trailing comments that are allowed. By default, comments containing only one word.",
    defaultValue = DEFAULT_LEGAL_COMMENT_PATTERN)
  public String legalCommentPattern = DEFAULT_LEGAL_COMMENT_PATTERN;

  private Pattern pattern;
  private boolean ignoreMultipleOccurences;
  private Set<SyntaxToken> visitedTokens;
  private int previousTokenLine;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.TOKEN,
      Tree.Kind.VARIABLE);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    previousTokenLine = -1;
    if (pattern == null) {
      pattern = Pattern.compile(legalCommentPattern);
    }
    visitedTokens = new HashSet<>();
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    visitedTokens.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    ignoreMultipleOccurences = true;
  }

  @Override
  public void leaveNode(Tree tree) {
    ignoreMultipleOccurences = false;
  }

  @Override
  public void visitToken(SyntaxToken syntaxToken) {
    if (ignoreMultipleOccurences && !visitedTokens.add(syntaxToken)) {
      return;
    }
    int tokenLine = LineUtils.startLine(syntaxToken);
    if (tokenLine != previousTokenLine) {
      syntaxToken.trivias().stream()
        .filter(trivia -> LineUtils.startLine(trivia) == previousTokenLine)
        .map(SyntaxTrivia::commentContent)
        .map(String::trim)
        .filter(comment -> !pattern.matcher(comment).matches() && !containsExcludedPattern(comment))
        .forEach(comment -> addIssue(previousTokenLine, "Move this trailing comment on the previous empty line."));
    }

    previousTokenLine = tokenLine;
  }

  private static boolean containsExcludedPattern(String comment) {
    for (String excludePattern : EXCLUDED_PATTERNS) {
      if (Strings.CI.contains(comment, excludePattern)) {
        return true;
      }
    }
    return false;
  }
}
