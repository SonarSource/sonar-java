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
import java.util.Locale;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
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
    if (!isLiteralConversion(binaryExpression)) {
      return;
    }
    if (isInConstantContext(binaryExpression)) {
      ExpressionTree concatenation = enclosingConstantConcatenation(binaryExpression);
      if (firstLiteralConversion(concatenation).filter(first -> first == binaryExpression).isPresent()) {
        String replacement = constantValue(concatenation)
          .map(value -> "the string literal \"" + escapeString(value.toString()) + "\"")
          .orElse("a string literal");
        reportIssue(concatenation, "Replace this concatenation with " + replacement + ".");
      }
    } else {
      reportIssue(binaryExpression, MESSAGE);
    }
  }

  private static boolean isLiteralConversion(BinaryExpressionTree binaryExpression) {
    ExpressionTree leftOperand = ExpressionUtils.skipParentheses(binaryExpression.leftOperand());
    ExpressionTree rightOperand = ExpressionUtils.skipParentheses(binaryExpression.rightOperand());
    return (isEmptyStringLiteral(leftOperand) && isPrimitiveLiteral(rightOperand))
      || (isPrimitiveLiteral(leftOperand) && isEmptyStringLiteral(rightOperand));
  }

  private static ExpressionTree enclosingConstantConcatenation(ExpressionTree tree) {
    ExpressionTree expression = tree;
    Tree parent = expression.parent();
    while (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION, Tree.Kind.PLUS)
      && constantValue((ExpressionTree) parent).filter(String.class::isInstance).isPresent()) {
      expression = (ExpressionTree) parent;
      parent = expression.parent();
    }
    return ExpressionUtils.skipParentheses(expression);
  }

  private static Optional<BinaryExpressionTree> firstLiteralConversion(ExpressionTree tree) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(tree);
    if (!(expression instanceof BinaryExpressionTree binaryExpression) || !expression.is(Tree.Kind.PLUS)) {
      return Optional.empty();
    }
    if (isLiteralConversion(binaryExpression)) {
      return Optional.of(binaryExpression);
    }
    return firstLiteralConversion(binaryExpression.leftOperand())
      .or(() -> firstLiteralConversion(binaryExpression.rightOperand()));
  }

  private static Optional<Object> constantValue(ExpressionTree tree) {
    try {
      return resolveConstantValue(ExpressionUtils.skipParentheses(tree));
    } catch (IllegalArgumentException | ArithmeticException exception) {
      return Optional.empty();
    }
  }

  private static Optional<Object> resolveConstantValue(ExpressionTree expression) {
    if (expression.is(Tree.Kind.PLUS)) {
      BinaryExpressionTree binaryExpression = (BinaryExpressionTree) expression;
      return constantValue(binaryExpression.leftOperand()).flatMap(left ->
        constantValue(binaryExpression.rightOperand()).flatMap(right ->
          left instanceof String || right instanceof String
            ? Optional.of(left.toString() + right)
            : expression.asConstant()));
    }
    if (expression instanceof LiteralTree literal) {
      return literalValue(literal);
    }
    return expression.asConstant();
  }

  private static Optional<Object> literalValue(LiteralTree literal) {
    return switch (literal.kind()) {
      case STRING_LITERAL -> Optional.of(LiteralUtils.trimQuotes(literal.value()).translateEscapes());
      case CHAR_LITERAL -> Optional.of(LiteralUtils.trimQuotes(literal.value()).translateEscapes().charAt(0));
      case FLOAT_LITERAL -> Optional.of(Float.parseFloat(literal.value().replace("_", "")));
      case DOUBLE_LITERAL -> Optional.of(Double.parseDouble(literal.value().replace("_", "")));
      case TEXT_BLOCK -> Optional.empty();
      default -> literal.asConstant();
    };
  }

  private static String escapeString(String value) {
    StringBuilder escaped = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      escaped.append(switch (character) {
        case '\\' -> "\\\\";
        case '"' -> "\\\"";
        case '\n' -> "\\n";
        case '\r' -> "\\r";
        case '\t' -> "\\t";
        case '\b' -> "\\b";
        case '\f' -> "\\f";
        default -> Character.isISOControl(character) ? String.format(Locale.ROOT, "\\u%04x", (int) character) : Character.toString(character);
      });
    }
    return escaped.toString();
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
