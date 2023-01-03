/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class BaseTreeVisitorIssueFilter extends BaseTreeVisitor implements JavaIssueFilter {

  private String componentKey;
  private final Map<String, Set<Integer>> excludedLinesByRule;
  private final Map<Class<? extends JavaCheck>, String> rulesKeysByRulesClass;

  protected BaseTreeVisitorIssueFilter() {
    excludedLinesByRule = new HashMap<>();
    rulesKeysByRulesClass = rulesKeysByRulesClass(filteredRules());
  }

  private static Map<Class<? extends JavaCheck>, String> rulesKeysByRulesClass(Set<Class<? extends JavaCheck>> rules) {
    Map<Class<? extends JavaCheck>, String> results = new HashMap<>();
    for (Class<? extends JavaCheck> ruleClass : rules) {
      Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, Rule.class);
      if (ruleAnnotation != null) {
        results.put(ruleClass, ruleAnnotation.key());
      }
    }
    return results;
  }

  public String getComponentKey() {
    return componentKey;
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    componentKey = context.getInputFile().key();
    excludedLinesByRule.clear();
    scan(context.getTree());
  }

  @Override
  public boolean accept(FilterableIssue issue) {
    return !(issue.componentKey().equals(componentKey) && excludedLinesByRule.getOrDefault(issue.ruleKey().rule(), new HashSet<>()).contains(issue.line()));
  }

  public Map<String, Set<Integer>> excludedLinesByRule() {
    return excludedLinesByRule;
  }

  public void acceptLines(@Nullable Tree tree, Iterable<Class<? extends JavaCheck>> rules) {
    for (Class<? extends JavaCheck> rule : rules) {
      acceptLines(tree, rule);
    }
  }

  public void acceptLines(@Nullable Tree tree, Class<? extends JavaCheck> rule) {
    computeFilteredLinesForRule(tree, rule, false);
  }

  public void excludeLines(@Nullable Tree tree, Iterable<Class<? extends JavaCheck>> rules) {
    for (Class<? extends JavaCheck> rule : rules) {
      excludeLines(tree, rule);
    }
  }

  public void excludeLines(Set<Integer> lines, String ruleKey) {
    computeFilteredLinesForRule(lines, ruleKey, true);
  }

  public void excludeLines(@Nullable Tree tree, Class<? extends JavaCheck> rule) {
    computeFilteredLinesForRule(tree, rule, true);
  }

  private void computeFilteredLinesForRule(@Nullable Tree tree, Class<? extends JavaCheck> filteredRule, boolean excludeLine) {
    if (tree == null) {
      return;
    }
    SyntaxToken firstSyntaxToken = tree.firstToken();
    SyntaxToken lastSyntaxToken = tree.lastToken();
    if (firstSyntaxToken != null && lastSyntaxToken != null) {
      Set<Integer> filteredLines = IntStream.rangeClosed(LineUtils.startLine(firstSyntaxToken), LineUtils.startLine(lastSyntaxToken))
        .boxed()
        .collect(Collectors.toSet());
      computeFilteredLinesForRule(filteredLines, rulesKeysByRulesClass.get(filteredRule), excludeLine);
    }
  }

  private void computeFilteredLinesForRule(Set<Integer> lines, String ruleKey, boolean excludeLine) {
    if (excludeLine) {
      excludedLinesByRule.computeIfAbsent(ruleKey, k -> new HashSet<>()).addAll(lines);
    } else {
      excludedLinesByRule.getOrDefault(ruleKey, Collections.emptySet()).removeAll(lines);
    }
  }
}
