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
package org.sonar.java.checks.aws;

import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6244")
public class AwsConsumerBuilderUsageCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofType(type -> type.fullyQualifiedName().startsWith("software.amazon.awssdk."))
      .anyName()
      .withAnyParameters()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    MethodSymbol methodSymbol = mit.methodSymbol();
    Symbol parentClass = Optional.ofNullable(methodSymbol.owner()).orElse(Symbols.unknownTypeSymbol);
    Symbol.TypeSymbol returnType = methodSymbol.returnType();
    if (!returnType.isUnknown() && "Builder".equals(parentClass.name())) {
      String returnTypeName = returnType.type().fullyQualifiedName();
      // only focus on method of "Builder" class returning itself
      if (!returnTypeName.equals(parentClass.type().fullyQualifiedName())) {
        return;
      }
      ExpressionTree arg = getNonConsumerSingleArgument(mit);
      if (arg != null && hasMatchingMethodWithConsumer(parentClass, methodSymbol, returnTypeName, arg.symbolType()) &&
        (isBuilder(arg) || isVariableContainingABuilderResult(arg))) {
        reportIssue(ExpressionUtils.methodName(mit), "Consider using the Consumer Builder method instead of creating this nested builder.");
      }
    }
  }

  private static boolean hasMatchingMethodWithConsumer(Symbol parentClass, MethodSymbol methodSymbol, String returnType, Type argType) {
    String consumerTypeArgument = argType.fullyQualifiedName() + "$Builder";
    return ((Symbol.TypeSymbol) parentClass).memberSymbols().stream()
      .anyMatch(symbol -> isMatchingConsumerSingleArgumentMethod(symbol, methodSymbol.name(), returnType, consumerTypeArgument));
  }

  @CheckForNull
  private static ExpressionTree getNonConsumerSingleArgument(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    if (arguments.size() != 1) {
      return null;
    }
    ExpressionTree arg = arguments.get(0);
    Type type = arg.symbolType();
    if (type.isUnknown() || type.is("java.util.function.Consumer")) {
      return null;
    }
    return arg;
  }

  private static boolean isMatchingConsumerSingleArgumentMethod(Symbol symbol, String methodName, String methodReturnType, String consumerTypeArgument) {
    if (!symbol.isMethodSymbol()) {
      return false;
    }
    MethodSymbol methodSymbol = (MethodSymbol) symbol;
    if (!methodSymbol.name().equals(methodName) || !methodSymbol.returnType().type().fullyQualifiedName().equals(methodReturnType)) {
      return false;
    }
    List<Type> parameterTypes = methodSymbol.parameterTypes();
    if (parameterTypes.size() != 1) {
      return false;
    }
    Type parameterType = parameterTypes.get(0);
    if (!parameterType.is("java.util.function.Consumer")) {
      return false;
    }
    List<Type> typeArguments = parameterType.typeArguments();
    if (typeArguments.size() != 1) {
      return false;
    }
    Type typeArgument = typeArguments.get(0);
    return typeArgument.fullyQualifiedName().equals(consumerTypeArgument);
  }

  private static boolean isVariableContainingABuilderResult(ExpressionTree expression) {
    if (!expression.is(Tree.Kind.IDENTIFIER)) {
      return false;
    }
    Symbol variable = ((IdentifierTree) expression).symbol();
    return variable.isLocalVariable() &&
      ExpressionsHelper.initializedAndAssignedExpressionStream(variable)
        .anyMatch(AwsConsumerBuilderUsageCheck::isBuilder);
  }

  private static boolean isBuilder(ExpressionTree expression) {
    return expression.is(Tree.Kind.METHOD_INVOCATION) && "build".equals(((MethodInvocationTree) expression).methodSymbol().name());
  }

}
