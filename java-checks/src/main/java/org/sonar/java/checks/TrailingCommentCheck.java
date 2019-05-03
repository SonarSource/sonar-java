/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "TrailingCommentCheck")
@RspecKey("S139")
public class TrailingCommentCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_LEGAL_COMMENT_PATTERN = "^\\s*+[^\\s]++$";
  private static final Set<String> EXCLUDED_PATTERNS = ImmutableSet.of("NOSONAR", "NOPMD", "CHECKSTYLE:", "$NON-NLS");

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
    int tokenLine = syntaxToken.line();
    if (tokenLine != previousTokenLine) {
      syntaxToken.trivias().stream()
        .filter(trivia -> trivia.startLine() == previousTokenLine)
        .map(SyntaxTrivia::comment)
        .map(comment -> comment.startsWith("//") ? comment.substring(2) : comment.substring(2, comment.length() - 2))
        .map(String::trim)
        .filter(comment -> !pattern.matcher(comment).matches() && !containsExcludedPattern(comment))
        .forEach(comment -> addIssue(previousTokenLine, "Move this trailing comment on the previous empty line."));
    }

    previousTokenLine = tokenLine;
  }

  private static boolean containsExcludedPattern(String comment) {
    for (String excludePattern : EXCLUDED_PATTERNS) {
      if (StringUtils.containsIgnoreCase(comment, excludePattern)) {
        return true;
      }
    }
    return false;
  }
}
