/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Set;

@Rule(key = "S1871")
public class IdenticalCasesInSwitchCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.IF_STATEMENT, Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree node) {
    if (node.is(Tree.Kind.SWITCH_STATEMENT)) {
      checkSwitchStatement((SwitchStatementTree) node);
    } else if (node.is(Tree.Kind.IF_STATEMENT)) {
      checkIfStatement((IfStatementTree) node);
    } else {
      checkConditionalExpression((ConditionalExpressionTree) node);
    }
  }

  public void checkSwitchStatement(SwitchStatementTree node) {
    int index = 0;
    List<CaseGroupTree> cases = node.cases();
    Set<CaseLabelTree> reportedLabels = Sets.newHashSet();
    for (CaseGroupTree caseGroupTree : cases) {
      index++;
      for (int i = index; i < cases.size(); i++) {
        checkCaseEquivalence(reportedLabels, caseGroupTree, cases.get(i));
      }
    }
  }

  private void checkCaseEquivalence(Set<CaseLabelTree> reportedLabels, CaseGroupTree caseGroupTree, CaseGroupTree current) {
    if (SyntacticEquivalence.areEquivalent(caseGroupTree.body(), current.body())) {
      CaseLabelTree labelToReport = getLastLabel(current);
      if (!reportedLabels.contains(labelToReport)) {
        reportedLabels.add(labelToReport);
        createIssue(current, issueMessage("case", caseGroupTree), caseGroupTree);
      }
    }
  }

  private void checkIfStatement(IfStatementTree node) {
    StatementTree thenStatement = node.thenStatement();
    StatementTree elseStatement = node.elseStatement();
    while (elseStatement != null && elseStatement.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatement = (IfStatementTree) elseStatement;
      if (areIfBlocksSyntacticalEquivalent(thenStatement, ifStatement.thenStatement())) {
        createIssue(ifStatement.thenStatement(), issueMessage("branch", thenStatement), thenStatement);
        break;
      }
      elseStatement = ifStatement.elseStatement();
    }
    if (elseStatement != null && areIfBlocksSyntacticalEquivalent(thenStatement, elseStatement)) {
      createIssue(elseStatement, issueMessage("branch", thenStatement), thenStatement);
    }
  }

  private void createIssue(Tree node, String message, Tree secondary) {
    reportIssue(node, message, ImmutableList.of(new JavaFileScannerContext.Location("Original", secondary)), null);
  }

  private static boolean areIfBlocksSyntacticalEquivalent(StatementTree first, StatementTree second) {
    return isNotEmptyBlock(first) && SyntacticEquivalence.areEquivalent(first, second);
  }

  private static boolean isNotEmptyBlock(StatementTree node) {
    return !(node.is(Tree.Kind.BLOCK) && ((BlockTree) node).body().isEmpty());
  }

  private static String issueMessage(String type, Tree node) {
    return "This " + type + "'s code block is the same as the block for the " + type + " on line " + node.firstToken().line() + ".";
  }

  private void checkConditionalExpression(ConditionalExpressionTree node) {
    if (SyntacticEquivalence.areEquivalent(ExpressionUtils.skipParentheses(node.trueExpression()), ExpressionUtils.skipParentheses(node.falseExpression()))) {
      createIssue(node.falseExpression(), "This conditional operation returns the same value whether the condition is \"true\" or \"false\".", node.trueExpression());
    }
  }

  private static CaseLabelTree getLastLabel(CaseGroupTree cases) {
    return cases.labels().get(cases.labels().size() - 1);
  }

}
