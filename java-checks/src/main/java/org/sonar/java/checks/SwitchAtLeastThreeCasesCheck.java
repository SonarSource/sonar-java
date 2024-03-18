/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1301")
public class SwitchAtLeastThreeCasesCheck extends IssuableSubscriptionVisitor {

  private JavaFileScannerContext context;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
    int count = 0;
    for (CaseGroupTree caseGroup : switchStatementTree.cases()) {
      // whenever there is a type, record or guarded pattern, it would decrease readability to replace the switch by if
      // so we don't raise an issue
      if (hasLabelWithAllowedPattern(caseGroup)) {
        return;
      }
      count += caseGroup.labels().size();
    }
    if (count < 3) {
      reportIssue(switchStatementTree.switchKeyword(), "Replace this \"switch\" statement by \"if\" statements to increase readability.");
    }
  }

  private static boolean hasLabelWithAllowedPattern(CaseGroupTree caseGroupTree) {
    return caseGroupTree.labels().stream()
      .flatMap(label -> label.expressions().stream())
      .anyMatch(expression -> expression.is(Tree.Kind.TYPE_PATTERN, Tree.Kind.RECORD_PATTERN, Tree.Kind.GUARDED_PATTERN));
  }

}
