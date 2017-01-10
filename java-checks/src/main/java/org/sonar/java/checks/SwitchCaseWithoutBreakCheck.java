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

import com.google.common.collect.Lists;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

@Rule(key = "S128")
public class SwitchCaseWithoutBreakCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree switchStatement) {
    switchStatement.cases().stream()
      // Exclude the last case as stated in RSPEC. This also excludes switches with no or a single case group.
      .limit(Math.max(0, switchStatement.cases().size() - 1))
      .forEach(caseGroup -> {
        // Assign issues to the last label in the group
        CaseLabelTree caseLabel = caseGroup.labels().get(caseGroup.labels().size() - 1);

          // Reverse the body as commonly the unconditional exit will be at the end of the body.
        if (Lists.reverse(caseGroup.body()).stream().noneMatch(SwitchCaseWithoutBreakCheck::isUnconditionalExit)) {
          context.reportIssue(this, caseLabel, "End this switch case with an unconditional break, return or throw statement.");
        }
      });

    super.visitSwitchStatement(switchStatement);
  }

  private static boolean isUnconditionalExit(Tree syntaxNode) {
    switch (syntaxNode.kind()) {
      case BREAK_STATEMENT:
      case THROW_STATEMENT:
      case RETURN_STATEMENT:
      case CONTINUE_STATEMENT:
        return true;
      case BLOCK:
        return ((BlockTree) syntaxNode).body().stream().anyMatch(SwitchCaseWithoutBreakCheck::isUnconditionalExit);
      case TRY_STATEMENT:
        return isUnconditionalExitInTryCatchStatement((TryStatementTree) syntaxNode);
      case IF_STATEMENT:
        return isUnconditionalExitInIfStatement((IfStatementTree) syntaxNode);
      default:
        return false;
    }
  }

  private static boolean isUnconditionalExitInTryCatchStatement(TryStatementTree tryStatement) {
    return isUnconditionalExit(tryStatement.block())
        && tryStatement.catches().stream().allMatch(catchTree -> isUnconditionalExit(catchTree.block()));
  }

  private static boolean isUnconditionalExitInIfStatement(IfStatementTree ifStatement) {
    if (!isUnconditionalExit(ifStatement.thenStatement())) {
      return false;
    }

    StatementTree elseStatement = ifStatement.elseStatement();
    if (elseStatement == null) {
      // Without else this is a conditional exit.
      return false;
    }

    return isUnconditionalExit(elseStatement);
  }
}
