/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.regex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.BackReferenceTree;
import org.sonar.java.regex.ast.CapturingGroupTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5860")
public class UnusedGroupNamesCheck extends AbstractRegexCheck {

  private static final String ISSUE_NO_GROUP_WITH_SUCH_NAME = "There is no group named '%s' in the regular expression.";
  private static final String ISSUE_USE_NAME_INSTEAD_OF_NUMBER = "Directly use '%s' instead of its group number.";
  private static final String ISSUE_USE_GROUPS_OR_REMOVE = "Use the named groups of this regex or remove the names.";

  private static final String JAVA_UTIL_REGEX_PATTERN = "java.util.regex.Pattern";
  private static final String JAVA_UTIL_REGEX_MATCHER = "java.util.regex.Matcher";

  private static final MethodMatchers PATTERN_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_REGEX_PATTERN)
    .names("matcher")
    .addParametersMatcher("java.lang.CharSequence")
    .build();
  private static final MethodMatchers MATCHER_GROUP = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_REGEX_MATCHER)
    .names("group")
    // covers both 'group(String)' and 'group(int)'
    .addParametersMatcher(JAVA_LANG_STRING)
    .addParametersMatcher("int")
    .build();

  private final Map<Symbol.VariableSymbol, KnownGroupsCollector> knownPatternsWithGroups = new HashMap<>();
  private final Map<Symbol.VariableSymbol, Symbol.VariableSymbol> matcherToPattern = new HashMap<>();

  private final Set<Symbol.VariableSymbol> returnedVariables = new HashSet<>();
  private final Set<RegexTree> usedGroupsRegexes = new HashSet<>();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    // extends default regex methods with matcher-related methods
    return MethodMatchers.or(REGEX_METHODS, PATTERN_MATCHER, MATCHER_GROUP);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    List<Tree.Kind> nodes = new ArrayList<>(super.nodesToVisit());
    // visit more nodes than method invocations
    nodes.add(Tree.Kind.COMPILATION_UNIT);
    nodes.add(Tree.Kind.RETURN_STATEMENT);
    return nodes;
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.RETURN_STATEMENT)) {
      collectReturnedVariables(((ReturnStatementTree) tree).expression());
    } else if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      checkNotUsingAnyNamedGroup();

      // clear all the structures used during analysis to start fresh in next file
      knownPatternsWithGroups.clear();
      matcherToPattern.clear();
      usedGroupsRegexes.clear();
      returnedVariables.clear();
    }
  }

  private void collectReturnedVariables(@Nullable ExpressionTree returnedExpression) {
    if (returnedExpression == null || !returnedExpression.is(Tree.Kind.IDENTIFIER)) {
      return;
    }
    Symbol returnedSymbol = ((IdentifierTree) returnedExpression).symbol();
    if (!isPrivateEffectivelyFinalVariable(returnedSymbol)) {
      return;
    }
    Type returnedType = returnedSymbol.type();
    if (returnedType.is(JAVA_UTIL_REGEX_MATCHER) || returnedType.is(JAVA_UTIL_REGEX_PATTERN)) {
      returnedVariables.add((Symbol.VariableSymbol) returnedSymbol);
    }
  }

  private static boolean isPrivateEffectivelyFinalVariable(Symbol symbol) {
    return (symbol.isPrivate() || symbol.owner().isMethodSymbol())
      && symbol.isVariableSymbol()
      && (symbol.isFinal() || JUtils.isEffectivelyFinal((Symbol.VariableSymbol) symbol));
  }

  private void checkNotUsingAnyNamedGroup() {
    knownPatternsWithGroups.entrySet().stream()
      .filter(e -> isNotReturned(e.getKey()))
      .map(Map.Entry::getValue)
      .filter(this::isNotCallingGroups)
      .filter(UnusedGroupNamesCheck::isNotUsingBackReferences)
      .forEach(knownGroups -> {
        List<CapturingGroupTree> namedGroups = new ArrayList<>(knownGroups.groupsByName.values());
        List<RegexIssueLocation> secondaries = namedGroups.stream()
          .map(group -> toLocation(group, "Named group '%s'", g -> g.getName().get()))
          .collect(Collectors.toList());
        reportIssue(namedGroups.get(0), ISSUE_USE_GROUPS_OR_REMOVE, null, secondaries);
      });
  }

  private boolean isNotReturned(Symbol.VariableSymbol regex) {
    if (returnedVariables.contains(regex)) {
      // a known pattern is returned
      return false;
    }
    return matcherToPattern.entrySet().stream()
      // a known matcher associated with a known pattern is returned
      .noneMatch(e -> returnedVariables.contains(e.getKey()) && e.getValue().equals(regex));
  }

  private boolean isNotCallingGroups(KnownGroupsCollector knownGroups) {
    return !usedGroupsRegexes.contains(knownGroups.target);
  }

  private static boolean isNotUsingBackReferences(KnownGroupsCollector knownGroups) {
    // kill the noise
    return !knownGroups.usesBackReferences;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String methodName = mit.symbol().name();
    if (PATTERN_MATCHER.matches(mit)) {
      collectPattern(mit);
    } else if (MATCHER_GROUP.matches(mit)) {
      checkGroupUsage(mit);
    } else if ("compile".equals(methodName)) {
      // only interested in compiled patterns
      super.onMethodInvocationFound(mit);
    }
  }

  private void collectPattern(MethodInvocationTree mit) {
    for (Symbol.VariableSymbol knownPattern : knownPatternsWithGroups.keySet()) {
      if (ExpressionUtils.isInvocationOnVariable(mit, knownPattern, false)) {
        getAssignedPrivateVariable(mit).ifPresent(knownMatcher -> matcherToPattern.put(knownMatcher, knownPattern));
        break;
      }
    }
  }

  private void checkGroupUsage(MethodInvocationTree mit) {
    matcherToPattern.forEach((knownMatcher, knownPattern) -> {
      if (!ExpressionUtils.isInvocationOnVariable(mit, knownMatcher, false)) {
        return;
      }
      KnownGroupsCollector knownGroups = knownPatternsWithGroups.get(knownPattern);
      usedGroupsRegexes.add(knownGroups.target);
      ExpressionTree arg0 = mit.arguments().get(0);
      Type arg0Type = arg0.symbolType();
      if (arg0Type.is("int")) {
        checkUsingNumberInsteadOfName(knownGroups, arg0);
      } else {
        checkNoSuchName(knownGroups, arg0);
      }
    });
  }

  private void checkUsingNumberInsteadOfName(KnownGroupsCollector knownGroups, ExpressionTree arg0) {
    Optional<Integer> groupNumber = arg0.asConstant(Integer.class);
    if (!groupNumber.isPresent()) {
      return;
    }
    Integer groupNumberValue = groupNumber.get();
    CapturingGroupTree capturingGroupTree = knownGroups.groupsByNumber.get(groupNumberValue);
    if (capturingGroupTree == null) {
      return;
    }
    String message = String.format(ISSUE_USE_NAME_INSTEAD_OF_NUMBER, capturingGroupTree.getName().orElse("?"));
    RegexIssueLocation secondary = toLocation(capturingGroupTree, "Group %d", g -> groupNumberValue);
    reportIssue(arg0, message, null, Collections.singletonList(secondary));
  }

  private void checkNoSuchName(KnownGroupsCollector knownGroups, ExpressionTree arg0) {
    Optional<String> groupName = arg0.asConstant(String.class);
    if (!groupName.isPresent()) {
      return;
    }
    String groupNameValue = groupName.get();
    if (!knownGroups.groupsByName.keySet().contains(groupNameValue)) {
      String message = String.format(ISSUE_NO_GROUP_WITH_SUCH_NAME, groupNameValue);
      List<RegexIssueLocation> secondaries = knownGroups.groupsByName.values()
        .stream()
        .map(group -> toLocation(group, "Named group '%s'", g -> g.getName().get()))
        .collect(Collectors.toList());
      reportIssue(arg0, message, null, secondaries);
    }
  }

  private static RegexCheck.RegexIssueLocation toLocation(CapturingGroupTree group, String message, Function<CapturingGroupTree, Object> arg) {
    return new RegexCheck.RegexIssueLocation(group, String.format(message, arg.apply(group)));
  }

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    getAssignedPrivateVariable(mit)
      .ifPresent(variableSymbol -> collectGroups(regexForLiterals)
        .ifPresent(knownGroups -> knownPatternsWithGroups.put(variableSymbol, knownGroups)));
  }

  private static Optional<Symbol.VariableSymbol> getAssignedPrivateVariable(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    Symbol symbol = null;
    if (parent.is(Tree.Kind.VARIABLE)) {
      symbol = ((VariableTree) parent).symbol();
    } else if (parent.is(Tree.Kind.ASSIGNMENT)) {
      ExpressionTree variable = ((AssignmentExpressionTree) parent).variable();
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        symbol = ((IdentifierTree) variable).symbol();
      }
    }
    if (symbol == null || !isPrivateEffectivelyFinalVariable(symbol)) {
      return Optional.empty();
    }
    return Optional.of((Symbol.VariableSymbol) symbol);
  }

  private static Optional<KnownGroupsCollector> collectGroups(RegexParseResult regex) {
    KnownGroupsCollector visitor = new KnownGroupsCollector();
    visitor.visit(regex);
    return visitor.groupsByName.isEmpty() ? Optional.empty() : Optional.of(visitor);
  }

  private static class KnownGroupsCollector extends RegexBaseVisitor {

    private RegexTree target;
    private Map<String, CapturingGroupTree> groupsByName = new HashMap<>();
    private Map<Integer, CapturingGroupTree> groupsByNumber = new HashMap<>();
    private boolean usesBackReferences = false;

    @Override
    protected void before(RegexParseResult regexParseResult) {
      target = regexParseResult.getResult();
    }

    @Override
    public void visitCapturingGroup(CapturingGroupTree tree) {
      tree.getName().ifPresent(name -> {
        groupsByName.put(name, tree);
        groupsByNumber.put(tree.getGroupNumber(), tree);
      });
      super.visitCapturingGroup(tree);
    }

    @Override
    public void visitBackReference(BackReferenceTree tree) {
      usesBackReferences = true;
      super.visitBackReference(tree);
    }
  }
}
