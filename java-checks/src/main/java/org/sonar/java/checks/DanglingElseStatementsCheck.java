/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S5261")
public class DanglingElseStatementsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    IfStatementTree ifstmt = (IfStatementTree) tree;
    if (hasElse(ifstmt) && isNested(ifstmt)) {
      reportIssue(ifstmt.elseKeyword(), "Add explicit curly braces to avoid dangling else.");
    }
  }

  private static boolean hasElse(IfStatementTree ifstmt) {
    return ifstmt.elseStatement() != null;
  }

  private static boolean isNested(IfStatementTree ifstmt) {
    return ifstmt.parent().is(Kind.IF_STATEMENT) && ((IfStatementTree) ifstmt.parent()).thenStatement() == ifstmt;
  }
}
