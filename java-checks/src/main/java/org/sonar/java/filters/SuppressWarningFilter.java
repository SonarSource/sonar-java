/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.java.checks.SuppressWarningsCheck;
import org.sonar.java.model.LineUtils;
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

  public static final String SQUID = "squid";
  private static final Map<String, RuleKey> DEPRECATED_RULE_KEYS = MapBuilder.<String, RuleKey>newMap()
    .put("S139", RuleKey.of(SQUID, "TrailingCommentCheck"))
    .put("S1128", RuleKey.of(SQUID, "UselessImportCheck"))
    .put("S100", RuleKey.of(SQUID, "S00100"))
    .put("S1120", RuleKey.of(SQUID, "IndentationCheck"))
    .put("S101", RuleKey.of(SQUID, "S00101"))
    .put("S1121", RuleKey.of(SQUID, "AssignmentInSubExpressionCheck"))
    .put("S1123", RuleKey.of(SQUID, "MissingDeprecatedCheck"))
    .put("S1124", RuleKey.of(SQUID, "ModifiersOrderCheck"))
    .put("S127", RuleKey.of(SQUID, "ForLoopCounterChangedCheck"))
    .put("S125", RuleKey.of(SQUID, "CommentedOutCodeLine"))
    .put("S1116", RuleKey.of(SQUID, "EmptyStatementUsageCheck"))
    .put("S1117", RuleKey.of(SQUID, "HiddenFieldCheck"))
    .put("S1119", RuleKey.of(SQUID, "LabelsShouldNotBeUsedCheck"))
    .put("S131", RuleKey.of(SQUID, "SwitchLastCaseIsDefaultCheck"))
    .put("S1110", RuleKey.of(SQUID, "UselessParenthesesCheck"))
    .put("S1111", RuleKey.of(SQUID, "ObjectFinalizeCheck"))
    .put("S1113", RuleKey.of(SQUID, "ObjectFinalizeOverridenCheck"))
    .put("S1114", RuleKey.of(SQUID, "ObjectFinalizeOverridenCallsSuperFinalizeCheck"))
    .put("S1874", RuleKey.of(SQUID, "CallToDeprecatedMethod"))
    .put("S119", RuleKey.of(SQUID, "S00119"))
    .put("S117", RuleKey.of(SQUID, "S00117"))
    .put("S118", RuleKey.of(SQUID, "S00118"))
    .put("S115", RuleKey.of(SQUID, "S00115"))
    .put("S116", RuleKey.of(SQUID, "S00116"))
    .put("S113", RuleKey.of(SQUID, "S00113"))
    .put("S114", RuleKey.of(SQUID, "S00114"))
    .put("S1105", RuleKey.of(SQUID, "LeftCurlyBraceEndLineCheck"))
    .put("S1106", RuleKey.of(SQUID, "LeftCurlyBraceStartLineCheck"))
    .put("S1107", RuleKey.of(SQUID, "RightCurlyBraceSameLineAsNextBlockCheck"))
    .put("S1108", RuleKey.of(SQUID, "RightCurlyBraceDifferentLineAsNextBlockCheck"))
    .put("S1109", RuleKey.of(SQUID, "RightCurlyBraceStartLineCheck"))
    .put("S122", RuleKey.of(SQUID, "S00122"))
    .put("S120", RuleKey.of(SQUID, "S00120"))
    .put("S121", RuleKey.of(SQUID, "S00121"))
    .put("S1144", RuleKey.of(SQUID, "UnusedPrivateMethod"))
    .put("S1541", RuleKey.of(SQUID, "MethodCyclomaticComplexity"))
    .put("S1104", RuleKey.of(SQUID, "ClassVariableVisibilityCheck"))
    .put("S108", RuleKey.of(SQUID, "S00108"))
    .put("S107", RuleKey.of(SQUID, "S00107"))
    .put("S104", RuleKey.of(SQUID, "S00104"))
    .put("S2260", RuleKey.of(SQUID, "ParsingError"))
    .put("S105", RuleKey.of(SQUID, "S00105"))
    .put("S103", RuleKey.of(SQUID, "S00103"))
    .put("S2309", RuleKey.of(SQUID, "EmptyFile"))
    .put("S2308", RuleKey.of(SQUID, "CallToFileDeleteOnExitMethod"))
    .put("S1130", RuleKey.of(SQUID, "RedundantThrowsDeclarationCheck"))
    .put("S112", RuleKey.of(SQUID, "S00112"))
    .put("S1176", RuleKey.of(SQUID, "UndocumentedApi"))
    .put("S110", RuleKey.of(SQUID, "MaximumInheritanceDepth"))
    .build();

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
      RuleKey deprecatedRuleKey = DEPRECATED_RULE_KEYS.get(ruleKey.rule());
      RuleKey squidRuleKey = RuleKey.of(SQUID, ruleKey.rule());
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
      int endLine = LineUtils.startLine(tree.lastToken());
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
      return LineUtils.startLine(firstToken.trivias().get(0));
    }
    return LineUtils.startLine(firstToken);
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
