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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ComparisonMethodUtils;
import org.sonar.java.checks.helpers.IntegerOverflowRange;
import org.sonar.java.checks.helpers.IntegerOverflowRange.Range;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3949")
public class S3949Check extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Rewrite this calculation to prevent overflow and preserve the result.";
  private static final MethodMatchers TIMESTAMP_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofTypes("java.util.Date", "java.sql.Timestamp").constructor().addParametersMatcher("long").build(),
    MethodMatchers.create().ofTypes("java.time.Instant").names("ofEpochSecond")
      .addParametersMatcher("long").addParametersMatcher("long", "long").build(),
    MethodMatchers.create().ofTypes("java.time.Instant").names("ofEpochMilli").addParametersMatcher("long").build(),
    MethodMatchers.create().ofSubTypes("java.util.Calendar").names("setTimeInMillis").addParametersMatcher("long").build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.PLUS,
      Tree.Kind.MINUS,
      Tree.Kind.MULTIPLY,
      Tree.Kind.UNARY_MINUS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null || !(tree instanceof ExpressionTree expression) || !isIntOrLong(expression.symbolType())) {
      return;
    }
    if (tree.is(Tree.Kind.PLUS) && isUnsafeMidpoint((BinaryExpressionTree) tree)) {
      reportIssue(tree, MESSAGE);
      return;
    }
    if (tree.is(Tree.Kind.UNARY_MINUS)) {
      checkNegation((UnaryExpressionTree) tree);
    } else {
      checkBinary((BinaryExpressionTree) tree);
    }
  }

  private void checkBinary(BinaryExpressionTree tree) {
    if ((tree.is(Tree.Kind.MINUS) && isComparatorSubtraction(tree)) || isWidenedSink(tree) || isTimestampCast(tree)) {
      return;
    }
    Range left = IntegerOverflowRange.rangeOf(tree.leftOperand());
    Range right = IntegerOverflowRange.rangeOf(tree.rightOperand());
    if (left == null || right == null) {
      return;
    }
    Range result = switch (tree.kind()) {
      case PLUS -> left.add(right);
      case MINUS -> left.subtract(right);
      case MULTIPLY -> left.multiply(right);
      default -> throw new IllegalStateException();
    };
    if (result.exceeds(tree.symbolType())) {
      reportIssue(tree, MESSAGE);
    }
  }

  private void checkNegation(UnaryExpressionTree tree) {
    Range operand = IntegerOverflowRange.rangeOf(tree.expression());
    if (operand == null || isDirectMinimumValue(tree.expression())) {
      return;
    }
    if (operand.negate().exceeds(tree.symbolType())) {
      reportIssue(tree, MESSAGE);
    }
  }

  private static boolean isUnsafeMidpoint(BinaryExpressionTree addition) {
    Tree parent = ExpressionUtils.skipParenthesesUpwards(addition.parent());
    if (!(parent instanceof BinaryExpressionTree division) || !division.is(Tree.Kind.DIVIDE)
      || ExpressionUtils.skipParentheses(division.leftOperand()) != addition) {
      return false;
    }
    Integer denominator = ExpressionUtils.skipParentheses(division.rightOperand()).asConstant(Integer.class).orElse(null);
    Long longDenominator = ExpressionUtils.skipParentheses(division.rightOperand()).asConstant(Long.class).orElse(null);
    Double floatingDenominator = LiteralUtils.doubleLiteralValue(ExpressionUtils.skipParentheses(division.rightOperand()));
    if (!Integer.valueOf(2).equals(denominator) && !Long.valueOf(2L).equals(longDenominator)
      && !Double.valueOf(2.0).equals(floatingDenominator)) {
      return false;
    }
    Range left = IntegerOverflowRange.rangeOf(addition.leftOperand());
    Range right = IntegerOverflowRange.rangeOf(addition.rightOperand());
    return left == null || right == null || left.add(right).exceeds(addition.symbolType());
  }

  private static boolean isDirectMinimumValue(ExpressionTree expression) {
    expression = ExpressionUtils.skipParentheses(expression);
    if (!expression.is(Tree.Kind.MEMBER_SELECT)) {
      return false;
    }
    Symbol symbol = ((MemberSelectExpressionTree) expression).identifier().symbol();
    return "MIN_VALUE".equals(symbol.name())
      && (symbol.owner().type().is("java.lang.Integer") || symbol.owner().type().is("java.lang.Long"));
  }

  private static boolean isComparatorSubtraction(Tree tree) {
    Tree result = tree;
    Tree parent = result.parent();
    while (parent != null && (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION)
      || parent.is(Tree.Kind.TYPE_CAST) && ((TypeCastTree) parent).type().symbolType().isPrimitive(Type.Primitives.INT))) {
      result = parent;
      parent = parent.parent();
    }
    if (parent instanceof LambdaExpressionTree lambda) {
      return lambda.body() == result && ComparisonMethodUtils.isComparatorLambda(lambda);
    }
    if (parent instanceof ReturnStatementTree) {
      MethodTree method = enclosingMethod(parent);
      return method != null && ComparisonMethodUtils.isCompareMethod(method);
    }
    return false;
  }

  private static boolean isWidenedSink(ExpressionTree expression) {
    Tree parent = ExpressionUtils.skipParenthesesUpwards(expression.parent());
    if (parent instanceof VariableTree variable && variable.initializer() != null) {
      return isWider(variable.type().symbolType(), expression.symbolType());
    }
    if (parent instanceof AssignmentExpressionTree assignment && assignment.is(Tree.Kind.ASSIGNMENT)) {
      return isWider(assignment.variable().symbolType(), expression.symbolType());
    }
    if (parent instanceof ReturnStatementTree) {
      MethodTree method = enclosingMethod(parent);
      return method != null && method.returnType() != null && isWider(method.returnType().symbolType(), expression.symbolType());
    }
    if (parent instanceof Arguments arguments) {
      Tree invocation = arguments.parent();
      if (invocation instanceof MethodInvocationTree methodInvocation) {
        return isWiderArgument(arguments, methodInvocation.methodSymbol(), expression);
      }
      if (invocation instanceof NewClassTree newClass) {
        return isWiderArgument(arguments, newClass.methodSymbol(), expression);
      }
    }
    if (parent instanceof MethodInvocationTree invocation) {
      return isWiderArgument(invocation.arguments(), invocation.methodSymbol(), expression);
    }
    if (parent instanceof NewClassTree newClass) {
      return isWiderArgument(newClass.arguments(), newClass.methodSymbol(), expression);
    }
    return false;
  }

  private static boolean isTimestampCast(ExpressionTree expression) {
    Tree parent = ExpressionUtils.skipParenthesesUpwards(expression.parent());
    if (!(parent instanceof TypeCastTree cast) || !cast.type().symbolType().isPrimitive(Type.Primitives.LONG)) {
      return false;
    }
    Tree argumentsTree = ExpressionUtils.skipParenthesesUpwards(cast.parent());
    if (!(argumentsTree instanceof Arguments arguments)) {
      return false;
    }
    Tree invocation = arguments.parent();
    return (invocation instanceof MethodInvocationTree methodInvocation && TIMESTAMP_METHODS.matches(methodInvocation))
      || (invocation instanceof NewClassTree newClass && TIMESTAMP_METHODS.matches(newClass));
  }

  private static MethodTree enclosingMethod(Tree tree) {
    Tree current = tree.parent();
    while (current != null && !(current instanceof MethodTree)) {
      current = current.parent();
    }
    return (MethodTree) current;
  }

  private static boolean isWiderArgument(Arguments arguments, Symbol.MethodSymbol method, ExpressionTree expression) {
    if (method.isUnknown() || arguments.size() != method.parameterTypes().size()) {
      return false;
    }
    for (int i = 0; i < arguments.size(); i++) {
      if (ExpressionUtils.skipParentheses(arguments.get(i)) == expression) {
        return isWider(method.parameterTypes().get(i), expression.symbolType());
      }
    }
    return false;
  }

  private static boolean isWider(Type sink, Type source) {
    boolean floatingSink = sink.isPrimitive(Type.Primitives.FLOAT) || sink.isPrimitive(Type.Primitives.DOUBLE);
    return (source.isPrimitive(Type.Primitives.INT) && (sink.isPrimitive(Type.Primitives.LONG) || floatingSink))
      || (source.isPrimitive(Type.Primitives.LONG) && floatingSink);
  }

  private static boolean isIntOrLong(Type type) {
    return type.isPrimitive(Type.Primitives.INT) || type.isPrimitive(Type.Primitives.LONG);
  }
}
