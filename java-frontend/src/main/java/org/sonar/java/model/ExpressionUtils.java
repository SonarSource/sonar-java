/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.function.BiFunction;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public final class ExpressionUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ExpressionUtils.class);

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

  /**
   * Return whether we are sure that the method invocation is on a given variable.
   *
   * If unsure (variable is null, or we can not extract an identifier from the method invocation),
   * return a default value
   */
  public static boolean isInvocationOnVariable(MethodInvocationTree mit, @Nullable Symbol variable, boolean defaultReturn) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (variable == null || !methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      return defaultReturn;
    }
    return extractIdentifierSymbol(((MemberSelectExpressionTree) methodSelect).expression())
      .map(variable::equals)
      .orElse(defaultReturn);
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

  /**
   * Return the first enclosing method or constructor containing the given expression.
   */
  @CheckForNull
  public static MethodTree getEnclosingMethod(ExpressionTree expr) {
    return (MethodTree) getEnclosingTree(expr, Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @CheckForNull
  public static Tree getEnclosingTree(Tree expr, Tree.Kind... kinds) {
    Tree result = expr.parent();
    while (result != null && !result.is(kinds)) {
      result = result.parent();
    }
    return result;
  }

  @Nullable
  public static Tree getParentOfType(Tree tree, Tree.Kind... kinds) {
    Tree result = tree.parent();
    while (result != null && !result.is(kinds)) {
      result = result.parent();
    }
    return result;
  }

  public static Optional<Symbol> getAssignedSymbol(ExpressionTree exp) {
    Tree parent = exp.parent();
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
    if (expression.is(Tree.Kind.STRING_LITERAL, Tree.Kind.TEXT_BLOCK)) {
      return LiteralUtils.getAsStringValue((LiteralTree) expression);
    }
    if (expression instanceof UnaryExpressionTree unaryExpressionTree) {
      return resolveUnaryExpression(unaryExpressionTree);
    }
    if (expression.is(Tree.Kind.INT_LITERAL)) {
      return LiteralUtils.intLiteralValue(expression);
    }
    if (expression.is(Tree.Kind.LONG_LITERAL)) {
      return LiteralUtils.longLiteralValue(expression);
    }
    if (expression.is(Tree.Kind.PLUS)) {
      return resolvePlus((BinaryExpressionTree) expression);
    }
    if (expression.is(Tree.Kind.OR)) {
      return resolveOr((BinaryExpressionTree) expression);
    }
    if (expression.is(Tree.Kind.MINUS)) {
      return resolveArithmeticOperation((BinaryExpressionTree) expression, (a, b) -> a - b, (a, b) -> a - b);
    }
    if (expression.is(Tree.Kind.MULTIPLY)) {
      return resolveArithmeticOperation((BinaryExpressionTree) expression, (a, b) -> a * b, (a, b) -> a * b);
    }
    if (expression.is(Tree.Kind.DIVIDE)) {
      return resolveArithmeticOperation((BinaryExpressionTree) expression, (a, b) -> a / b, (a, b) -> a / b);
    }
    if (expression.is(Tree.Kind.REMAINDER)) {
      return resolveArithmeticOperation((BinaryExpressionTree) expression, (a, b) -> a % b, (a, b) -> a % b);
    }
    return null;
  }

  public static boolean areVariablesSame(Tree tree1, Tree tree2, boolean defaultValue) {
    Symbol symbol1 = getSymbol(tree1);
    Symbol symbol2 = getSymbol(tree2);
    if (symbol1 == null || symbol1.isUnknown() || symbol2 == null || symbol2.isUnknown()) {
      return defaultValue;
    }
    return symbol1.name().equals(symbol2.name());
  }

  @CheckForNull
  private static Symbol getSymbol(Tree tree) {
    Symbol symbol = null;
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      symbol = ((IdentifierTree) tree).symbol();
    } else if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      symbol = ((MemberSelectExpressionTree) tree).identifier().symbol();
    }
    return symbol;
  }

  @CheckForNull
  private static Object resolveUnaryExpression(UnaryExpressionTree unaryExpression) {
    Object value = resolveAsConstant(unaryExpression.expression());
    if (unaryExpression.is(Tree.Kind.UNARY_PLUS)) {
      return value;
    } else if (unaryExpression.is(Tree.Kind.UNARY_MINUS)) {
      if (value instanceof Long longValue) {
        return -longValue;
      } else if (value instanceof Integer intValue) {
        return -intValue;
      }
    } else if (unaryExpression.is(Tree.Kind.BITWISE_COMPLEMENT)) {
      if (value instanceof Long longValue) {
        return ~longValue;
      } else if (value instanceof Integer intValue) {
        return ~intValue;
      }
    } else if (unaryExpression.is(Tree.Kind.LOGICAL_COMPLEMENT) && value instanceof Boolean bool) {
      return !bool;
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
    return ((Symbol.VariableSymbol) symbol).constantValue().orElse(null);
  }

  @CheckForNull
  private static Object resolvePlus(BinaryExpressionTree binaryExpression) {
    Object left = resolveAsConstant(binaryExpression.leftOperand());
    Object right = resolveAsConstant(binaryExpression.rightOperand());
    if (left == null || right == null) {
      return null;
    } else if (left instanceof String leftString) {
      return leftString + right;
    } else if (right instanceof String rightString) {
      return left + rightString;
    }
    return resolveArithmeticOperation(left, right, Long::sum, Integer::sum);
  }

  @CheckForNull
  private static Object resolveArithmeticOperation(BinaryExpressionTree binaryExpression,
                                                   BiFunction<Long, Long, Object> longOperation,
                                                   BiFunction<Integer, Integer, Object> intOperation) {
    Object left = resolveAsConstant(binaryExpression.leftOperand());
    Object right = resolveAsConstant(binaryExpression.rightOperand());
    if (left == null || right == null) {
      return null;
    }
    return resolveArithmeticOperation(left, right, longOperation, intOperation);
  }

  @CheckForNull
  private static Object resolveArithmeticOperation(Object left, Object right, BiFunction<Long, Long, Object> longOperation, BiFunction<Integer, Integer, Object> intOperation) {
    try {
      if (left instanceof Integer leftInt && right instanceof Integer rightInt) {
        return intOperation.apply(leftInt, rightInt);
      } else if ((left instanceof Long || right instanceof Long) && (left instanceof Integer || right instanceof Integer)) {
        return longOperation.apply(((Number) left).longValue(), ((Number) right).longValue());
      }
    } catch (ArithmeticException e) {
      LOG.debug("Arithmetic exception while resolving arithmetic operation value", e);
    }
    return null;
  }

  @CheckForNull
  private static Object resolveOr(BinaryExpressionTree binaryExpression) {
    Object left = resolveAsConstant(binaryExpression.leftOperand());
    Object right = resolveAsConstant(binaryExpression.rightOperand());
    if (left == null || right == null) {
      return null;
    } else if (left instanceof Long leftLong && right instanceof Long rightLong) {
      return leftLong | rightLong;
    } else if (left instanceof Long leftLong && right instanceof Integer rightInt) {
      return leftLong | rightInt;
    } else if (left instanceof Integer leftInt && right instanceof Long rightLong) {
      return leftInt | rightLong;
    } else if (left instanceof Integer leftInt && right instanceof Integer rightInt) {
      return leftInt | rightInt;
    }
    return null;
  }

}
