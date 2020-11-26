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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.BackReferenceTree;
import org.sonar.java.regex.ast.CapturingGroupTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5860")
public class UnusedGroupNamesCheck extends AbstractRegexCheckTrackingMatchers {

  private static final String ISSUE_NO_GROUP_WITH_SUCH_NAME = "There is no group named '%s' in the regular expression.";
  private static final String ISSUE_USE_NAME_INSTEAD_OF_NUMBER = "Directly use '%s' instead of its group number.";
  private static final String ISSUE_USE_GROUPS_OR_REMOVE = "Use the named groups of this regex or remove the names.";

  private static final String JAVA_UTIL_REGEX_MATCHER = "java.util.regex.Matcher";

  private static final MethodMatchers MATCHER_GROUP = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_REGEX_MATCHER)
    .names("group")
    // covers both 'group(String)' and 'group(int)'
    .addParametersMatcher(JAVA_LANG_STRING)
    .addParametersMatcher("int")
    .build();

  @Override
  protected MethodMatchers trackedMethodMatchers() {
    return MATCHER_GROUP;
  }

  @Override
  protected void checkRegex(RegexParseResult regexForLiterals, List<MethodInvocationTree> trackedMethodsCalled, boolean didEscape) {
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
    ExpressionTree arg0 = mit.arguments().get(0);
    Type arg0Type = arg0.symbolType();
    if (arg0Type.is("int")) {
      checkUsingNumberInsteadOfName(knownGroups, arg0);
    } else {
      checkNoSuchName(knownGroups, arg0);
    }
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
