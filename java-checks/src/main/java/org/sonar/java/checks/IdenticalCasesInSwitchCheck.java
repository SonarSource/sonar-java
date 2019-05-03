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
package org.sonar.java.checks;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import org.sonar.check.Rule;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

@Rule(key = "S1871")
public class IdenticalCasesInSwitchCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree node) {
    if (node.is(Tree.Kind.SWITCH_STATEMENT)) {
      SwitchStatementTree switchStatement = (SwitchStatementTree) node;
      Multimap<CaseGroupTree, CaseGroupTree> identicalBranches = checkSwitchStatement(switchStatement);
      boolean allBranchesSame = allBranchesSame(identicalBranches, switchStatement.cases().size());
      boolean allBranchesSameWithoutDefault = allBranchesSame && !hasDefaultClause(switchStatement);
      if (!allBranchesSame || allBranchesSameWithoutDefault) {
        identicalBranches.asMap().forEach((first, others) -> {
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

  protected static boolean allBranchesSame(Multimap<? extends Tree, ? extends Tree> identicalBranches, int size) {
    return identicalBranches.keySet().size() == 1 && identicalBranches.size() == size - 1;
  }

  private static boolean isTrivialCase(List<StatementTree> body) {
    return body.size() == 1 || (body.size() == 2 && body.get(1).is(Tree.Kind.BREAK_STATEMENT));
  }

  protected Multimap<CaseGroupTree, CaseGroupTree> checkSwitchStatement(SwitchStatementTree node) {
    SetMultimap<CaseGroupTree, CaseGroupTree> identicalBranches = HashMultimap.create();
    int index = 0;
    List<CaseGroupTree> cases = node.cases();
    for (CaseGroupTree caseGroupTree : cases) {
      index++;
      if (identicalBranches.containsValue(caseGroupTree)) {
        continue;
      }
      for (int i = index; i < cases.size(); i++) {
        if (SyntacticEquivalence.areEquivalent(caseGroupTree.body(), cases.get(i).body())) {
          identicalBranches.put(caseGroupTree, cases.get(i));
        }
      }
    }
    return identicalBranches;
  }

  protected static class IfElseChain {
    Multimap<StatementTree, StatementTree> branches = HashMultimap.create();
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
    for (int i = 0; i < allBranches.size(); i++) {
      if (ifElseChain.branches.containsValue(allBranches.get(i))) {
        continue;
      }
      for (int j = i + 1; j < allBranches.size(); j++) {
        if (SyntacticEquivalence.areEquivalent(allBranches.get(i), allBranches.get(j))) {
          ifElseChain.branches.put(allBranches.get(i), allBranches.get(j));
        }
      }
    }
    ifElseChain.totalBranchCount = allBranches.size();
    return ifElseChain;
  }

  private void reportIdenticalIfChainBranches(Multimap<StatementTree, StatementTree> identicalBranches, int totalBranchCount, boolean withElseClause) {
    boolean allBranchesSame = allBranchesSame(identicalBranches, totalBranchCount);
    boolean allBranchesSameWithoutElse = allBranchesSame && !withElseClause;
    if (!allBranchesSame || allBranchesSameWithoutElse) {
      identicalBranches.asMap().forEach((first, others) -> {
        if (!isTrivialIfStatement(first) || allBranchesSameWithoutElse) {
          others.forEach(other -> createIssue(other, issueMessage("branch", first), first));
        }
      });
    }
  }

  private static boolean isTrivialIfStatement(StatementTree node) {
    return !node.is(Tree.Kind.BLOCK) || ((BlockTree) node).body().size() <= 1;
  }

  protected static boolean hasDefaultClause(SwitchStatementTree switchStatement) {
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
    return "This " + type + "'s code block is the same as the block for the " + type + " on line " + node.firstToken().line() + ".";
  }

}
