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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.LineUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

@Rule(key = "S2147")
public class CombineCatchCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    List<CatchTree> catches = new ArrayList<>();
    for (CatchTree catchTree : ((TryStatementTree) tree).catches()) {
      for (CatchTree catchTreeToBeCompared : catches) {
        if (SyntacticEquivalence.areSemanticallyEquivalent(catchTree.block().body(), catchTreeToBeCompared.block().body())) {
          reportIssueWithQuickFix(catchTree, catchTreeToBeCompared);
          break;
        }
      }
      catches.add(catchTree);
    }
  }

  private void reportIssueWithQuickFix(CatchTree catchTree, CatchTree catchTreeToBeCompared) {
    String quickFixMessage = "Combine this catch with the one at line " + LineUtils.startLine(catchTreeToBeCompared.catchKeyword());
    String issueMessage = quickFixMessage + ", which has the same body." + context.getJavaVersion().java7CompatibilityMessage();
    List<JavaFileScannerContext.Location> flow = Collections.singletonList(new JavaFileScannerContext.Location("Combine with this catch", catchTreeToBeCompared));
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(catchTree.parameter())
      .withMessage(issueMessage)
      .withSecondaries(flow)
      .withQuickFix( () -> computeQuickFix(catchTree, catchTreeToBeCompared, quickFixMessage) )
      .report();
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }

  private JavaQuickFix computeQuickFix(CatchTree catchTree, CatchTree catchTreeToBeCompared, String qfMessage) {
    var builder = JavaQuickFix.newQuickFix(qfMessage);
    builder.addTextEdit(JavaTextEdit.removeTree(catchTree));
    SyntaxToken openParent = catchTreeToBeCompared.openParenToken();
    builder.addTextEdit(
      JavaTextEdit.insertAfterTree(
        openParent,
        QuickFixHelper.contentForTree(catchTree.parameter().type(), context) + " | "));
    return builder.build();
  }

}
