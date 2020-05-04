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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5669")
public class ConfusingVarargCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    Symbol symbol;
    Arguments arguments;
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      symbol = mit.symbol();
      arguments = mit.arguments();
    } else {
      NewClassTree nct = (NewClassTree) tree;
      symbol = nct.constructorSymbol();
      arguments = nct.arguments();
    }
    if (symbol.isMethodSymbol()) {
      checkConfusingVararg((Symbol.MethodSymbol) symbol, arguments);
    }
  }

  private void checkConfusingVararg(Symbol.MethodSymbol method, Arguments arguments) {
    if (!JUtils.isVarArgsMethod(method)) {
      return;
    }
    List<Type> parameterTypes = method.parameterTypes();
    if (arguments.size() != parameterTypes.size()) {
      // providing more arguments: implicitly filling the array
      // providing less arguments: not using the vararg
      return;
    }
    ExpressionTree varargArgument = ExpressionUtils.skipParentheses(arguments.get(arguments.size() - 1));
    Type varargParameter = parameterTypes.get(parameterTypes.size() - 1);
    if (varargArgument.is(Tree.Kind.NULL_LITERAL)
      || (isPrimitiveArray(varargArgument.symbolType()) && !isPrimitiveArray(varargParameter))) {
      reportIssue(varargArgument, message(varargParameter));
    }
  }

  private static boolean isPrimitiveArray(Type type) {
    return type.isArray() && ((Type.ArrayType) type).elementType().isPrimitive();
  }

  private static String message(Type varargParameter) {
    String message = "Cast this argument to '%s' to pass a single element to the vararg method.";
    Type elementType = ((Type.ArrayType) varargParameter).elementType();
    if (elementType.isPrimitive()) {
      message = "Remove this argument or pass an empty '%s' array to the vararg method.";
    }
    return String.format(message, elementType.name());
  }

}
