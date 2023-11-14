/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S2185")
public class ConstantMathCheck extends IssuableSubscriptionVisitor {

  private static final String ABS = "abs";
  private static final String CEIL = "ceil";
  private static final String DOUBLE = "double";
  private static final String FLOAT = "float";
  private static final String FLOOR = "floor";
  private static final String MATH_PACKAGE_NAME = "java.lang.Math";
  private static final String ROUND = "round";

  private static final MethodMatchers CONSTANT_WITH_LITERAL_METHODS =
    MethodMatchers.create().ofTypes(MATH_PACKAGE_NAME).names(ABS)
      .addParametersMatcher(DOUBLE)
      .addParametersMatcher(FLOAT)
      .addParametersMatcher("int")
      .addParametersMatcher("long")
      .build();

  private static final MethodMatchers TRUNCATION_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofTypes(MATH_PACKAGE_NAME).names(CEIL).addParametersMatcher(DOUBLE).addParametersMatcher(FLOAT).build(),
    MethodMatchers.create().ofTypes(MATH_PACKAGE_NAME).names(FLOOR).addParametersMatcher(DOUBLE).addParametersMatcher(FLOAT).build(),
    MethodMatchers.create().ofTypes(MATH_PACKAGE_NAME).names("rint").addParametersMatcher(DOUBLE).build(),
    MethodMatchers.create().ofTypes(MATH_PACKAGE_NAME).names(ROUND).addParametersMatcher(DOUBLE).addParametersMatcher(FLOAT).build()
  );

  private static final MethodMatchers CONSTANT_WITH_ZERO_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofTypes(MATH_PACKAGE_NAME).names("atan2").addParametersMatcher(DOUBLE, DOUBLE).build(),
    MethodMatchers.create().ofTypes(MATH_PACKAGE_NAME).names("cos", "cosh", "expm1", "sin", "sinh", "tan", "tanh", "toRadians").addParametersMatcher(DOUBLE).build()
  );

  private static final MethodMatchers CONSTANT_WITH_ZERO_OR_ONE_METHODS =
    MethodMatchers.create().ofTypes(MATH_PACKAGE_NAME).names("acos", "asin", "atan", "cbrt", "exp", "log", "log10", "sqrt", "toDegrees").addParametersMatcher(DOUBLE).build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.REMAINDER, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.REMAINDER)) {
      BinaryExpressionTree remainderTree = (BinaryExpressionTree) tree;
      if (isIntegralOne(remainderTree.rightOperand()) && isIntOrLong(remainderTree.leftOperand())) {
        reportIssue(remainderTree.operatorToken(), "Remove this computation of % 1, which always evaluates to zero.");
      }
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (isConstantWithLiteral(mit) || isTruncation(mit) || isConstantWithZero(mit) || isConstantWithZeroOrOne(mit)) {
        reportIssue(mit.methodSelect(), String.format("Remove this unnecessary call to \"Math.%s\"", mit.methodSymbol().name()));
      }
    }
  }

  private static boolean isIntOrLong(ExpressionTree expression) {
    Type type = expression.symbolType();
    return isIntegral(type) || (JUtils.isPrimitiveWrapper(type) && isIntegral(JUtils.primitiveType(type)));
  }

  private static boolean isTruncation(MethodInvocationTree methodTree) {
    return TRUNCATION_METHODS.matches(methodTree) && isCastFromIntegralToFloating(ExpressionUtils.skipParentheses(methodTree.arguments().get(0)));
  }

  private static boolean isConstantWithLiteral(MethodInvocationTree methodTree) {
    return CONSTANT_WITH_LITERAL_METHODS.matches(methodTree) && isConstant(methodTree.arguments().get(0));
  }

  private static boolean isConstantWithZero(MethodInvocationTree methodTree) {
    return CONSTANT_WITH_ZERO_METHODS.matches(methodTree) && isFloatingZero(methodTree.arguments().get(0));
  }

  private static boolean isConstantWithZeroOrOne(MethodInvocationTree methodTree) {
    return CONSTANT_WITH_ZERO_OR_ONE_METHODS.matches(methodTree) && isFloatingZeroOrOne(methodTree.arguments().get(0));
  }

  private static boolean isCastFromIntegralToFloating(ExpressionTree tree) {
    Type resultType = tree.symbolType();
    // explicit cast
    if (tree.is(Tree.Kind.TYPE_CAST) && isIntegral(getInnerType(((TypeCastTree) tree).expression())) && (resultType.is(DOUBLE) || resultType.is(FLOAT))) {
      return true;
    }
    // implicit cast
    return isIntegral(resultType);
  }

  private static boolean isConstant(ExpressionTree tree) {
    return getInnerExpression(tree).is(Tree.Kind.CHAR_LITERAL, Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL, Tree.Kind.INT_LITERAL, Tree.Kind.LONG_LITERAL);
  }

  private static boolean isIntegral(Type type) {
    return type.isPrimitive() && !type.is(DOUBLE) && !type.is(FLOAT);
  }

  private static boolean isIntegralOne(ExpressionTree tree) {
    Long value = LiteralUtils.longLiteralValue(tree);
    return value != null && value == 1;
  }

  private static ExpressionTree getInnerExpression(ExpressionTree tree) {
    ExpressionTree result = ExpressionUtils.skipParentheses(tree);
    while (result.is(Tree.Kind.TYPE_CAST)) {
      result = ExpressionUtils.skipParentheses(((TypeCastTree) result).expression());
    }
    return result;
  }

  private static Type getInnerType(ExpressionTree tree) {
    return getInnerExpression(tree).symbolType();
  }

  private static boolean isFloatingZero(ExpressionTree tree) {
    Integer value = getFloatingZeroOrOne(tree);
    return value != null && value == 0;
  }

  private static boolean isFloatingZeroOrOne(ExpressionTree tree) {
    return getFloatingZeroOrOne(tree) != null;
  }

  @CheckForNull
  private static Integer getFloatingZeroOrOne(ExpressionTree tree) {
    ExpressionTree expressionTree = ExpressionUtils.skipParentheses(tree);
    if (expressionTree.is(Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL)) {
      String value = ((LiteralTree) expressionTree).value();
      if ("0.0".equals(value) || "0.0d".equalsIgnoreCase(value) || "0.0f".equalsIgnoreCase(value)) {
        return 0;
      } else if ("1.0".equals(value) || "1.0d".equalsIgnoreCase(value) || "1.0f".equalsIgnoreCase(value)) {
        return 1;
      }
    }
    return null;
  }

}
