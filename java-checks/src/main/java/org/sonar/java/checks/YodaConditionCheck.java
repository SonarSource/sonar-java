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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S9360")
public class YodaConditionCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_MATH = "java.lang.Math";

  private static final MethodMatchers CONSTANT_MATH_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofTypes(JAVA_LANG_MATH)
      .names("max", "min")
      .addParametersMatcher("int", "int")
      .addParametersMatcher("long", "long")
      .addParametersMatcher("float", "float")
      .addParametersMatcher("double", "double")
      .build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_MATH)
      .names("abs", "absExact", "negateExact", "incrementExact", "decrementExact")
      .addParametersMatcher("int")
      .addParametersMatcher("long")
      .build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_MATH)
      .names("abs")
      .addParametersMatcher("float")
      .addParametersMatcher("double")
      .build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_MATH)
      .names("sqrt", "cbrt", "ceil", "floor", "rint", "log", "log10", "exp",
        "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh",
        "toDegrees", "toRadians", "signum", "expm1", "log1p")
      .addParametersMatcher("double")
      .build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_MATH)
      .names("round")
      .addParametersMatcher("float")
      .addParametersMatcher("double")
      .build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_MATH)
      .names("pow", "atan2", "IEEEremainder", "copySign")
      .addParametersMatcher("double", "double")
      .build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_MATH)
      .names("addExact", "subtractExact", "multiplyExact", "floorDiv", "floorMod")
      .addParametersMatcher("int", "int")
      .addParametersMatcher("long", "long")
      .build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_MATH)
      .names("toIntExact")
      .addParametersMatcher("long")
      .build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_MATH)
      .names("signum")
      .addParametersMatcher("float")
      .build()
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(
      Tree.Kind.EQUAL_TO,
      Tree.Kind.NOT_EQUAL_TO,
      Tree.Kind.LESS_THAN,
      Tree.Kind.GREATER_THAN,
      Tree.Kind.LESS_THAN_OR_EQUAL_TO,
      Tree.Kind.GREATER_THAN_OR_EQUAL_TO,
      Tree.Kind.METHOD_INVOCATION
    );
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      checkConstantMathCall((MethodInvocationTree) tree);
    } else {
      checkYodaCondition((BinaryExpressionTree) tree);
    }
  }

  private void checkYodaCondition(BinaryExpressionTree binaryExpression) {
    ExpressionTree left = ExpressionUtils.skipParentheses(binaryExpression.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(binaryExpression.rightOperand());

    if (isLiteral(left) && !isLiteral(right)) {
      reportIssue(left, "Put the variable on the left side of this comparison.");
    }
  }

  private void checkConstantMathCall(MethodInvocationTree methodInvocation) {
    if (CONSTANT_MATH_METHODS.matches(methodInvocation) && allArgumentsAreLiterals(methodInvocation)) {
      reportIssue(methodInvocation.methodSelect(),
        String.format("Replace this call to \"%s\" with the precomputed constant value.", methodInvocation.methodSymbol().name()));
    }
  }

  private static boolean allArgumentsAreLiterals(MethodInvocationTree methodInvocation) {
    return methodInvocation.arguments().stream().allMatch(YodaConditionCheck::isNumericLiteral);
  }

  private static boolean isNumericLiteral(ExpressionTree tree) {
    ExpressionTree expr = ExpressionUtils.skipParentheses(tree);
    if (expr.is(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
      expr = ExpressionUtils.skipParentheses(((UnaryExpressionTree) expr).expression());
    }
    return expr.is(
      Tree.Kind.INT_LITERAL,
      Tree.Kind.LONG_LITERAL,
      Tree.Kind.FLOAT_LITERAL,
      Tree.Kind.DOUBLE_LITERAL
    );
  }

  private static boolean isLiteral(ExpressionTree tree) {
    return tree.is(
      Tree.Kind.INT_LITERAL,
      Tree.Kind.LONG_LITERAL,
      Tree.Kind.FLOAT_LITERAL,
      Tree.Kind.DOUBLE_LITERAL,
      Tree.Kind.BOOLEAN_LITERAL,
      Tree.Kind.CHAR_LITERAL,
      Tree.Kind.STRING_LITERAL,
      Tree.Kind.NULL_LITERAL
    );
  }
}
