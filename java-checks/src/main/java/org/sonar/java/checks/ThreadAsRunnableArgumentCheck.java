/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

@Rule(key = "S2438")
public class ThreadAsRunnableArgumentCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.NEW_CLASS, Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    List<ExpressionTree> arguments;
    Symbol methodSymbol;
    if (tree.is(Kind.NEW_CLASS)) {
      NewClassTree nct = (NewClassTree) tree;
      methodSymbol = nct.constructorSymbol();
      arguments = nct.arguments();
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      methodSymbol = mit.symbol();
      arguments = mit.arguments();
    }
    if (!arguments.isEmpty() && methodSymbol.isMethodSymbol()) {
      checkArgumentsTypes(arguments, (MethodJavaSymbol) methodSymbol);
    }
  }

  private void checkArgumentsTypes(List<ExpressionTree> arguments, MethodJavaSymbol methodSymbol) {
    List<Type> parametersTypes = methodSymbol.parameterTypes();
    // FIXME static imports.
    // FIXME As arguments are not handled for method resolution using static imports, the provided methodSymbol may not match.
    if (!parametersTypes.isEmpty()) {
      for (int index = 0; index < arguments.size(); index++) {
        ExpressionTree argument = arguments.get(index);
        Type providedType = argument.symbolType();
        if (!argument.is(Kind.NULL_LITERAL) && isThreadAsRunnable(providedType, parametersTypes, index, methodSymbol.isVarArgs())) {
          reportIssue(argument, getMessage(argument, providedType, index));
        }
      }
    }
  }

  private static boolean isThreadAsRunnable(Type providedType, List<Type> parametersTypes, int index, boolean varargs) {
    Type expectedType = getExpectedType(providedType, parametersTypes, index, varargs);
    return (expectedType.is("java.lang.Runnable") && providedType.isSubtypeOf("java.lang.Thread"))
      || (expectedType.is("java.lang.Runnable[]") && providedType.isSubtypeOf("java.lang.Thread[]"));
  }

  private static Type getExpectedType(Type providedType, List<Type> parametersTypes, int index, boolean varargs) {
    int lastParameterIndex = parametersTypes.size() - 1;
    Type lastParameterType = parametersTypes.get(lastParameterIndex);
    Type lastExpectedType = varargs ? ((Type.ArrayType) lastParameterType).elementType() : lastParameterType;
    if (index > lastParameterIndex || (index == lastParameterIndex && varargs && !providedType.isArray())) {
      return lastExpectedType;
    }
    return parametersTypes.get(index);
  }

  private static String getMessage(ExpressionTree argument, Type providedType, int index) {
    String array = providedType.isArray() ? "[]" : "";
    return MessageFormat.format("\"{0}\" is a \"Thread{1}\".", getArgName(argument, index), array);
  }

  private static String getArgName(ExpressionTree tree, int index) {
    if (tree.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).name();
    }
    return "Argument " + (index + 1);
  }
}
