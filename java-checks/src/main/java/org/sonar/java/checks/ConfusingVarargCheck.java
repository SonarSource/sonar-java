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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5669")
public class ConfusingVarargCheck extends IssuableSubscriptionVisitor {

  // these methods explicitly handle vararg argument as being null
  private static final MethodMatchers ALLOWED_VARARG_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.lang.Class")
      .names("getMethod", "getDeclaredMethod")
      .addParametersMatcher("java.lang.String", "java.lang.Class[]")
      .build(),
    MethodMatchers.create()
      .ofTypes("java.lang.Class")
      .names("getConstructor", "getDeclaredConstructor")
      .addParametersMatcher("java.lang.Class[]")
      .build(),
    MethodMatchers.create()
      .ofTypes("java.lang.reflect.Method")
      .names("invoke")
      .addParametersMatcher("java.lang.Object", "java.lang.Object[]")
      .build(),
    MethodMatchers.create()
      .ofTypes("java.lang.reflect.Constructor")
      .names("newInstance")
      .addParametersMatcher("java.lang.Object[]")
      .build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    Symbol.MethodSymbol symbol;
    Arguments arguments;
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      symbol = mit.methodSymbol();
      arguments = mit.arguments();
    } else {
      NewClassTree nct = (NewClassTree) tree;
      symbol = nct.methodSymbol();
      arguments = nct.arguments();
    }
    if (!symbol.isUnknown() && !ALLOWED_VARARG_METHODS.matches(symbol)) {
      checkConfusingVararg(symbol, arguments);
    }
  }

  private void checkConfusingVararg(Symbol.MethodSymbol method, Arguments arguments) {
    if (!method.isVarArgsMethod()) {
      return;
    }
    List<Type> parameterTypes = method.parameterTypes();
    if (arguments.size() != parameterTypes.size()) {
      // providing more arguments: implicitly filling the array
      // providing less arguments: not using the vararg
      return;
    }
    Type varargParameter = parameterTypes.get(parameterTypes.size() - 1);
    ExpressionTree varargArgument = ExpressionUtils.skipParentheses(arguments.get(arguments.size() - 1));
    Type varargArgumentType = varargArgument.symbolType();
    if (varargArgument.is(Tree.Kind.NULL_LITERAL) || isIncompatibleArray(varargArgumentType, varargParameter)) {
      reportIssue(varargArgument, message(varargParameter, varargArgumentType));
    }
  }

  private static boolean isIncompatibleArray(Type varargArgument, Type varargParameter) {
    return isPrimitiveArray(varargArgument)
      && !isPrimitiveArray(varargParameter)
      && !varargArgument.equals(((Type.ArrayType) varargParameter).elementType());
  }

  private static boolean isPrimitiveArray(Type type) {
    return type.isArray() && ((Type.ArrayType) type).elementType().isPrimitive();
  }

  private static String message(Type varargParameter, Type varargArgument) {
    String message = "Cast this argument to '%s' to pass a single element to the vararg method.";
    Type parameterType = ((Type.ArrayType) varargParameter).elementType();
    if (parameterType.isPrimitive()) {
      message = "Remove this argument or pass an empty '%s' array to the vararg method.";
    } else if (isPrimitiveArray(varargArgument)) {
      Type argumentType = ((Type.ArrayType) varargArgument).elementType();
      return String.format("Use an array of '%s' instead of an array of '%s'.", argumentType.primitiveWrapperType().name(), argumentType.name());
    }
    return String.format(message, parameterType.name());
  }

}
