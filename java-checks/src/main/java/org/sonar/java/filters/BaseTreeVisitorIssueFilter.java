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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ranges;
import com.google.common.collect.Sets;

import org.sonar.api.issue.Issue;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.java.syntaxtoken.LastSyntaxTokenFinder;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Map;
import java.util.Set;

public abstract class BaseTreeVisitorIssueFilter extends BaseTreeVisitor implements JavaIssueFilter {

  private String componentKey;
  private final Map<String, Multimap<String, Integer>> ignoredLinesByComponentAndRule;
  private final Map<Class<? extends JavaCheck>, String> rulesKeysByRulesClass;
  private final Set<String> filteredRulesKeys;

  public BaseTreeVisitorIssueFilter() {
    ignoredLinesByComponentAndRule = Maps.newHashMap();
    rulesKeysByRulesClass = rulesKeysByRulesClass(filteredRules());
    filteredRulesKeys = Sets.newHashSet(rulesKeysByRulesClass.values());
  }

  private static Map<Class<? extends JavaCheck>, String> rulesKeysByRulesClass(Set<Class<? extends JavaCheck>> rules) {
    Map<Class<? extends JavaCheck>, String> results = Maps.newHashMap();
    for (Class<? extends JavaCheck> ruleClass : rules) {
      Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, Rule.class);
      if (ruleAnnotation != null) {
        results.put(ruleClass, ruleAnnotation.key());
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
    String ruleKey = issue.ruleKey().rule();
    if (filteredRulesKeys.contains(ruleKey) && isIgnoredLine(issue.componentKey(), ruleKey, issue.line())) {
      return false;
    }
    return true;
  }

  public void ignoreIssuesInTree(Tree tree, Class<? extends JavaCheck> filteredRule) {
    SyntaxToken firstSyntaxToken = FirstSyntaxTokenFinder.firstSyntaxToken(tree);
    SyntaxToken lastSyntaxToken = LastSyntaxTokenFinder.lastSyntaxToken(tree);
    if (firstSyntaxToken != null && lastSyntaxToken != null) {
      if (!ignoredLinesByComponentAndRule.containsKey(componentKey)) {
        ignoredLinesByComponentAndRule.put(componentKey, HashMultimap.<String, Integer>create());
      }
      Set<Integer> filteredlines = Sets.newHashSet(Ranges.closed(firstSyntaxToken.line(), lastSyntaxToken.line()).asSet(DiscreteDomains.integers()));
      String ruleKey = rulesKeysByRulesClass.get(filteredRule);
      ignoredLinesByComponentAndRule.get(componentKey).putAll(ruleKey, filteredlines);
    }
  }

  private boolean isIgnoredLine(String componentKey, String ruleKey, Integer line) {
    Multimap<String, Integer> ignoredLinesByRule = ignoredLinesByComponentAndRule.get(componentKey);
    if (ignoredLinesByRule == null) {
      return false;
    }
    return ignoredLinesByRule.get(ruleKey).contains(line);
  }

}
