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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S9403")
public class ArraysFillIncompatibleTypeCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_UTIL_ARRAYS = "java.util.Arrays";
  private static final String FILL = "fill";

  private static final MethodMatchers ARRAYS_FILL_OBJECT = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_ARRAYS)
    .names(FILL)
    .addParametersMatcher("java.lang.Object[]", ANY)
    .addParametersMatcher("java.lang.Object[]", "int", "int", ANY)
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (!ARRAYS_FILL_OBJECT.matches(mit)) {
      return;
    }

    ExpressionTree arrayArg = mit.arguments().get(0);
    Type arrayType = arrayArg.symbolType();
    if (arrayType.isUnknown() || !arrayType.isArray()) {
      return;
    }

    Type componentType = ((Type.ArrayType) arrayType).elementType();
    if (componentType.isUnknown() || componentType.isTypeVar()) {
      return;
    }

    ExpressionTree fillingArg = mit.arguments().get(mit.arguments().size() - 1);
    checkFillingArgument(mit, arrayArg, arrayType, componentType, fillingArg);
  }

  private void checkFillingArgument(MethodInvocationTree mit, ExpressionTree arrayArg, Type arrayType, Type componentType, ExpressionTree fillingArg) {
    ExpressionTree unwrappedFillingArg = ExpressionUtils.skipParentheses(fillingArg);
    if (unwrappedFillingArg.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
      ConditionalExpressionTree conditional = (ConditionalExpressionTree) unwrappedFillingArg;
      ExpressionTree trueExpr = ExpressionUtils.skipParentheses(conditional.trueExpression());
      ExpressionTree falseExpr = ExpressionUtils.skipParentheses(conditional.falseExpression());

      if (isMismatched(componentType, trueExpr)) {
        reportMismatch(mit, arrayArg, arrayType, trueExpr);
        return;
      }
      if (isMismatched(componentType, falseExpr)) {
        reportMismatch(mit, arrayArg, arrayType, falseExpr);
        return;
      }
      return;
    }

    if (isMismatched(componentType, unwrappedFillingArg)) {
      reportMismatch(mit, arrayArg, arrayType, unwrappedFillingArg);
    }
  }

  private static boolean isMismatched(Type componentType, ExpressionTree expr) {
    Type fillingType = expr.symbolType();
    if (fillingType.isUnknown() || fillingType.isTypeVar() || fillingType.isNullType()) {
      return false;
    }

    Type effectiveFillingType = fillingType.isPrimitive() ? fillingType.primitiveWrapperType() : fillingType;
    if (effectiveFillingType == null || effectiveFillingType.isUnknown()) {
      return false;
    }

    return !effectiveFillingType.isSubtypeOf(componentType);
  }

  private void reportMismatch(MethodInvocationTree mit, ExpressionTree arrayArg, Type arrayType, ExpressionTree incompatibleArg) {
    String message = String.format("An array of type \"%s\" cannot be filled with a value of type \"%s\".",
      arrayType.name(), incompatibleArg.symbolType().name());
    List<JavaFileScannerContext.Location> secondaries = Collections.singletonList(
      new JavaFileScannerContext.Location(String.format("Array of type \"%s\".", arrayType.name()), arrayArg)
    );
    reportIssue(ExpressionUtils.methodName(mit), message, secondaries, null);
  }
}
