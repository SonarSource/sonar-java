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
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "S1479")
public class SwitchWithTooManyCasesCheck extends IssuableSubscriptionVisitor {


  private static final int DEFAULT_MAXIMUM_CASES = 30;

  @RuleProperty(
    key = "maximum",
    description = "Maximum number of case",
    defaultValue = "" + DEFAULT_MAXIMUM_CASES)
  public int maximumCases = DEFAULT_MAXIMUM_CASES;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
    Set<CaseLabelTree> cases = new HashSet<>();
    for (CaseGroupTree caseGroupTree : switchStatementTree.cases()) {
      cases.addAll(caseGroupTree.labels());
    }
    int size = cases.size();
    if (size > maximumCases) {
      List<JavaFileScannerContext.Location> secondary = new ArrayList<>();
      for (Tree element : cases) {
        secondary.add(new JavaFileScannerContext.Location("Case + 1", element));
      }
      reportIssue(switchStatementTree.switchKeyword(), "Reduce the number of switch cases from " + size + " to at most " + maximumCases + ".", secondary, null);
    }
  }
}
