/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

@Rule(key = "S3878")
public class ArrayForVarArgCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    Symbol sym;
    Arguments args;
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree nct = (NewClassTree) tree;
      sym = nct.constructorSymbol();
      args = nct.arguments();
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      sym = mit.symbol();
      args = mit.arguments();
    }

    if (sym.isMethodSymbol()) {
      Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) sym;
      if (isLastArgumentVarargs(methodSymbol, args)) {
        ExpressionTree lastArg = args.get(args.size() - 1);
        checkInvokedMethod(methodSymbol, lastArg);
      }
    }
  }

  private void checkInvokedMethod(Symbol.MethodSymbol methodSymbol, ExpressionTree lastArg) {
    if (lastArg.is(Tree.Kind.NEW_ARRAY)) {
      Type lastParamType = getLastParameterType(methodSymbol.parameterTypes());
      Type lastArgType = lastArg.symbolType();
      if (lastParamType.isUnknown() || lastArgType.isUnknown()) {
        return;
      }
      if (lastArgType.equals(lastParamType)) {
        reportIssueForSameType(methodSymbol, (NewArrayTree) lastArg);
      } else {
        String type = ((Type.ArrayType) lastParamType).elementType().name();
        reportIssue(lastArg, "Disambiguate this call by either casting as \"" + type + "\" or \"" + type + "[]\".");
      }
    }
  }

  private void reportIssueForSameType(Symbol.MethodSymbol methodSymbol, NewArrayTree newArrayTree) {
    String message = "Remove this array creation";
    if (newArrayTree.openBraceToken() == null) {
      ExpressionTree expression = newArrayTree.dimensions().get(0).expression();
      Integer literalValue = LiteralUtils.intLiteralValue(expression);
      if (literalValue == null || literalValue != 0 || isCallingOverload(methodSymbol, newArrayTree)) {
        return;
      }
    } else if (!newArrayTree.initializers().isEmpty()) {
      message += " and simply pass the elements";
    }
    reportIssue(newArrayTree, message + ".");
  }

  private static boolean isLastArgumentVarargs(Symbol.MethodSymbol methodSymbol, Arguments args) {
    // If we have less arguments than parameter types, it means that no arguments was pass to the varargs.
    // If we have more, the last argument can not be an array.
    return !args.isEmpty() && JUtils.isVarArgsMethod(methodSymbol) && args.size() == methodSymbol.parameterTypes().size();
  }

  private static Type getLastParameterType(List<? extends Type> list) {
    return list.get(list.size() - 1);
  }

  private static boolean isCallingOverload(Symbol.MethodSymbol methodSymbol, ExpressionTree lastArg) {
    MethodTree enclosing = ExpressionUtils.getEnclosingMethod(lastArg);
    return enclosing != null && haveSameParamButLast(enclosing.symbol(), methodSymbol);
  }

  private static boolean haveSameParamButLast(Symbol.MethodSymbol enclosing, Symbol.MethodSymbol methodSymbol) {
    return enclosing.name().equals(methodSymbol.name())
      && IntStream.range(0, enclosing.parameterTypes().size()).allMatch(i -> enclosing.parameterTypes().get(i) == methodSymbol.parameterTypes().get(i));
  }
}
