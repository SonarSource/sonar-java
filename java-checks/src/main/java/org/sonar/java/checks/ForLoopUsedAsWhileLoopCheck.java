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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.prettyprint.FileConfig;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.prettyprint.PrintableNodesCreation.whileLoop;

@Rule(key = "S1264")
public class ForLoopUsedAsWhileLoopCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Replace this \"for\" loop with a \"while\" loop.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ForStatementTree forStatementTree = (ForStatementTree) tree;
    if (forStatementTree.initializer().isEmpty() && forStatementTree.update().isEmpty() && forStatementTree.condition() != null) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(forStatementTree.firstToken())
        .withMessage(ISSUE_MESSAGE)
        .withQuickFix(() -> computeQuickfix(forStatementTree))
        .report();
    }
  }

  private JavaQuickFix computeQuickfix(ForStatementTree oldForLoop){
    var newWhileLoop = whileLoop(oldForLoop.condition(), oldForLoop.statement());
    return JavaQuickFix.newQuickFix(ISSUE_MESSAGE)
      .addTextEdit(JavaTextEdit.replaceTree(oldForLoop, newWhileLoop, FileConfig.DEFAULT_FILE_CONFIG))
      .build();
  }

}
