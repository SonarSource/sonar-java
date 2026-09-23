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
import org.sonar.plugins.java.api.JavaFileLocation;
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
    if (!arrayType.isArray()) {
      return;
    }

    Type componentType = ((Type.ArrayType) arrayType).elementType();
    ExpressionTree fillingArg = mit.arguments().get(mit.arguments().size() - 1);
    checkFillingArgument(mit, arrayArg, arrayType, componentType, fillingArg);
  }

  private boolean checkFillingArgument(MethodInvocationTree mit, ExpressionTree arrayArg, Type arrayType, Type componentType, ExpressionTree fillingArg) {
    ExpressionTree unwrappedFillingArg = ExpressionUtils.skipParentheses(fillingArg);
    if (unwrappedFillingArg.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
      if (unwrappedFillingArg.symbolType().isPrimitive()) {
        if (isMismatched(componentType, unwrappedFillingArg)) {
          reportMismatch(mit, arrayArg, arrayType, unwrappedFillingArg);
          return true;
        }
        return false;
      }

      ConditionalExpressionTree conditional = (ConditionalExpressionTree) unwrappedFillingArg;
      return checkFillingArgument(mit, arrayArg, arrayType, componentType, conditional.trueExpression())
        || checkFillingArgument(mit, arrayArg, arrayType, componentType, conditional.falseExpression());
    }

    if (isMismatched(componentType, unwrappedFillingArg)) {
      reportMismatch(mit, arrayArg, arrayType, unwrappedFillingArg);
      return true;
    }
    return false;
  }

  private static boolean isMismatched(Type componentType, ExpressionTree expr) {
    Type fillingType = expr.symbolType();
    if (fillingType.isNullType()) {
      return false;
    }

    Type effectiveFillingType = fillingType.isPrimitive() ? fillingType.primitiveWrapperType() : fillingType;
    return !isCompatible(effectiveFillingType, componentType);
  }

  private static boolean isCompatible(Type fillingType, Type componentType) {
    if (fillingType.isUnknown() || componentType.isUnknown() || fillingType.isTypeVar() || componentType.isTypeVar()) {
      return true;
    }
    if (fillingType.isSubtypeOf(componentType.erasure()) || componentType.isSubtypeOf(fillingType.erasure())) {
      return true;
    }
    if (fillingType.isArray() && componentType.isArray()) {
      return isCompatible(((Type.ArrayType) fillingType).elementType(), ((Type.ArrayType) componentType).elementType());
    }
    if (fillingType.isArray() || componentType.isArray()) {
      return false;
    }
    return canTypesOverlap(fillingType, componentType);
  }

  private static boolean canTypesOverlap(Type type1, Type type2) {
    if (type1.isPrimitive() || type2.isPrimitive()) {
      return false;
    }
    if (!type1.symbol().isInterface() && !type2.symbol().isInterface()) {
      return false;
    }
    return !type1.symbol().isFinal() && !type2.symbol().isFinal();
  }

  private void reportMismatch(MethodInvocationTree mit, ExpressionTree arrayArg, Type arrayType, ExpressionTree incompatibleArg) {
    String message = String.format("An array of type \"%s\" cannot be filled with a value of type \"%s\".",
      arrayType.name(), incompatibleArg.symbolType().name());
    List<JavaFileLocation> secondaries = Collections.singletonList(
      new JavaFileLocation(String.format("Array of type \"%s\".", arrayType.name()), arrayArg)
    );
    reportIssue(ExpressionUtils.methodName(mit), message, secondaries, null);
  }
}
