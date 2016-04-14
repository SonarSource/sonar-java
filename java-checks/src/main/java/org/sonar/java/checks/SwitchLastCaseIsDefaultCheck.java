/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "SwitchLastCaseIsDefaultCheck")
@RspecKey("S131")
public class SwitchLastCaseIsDefaultCheck extends IssuableSubscriptionVisitor {

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
      reportIssue(switchStatementTree.switchKeyword(), "Add a default case to this switch.");
    } else if (!defaultLabel.equals(lastLabel)) {
      reportIssue(defaultLabel, "Move this default to the end of the switch.");
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
