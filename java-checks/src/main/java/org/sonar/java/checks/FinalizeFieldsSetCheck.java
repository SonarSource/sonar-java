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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2165")
public class FinalizeFieldsSetCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (hasSemantic() && isFinalizeDeclaration(methodTree)) {
      methodTree.accept(new AssignmentVisitor());
    }
  }

  private static boolean isFinalizeDeclaration(MethodTree tree) {
    return isMethodNamedFinalize(tree) && hasNoParameters(tree);
  }

  private static boolean isMethodNamedFinalize(MethodTree tree) {
    return "finalize".equals(tree.simpleName().name());
  }

  private static boolean hasNoParameters(MethodTree tree) {
    return tree.parameters().isEmpty();
  }

  private class AssignmentVisitor extends BaseTreeVisitor {
    @Override
    public void visitClass(ClassTree tree) {
      // Do not visit inner classes as their methods will be visited by main visitor
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (isFieldAssignment(tree) && isNullAssignment(tree)) {
        reportIssue(tree.expression(), "Remove this nullification of \"" + getFieldName(tree) + "\".");
      }
    }

    private boolean isFieldAssignment(AssignmentExpressionTree tree) {
      ExpressionTree variable = tree.variable();
      if (variable.is(Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) variable;
        if (!ExpressionUtils.isThis(memberSelectExpressionTree.expression())) {
          return false;
        }
        variable = memberSelectExpressionTree.identifier();
      }
      if (variable.is(Kind.IDENTIFIER)) {
        Symbol variableSymbol = ((IdentifierTree) variable).symbol();
        return variableSymbol.owner().isTypeSymbol();
      }
      return false;
    }

    private boolean isNullAssignment(AssignmentExpressionTree tree) {
      return tree.expression().is(Kind.NULL_LITERAL);
    }

    private String getFieldName(AssignmentExpressionTree tree) {
      ExpressionTree variable = tree.variable();
      if (variable.is(Kind.MEMBER_SELECT)) {
        variable = ((MemberSelectExpressionTree) variable).identifier();
      }
      return ((IdentifierTree) variable).name();
    }
  }
}
