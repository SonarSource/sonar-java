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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import javax.annotation.CheckForNull;
import java.util.List;

@Rule(key = "S2445")
public class SynchronizedFieldAssignmentCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      SynchronizedStatementTree sst = (SynchronizedStatementTree) tree;
      Symbol field = getField(sst.expression());
      if (field != null) {
        sst.block().accept(new AssignmentVisitor(field, sst.expression()));
      }
    }
  }

  @CheckForNull
  private static Symbol getField(ExpressionTree tree) {
    if (tree.is(Kind.IDENTIFIER)) {
      Symbol reference = ((IdentifierTree) tree).symbol();
      if (!reference.isUnknown() && reference.owner().isTypeSymbol()) {
        return reference;
      }
    } else if (tree.is(Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree;
      if (isField(mse)) {
        return getField(mse.identifier());
      }
    }
    return null;
  }

  private static boolean isField(ExpressionTree tree) {
    if (tree.is(Kind.IDENTIFIER)) {
      Symbol reference = ((IdentifierTree) tree).symbol();
      return !reference.isUnknown() && reference.owner().isTypeSymbol();
    } else if (tree.is(Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree;
      ExpressionTree mseExpression = mse.expression();
      if (isThis(mseExpression)) {
        return isField(mse.identifier());
      } else {
        return isField(mseExpression);
      }
    }
    return false;
  }

  private static boolean isThis(ExpressionTree expression) {
    return expression.is(Kind.IDENTIFIER) && "this".equals(((IdentifierTree) expression).name());
  }

  private class AssignmentVisitor extends BaseTreeVisitor {

    private final Symbol field;
    private final Tree synchronizedStatement;

    public AssignmentVisitor(Symbol field, Tree tree) {
      this.field = field;
      this.synchronizedStatement = tree;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      checkSymbolAssignment(tree.variable());
    }

    private void checkSymbolAssignment(Tree variable) {
      if (variable.is(Kind.IDENTIFIER)) {
        if (field.equals(((IdentifierTree) variable).symbol())) {
          reportIssue(
            synchronizedStatement,
            String.format("Don't synchronize on \"%s\" or remove its reassignment on line %d.", field.name(), variable.firstToken().line()));
        }
      } else if (variable.is(Kind.MEMBER_SELECT)) {
        checkSymbolAssignment(((MemberSelectExpressionTree) variable).identifier());
      }
    }

  }
}
