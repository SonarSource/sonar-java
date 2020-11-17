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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.AutomatonState;
import org.sonar.java.regex.ast.BackReferenceTree;
import org.sonar.java.regex.ast.CapturingGroupTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S6001")
public class ImpossibleBackReferenceCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    ValidBackReferenceFinder finder = new ValidBackReferenceFinder();
    finder.findIn(regexForLiterals.getStartState());
    CapturingGroupCollector groupCollector = new CapturingGroupCollector();
    groupCollector.visit(regexForLiterals);
    new ImpossibleBackReferenceFinder(finder.legalBackReferences, groupCollector.capturingGroups)
      .visit(regexForLiterals);
  }

  private static class CapturingGroupCollector extends RegexBaseVisitor {
    private final Map<String, CapturingGroupTree> capturingGroups = new HashMap<>();
    @Override
    public void visitCapturingGroup(CapturingGroupTree group) {
      capturingGroups.put("" + group.getGroupNumber(), group);
      group.getName().ifPresent(name -> capturingGroups.put(name, group));
      super.visitCapturingGroup(group);
    }
  }

  private static class ValidBackReferenceFinder {
    private final Set<BackReferenceTree> legalBackReferences = new HashSet<>();
    private final Set<String> groupNames = new HashSet<>();
    private final Set<AutomatonState> visited = new HashSet<>();

    public void findIn(AutomatonState state) {
      if (visited.contains(state) || impossiblePath(state)) {
        return;
      }
      visited.add(state);
      for (AutomatonState succ : state.successors()) {
        findIn(succ);
      }
      if (state instanceof CapturingGroupTree) {
        CapturingGroupTree group = (CapturingGroupTree) state;
        markBackReferences(group, group.continuation(), new HashSet<>());
        groupNames.add("" + group.getGroupNumber());
        group.getName().ifPresent(groupNames::add);
      }
    }

    private boolean impossiblePath(AutomatonState state) {
      return state instanceof BackReferenceTree && !groupNames.contains(((BackReferenceTree) state).groupName());
    }

    private void markBackReferences(CapturingGroupTree group, AutomatonState state, Set<AutomatonState> visited) {
      if (visited.contains(state)) {
        return;
      }
      visited.add(state);
      if (state instanceof BackReferenceTree) {
        BackReferenceTree backReference = (BackReferenceTree) state;
        if ((backReference.isNumerical() && backReference.groupNumber() == group.getGroupNumber())
          || (backReference.isNamedGroup() && group.getName().filter(name -> name.equals(backReference.groupName())).isPresent())) {
          legalBackReferences.add(backReference);
        }
      }
      for (AutomatonState successor : state.successors()) {
        markBackReferences(group, successor, visited);
      }
    }
  }

  private class ImpossibleBackReferenceFinder extends RegexBaseVisitor {
    private final Set<BackReferenceTree> legalBackReferences;
    private final Map<String, CapturingGroupTree> capturingGroups;

    public ImpossibleBackReferenceFinder(Set<BackReferenceTree> legalBackReferences, Map<String, CapturingGroupTree> capturingGroups) {
      this.legalBackReferences = legalBackReferences;
      this.capturingGroups = capturingGroups;
    }

    @Override
    public void visitBackReference(BackReferenceTree tree) {
      if (!legalBackReferences.contains(tree)) {
        String message;
        List<RegexIssueLocation> secondaries = new ArrayList<>();
        if (capturingGroups.containsKey(tree.groupName())) {
          message = "Fix this backreference, so that it refers to a group that can be matched before it.";
          CapturingGroupTree group = capturingGroups.get(tree.groupName());
          secondaries.add(new RegexIssueLocation(group, "This group is used in a backreference before it is defined"));
        } else {
          message = "Fix this backreference - it refers to a capturing group that doesn't exist.";
        }
        reportIssue(tree, message, null, secondaries);
      }
    }
  }

}
