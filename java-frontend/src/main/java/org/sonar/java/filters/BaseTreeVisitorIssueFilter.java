/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.DiscreteDomains;
import com.google.common.collect.Maps;
import com.google.common.collect.Ranges;
import com.google.common.collect.Sets;

import org.sonar.api.issue.Issue;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.java.syntaxtoken.LastSyntaxTokenFinder;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class BaseTreeVisitorIssueFilter extends BaseTreeVisitor implements JavaIssueFilter {

  @Nullable
  private String componentKey;
  private final Map<String, Set<Integer>> ignoredLinesByComponent = Maps.newHashMap();
  private final Set<String> filteredRulesKeys;

  public BaseTreeVisitorIssueFilter() {
    filteredRulesKeys = ruleKeys(filteredRules());
  }

  private static Set<String> ruleKeys(Set<Class<? extends JavaFileScanner>> rules) {
    Set<String> results = new HashSet<>();
    for (Class<? extends JavaFileScanner> ruleClass : rules) {
      Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, Rule.class);
      if (ruleAnnotation != null) {
        results.add(ruleAnnotation.key());
      }
    }
    return results;
  }

  @Override
  public void setComponentKey(String componentKey) {
    this.componentKey = componentKey;
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    scan(context.getTree());
  }

  @Override
  public boolean accept(Issue issue) {
    if (filteredRulesKeys.contains(issue.ruleKey().rule()) && isIgnoredLine(issue.componentKey(), issue.line())) {
      return false;
    }
    return true;
  }

  public void ignoreIssuesInTree(Tree tree) {
    SyntaxToken firstSyntaxToken = FirstSyntaxTokenFinder.firstSyntaxToken(tree);
    SyntaxToken lastSyntaxToken = LastSyntaxTokenFinder.lastSyntaxToken(tree);
    if (firstSyntaxToken != null && lastSyntaxToken != null) {
      Set<Integer> newIgnoredlines = Sets.newHashSet(Ranges.closed(firstSyntaxToken.line(), lastSyntaxToken.line()).asSet(DiscreteDomains.integers()));
      if (componentKey == null) {
        return;
      }
      if (!ignoredLinesByComponent.containsKey(componentKey)) {
        ignoredLinesByComponent.put(componentKey, newIgnoredlines);
      } else {
        ignoredLinesByComponent.get(componentKey).addAll(newIgnoredlines);
      }
    }
  }

  private boolean isIgnoredLine(String componentKey, Integer line) {
    Set<Integer> ignoredLines = ignoredLinesByComponent.get(componentKey);
    return ignoredLines != null && ignoredLines.contains(line);
  }

}
