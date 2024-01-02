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
package org.sonar.java.checks.regex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.regex.RegexCheck;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.BackReferenceTree;
import org.sonarsource.analyzer.commons.regex.ast.CapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5860")
public class UnusedGroupNamesCheck extends AbstractRegexCheckTrackingMatchers {

  private static final String ISSUE_NO_GROUP_WITH_SUCH_NAME = "There is no group named '%s' in the regular expression.";
  private static final String ISSUE_USE_NAME_INSTEAD_OF_NUMBER = "Directly use '%s' instead of its group number.";
  private static final String ISSUE_USE_GROUPS_OR_REMOVE = "Use the named groups of this regex or remove the names.";

  private static final String JAVA_UTIL_REGEX_MATCHER = "java.util.regex.Matcher";

  private static final Pattern GROUP_NUMBER_REPLACEMENT_REGEX = Pattern.compile("(?<!\\\\)\\$(?<number>\\d++)");
  private static final Pattern GROUP_NAME_REPLACEMENT_REGEX = Pattern.compile("(?<!\\\\)\\$\\{(?<name>[A-Za-z][0-9A-Za-z]*+)\\}");

  private static final List<String> NAMES_OF_METHODS_WITH_GROUP_ARGUMENT = List.of("group", "start", "end");

  private static final MethodMatchers MATCHER_GROUP = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_UTIL_REGEX_MATCHER)
      .names(NAMES_OF_METHODS_WITH_GROUP_ARGUMENT.toArray(String[]::new))
      .addParametersMatcher(JAVA_LANG_STRING)
      .addParametersMatcher("int")
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_UTIL_REGEX_MATCHER)
      .names("appendReplacement")
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_UTIL_REGEX_MATCHER)
      .names("replaceAll", "replaceFirst")
      .addParametersMatcher(MethodMatchers.ANY)
      .build());

  @Override
  protected MethodMatchers trackedMethodMatchers() {
    return MATCHER_GROUP;
  }

  @Override
  protected void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation, List<MethodInvocationTree> trackedMethodsCalled, boolean didEscape) {
    KnownGroupsCollector knownGroups = collectGroups(regexForLiterals);
    List<CapturingGroupTree> namedGroups = new ArrayList<>(knownGroups.groupsByName.values());
    if (trackedMethodsCalled.isEmpty() && !didEscape && !namedGroups.isEmpty() && !knownGroups.usesBackReferences) {
      List<RegexIssueLocation> secondaries = namedGroups.stream()
        .map(group -> toLocation(group, "Named group '%s'", g -> g.getName().get()))
        .collect(Collectors.toList());
      reportIssue(namedGroups.get(0), ISSUE_USE_GROUPS_OR_REMOVE, null, secondaries);
    }
    for (MethodInvocationTree groupInvocation : trackedMethodsCalled) {
      checkGroupUsage(groupInvocation, knownGroups);
    }
  }

  private void checkGroupUsage(MethodInvocationTree mit, KnownGroupsCollector knownGroups) {
    String methodName = ExpressionUtils.methodName(mit).name();
    if (NAMES_OF_METHODS_WITH_GROUP_ARGUMENT.contains(methodName)) {
      ExpressionTree arg0 = mit.arguments().get(0);
      if (arg0.symbolType().is("int")) {
        arg0.asConstant(Integer.class).ifPresent(index -> checkUsingNumberInsteadOfName(knownGroups, arg0, index, false));
      } else {
        arg0.asConstant(String.class).ifPresent(name -> checkNoSuchName(knownGroups, arg0, name));
      }
    } else {
      int argIndex = "appendReplacement".equals(methodName) ? 1 : 0;
      ExpressionTree arg = mit.arguments().get(argIndex);
      arg.asConstant(String.class).ifPresent(replacement -> checkUsingReplacementString(knownGroups, arg, replacement));
    }
  }

  private void checkUsingReplacementString(KnownGroupsCollector knownGroups, ExpressionTree arg, String replacement) {
    Matcher indexMatcher = GROUP_NUMBER_REPLACEMENT_REGEX.matcher(replacement);
    while (indexMatcher.find()) {
      int groupNumber = Integer.parseInt(indexMatcher.group("number"));
      checkUsingNumberInsteadOfName(knownGroups, arg, groupNumber, true);
    }
    Matcher nameMatcher = GROUP_NAME_REPLACEMENT_REGEX.matcher(replacement);
    while (nameMatcher.find()) {
      checkNoSuchName(knownGroups, arg, nameMatcher.group("name"));
    }
  }

  private void checkUsingNumberInsteadOfName(KnownGroupsCollector knownGroups, ExpressionTree arg0, int groupNumber, boolean dollarReference) {
    CapturingGroupTree capturingGroupTree = knownGroups.groupsByNumber.get(groupNumber);
    if (capturingGroupTree == null) {
      return;
    }
    String groupName = capturingGroupTree.getName().map(name -> dollarReference ? ("${" + name + "}") : name).orElse("?");
    String message = String.format(ISSUE_USE_NAME_INSTEAD_OF_NUMBER, groupName);
    RegexIssueLocation secondary = toLocation(capturingGroupTree, "Group %d", g -> groupNumber);
    reportIssue(arg0, message, null, Collections.singletonList(secondary));
  }

  private void checkNoSuchName(KnownGroupsCollector knownGroups, ExpressionTree arg0, String groupName) {
    if (!knownGroups.groupsByName.containsKey(groupName)) {
      String message = String.format(ISSUE_NO_GROUP_WITH_SUCH_NAME, groupName);
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

  private static KnownGroupsCollector collectGroups(RegexParseResult regex) {
    KnownGroupsCollector visitor = new KnownGroupsCollector();
    visitor.visit(regex);
    return visitor;
  }

  private static class KnownGroupsCollector extends RegexBaseVisitor {

    private final Map<String, CapturingGroupTree> groupsByName = new HashMap<>();
    private final Map<Integer, CapturingGroupTree> groupsByNumber = new HashMap<>();
    private boolean usesBackReferences = false;

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
