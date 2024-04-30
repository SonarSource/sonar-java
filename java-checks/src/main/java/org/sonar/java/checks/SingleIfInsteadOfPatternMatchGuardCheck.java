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
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.prettyprint.FileConfig;
import org.sonar.java.checks.prettyprint.PrettyPrintStringBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.GuardedPatternTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.NullPatternTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S6916")
public class SingleIfInsteadOfPatternMatchGuardCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String ISSUE_MESSAGE_REPLACE = "Replace this \"if\" statement with a pattern match guard.";
  private static final String ISSUE_MESSAGE_MERGE = "Merge this \"if\" statement with the enclosing pattern match guard.";

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CASE_GROUP);
  }

  @Override
  public void visitNode(Tree tree) {
    var caseGroup = (CaseGroupTree) tree;

    var ifStatement = getFirstIfStatementInCaseBody(caseGroup);
    // We do not want to inspect case groups where the body does not have an if statement or if it has also an else statement
    if (ifStatement == null || ifStatement.elseStatement() != null) {
      return;
    }
    var caseLabel = caseGroup.labels().get(0);
    var pattern = extractNonDefaultPattern(caseLabel);
    if (pattern == null) {
      return;
    }

    QuickFixHelper.newIssue(context).forRule(this)
      .onTree(ifStatement)
      .withMessage(pattern instanceof GuardedPatternTree ? ISSUE_MESSAGE_MERGE : ISSUE_MESSAGE_REPLACE)
      .withQuickFix(() -> computeQuickFix(caseGroup, pattern, ifStatement, context))
      .report();

  }

  private static IfStatementTree getFirstIfStatementInCaseBody(CaseGroupTree caseGroup) {
    if (!caseGroup.body().isEmpty() && caseGroup.body().get(0) instanceof BlockTree caseBlock) {
      // We need to check if the first and only element of the body is an if statement
      var blockBody = caseBlock.body();
      if (blockBody.size() == 1 && blockBody.get(0) instanceof IfStatementTree ifStatement) {
        return ifStatement;
      }
    }
    return null;
  }

  private static PatternTree extractNonDefaultPattern(CaseLabelTree caseLabel) {
    return (caseLabel.expressions().size() == 1
      && caseLabel.expressions().get(0) instanceof PatternTree patternTree
      && !(patternTree instanceof NullPatternTree)
    ) ?
      patternTree : null;
  }

  private static JavaQuickFix computeQuickFix(CaseGroupTree caseGroup, PatternTree pattern, IfStatementTree ifStatement,
                                              JavaFileScannerContext context) {
    var shouldMergeConditions = pattern instanceof GuardedPatternTree;
    var quickFixBuilder = JavaQuickFix.newQuickFix(shouldMergeConditions ? ISSUE_MESSAGE_MERGE : ISSUE_MESSAGE_REPLACE);
    var pps = new PrettyPrintStringBuilder(FileConfig.DEFAULT_FILE_CONFIG, caseGroup.firstToken(), false);
    pps.add("case ");
    if (pattern instanceof GuardedPatternTree guardedPattern){
      pps.addTreeContentRaw(guardedPattern.pattern(), context)
        .add(" when ").addBinop(guardedPattern.expression(), Tree.Kind.CONDITIONAL_AND, ifStatement.condition(), context);
    } else {
      pps.addTreeContentRaw(pattern, context).add(" when ").addTreeContentRaw(ifStatement.condition(), context);
    }
    pps.add(" -> ").addTreeContentWithIndentBasedOnLastLine(ifStatement.thenStatement(), context);
    quickFixBuilder.addTextEdit(JavaTextEdit.replaceTree(caseGroup, pps.toString()));
    return quickFixBuilder.build();
  }

}
