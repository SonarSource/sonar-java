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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.SuppressWarningsCheck;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class SuppressWarningFilter extends BaseTreeVisitorIssueFilter {

  private final Map<String, Multimap<String, Integer>> excludedLinesByComponent = new HashMap<>();

  private static final String SUPPRESS_WARNING_RULE_KEY = getSuppressWarningRuleKey();

  private static String getSuppressWarningRuleKey() {
    return AnnotationUtils.getAnnotation(SuppressWarningsCheck.class, Rule.class).key();
  }

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return Collections.emptySet();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    excludedLinesByComponent.put(getComponentKey(), HashMultimap.create(excludedLinesByRule()));
  }

  @Override
  public boolean accept(FilterableIssue issue) {
    Multimap<String, Integer> excludedLinesByRule = HashMultimap.create();
    if (excludedLinesByComponent.containsKey(issue.componentKey())) {
      excludedLinesByRule = excludedLinesByComponent.get(issue.componentKey());
    }
    return !issueShouldNotBeReported(issue, excludedLinesByRule);
  }

  private static boolean issueShouldNotBeReported(FilterableIssue issue, Multimap<String, Integer> excludedLineByRule) {
    RuleKey issueRuleKey = issue.ruleKey();
    for (String excludedRule : excludedLineByRule.keySet()) {
      if (("all".equals(excludedRule) || isRuleKey(excludedRule, issueRuleKey)) && !isSuppressWarningRule(issueRuleKey)) {
        Collection<Integer> excludedLines = excludedLineByRule.get(excludedRule);
        if (excludedLines.contains(issue.line())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isRuleKey(String rule, RuleKey ruleKey) {
    try {
      // format of the rules requires a repository: "repo:key"
      return ruleKey.equals(RuleKey.parse(rule));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean isSuppressWarningRule(RuleKey ruleKey) {
    return SUPPRESS_WARNING_RULE_KEY.equals(ruleKey.rule());
  }

  @Override
  public void visitClass(ClassTree tree) {
    handleSuppressWarning(tree.modifiers().annotations(), tree);
    super.visitClass(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    handleSuppressWarning(tree.modifiers().annotations(), tree);
    super.visitMethod(tree);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    handleSuppressWarning(tree.modifiers().annotations(), tree);
    super.visitVariable(tree);
  }

  private void handleSuppressWarning(List<AnnotationTree> annotationTrees, Tree tree) {
    int startLine = -1;
    List<String> rules = new ArrayList<>();
    for (AnnotationTree annotationTree : annotationTrees) {
      if (isSuppressWarningsAnnotation(annotationTree)) {
        startLine = startLineIncludingTrivia(tree);
        rules.addAll(getRules(annotationTree));
        break;
      }
    }

    if (startLine != -1) {
      int endLine = tree.lastToken().line();
      Set<Integer> filteredlines = ContiguousSet.create(Range.closed(startLine, endLine), DiscreteDomain.integers());
      for (String rule : rules) {
        excludeLines(filteredlines, rule);
      }
    }
  }

  private static int startLineIncludingTrivia(Tree tree) {
    SyntaxToken firstToken = tree.firstToken();
    // first token can't be null, because tree has @SuppressWarnings annotation
    if (!firstToken.trivias().isEmpty()) {
      return firstToken.trivias().get(0).startLine();
    }
    return firstToken.line();
  }

  private static boolean isSuppressWarningsAnnotation(AnnotationTree annotationTree) {
    return annotationTree.annotationType().symbolType().is("java.lang.SuppressWarnings") && !annotationTree.arguments().isEmpty();
  }

  private static List<String> getRules(AnnotationTree annotationTree) {
    return getRulesFromExpression(annotationTree.arguments().get(0));
  }

  private static List<String> getRulesFromExpression(ExpressionTree expression) {
    List<String> args = new ArrayList<>();
    if (expression.is(Tree.Kind.NEW_ARRAY)) {
      for (ExpressionTree initializer : ((NewArrayTree) expression).initializers()) {
        args.addAll(getRulesFromExpression(initializer));
      }
    } else {
      String constant = ConstantUtils.resolveAsStringConstant(expression);
      if (constant != null) {
        args.add(constant);
      }
    }
    return args;
  }
}
