/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

@Rule(key = "S6205")
public class SwitchRedundantKeywordCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Remove this redundant %s.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CASE_GROUP);
  }

  @Override
  public void visitNode(Tree tree) {
    CaseGroupTree caseGroupTree = (CaseGroupTree) tree;
    boolean isCaseWithArrow = caseGroupTree.labels().stream().noneMatch(CaseLabelTree::isFallThrough);
    if (isCaseWithArrow) {
      caseGroupTree.body().stream()
        .filter(t -> t.is(Tree.Kind.BLOCK))
        .map(BlockTree.class::cast)
        .filter(b -> !b.body().isEmpty())
        .forEach(this::reportRedundantKeywords);
    }
  }

  private void reportRedundantKeywords(BlockTree blockTree) {
    List<StatementTree> body = blockTree.body();
    int statementsInBody = body.size();
    StatementTree lastStatement = body.get(statementsInBody - 1);

    if (statementsInBody == 1) {
      if (lastStatement.is(Tree.Kind.YIELD_STATEMENT)) {
        SyntaxToken yieldKeyword = ((YieldStatementTree) lastStatement).yieldKeyword();
        // Yield can never be implicit in a block, still checking it for defensive programming
        if (yieldKeyword != null) {
          reportStatementInBlock(yieldKeyword, blockTree, "block and \"yield\"");
        }
      } else {
        reportIssue(blockTree.openBraceToken(),
          String.format(MESSAGE, "block"),
          Collections.singletonList(new JavaFileScannerContext.Location("Redundant close brace", blockTree.closeBraceToken())),
          null);
      }
    } else if (lastStatement.is(Tree.Kind.BREAK_STATEMENT)) {
      if (statementsInBody == 2) {
        reportStatementInBlock(lastStatement, blockTree, "block and \"break\"");
      } else {
        reportIssue(lastStatement, String.format(MESSAGE, "\"break\""));
      }
    }
  }

  private void reportStatementInBlock(Tree statement, BlockTree blockTree, String redundantParts) {
    reportIssue(statement, String.format(MESSAGE, redundantParts), blockBraceLocations(blockTree), null);
  }

  private static List<JavaFileScannerContext.Location> blockBraceLocations(BlockTree blockTree) {
    return Arrays.asList(new JavaFileScannerContext.Location("Redundant opening brace", blockTree.openBraceToken()),
      new JavaFileScannerContext.Location("Redundant closing brace", blockTree.closeBraceToken()));
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava14Compatible();
  }

}
