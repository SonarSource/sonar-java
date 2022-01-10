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
package org.sonar.java.checks.regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.AutomatonState;
import org.sonarsource.analyzer.commons.regex.ast.BackReferenceTree;
import org.sonarsource.analyzer.commons.regex.ast.CapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.EndOfCapturingGroupState;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.RepetitionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S6001")
public class ImpossibleBackReferenceCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    new ImpossibleBackReferenceFinder().visit(regexForLiterals);
  }

  private class ImpossibleBackReferenceFinder extends RegexBaseVisitor {
    private Set<BackReferenceTree> impossibleBackReferences = new LinkedHashSet<>();
    private Map<String, CapturingGroupTree> capturingGroups = new HashMap<>();

    @Override
    public void visitBackReference(BackReferenceTree tree) {
      if (!capturingGroups.containsKey(tree.groupName())) {
        impossibleBackReferences.add(tree);
      }
    }

    @Override
    public void visitCapturingGroup(CapturingGroupTree group) {
      super.visitCapturingGroup(group);
      addGroup(group);
    }

    private void addGroup(CapturingGroupTree group) {
      capturingGroups.put("" + group.getGroupNumber(), group);
      group.getName().ifPresent(name -> capturingGroups.put(name, group));
    }

    @Override
    public void visitDisjunction(DisjunctionTree tree) {
      Map<String, CapturingGroupTree> originalCapturingGroups = capturingGroups;
      Map<String, CapturingGroupTree> allCapturingGroups = new HashMap<>();
      for (RegexTree alternative : tree.getAlternatives()) {
        capturingGroups = new HashMap<>(originalCapturingGroups);
        visit(alternative);
        allCapturingGroups.putAll(capturingGroups);
      }
      capturingGroups = allCapturingGroups;
    }

    @Override
    public void visitRepetition(RepetitionTree tree) {
      Integer maximumRepetitions = tree.getQuantifier().getMaximumRepetitions();
      if (maximumRepetitions != null && maximumRepetitions < 2) {
        super.visitRepetition(tree);
        return;
      }
      Set<BackReferenceTree> originalImpossibleBackReferences = impossibleBackReferences;
      impossibleBackReferences = new LinkedHashSet<>();
      Map<String, CapturingGroupTree> originalCapturingGroups = new HashMap<>(capturingGroups);
      super.visitRepetition(tree);
      if (!impossibleBackReferences.isEmpty()) {
        capturingGroups = originalCapturingGroups;
        findReachableGroups(tree.getElement(), tree.continuation(), impossibleBackReferences, new HashSet<>());
        // Visit the body of the loop a second time, this time with the groups that could be set in the first iteration
        impossibleBackReferences = originalImpossibleBackReferences;
        super.visitRepetition(tree);
      }
    }

    private void findReachableGroups(AutomatonState start, AutomatonState stop, Set<BackReferenceTree> preliminaryImpossibleReferences, Set<AutomatonState> visited) {
      if (start == stop || (start instanceof BackReferenceTree && preliminaryImpossibleReferences.contains(start)) || visited.contains(start)) {
        return;
      }
      visited.add(start);
      if (start instanceof EndOfCapturingGroupState) {
        addGroup(((EndOfCapturingGroupState) start).group());
      }
      for (AutomatonState successor: start.successors()) {
        findReachableGroups(successor, stop, preliminaryImpossibleReferences, visited);
      }
    }

    @Override
    protected void after(RegexParseResult regexParseResult) {
      for (BackReferenceTree backReference : impossibleBackReferences) {
        String message;
        List<RegexIssueLocation> secondaries = new ArrayList<>();
        if (capturingGroups.containsKey(backReference.groupName())) {
          message = "Fix this backreference, so that it refers to a group that can be matched before it.";
          CapturingGroupTree group = capturingGroups.get(backReference.groupName());
          secondaries.add(new RegexIssueLocation(group, "This group is used in a backreference before it is defined"));
        } else {
          message = "Fix this backreference - it refers to a capturing group that doesn't exist.";
        }
        reportIssue(backReference, message, null, secondaries);
      }
    }
  }

}
