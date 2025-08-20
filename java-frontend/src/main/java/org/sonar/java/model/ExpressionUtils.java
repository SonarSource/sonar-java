/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
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

  @Nullable
  public static Tree skipParenthesesUpwards(@Nullable Tree tree) {
    while (tree != null && tree.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      tree = tree.parent();
    }
    return tree;
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

  public enum UnaryOperation {
    POSTFIX_INCREMENT(
      Tree.Kind.POSTFIX_INCREMENT,
      value -> value),
    POSTFIX_DECREMENT(
      Tree.Kind.POSTFIX_DECREMENT,
      value -> value),
    PREFIX_INCREMENT(
      Tree.Kind.PREFIX_INCREMENT,
      value -> BinaryOperation.PLUS.apply(value, 1)),
    PREFIX_DECREMENT(
      Tree.Kind.PREFIX_DECREMENT,
      value -> BinaryOperation.MINUS.apply(value, 1)),
    UNARY_PLUS(
      Tree.Kind.UNARY_PLUS,
      value -> value),
    UNARY_MINUS(
      Tree.Kind.UNARY_MINUS,
      value -> {
        // This operation does not support Boolean, String
        if (value instanceof Integer num) {
          return -num;
        } else if (value instanceof Long num) {
          return -num;
        } else if (value instanceof Character num) {
          return -num;
        } else if (value instanceof Float num) {
          return -num;
        } else if (value instanceof Double num) {
          return -num;
        } else if (value instanceof Byte num) {
          return -num;
        } else if (value instanceof Short num) {
          return -num;
        } else {
          return null;
        }
      }),
    BITWISE_COMPLEMENT(
      Tree.Kind.BITWISE_COMPLEMENT,
      value -> {
        // This operation does not support Boolean, String, Float, Double
        if (value instanceof Integer num) {
          return ~num;
        } else if (value instanceof Long num) {
          return ~num;
        } else if (value instanceof Character num) {
          return ~num;
        } else if (value instanceof Byte num) {
          return ~num;
        } else if (value instanceof Short num) {
          return ~num;
        } else {
          return null;
        }
      }),
    LOGICAL_COMPLEMENT(
      Tree.Kind.LOGICAL_COMPLEMENT,
      value -> {
        // This operation does not support String, Character, Byte, Short, Integer, Long, Float, Double
        if (value instanceof Boolean num) {
          return !num;
        } else {
          return null;
        }
      });

    private final Tree.Kind operationKind;
    private final UnaryOperator<Object> operation;

    private static final EnumMap<Tree.Kind, UnaryOperation> OPERATION_MAP = Arrays.stream(UnaryOperation.values())
      .collect(Collectors.toMap(e -> e.operationKind, Function.identity(),
        (a, b) -> {
          throw new IllegalStateException("Duplicate operation kind: " + a.operationKind + " for " + a + " and " + b);
        },
        () -> new EnumMap<>(Tree.Kind.class)));

    UnaryOperation(Tree.Kind operationKind, UnaryOperator<Object> operation) {
      this.operationKind = operationKind;
      this.operation = operation;
    }

    /**
     * Applies the unary operation to the given operand.
     * If the operation is not applicable to the given operand, returns null.
     *
     * @param operationKind The kind of operation to apply
     * @param operand The operand to apply the operation to
     * @return The result of the operation, or null if the operation is not applicable
     */
    @Nullable
    public static Object apply(Tree.Kind operationKind, @Nullable Object operand) {
      if (operand == null) {
        return  null;
      }
      UnaryOperation operation = OPERATION_MAP.get(operationKind);
      return operation == null ? null : operation.apply(operand);
    }

    @Nullable
    public Object apply(@Nullable Object operand) {
      if (operand == null) {
        return  null;
      }
      try {
        return operation.apply(operand);
      } catch (ArithmeticException e) {
        LOG.debug("Arithmetic exception while resolving arithmetic operation value", e);
        return null;
      }
    }

  }

  public enum BinaryOperation {
    MULTIPLY(
      Tree.Kind.MULTIPLY,
      null,
      null,
      (a, b) -> a * b,
      (a, b) -> a * b,
      (a, b) -> a * b,
      (a, b) -> a * b),
    DIVIDE(
      Tree.Kind.DIVIDE,
      null,
      null,
      (a, b) -> (b == 0 ? null : (a / b)),
      (a, b) -> (b == 0L ? null : (a / b)),
      (a, b) -> a / b,
      (a, b) -> a / b),
    REMAINDER(
      Tree.Kind.REMAINDER,
      null,
      null,
      (a, b) -> (b == 0 ? null : (a % b)),
      (a, b) -> (b == 0L ? null : (a % b)),
      (a, b) -> a % b,
      (a, b) -> a % b),
    PLUS(
      Tree.Kind.PLUS,
      null,
      (a, b) -> a + b,
      (a, b) -> a + b,
      (a, b) -> a + b,
      (a, b) -> a + b,
      (a, b) -> a + b),
    MINUS(
      Tree.Kind.MINUS,
      null,
      null,
      (a, b) -> a - b,
      (a, b) -> a - b,
      (a, b) -> a - b,
      (a, b) -> a - b),
    LEFT_SHIFT(
      Tree.Kind.LEFT_SHIFT,
      null,
      null,
      (a, b) -> a << b,
      (a, b) -> a << b,
      null,
      null),
    RIGHT_SHIFT(
      Tree.Kind.RIGHT_SHIFT,
      null,
      null,
      (a, b) -> a >> b,
      (a, b) -> a >> b,
      null,
      null),
    UNSIGNED_RIGHT_SHIFT(
      Tree.Kind.UNSIGNED_RIGHT_SHIFT,
      null,
      null,
      (a, b) -> a >>> b,
      (a, b) -> a >>> b,
      null,
      null),
    LESS_THAN(
      Tree.Kind.LESS_THAN,
      null,
      null,
      (a, b) -> a < b,
      (a, b) -> a < b,
      (a, b) -> a < b,
      (a, b) -> a < b),
    GREATER_THAN(
      Tree.Kind.GREATER_THAN,
      null,
      null,
      (a, b) -> a > b,
      (a, b) -> a > b,
      (a, b) -> a > b,
      (a, b) -> a > b),
    LESS_THAN_OR_EQUAL_TO(
      Tree.Kind.LESS_THAN_OR_EQUAL_TO,
      null,
      null,
      (a, b) -> a <= b,
      (a, b) -> a <= b,
      (a, b) -> a <= b,
      (a, b) -> a <= b),
    GREATER_THAN_OR_EQUAL_TO(
      Tree.Kind.GREATER_THAN_OR_EQUAL_TO,
      null,
      null,
      (a, b) -> a >= b,
      (a, b) -> a >= b,
      (a, b) -> a >= b,
      (a, b) -> a >= b),
    EQUAL_TO(
      Tree.Kind.EQUAL_TO,
      (a, b) -> a == b,
      null,
      (a, b) -> a == b,
      (a, b) -> a == b,
      (a, b) -> a == b,
      (a, b) -> a == b),
    NOT_EQUAL_TO(
      Tree.Kind.NOT_EQUAL_TO,
      (a, b) -> a != b,
      null,
      (a, b) -> a != b,
      (a, b) -> a != b,
      (a, b) -> a != b,
      (a, b) -> a != b),
    AND(
      Tree.Kind.AND,
      (a, b) -> a & b,
      null,
      (a, b) -> a & b,
      (a, b) -> a & b,
      null,
      null),
    XOR(
      Tree.Kind.XOR,
      (a, b) -> a ^ b,
      null,
      (a, b) -> a ^ b,
      (a, b) -> a ^ b,
      null,
      null),
    OR(
      Tree.Kind.OR,
      (a, b) -> a | b,
      null,
      (a, b) -> a | b,
      (a, b) -> a | b,
      null,
      null),
    CONDITIONAL_AND(
      Tree.Kind.CONDITIONAL_AND,
      (a, b) -> a && b,
      null,
      null,
      null,
      null,
      null),
    CONDITIONAL_OR(
      Tree.Kind.CONDITIONAL_OR,
      (a, b) -> a || b,
      null,
      null,
      null,
      null,
      null);

    private final Tree.Kind operationKind;
    @Nullable
    private final BooleanBinaryOperator booleanOp;
    @Nullable
    private final StringBinaryOperator stringOp;
    @Nullable
    private final IntBinaryOperator intOp;
    @Nullable
    private final LongBinaryOperator longOp;
    @Nullable
    private final FloatBinaryOperator floatOp;
    @Nullable
    private final DoubleBinaryOperator doubleOp;

    private static final EnumMap<Tree.Kind, BinaryOperation> OPERATION_MAP = Arrays.stream(BinaryOperation.values())
        .collect(Collectors.toMap(e -> e.operationKind, Function.identity(),
          (a, b) -> {
            throw new IllegalStateException("Duplicate operation kind: " + a.operationKind + " for " + a + " and " + b);
          },
          () -> new EnumMap<>(Tree.Kind.class)));

    BinaryOperation(
      Tree.Kind operationKind,
      @Nullable BooleanBinaryOperator booleanOp,
      @Nullable StringBinaryOperator stringOp,
      @Nullable IntBinaryOperator intOp,
      @Nullable LongBinaryOperator longOp,
      @Nullable FloatBinaryOperator floatOp,
      @Nullable DoubleBinaryOperator doubleOp) {
      this.operationKind = operationKind;
      this.booleanOp = booleanOp;
      this.stringOp = stringOp;
      this.intOp = intOp;
      this.longOp = longOp;
      this.floatOp = floatOp;
      this.doubleOp = doubleOp;
    }
    /**
     * Applies the binary operation to the given operands.
     * If the operation is not applicable to the given operands, returns null.
     *
     * @param operationKind The kind of operation to apply
     * @param leftOperand The left operand
     * @param rightOperand The right operand
     * @return The result of the operation, or null if the operation is not applicable
     */
    @Nullable
    public static Object apply(Tree.Kind operationKind, @Nullable Object leftOperand, @Nullable Object rightOperand) {
      if (leftOperand == null || rightOperand == null) {
        return  null;
      }
      BinaryOperation operation = OPERATION_MAP.get(operationKind);
      return operation == null ? null : operation.apply(leftOperand, rightOperand);
    }

    @Nullable
    public Object apply(@Nullable Object leftOperand, @Nullable Object rightOperand) {
      if (leftOperand == null || rightOperand == null) {
        return  null;
      }
      try {
        if (leftOperand instanceof String || rightOperand instanceof String) {
          return stringOp != null ? stringOp.applyAsString(leftOperand.toString(), rightOperand.toString()) : null;
        } else if (leftOperand instanceof Number left && rightOperand instanceof Number right) {
          // resolve numeric promotion
          if (left instanceof Double || right instanceof Double) {
            return doubleOp != null ? doubleOp.applyAsDouble(left.doubleValue(), right.doubleValue()) : null;
          } else if (left instanceof Float || right instanceof Float) {
            return floatOp != null ? floatOp.applyAsFloat(left.floatValue(), right.floatValue()) : null;
          } else if (left instanceof Long || right instanceof Long) {
            return longOp != null ? longOp.applyAsLong(left.longValue(), right.longValue()) : null;
          } else {
            return intOp != null ? intOp.applyAsInt(left.intValue(), right.intValue()) : null;
          }
        } else if (leftOperand instanceof Boolean left && rightOperand instanceof Boolean right) {
          return booleanOp != null ? booleanOp.applyAsBoolean(left, right) : null;
        } else {
          return null;
        }
      } catch (ArithmeticException e) {
        LOG.debug("Arithmetic exception while resolving arithmetic operation value", e);
        return null;
      }
    }

    @FunctionalInterface
    interface BooleanBinaryOperator {
      Object applyAsBoolean(boolean left, boolean right);
    }

    @FunctionalInterface
    interface StringBinaryOperator {
      Object applyAsString(String left, String right);
    }

    @FunctionalInterface
    interface IntBinaryOperator {
      Object applyAsInt(int left, int right);
    }

    @FunctionalInterface
    interface LongBinaryOperator {
      Object applyAsLong(long left, long right);
    }

    @FunctionalInterface
    interface FloatBinaryOperator {
      Object applyAsFloat(float left, float right);
    }

    @FunctionalInterface
    interface DoubleBinaryOperator {
      Object applyAsDouble(double left, double right);
    }

  }

}
