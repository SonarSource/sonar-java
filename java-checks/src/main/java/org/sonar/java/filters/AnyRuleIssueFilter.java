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
package org.sonar.java.filters;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.scan.issue.filter.FilterableIssue;
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
      int startLine = firstSyntaxToken.line();
      int endLine = lastSyntaxToken.line();

      // includes trivia on top of first syntax token.
      List<SyntaxTrivia> trivias = firstSyntaxToken.trivias();
      if (!trivias.isEmpty()) {
        startLine = trivias.get(0).startLine();
      }

      return ContiguousSet.create(Range.closed(startLine, endLine), DiscreteDomain.integers());
    }
    return new HashSet<>();
  }
}
