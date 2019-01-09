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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.MethodJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

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
    Tree methodName;
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree nct = (NewClassTree) tree;
      sym = nct.constructorSymbol();
      args = nct.arguments();
      methodName = nct.identifier();
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      sym = mit.symbol();
      args = mit.arguments();
      methodName = mit.methodSelect();
    }

    if (sym.isMethodSymbol() && !args.isEmpty()) {
      ExpressionTree lastArg = args.get(args.size() - 1);
      JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) sym;
      MethodJavaType methodType = getMethodType(methodSymbol, methodName);
      checkInvokedMethod(methodSymbol, methodType, lastArg);
    }
  }

  @CheckForNull
  private static MethodJavaType getMethodType(JavaSymbol.MethodJavaSymbol methodSymbol, Tree methodName) {
    if (!methodSymbol.isParametrized()) {
      return null;
    }
    Type type = null;
    if (methodName.is(Tree.Kind.MEMBER_SELECT)) {
      type = ((MemberSelectExpressionTree) methodName).identifier().symbolType();
    } else if (methodName.is(Tree.Kind.IDENTIFIER)) {
      type = ((IdentifierTree) methodName).symbolType();
    } else if (methodName.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      type = ((ParameterizedTypeTree) methodName).symbolType();
    }
    if (type instanceof MethodJavaType) {
      return (MethodJavaType) type;
    }
    return null;
  }

  private void checkInvokedMethod(JavaSymbol.MethodJavaSymbol methodSymbol, @Nullable MethodJavaType methodType, ExpressionTree lastArg) {
    if (methodSymbol.isVarArgs() && lastArg.is(Tree.Kind.NEW_ARRAY)) {
      if (lastParamHasSameType(methodSymbol, methodType, lastArg.symbolType())) {
        String message = "Remove this array creation";
        NewArrayTree newArrayTree = (NewArrayTree) lastArg;
        if (newArrayTree.openBraceToken() == null) {
          ExpressionTree expression = newArrayTree.dimensions().get(0).expression();
          Integer literalValue = LiteralUtils.intLiteralValue(expression);
          if (literalValue == null || literalValue != 0 || isCallingOverload(methodSymbol, lastArg)) {
            return;
          }
        } else if (!newArrayTree.initializers().isEmpty()) {
          message += " and simply pass the elements";
        }
        reportIssue(lastArg, message + ".");
      } else {
        String type = ((Type.ArrayType) getLastParameterType(methodSymbol.parameterTypes())).elementType().name();
        reportIssue(lastArg, "Disambiguate this call by either casting as \"" + type + "\" or \"" + type + "[]\".");
      }
    }
  }

  private static boolean isCallingOverload(JavaSymbol.MethodJavaSymbol methodSymbol, ExpressionTree lastArg) {
    MethodTree enclosing = ExpressionUtils.getEnclosingMethod(lastArg);
    return enclosing != null && haveSameParamButLast(enclosing.symbol(), methodSymbol);
  }

  private static boolean haveSameParamButLast(Symbol.MethodSymbol enclosing, JavaSymbol.MethodJavaSymbol methodSymbol) {
    return enclosing.name().equals(methodSymbol.name())
      && IntStream.range(0, enclosing.parameterTypes().size()).allMatch(i -> enclosing.parameterTypes().get(i) == methodSymbol.parameterTypes().get(i));
  }

  private static boolean lastParamHasSameType(JavaSymbol.MethodJavaSymbol methodSymbol, @Nullable MethodJavaType methodType, Type lastArgType) {
    Type lastParamType = methodType != null ? getLastParameterType(methodType.argTypes()) : getLastParameterType(methodSymbol.parameterTypes());
    return lastArgType.equals(lastParamType);
  }

  private static Type getLastParameterType(List<? extends Type> list) {
    return list.get(list.size() - 1);
  }

}
