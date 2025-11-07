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
package org.sonar.java.filters;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class AnyRuleIssueFilter extends BaseTreeVisitor implements JavaIssueFilter {

  private String componentKey;
  private final Set<Integer> excludedLines = new HashSet<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    componentKey = context.getInputFile().key();
    excludedLines.clear();
    scan(context.getTree());
  }

  @Override
  public boolean accept(FilterableIssue issue) {
    return !(issue.componentKey().equals(componentKey) && excludedLines.contains(issue.line()));
  }

  @Override
  public final Set<Class<? extends JavaCheck>> filteredRules() {
    return Collections.emptySet();
  }

  public void excludeLines(Tree tree) {
    excludedLines.addAll(filteredLines(tree));
  }

  private static Set<Integer> filteredLines(Tree tree) {
    SyntaxToken firstSyntaxToken = tree.firstToken();
    SyntaxToken lastSyntaxToken = tree.lastToken();
    if (firstSyntaxToken != null && lastSyntaxToken != null) {
      int startLine = LineUtils.startLine(firstSyntaxToken);
      int endLine = LineUtils.startLine(lastSyntaxToken);

      // includes trivia on top of first syntax token.
      List<SyntaxTrivia> trivias = firstSyntaxToken.trivias();
      if (!trivias.isEmpty()) {
        startLine = LineUtils.startLine(trivias.get(0));
      }

      return IntStream.rangeClosed(startLine, endLine).boxed().collect(Collectors.toSet());
    }
    return new HashSet<>();
  }
}
