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

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S3078")
public class VolatileVariablesOperationsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.PREFIX_INCREMENT,
      Tree.Kind.POSTFIX_INCREMENT,
      Tree.Kind.PREFIX_DECREMENT,
      Tree.Kind.POSTFIX_DECREMENT,
      Tree.Kind.LOGICAL_COMPLEMENT,
      Tree.Kind.MULTIPLY_ASSIGNMENT,
      Tree.Kind.DIVIDE_ASSIGNMENT,
      Tree.Kind.REMAINDER_ASSIGNMENT,
      Tree.Kind.PLUS_ASSIGNMENT,
      Tree.Kind.MINUS_ASSIGNMENT,
      Tree.Kind.LEFT_SHIFT_ASSIGNMENT,
      Tree.Kind.RIGHT_SHIFT_ASSIGNMENT,
      Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
      Tree.Kind.XOR_ASSIGNMENT,
      Tree.Kind.OR_ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ExpressionTree expression;
    if (tree instanceof UnaryExpressionTree) {
      expression = ExpressionUtils.skipParentheses(((UnaryExpressionTree) tree).expression());
    } else {
      expression = ((AssignmentExpressionTree) tree).variable();
    }
    IdentifierTree identifier = getVariableIdentifier(expression);
    if (identifier == null || !identifier.symbol().isVolatile()) {
      return;
    }

    if (tree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      checkBooleanToggling(tree, identifier.symbol());
    } else {
      checkIncrementDecrement(tree, identifier);
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

  private void checkBooleanToggling(Tree tree, Symbol identifierSymbol) {
    Tree parent = tree.parent();
    if (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      checkBooleanToggling(parent, identifierSymbol);
    } else if (parent.is(Tree.Kind.ASSIGNMENT)) {
      IdentifierTree variableIdentifier = getVariableIdentifier(((AssignmentExpressionTree) parent).variable());
      if (variableIdentifier != null && identifierSymbol.equals(variableIdentifier.symbol())) {
        reportIssueIfNotInExcludedContext(tree, "AtomicBoolean");
      }
    }
  }

  private void checkIncrementDecrement(Tree tree, IdentifierTree identifier) {
    Type symbolType = identifier.symbol().type();
    if (symbolType.is("int") || symbolType.is("java.lang.Integer")) {
      reportIssueIfNotInExcludedContext(tree, "AtomicInteger");
    } else if (symbolType.is("long") || symbolType.is("java.lang.Long")) {
      reportIssueIfNotInExcludedContext(tree, "AtomicLong");
    }
  }

  private void reportIssueIfNotInExcludedContext(Tree tree, String recommendedType) {
    Tree current = tree.parent();
    boolean foundClass = false;
    while (!foundClass) {
      switch (current.kind()) {
        case LAMBDA_EXPRESSION:
        case SYNCHRONIZED_STATEMENT:
        case COMPILATION_UNIT:
          return;
        case METHOD:
          if (ModifiersUtils.hasModifier(((MethodTree) current).modifiers(), Modifier.SYNCHRONIZED)) {
            return;
          }
          break;
        case ENUM:
        case CLASS:
          if (((ClassTree) current).simpleName() == null) {
            return;
          }
          // we got to a non anonymous class, we can safely raise an issue
          foundClass = true;
          break;
      }
      current = current.parent();
    }
    reportIssue(tree, String.format("Use an \"%s\" for this field; its operations are atomic.", recommendedType));
  }

}
