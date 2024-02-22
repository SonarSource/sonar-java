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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.GuardedPatternTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S6916")
public class SingleIfInsteadOfPatternMatchGuardCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CASE_GROUP);
  }

  @Override
  public void visitNode(Tree tree) {
    var caseGroup = (CaseGroupTree) tree;
    var caseLabel = caseGroup.labels().get(0);
    if (isCaseDefaultOrNull(caseLabel)) {
      return;
    }
    var caseExpression = caseLabel.expressions().get(0);

    // We only want to inspect type patterns that do not already have a guard
    if (!caseExpression.is(Tree.Kind.TYPE_PATTERN) || caseExpression instanceof GuardedPatternTree) {
      return;
    }
    var ifStatement = getFirstIfStatementInCaseBody(caseGroup);
    if (ifStatement != null && ifStatement.elseStatement() == null) {
      reportIssue(ifStatement, "Replace this \"if\" statement with a pattern match guard.");
    }
  }

  private static IfStatementTree getFirstIfStatementInCaseBody(CaseGroupTree caseGroup) {
    // For type patterns without the guard we are guaranteed to have a single block for the case group body
    if (caseGroup.body().get(0) instanceof BlockTree caseBlock) {
      // We need to check if the first and only element of the body is an if statement
      var blockBody = caseBlock.body();
      if (blockBody.size() == 1 && blockBody.get(0) instanceof IfStatementTree ifStatement) {
        return ifStatement;
      }
    }
    return null;
  }

  private static boolean isCaseDefaultOrNull(CaseLabelTree caseLabel) {
    return caseLabel.expressions().isEmpty();
  }

}
