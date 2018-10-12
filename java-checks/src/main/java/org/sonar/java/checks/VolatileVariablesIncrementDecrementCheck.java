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
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S3078")
public class VolatileVariablesIncrementDecrementCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE_TEMPLATE = "Use an \"%s\" for this field; its %s are atomic.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.PREFIX_INCREMENT, Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT, Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.LOGICAL_COMPLEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    UnaryExpressionTree unaryExpression = (UnaryExpressionTree) tree;
    ExpressionTree expression = ExpressionUtils.skipParentheses(unaryExpression.expression());
    IdentifierTree identifier = getVariableIdentifier(expression);

    if (identifier == null || !identifier.symbol().isVolatile()) {
      return;
    }

    if (tree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      checkBooleanToggling(tree, identifier);
    } else if (tree.is(Tree.Kind.PREFIX_INCREMENT) || tree.is(Tree.Kind.POSTFIX_INCREMENT)) {
      checkIncrementDecrement(tree, identifier, "increments");
    } else {
      checkIncrementDecrement(tree, identifier, "decrements");
    }
  }

  @Nullable
  private static IdentifierTree getVariableIdentifier(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) expressionTree;
    } else if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) expressionTree).identifier();
    }
    return null;
  }

  private void checkBooleanToggling(@Nullable Tree tree, IdentifierTree identifier) {
    Tree parent = tree != null ? tree.parent() : null;
    if (parent != null) {
      if (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
        checkBooleanToggling(parent, identifier);
      } else if (parent.is(Tree.Kind.ASSIGNMENT)) {
        ExpressionTree variable = ((AssignmentExpressionTree) parent).variable();
        if ((variable.is(Tree.Kind.IDENTIFIER) && identifier.name().equals(((IdentifierTree) variable).name()))
          || (variable.is(Tree.Kind.MEMBER_SELECT) && identifier.name().equals(((MemberSelectExpressionTree) variable).identifier().name()))) {
          reportIssue(parent, "Use an \"AtomicBoolean\" for this field");
        }
      }
    }
  }

  private void checkIncrementDecrement(Tree tree, IdentifierTree identifier, String type) {
    if (identifier.symbol().type().is("int") || identifier.symbol().type().is("java.lang.Integer")) {
      reportIssue(tree, String.format(MESSAGE_TEMPLATE, "AtomicInteger", type));
    } else if (identifier.symbol().type().is("long") || identifier.symbol().type().is("java.lang.Long")) {
      reportIssue(tree, String.format(MESSAGE_TEMPLATE, "AtomicLong", type));
    }
  }

}
