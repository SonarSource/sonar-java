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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "SwitchLastCaseIsDefaultCheck",
  name = "\"switch\" statements should end with a \"default\" clause",
  tags = {"cert", "cwe", "misra"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class SwitchLastCaseIsDefaultCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
    CaseLabelTree defaultLabel = getDefaultLabel(switchStatementTree);
    CaseLabelTree lastLabel = getLastLabel(switchStatementTree);
    if (defaultLabel == null) {
      addIssue(tree, "Add a default case to this switch.");
    } else if (!defaultLabel.equals(lastLabel)) {
      addIssue(defaultLabel, "Move this default to the end of the switch.");
    }
  }


  private static CaseLabelTree getDefaultLabel(SwitchStatementTree switchStatementTree) {
    for (CaseGroupTree caseGroupTree : switchStatementTree.cases()) {
      for (CaseLabelTree caseLabelTree : caseGroupTree.labels()) {
        if (JavaKeyword.DEFAULT.getValue().equals(caseLabelTree.caseOrDefaultKeyword().text())) {
          return caseLabelTree;
        }
      }
    }
    return null;
  }

  private static CaseLabelTree getLastLabel(SwitchStatementTree switchStatementTree) {
    if (!switchStatementTree.cases().isEmpty()) {
      List<CaseLabelTree> labels = switchStatementTree.cases().get(switchStatementTree.cases().size() - 1).labels();
      if (!labels.isEmpty()) {
        return labels.get(labels.size() - 1);
      }
    }
    return null;
  }
}
