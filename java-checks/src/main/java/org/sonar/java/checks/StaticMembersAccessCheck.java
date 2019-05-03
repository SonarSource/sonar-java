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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2209")
public class StaticMembersAccessCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.MEMBER_SELECT);
  }

  @Override
  public void visitNode(Tree tree) {
    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree;
    if (memberSelect.identifier().symbol().isStatic()) {
      ExpressionTree memberSelectExpression = memberSelect.expression();
      if (memberSelectExpression.is(Tree.Kind.MEMBER_SELECT)) {
        memberSelectExpression = ((MemberSelectExpressionTree) memberSelectExpression).identifier();
      }
      if (!memberSelectExpression.is(Tree.Kind.IDENTIFIER) || ((IdentifierTree) memberSelectExpression).symbol().isVariableSymbol()) {
        context.reportIssue(this, memberSelect, "Change this instance-reference to a static reference.");
      }
    }
  }
}
