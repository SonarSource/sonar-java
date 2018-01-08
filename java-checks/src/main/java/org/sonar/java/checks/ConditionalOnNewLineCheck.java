/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

@Rule(key = "S3972")
public class ConditionalOnNewLineCheck extends IssuableSubscriptionVisitor {

  private SyntaxToken previousToken;

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.IF_STATEMENT);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    previousToken = null;
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    IfStatementTree ifStatementTree = (IfStatementTree) tree;
    SyntaxToken lastToken = ifStatementTree.thenStatement().lastToken();
    if (ifStatementTree.elseKeyword() == null) {
      if(previousToken != null && isOnSameLineAsPreviousIf(ifStatementTree)) {
        reportIssue(ifStatementTree.ifKeyword(), "Move this \"if\" to a new line or add the missing \"else\".",
          Collections.singletonList(new JavaFileScannerContext.Location("", previousToken)), null);
      }
      previousToken = lastToken;
    }
  }

  private boolean isOnSameLineAsPreviousIf(IfStatementTree ifStatementTree) {
    // check column for nested if on one line case.
    return previousToken.line() == ifStatementTree.ifKeyword().line() && previousToken.column() < ifStatementTree.ifKeyword().column();
  }
}
