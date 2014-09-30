/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(
    key ="S1479",
    priority = Priority.MAJOR,
    tags = {"brain-overload"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class SwitchWithTooManyCasesCheck extends SubscriptionBaseVisitor {


  private static final int DEFAULT_MAXIMUM_CASES = 30;

  @RuleProperty(
      key = "maximum",
      defaultValue = "" + DEFAULT_MAXIMUM_CASES)
  public int maximumCases = DEFAULT_MAXIMUM_CASES;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
    int cases = 0;
    for (CaseGroupTree caseGroupTree : switchStatementTree.cases()) {
      cases+=caseGroupTree.labels().size();
    }
    if(cases > maximumCases) {
      addIssue(switchStatementTree, "Reduce the number of switch cases from "+cases+" to at most "+maximumCases+".");
    }
  }
}
