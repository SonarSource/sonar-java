/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

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
    return variable.is(Tree.Kind.IDENTIFIER) || isSelectOnThisOrSuper(tree);
  }

  /**
   * Checks of is the given tree is a {@link MemberSelectExpressionTree} which is selecting with <code>this</code> or <code>super</code>
   * @param tree The tree to check.
   * @return true when the tree is a select on <code>this</code> or <code>super</code>
   * @see #isSelectOnThisOrSuper(MemberSelectExpressionTree)
   */
  public static boolean isSelectOnThisOrSuper(AssignmentExpressionTree tree) {
    ExpressionTree variable = ExpressionUtils.skipParentheses(tree.variable());
    return variable.is(Tree.Kind.MEMBER_SELECT) && isSelectOnThisOrSuper((MemberSelectExpressionTree) variable);
  }

  /**
   * Checks of is the given tree is selecting with <code>this</code> or <code>super</code>
   * @param tree The tree to check.
   * @return true when the tree is a select on <code>this</code> or <code>super</code>
   * @see #isSelectOnThisOrSuper(AssignmentExpressionTree)
   */
  public static boolean isSelectOnThisOrSuper(MemberSelectExpressionTree tree) {
    if (!tree.expression().is(Tree.Kind.IDENTIFIER)) {
      // This is no longer simple.
      return false;
    }

    String selectSourceName = ((IdentifierTree) tree.expression()).name();
    return "this".equalsIgnoreCase(selectSourceName) || "super".equalsIgnoreCase(selectSourceName);
  }

  public static IdentifierTree extractIdentifier(AssignmentExpressionTree tree) {
    Optional<IdentifierTree> identifier = extractIdentifier(tree.variable());

    if (identifier.isPresent()) {
      return identifier.get();
    }

    // This should not be possible.
    // If it happens anyway, you should make sure the assignment is simple (by calling isSimpleAssignment) before.
    throw new IllegalArgumentException("Can not extract identifier.");
  }

  private static Optional<IdentifierTree> extractIdentifier(ExpressionTree tree) {
    ExpressionTree cleanedExpression = ExpressionUtils.skipParentheses(tree);
    if (cleanedExpression.is(Tree.Kind.IDENTIFIER)) {
      return Optional.of(((IdentifierTree) cleanedExpression));
    } else if (cleanedExpression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree selectTree = (MemberSelectExpressionTree) cleanedExpression;
      if (isSelectOnThisOrSuper(selectTree)) {
        return Optional.of(selectTree.identifier());
      }
    }
    return Optional.empty();
  }

  public static Optional<Symbol> extractIdentifierSymbol(ExpressionTree tree) {
    return extractIdentifier(tree).map(IdentifierTree::symbol);
  }

  public static ExpressionTree skipParentheses(ExpressionTree tree) {
    ExpressionTree result = tree;
    while (result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      result = ((ParenthesizedTree) result).expression();
    }
    return result;
  }

  public static boolean isNullLiteral(ExpressionTree tree) {
    return skipParentheses(tree).is(Tree.Kind.NULL_LITERAL);
  }

  public static boolean isSecuringByte(ExpressionTree expression) {
    if (expression.is(Tree.Kind.AND)) {
      BinaryExpressionTree and = (BinaryExpressionTree) expression;
      return LiteralUtils.is0xff(and.rightOperand()) || LiteralUtils.is0xff(and.leftOperand());
    }
    return false;
  }

  /**
   * Retrieve the identifier corresponding to the method name associated to the method invocation
   */
  public static IdentifierTree methodName(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    IdentifierTree id;
    if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      id = (IdentifierTree) methodSelect;
    } else {
      id = ((MemberSelectExpressionTree) methodSelect).identifier();
    }
    return id;
  }

  @CheckForNull
  public static MethodTree getEnclosingMethod(ExpressionTree expr) {
    Tree result = expr.parent();
    while (result != null && !result.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
      result = result.parent();
    }
    return (MethodTree) result;
  }

  public static Optional<Symbol> getAssignedSymbol(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    if (parent != null) {
      if (parent.is(Tree.Kind.ASSIGNMENT)) {
        return extractIdentifierSymbol(((AssignmentExpressionTree) parent).variable());
      } else if (parent.is(Tree.Kind.VARIABLE)) {
        return Optional.of(((VariableTree) parent).simpleName().symbol());
      }
    }
    return Optional.empty();
  }

  /**
   * Checks if the given expression refers to "this"
   * @param expression the expression to check
   * @return true if this expression refers to "this"
   */
  public static boolean isThis(ExpressionTree expression) {
    ExpressionTree newExpression = ExpressionUtils.skipParentheses(expression);
    return newExpression.is(Tree.Kind.IDENTIFIER) && "this".equals(((IdentifierTree) newExpression).name());
  }

  @CheckForNull
  public static Object resolveAsConstant(ExpressionTree tree) {
    ExpressionTree expression = tree;
    while (expression.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      expression = ((ParenthesizedTree) expression).expression();
    }
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      expression = ((MemberSelectExpressionTree) expression).identifier();
    }
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      return resolveIdentifier((IdentifierTree) expression);
    }
    if (expression.is(Tree.Kind.BOOLEAN_LITERAL)) {
      return Boolean.parseBoolean(((LiteralTree) expression).value());
    }
    if (expression.is(Tree.Kind.STRING_LITERAL)) {
      return LiteralUtils.trimQuotes(((LiteralTree) expression).value());
    }
    if (tree.is(Tree.Kind.INT_LITERAL, Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
      return LiteralUtils.intLiteralValue(tree);
    }
    if (tree.is(Tree.Kind.LONG_LITERAL)) {
      return LiteralUtils.longLiteralValue(tree);
    }
    if (expression.is(Tree.Kind.PLUS)) {
      return resolvePlus((BinaryExpressionTree) expression);
    }
    return null;
  }

  @CheckForNull
  private static Object resolveIdentifier(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    if (!symbol.isVariableSymbol()) {
      return null;
    }
    Symbol owner = symbol.owner();
    if (owner.isTypeSymbol() && owner.type().is("java.lang.Boolean")) {
      if ("TRUE".equals(symbol.name())) {
        return Boolean.TRUE;
      } else if ("FALSE".equals(symbol.name())) {
        return Boolean.FALSE;
      }
    }
    return JUtils.constantValue((Symbol.VariableSymbol) symbol).orElse(null);
  }

  @CheckForNull
  private static Object resolvePlus(BinaryExpressionTree binaryExpression) {
    Object left = resolveAsConstant(binaryExpression.leftOperand());
    Object right = resolveAsConstant(binaryExpression.rightOperand());
    if (left == null || right == null) {
      return null;
    } else if (left instanceof String) {
      return ((String) left) + right;
    } else if (right instanceof String) {
      return left + ((String) right);
    } else if (left instanceof Long && right instanceof Long) {
      return ((Long) left) + ((Long) right);
    } else if (left instanceof Long && right instanceof Integer) {
      return ((Long) left) + ((Integer) right);
    } else if (left instanceof Integer && right instanceof Long) {
      return ((Integer) left) + ((Long) right);
    } else if (left instanceof Integer && right instanceof Integer) {
      return ((Integer) left) + ((Integer) right);
    }
    return null;
  }

}
