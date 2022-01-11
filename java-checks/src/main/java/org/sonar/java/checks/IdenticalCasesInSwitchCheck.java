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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1871")
public class IdenticalCasesInSwitchCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.SWITCH_EXPRESSION, Tree.Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree node) {
    if (node.is(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.SWITCH_EXPRESSION)) {
      SwitchTree switchTree = (SwitchTree) node;
      Map<CaseGroupTree, Set<CaseGroupTree>> identicalBranches = checkSwitchStatement(switchTree);
      boolean allBranchesSame = allBranchesSame(identicalBranches, switchTree.cases().size());
      boolean allBranchesSameWithoutDefault = allBranchesSame && !hasDefaultClause(switchTree);
      if (!allBranchesSame || allBranchesSameWithoutDefault) {
        identicalBranches.forEach((first, others) -> {
          if (!isTrivialCase(first.body()) || allBranchesSameWithoutDefault) {
            others.forEach(other -> createIssue(other, issueMessage("case", first), first));
          }
        });
      }
    } else if (node.is(Tree.Kind.IF_STATEMENT) && !node.parent().is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatement = (IfStatementTree) node;
      IfElseChain ifElseChain = checkIfStatement(ifStatement);
      reportIdenticalIfChainBranches(ifElseChain.branches, ifElseChain.totalBranchCount, hasElseClause(ifStatement));
    }
  }

  protected static boolean allBranchesSame(Map<? extends Tree, ? extends Set<? extends Tree>> identicalBranches, int size) {
    return identicalBranches.keySet().size() == 1 && identicalBranchesSize(identicalBranches) == size - 1;
  }
  
  private static long identicalBranchesSize(Map<? extends Tree, ? extends Set<? extends Tree>> identicalBranches) {
    return identicalBranches.values().stream().flatMap(Collection::stream).count();
  }
  
  private static boolean isTrivialCase(List<StatementTree> body) {
    return body.size() == 1 || (body.size() == 2 && body.get(1).is(Tree.Kind.BREAK_STATEMENT));
  }

  protected Map<CaseGroupTree, Set<CaseGroupTree>> checkSwitchStatement(SwitchTree node) {
    Map<CaseGroupTree, Set<CaseGroupTree>> identicalBranches = new HashMap<>();
    int index = 0;
    List<CaseGroupTree> cases = node.cases();
    Set<CaseGroupTree> duplicates = new HashSet<>();
    for (CaseGroupTree caseGroupTree : cases) {
      index++;
      if (duplicates.contains(caseGroupTree)) {
        continue;
      }
      for (int i = index; i < cases.size(); i++) {
        CaseGroupTree caseI = cases.get(i);
        if (SyntacticEquivalence.areEquivalent(caseGroupTree.body(), caseI.body())) {
          duplicates.add(caseI);
          identicalBranches.computeIfAbsent(caseGroupTree, k -> new HashSet<>()).add(caseI);
        }
      }
    }
    return identicalBranches;
  }

  protected static class IfElseChain {
    Map<StatementTree, Set<StatementTree>> branches = new HashMap<>();
    int totalBranchCount;
  }

  protected static IfElseChain checkIfStatement(IfStatementTree node) {
    IfElseChain ifElseChain = new IfElseChain();
    ifElseChain.totalBranchCount = 1;
    List<StatementTree> allBranches = new ArrayList<>();
    allBranches.add(node.thenStatement());
    StatementTree elseStatement = node.elseStatement();
    while (elseStatement != null && elseStatement.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatement = (IfStatementTree) elseStatement;
      allBranches.add(ifStatement.thenStatement());
      elseStatement = ifStatement.elseStatement();
    }
    if (elseStatement != null) {
      allBranches.add(elseStatement);
    }
    return collectIdenticalBranches(allBranches);
  }

  private static IfElseChain collectIdenticalBranches(List<StatementTree> allBranches) {
    IfElseChain ifElseChain = new IfElseChain();
    Set<StatementTree> duplicates = new HashSet<>();
    for (int i = 0; i < allBranches.size(); i++) {
      if (duplicates.contains(allBranches.get(i))) {
        continue;
      }
      for (int j = i + 1; j < allBranches.size(); j++) {
        StatementTree statement1 = allBranches.get(i);
        StatementTree statement2 = allBranches.get(j);
        if (SyntacticEquivalence.areEquivalent(statement1, statement2)) {
          duplicates.add(statement2);
          ifElseChain.branches.computeIfAbsent(statement1, k -> new HashSet<>()).add(statement2);
        }
      }
    }
    ifElseChain.totalBranchCount = allBranches.size();
    return ifElseChain;
  }

  private void reportIdenticalIfChainBranches(Map<StatementTree, Set<StatementTree>> identicalBranches, int totalBranchCount, boolean withElseClause) {
    boolean allBranchesSame = allBranchesSame(identicalBranches, totalBranchCount);
    boolean allBranchesSameWithoutElse = allBranchesSame && !withElseClause;
    if (!allBranchesSame || allBranchesSameWithoutElse) {
      identicalBranches.forEach((first, others) -> {
        if (!isTrivialIfStatement(first) || allBranchesSameWithoutElse) {
          others.forEach(other -> createIssue(other, issueMessage("branch", first), first));
        }
      });
    }
  }

  private static boolean isTrivialIfStatement(StatementTree node) {
    return !node.is(Tree.Kind.BLOCK) || ((BlockTree) node).body().size() <= 1;
  }

  protected static boolean hasDefaultClause(SwitchTree switchStatement) {
    return switchStatement.cases().stream()
      .flatMap(caseGroupTree -> caseGroupTree.labels().stream())
      .anyMatch(caseLabelTree -> caseLabelTree.caseOrDefaultKeyword().text().equals("default"));
  }

  protected static boolean hasElseClause(IfStatementTree ifStatement) {
    StatementTree elseStatement = ifStatement.elseStatement();
    while (elseStatement != null && elseStatement.is(Tree.Kind.IF_STATEMENT)) {
      elseStatement = ((IfStatementTree) elseStatement).elseStatement();
    }
    return elseStatement != null;
  }

  private void createIssue(Tree node, String message, Tree secondary) {
    reportIssue(node, message, Collections.singletonList(new JavaFileScannerContext.Location("Original", secondary)), null);
  }

  private static String issueMessage(String type, Tree node) {
    return "This " + type + "'s code block is the same as the block for the " + type +
      " on line " + node.firstToken().range().start().line() + ".";
  }

}
