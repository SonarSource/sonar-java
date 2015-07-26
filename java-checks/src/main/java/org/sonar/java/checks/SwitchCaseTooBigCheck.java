/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1151",
  name = "\"switch case\" clauses should not have too many lines",
  tags = {"brain-overload"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
public class SwitchCaseTooBigCheck extends SubscriptionBaseVisitor {

  private static final int DEFAULT_MAX = 5;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX,
    description = "Maximum number of lines")
  public int max = DEFAULT_MAX;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
    for (CaseGroupTree caseGroupTree : switchStatementTree.cases()) {
      List<CaseLabelTree> labels = caseGroupTree.labels();
      for (int i = 0; i < labels.size() - 1; i++) {
        CaseLabelTree currentLabel = labels.get(i);
        int caseStartLine = line(currentLabel);
        int nextCaseStartLine = line(labels.get(i + 1));

        check(currentLabel, caseStartLine, nextCaseStartLine);
      }

      CaseLabelTree lastLabel = Iterables.getLast(labels);
      int lastLabelLine = line(lastLabel);
      int statementLine = firstStatementLine(caseGroupTree.body(), lastLabelLine);
      int startLine = Math.min(lastLabelLine + 1, statementLine);
      int endLine = getNextLine(switchStatementTree, caseGroupTree);
      check(lastLabel, startLine, endLine);
    }
  }

  private static int firstStatementLine(List<StatementTree> body, int lastLabelLine) {
    if (!body.isEmpty()) {
      StatementTree firstStatement = body.get(0);
      int firstStatementLine = line(body.get(0));

      List<SyntaxTrivia> trivias = FirstSyntaxTokenFinder.firstSyntaxToken(firstStatement).trivias();
      if (!trivias.isEmpty()) {
        int firstLineTrivia = firstLineTrivia(trivias);
        if(firstLineTrivia == lastLabelLine) {
          firstLineTrivia = firstLineTrivia+1;
        }
        return Math.min(firstLineTrivia, firstStatementLine);
      }
      return firstStatementLine;
    }
    return Integer.MAX_VALUE;
  }

  private static int firstLineTrivia(List<SyntaxTrivia> trivias) {
    return trivias.get(0).startLine();
  }

  private void check(CaseLabelTree caseLabelTree, int caseStartLine, int nextCaseStartLine) {
    int lines = Math.max(nextCaseStartLine - caseStartLine, 1);

    if (lines > max) {
      addIssue(caseLabelTree, "Reduce this switch case number of lines from " + lines + " to at most " + max + ", for example by extracting code into methods.");
    }
  }

  private static int getNextLine(SwitchStatementTree switchStatementTree, CaseGroupTree caseGroupTree) {
    int switchLastLine = line(switchStatementTree.closeBraceToken());
    List<CaseGroupTree> cases = switchStatementTree.cases();
    int indexOfCaseGroup = cases.indexOf(caseGroupTree);
    if (indexOfCaseGroup == cases.size() - 1) {
      return switchLastLine;
    } else {
      return line(cases.get(indexOfCaseGroup + 1));
    }
  }

  private static int line(Tree tree) {
    return FirstSyntaxTokenFinder.firstSyntaxToken(tree).line();
  }
}
