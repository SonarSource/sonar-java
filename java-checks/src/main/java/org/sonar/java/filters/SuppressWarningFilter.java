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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.CheckList;
import org.sonar.java.checks.SuppressWarningsCheck;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonarsource.analyzer.commons.collections.SetUtils;
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
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

public class SuppressWarningFilter extends BaseTreeVisitorIssueFilter {

  private static final Map<String, Set<String>> JAVAC_WARNING_SUPPRESSING_RULES = MapBuilder.<String, Set<String>>newMap()
      // JDK warnings
      .put("cast", Collections.singleton("java:S1905"))
      .put("deprecation", Collections.singleton("java:S1874"))
      .put("dep-ann", Collections.singleton("java:S1123"))
      .put("divzero", Collections.singleton("java:S3518"))
      .put("empty", SetUtils.immutableSetOf("java:S1116", "java:S108"))
      .put("fallthrough", Collections.singleton("java:S128"))
      .put("finally", Collections.singleton("java:S1143"))
      .put("overrides", Collections.singleton("java:S1206"))
      .put("removal", Collections.singleton("java:S5738"))
      .put("serial", Collections.singleton("java:S2057"))
      .put("static", SetUtils.immutableSetOf("java:S2696", "java:S2209"))
      .put("rawtypes", Collections.singleton("java:S3740"))
      // Eclipse (IDE) warnings
      .put("boxing", SetUtils.immutableSetOf("java:S2153", "java:S5411"))
      .put("hiding", Collections.singleton("java:S4977"))
      .put("javadoc", Collections.singleton("java:S1176"))
      .put("null", Collections.singleton("java:S2259"))
      .put("resource", SetUtils.immutableSetOf("java:S2093", "java:S2095"))
      .put("serial", Collections.singleton("java:S2057"))
      .put("static-access", SetUtils.immutableSetOf("java:S2696", "java:S2209"))
      .put("static-method", Collections.singleton("java:S2325"))
      .put("sync-override", Collections.singleton("java:S3551"))
      .put("unused", SetUtils.immutableSetOf("java:S1481", "java:S1065", "java:S1854", "java:S1068",
        "java:S3985", "java:S2326", "java:S1144", "java:S1128", "java:S2583"))
      .build();

  private final Map<String, Map<String, Set<Integer>>> excludedLinesByComponent = new HashMap<>();

  private static final String SUPPRESS_WARNING_RULE_KEY = getSuppressWarningRuleKey();

  private static final Map<String, RuleKey> DEPRRECATED_RULE_KEYS = getDeprecatedRuleKeys();

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
    excludedLinesByComponent.put(getComponentKey(), new HashMap<>(excludedLinesByRule()));
  }

  private static Map<String, RuleKey> getDeprecatedRuleKeys() {
    Map<String, RuleKey> deprecatedRuleKeys = new HashMap<>();
    CheckList.getChecks().forEach(c -> {
      String key = AnnotationUtils.getAnnotation(c, Rule.class).key();
      DeprecatedRuleKey deprecatedRuleKeyAnnotation = AnnotationUtils.getAnnotation(c, DeprecatedRuleKey.class);
      if (deprecatedRuleKeyAnnotation != null) {
        deprecatedRuleKeys.put(key, RuleKey.of(deprecatedRuleKeyAnnotation.repositoryKey(), deprecatedRuleKeyAnnotation.ruleKey()));
      }
    });
    return deprecatedRuleKeys;
  }

  @Override
  public boolean accept(FilterableIssue issue) {
    return !issueShouldNotBeReported(issue, excludedLinesByComponent.getOrDefault(issue.componentKey(), Collections.emptyMap()));
  }

  private static boolean issueShouldNotBeReported(FilterableIssue issue, Map<String, Set<Integer>> excludedLineByRule) {
    RuleKey issueRuleKey = issue.ruleKey();
    return excludedLineByRule.entrySet().stream().anyMatch(excludedRule -> {
      String suppressedWarning = excludedRule.getKey();
      return ("all".equals(suppressedWarning) || isRuleKey(suppressedWarning, issueRuleKey))
        && !isSuppressWarningRule(issueRuleKey)
        && excludedRule.getValue().contains(issue.line());
    });
  }

  private static boolean isRuleKey(String rule, RuleKey ruleKey) {
    try {
      // format of the rules requires a repository: "repo:key"
      RuleKey parsed = RuleKey.parse(rule);
      RuleKey deprecatedRuleKey = DEPRRECATED_RULE_KEYS.get(ruleKey.rule());
      RuleKey squidRuleKey = RuleKey.of("squid", ruleKey.rule());
      return ruleKey.equals(parsed) || squidRuleKey.equals(parsed) || parsed.equals(deprecatedRuleKey);
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
      int endLine = tree.lastToken().range().start().line();
      Set<Integer> filteredlines = IntStream.rangeClosed(startLine, endLine).boxed().collect(Collectors.toSet());
      for (String rule : rules) {
        excludeLines(filteredlines, rule);
      }
    }
  }

  private static int startLineIncludingTrivia(Tree tree) {
    SyntaxToken firstToken = tree.firstToken();
    // first token can't be null, because tree has @SuppressWarnings annotation
    if (!firstToken.trivias().isEmpty()) {
      return firstToken.trivias().get(0).range().start().line();
    }
    return firstToken.range().start().line();
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
      expression.asConstant(String.class).ifPresent(rule -> {
        if (JAVAC_WARNING_SUPPRESSING_RULES.containsKey(rule)) {
          args.addAll(JAVAC_WARNING_SUPPRESSING_RULES.get(rule));
        } else {
          args.add(rule);
        }
      });
    }
    return args;
  }
}
