/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9433")
public class LiteralToStringByConcatenationCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Use an explicit conversion instead of concatenating this literal with an empty string.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.PLUS);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpression = (BinaryExpressionTree) tree;
    ExpressionTree leftOperand = ExpressionUtils.skipParentheses(binaryExpression.leftOperand());
    ExpressionTree rightOperand = ExpressionUtils.skipParentheses(binaryExpression.rightOperand());
    boolean isLiteralConversion = (isEmptyStringLiteral(leftOperand) && isPrimitiveLiteral(rightOperand))
      || (isPrimitiveLiteral(leftOperand) && isEmptyStringLiteral(rightOperand));
    if (isLiteralConversion && !isInConstantContext(binaryExpression)) {
      reportIssue(binaryExpression, MESSAGE);
    }
  }

  /**
   * Returns true when the expression is part of a constant expression used where a compile-time constant is expected
   * or meaningful: annotation arguments, annotation element default values, case labels, and initializers of constant
   * fields (static final fields, or interface fields).
   * Only parentheses, string concatenations and conditional expressions are traversed to find the enclosing context,
   * so that concatenations nested in method invocations or other non-constant expressions are still reported.
   */
  private static boolean isInConstantContext(Tree tree) {
    Tree expression = tree;
    Tree parent = tree.parent();
    while (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION, Tree.Kind.PLUS, Tree.Kind.CONDITIONAL_EXPRESSION)) {
      expression = parent;
      parent = parent.parent();
    }
    if (parent.is(Tree.Kind.CASE_LABEL)) {
      return true;
    }
    if (parent.is(Tree.Kind.VARIABLE)) {
      return isConstantField((VariableTree) parent);
    }
    return isAnnotationValue(expression);
  }

  private static boolean isAnnotationValue(Tree expression) {
    Tree value = expression;
    Tree parent = expression.parent();
    if (parent.is(Tree.Kind.LIST) && parent.parent().is(Tree.Kind.NEW_ARRAY)) {
      value = parent.parent();
      parent = value.parent();
    }
    if (parent.is(Tree.Kind.METHOD)) {
      return value == ((MethodTree) parent).defaultValue();
    }
    if (parent.is(Tree.Kind.ASSIGNMENT)) {
      parent = parent.parent();
    }
    return parent.is(Tree.Kind.ARGUMENTS) && parent.parent().is(Tree.Kind.ANNOTATION);
  }

  private static boolean isConstantField(VariableTree variable) {
    Tree owner = variable.parent();
    return Tree.Kind.CLASS_KINDS.contains(owner.kind())
      && (owner.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE) || ModifiersUtils.hasAll(variable.modifiers(), Modifier.STATIC, Modifier.FINAL));
  }

  private static boolean isEmptyStringLiteral(ExpressionTree tree) {
    return tree.is(Tree.Kind.STRING_LITERAL) && tree.asConstant(String.class).filter(String::isEmpty).isPresent();
  }

  private static boolean isPrimitiveLiteral(ExpressionTree tree) {
    return Tree.Kind.LITERAL_KINDS.contains(tree.kind())
      && !tree.is(Tree.Kind.STRING_LITERAL, Tree.Kind.TEXT_BLOCK, Tree.Kind.NULL_LITERAL);
  }
}
