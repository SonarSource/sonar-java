/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

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
      sym = ((NewClassTree) tree).constructorSymbol();
      args = ((NewClassTree) tree).arguments();
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      sym = mit.symbol();
      args = mit.arguments();
    }
    if (sym.isMethodSymbol() && !args.isEmpty()) {
      ExpressionTree lastArg = args.get(args.size() - 1);
      checkInvokedMethod((JavaSymbol.MethodJavaSymbol) sym, lastArg);
    }
  }

  private void checkInvokedMethod(JavaSymbol.MethodJavaSymbol methodSymbol, ExpressionTree lastArg) {
    if (methodSymbol.isVarArgs() && lastArg.is(Tree.Kind.NEW_ARRAY)) {
      if (lastParamHasSameType(methodSymbol, lastArg)) {
        String message = "Remove this array creation";
        NewArrayTree newArrayTree = (NewArrayTree) lastArg;
        if (newArrayTree.openBraceToken() == null) {
          ExpressionTree expression = newArrayTree.dimensions().get(0).expression();
          Integer literalValue = LiteralUtils.intLiteralValue(expression);
          if (literalValue == null || literalValue != 0) {
            return;
          }
        } else if (!newArrayTree.initializers().isEmpty()) {
          message += " and simply pass the elements";
        }
        reportIssue(lastArg, message + ".");
      } else {
        String type = ((Type.ArrayType) getLastParameterType(methodSymbol)).elementType().name();
        reportIssue(lastArg, "Disambiguate this call by either casting as \""+type+"\" or \""+type+"[]\"");
      }
    }
  }

  private static boolean lastParamHasSameType(JavaSymbol.MethodJavaSymbol methodSymbol, ExpressionTree lastArg) {
    return lastArg.symbolType().equals(getLastParameterType(methodSymbol));
  }

  private static Type getLastParameterType(JavaSymbol.MethodJavaSymbol methodSymbol) {
    return methodSymbol.parameterTypes().get(methodSymbol.parameterTypes().size() - 1);
  }
}
