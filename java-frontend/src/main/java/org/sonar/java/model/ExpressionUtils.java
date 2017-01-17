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
package org.sonar.java.model;

import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public final class ExpressionUtils {

  private ExpressionUtils() {
  }

  /**
   * In case of simple assignments, only the expression is evaluated, as we only use the reference to the variable to store the result.
   * For SE-Based checks, only a single value should be unstacked if its the case. For other cases, two values should be unstacked.
   * See JLS8-15.26
   *
   * @param tree The assignment tree
   * @return true if the tree is a simple assignment
   * @see #extractIdentifier(AssignmentExpressionTree)
   */
  public static boolean isSimpleAssignment(AssignmentExpressionTree tree) {
    if (!tree.is(Tree.Kind.ASSIGNMENT)) {
      // This can't possibly be a simple assignment.
      return false;
    }

    ExpressionTree variable = ExpressionUtils.skipParentheses(tree.variable());
    return variable.is(Tree.Kind.IDENTIFIER) || (variable.is(Tree.Kind.MEMBER_SELECT) && isOwningInstanceAssignment((MemberSelectExpressionTree) variable));
  }

  public static boolean isSimpleAssignment(MemberSelectExpressionTree memberSelectExpressionTree) {
    return isOwningInstanceAssignment(memberSelectExpressionTree);
  }

  public static IdentifierTree extractIdentifier(AssignmentExpressionTree tree) {
    ExpressionTree variable = skipParentheses(tree.variable());
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) variable;
    }

    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree selectTree = (MemberSelectExpressionTree) variable;
      if (isOwningInstanceAssignment(selectTree)) {
        return selectTree.identifier();
      }
    }

    // This should not be possible.
    throw new IllegalArgumentException("Can not extract identifier.");
  }

  private static boolean isOwningInstanceAssignment(MemberSelectExpressionTree tree) {
    if (!tree.expression().is(Tree.Kind.IDENTIFIER)) {
      // This is no longer simple.
      return false;
    }

    SyntaxToken variableToken = tree.firstToken();
    return variableToken != null && ("this".equalsIgnoreCase(variableToken.text()) || "super".equalsIgnoreCase(variableToken.text()));
  }

  public static ExpressionTree skipParentheses(ExpressionTree tree) {
    ExpressionTree result = tree;
    while (result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      result = ((ParenthesizedTree) result).expression();
    }
    return result;
  }
}
